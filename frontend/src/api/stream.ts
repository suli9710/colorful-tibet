import { expireAuthSession } from './index'
import { endpoints } from './endpoints'
import { isAbortError, readJsonSseStream, type JsonSseMessage } from './sse'
import { apiBaseURL, isSameOriginApi } from '../utils/apiOrigin'
import { readBrowserStorage } from '../utils/browserStorage'
import { safeClientErrorMessage } from '../utils/errorMonitoring'

const sameOriginApi = isSameOriginApi(apiBaseURL)

function readCookie(name: string): string {
  if (!sameOriginApi || typeof document === 'undefined') return ''
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find(value => value.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : ''
}

function getCurrentLocale(): string {
  return readBrowserStorage('localStorage', 'locale', 'zh') === 'bo' ? 'bo' : 'zh'
}

function apiUrl(endpoint: string): string {
  return `${apiBaseURL}${endpoint}`
}

export interface StreamMeta {
  model: string
  days: string
  budget: string
  preference: string
}

export interface StreamCallbacks {
  onMeta?: (meta: StreamMeta) => void
  onDelta?: (text: string, fullText: string) => void
  onReplace?: (text: string) => void
  onDone?: (fullText: string) => void
  onError?: (message: string) => void
}

export interface RouteGenerationJobSnapshot {
  jobId: string
  routeRecordId?: number | null
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  content: string
  errorMessage?: string
  days: number
  budget: string
  preference: string
  cached: boolean
  createdAt: string
  updatedAt: string
}

export interface RouteGenerationJobCallbacks {
  onSnapshot?: (snapshot: RouteGenerationJobSnapshot) => void
  onDelta?: (text: string, fullText: string) => void
  onReplace?: (content: string) => void
  onDone?: (content: string) => void
  onError?: (message: string) => void
}

type StreamEventObject = Record<string, unknown>

const routeGenerationRateLimitMessage = 'AI route generation is temporarily rate limited. Please wait a moment before starting a new route.'
const routeJobRateLimitMessage = 'AI route job status check is temporarily rate limited. Please wait a moment and retry.'
const routeStreamRateLimitMessage = 'AI route stream recovery is temporarily rate limited. Please wait a moment and retry.'
const routeGenerationFailedMessage = 'AI route generation failed. Please try again.'
const routeStreamFailedMessage = 'Stream connection failed. Please try again.'
const safeLocalErrorMessages = new Set([
  routeGenerationRateLimitMessage,
  routeJobRateLimitMessage,
  routeStreamRateLimitMessage
])

function asObject(value: unknown): StreamEventObject | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? value as StreamEventObject
    : null
}

function firstString(...values: unknown[]): string {
  for (const value of values) {
    if (typeof value === 'string') return value
  }
  return ''
}

function getPayloadObject(event: StreamEventObject): StreamEventObject | null {
  return asObject(event.payload)
}

function getMessageType(message: JsonSseMessage<unknown>, event: StreamEventObject): string {
  const explicitType = firstString(event.type)
  if (explicitType) return explicitType
  return message.event && message.event !== 'message' ? message.event : ''
}

function getStreamErrorMessage(error: unknown, fallback = routeStreamFailedMessage): string {
  if (error instanceof Error && safeLocalErrorMessages.has(error.message)) {
    return error.message
  }
  return safeClientErrorMessage(error, fallback)
}

function isRouteGenerationJobSnapshot(value: unknown): value is RouteGenerationJobSnapshot {
  const snapshot = asObject(value)
  return Boolean(
    snapshot
      && typeof snapshot.jobId === 'string'
      && (snapshot.status === 'RUNNING' || snapshot.status === 'COMPLETED' || snapshot.status === 'FAILED')
  )
}

function normalizeMeta(value: unknown): StreamMeta | null {
  const meta = asObject(value)
  if (!meta) return null

  const normalized = {
    model: firstString(meta.model),
    days: firstString(meta.days),
    budget: firstString(meta.budget),
    preference: firstString(meta.preference)
  }

  return Object.values(normalized).some(Boolean) ? normalized : null
}

async function throwIfErrorResponse(response: Response, rateLimitMessage: string): Promise<void> {
  if (response.ok) return

  if (response.status === 401) {
    expireAuthSession('/login')
  }
  if (response.status === 429) {
    throw new Error(rateLimitMessage)
  }
  throw new Error(`HTTP ${response.status}`)
}

export async function startRouteGenerationJob(
  requestBody: { days: number; budget: string; preference: string; locale?: string }
): Promise<RouteGenerationJobSnapshot> {
  const locale = requestBody.locale || getCurrentLocale()
  const csrfToken = readCookie('XSRF-TOKEN')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Accept-Language': locale,
  }
  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken
  }

  const response = await fetch(apiUrl(endpoints.routes.generateJob), {
    method: 'POST',
    credentials: sameOriginApi ? 'include' : 'omit',
    headers,
    body: JSON.stringify({ ...requestBody, locale }),
  })

  await throwIfErrorResponse(response, routeGenerationRateLimitMessage)

  return response.json()
}

export async function getRouteGenerationJob(jobId: string): Promise<RouteGenerationJobSnapshot> {
  const response = await fetch(apiUrl(endpoints.routes.generateJobDetail(jobId)), {
    method: 'GET',
    credentials: sameOriginApi ? 'include' : 'omit',
    headers: {
      'Accept': 'application/json',
      'Accept-Language': getCurrentLocale(),
    },
  })

  await throwIfErrorResponse(response, routeJobRateLimitMessage)

  return response.json()
}

export async function streamRouteGenerationJob(
  jobId: string,
  callbacks: RouteGenerationJobCallbacks,
  abortSignal?: AbortSignal
): Promise<void> {
  let response: Response
  try {
    response = await fetch(apiUrl(endpoints.routes.generateJobStream(jobId)), {
      method: 'GET',
      credentials: sameOriginApi ? 'include' : 'omit',
      headers: {
        'Accept': 'text/event-stream',
        'Accept-Language': getCurrentLocale(),
      },
      signal: abortSignal,
    })
  } catch (error) {
    if (isAbortError(error)) return
    throw error
  }

  await throwIfErrorResponse(response, routeStreamRateLimitMessage)

  let fullText = ''

  try {
    await readJsonSseStream(response, {
      onMessage: message => {
        const event = asObject(message.data)
        if (!event) return

        const payload = getPayloadObject(event)
        switch (getMessageType(message, event)) {
          case 'snapshot':
            {
              const snapshotSource = payload || event
              if (!isRouteGenerationJobSnapshot(snapshotSource)) return
              if (snapshotSource.content) {
                fullText = snapshotSource.content
              }
              callbacks.onSnapshot?.(snapshotSource)
            }
            break
          case 'delta':
            {
              const text = firstString(event.text, event.delta, event.content, payload?.text, payload?.delta, payload?.content)
              if (text) {
                if (fullText && text.startsWith(fullText)) {
                  fullText = text
                } else {
                  fullText += text
                }
                callbacks.onDelta?.(text, fullText)
              }
            }
            break
          case 'replace':
            {
              fullText = firstString(event.content, event.text, payload?.content, payload?.text)
              callbacks.onReplace?.(fullText)
            }
            break
          case 'done':
            callbacks.onDone?.(firstString(event.content, payload?.content, event.text, payload?.text) || fullText)
            return false
          case 'error':
            callbacks.onError?.(routeGenerationFailedMessage)
            return false
        }
      }
    })
  } catch (error) {
    if (isAbortError(error)) return
    callbacks.onError?.(getStreamErrorMessage(error))
  }
}

export async function generateRouteStream(
  requestBody: { days: number; budget: string; preference: string; locale?: string },
  callbacks: StreamCallbacks,
  abortSignal?: AbortSignal
): Promise<void> {
  const locale = requestBody.locale || getCurrentLocale()
  const csrfToken = readCookie('XSRF-TOKEN')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    'Accept-Language': locale,
  }
  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken
  }

  let response: Response
  try {
    response = await fetch(apiUrl(endpoints.routes.generateStream), {
      method: 'POST',
      credentials: sameOriginApi ? 'include' : 'omit',
      headers,
      body: JSON.stringify({ ...requestBody, locale }),
      signal: abortSignal,
    })
  } catch (error) {
    if (isAbortError(error)) return
    throw error
  }

  await throwIfErrorResponse(response, routeGenerationRateLimitMessage)

  let fullText = ''

  try {
    const result = await readJsonSseStream(response, {
      onMessage: message => {
        const event = asObject(message.data)
        if (!event) return

        const payload = getPayloadObject(event)
        switch (getMessageType(message, event)) {
          case 'meta':
            {
              const meta = normalizeMeta(payload || event)
              if (meta) callbacks.onMeta?.(meta)
            }
            break
          case 'delta':
            {
              const text = firstString(event.text, payload?.text, event.delta, payload?.delta)
              if (text) {
                if (fullText && text.startsWith(fullText)) {
                  fullText = text
                } else {
                  fullText += text
                }
                callbacks.onDelta?.(text, fullText)
              }
            }
            break
          case 'replace':
            fullText = firstString(event.text, event.content, payload?.text, payload?.content)
            callbacks.onReplace?.(fullText)
            break
          case 'done':
            callbacks.onDone?.(firstString(event.content, payload?.content, event.text, payload?.text) || fullText)
            return false
          case 'error':
            callbacks.onError?.(routeGenerationFailedMessage)
            return false
        }
      }
    })

    if (!result.stopped) {
      callbacks.onDone?.(fullText)
    }
  } catch (error) {
    if (isAbortError(error)) return
    callbacks.onError?.(getStreamErrorMessage(error))
  }
}
