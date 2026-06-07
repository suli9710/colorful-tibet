import { expireAuthSession } from './index'
import { apiBaseURL, isSameOriginApi } from '../utils/apiOrigin'

const sameOriginApi = isSameOriginApi(apiBaseURL)

function readCookie(name: string): string {
  if (!sameOriginApi) return ''
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find(value => value.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : ''
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

export async function startRouteGenerationJob(
  requestBody: { days: number; budget: string; preference: string; locale?: string }
): Promise<RouteGenerationJobSnapshot> {
  const locale = requestBody.locale || localStorage.getItem('locale') || 'zh'
  const csrfToken = readCookie('XSRF-TOKEN')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Accept-Language': locale,
  }
  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken
  }

  const response = await fetch(`${apiBaseURL}/routes/generate/jobs`, {
    method: 'POST',
    credentials: sameOriginApi ? 'include' : 'omit',
    headers,
    body: JSON.stringify({ ...requestBody, locale }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    if (response.status === 401) {
      expireAuthSession('/login')
    }
    if (response.status === 429) {
      throw new Error(errorText || 'AI route generation is temporarily rate limited. Please wait a moment before starting a new route.')
    }
    throw new Error(errorText || `HTTP ${response.status}`)
  }

  return response.json()
}

export async function getRouteGenerationJob(jobId: string): Promise<RouteGenerationJobSnapshot> {
  const response = await fetch(`${apiBaseURL}/routes/generate/jobs/${encodeURIComponent(jobId)}`, {
    method: 'GET',
    credentials: sameOriginApi ? 'include' : 'omit',
    headers: {
      'Accept': 'application/json',
      'Accept-Language': localStorage.getItem('locale') || 'zh',
    },
  })

  if (!response.ok) {
    const errorText = await response.text()
    if (response.status === 401) {
      expireAuthSession('/login')
    }
    if (response.status === 429) {
      throw new Error(errorText || 'AI route job status check is temporarily rate limited. Please wait a moment and retry.')
    }
    throw new Error(errorText || `HTTP ${response.status}`)
  }

  return response.json()
}

export async function streamRouteGenerationJob(
  jobId: string,
  callbacks: RouteGenerationJobCallbacks,
  abortSignal?: AbortSignal
): Promise<void> {
  const response = await fetch(`${apiBaseURL}/routes/generate/jobs/${encodeURIComponent(jobId)}/stream`, {
    method: 'GET',
    credentials: sameOriginApi ? 'include' : 'omit',
    headers: {
      'Accept': 'text/event-stream',
      'Accept-Language': localStorage.getItem('locale') || 'zh',
    },
    signal: abortSignal,
  })

  if (!response.ok) {
    const errorText = await response.text()
    if (response.status === 401) {
      expireAuthSession('/login')
    }
    if (response.status === 429) {
      throw new Error(errorText || 'AI route stream recovery is temporarily rate limited. Please wait a moment and retry.')
    }
    throw new Error(errorText || `HTTP ${response.status}`)
  }

  const reader = response.body!.getReader()
  const decoder = new TextDecoder()
  let fullText = ''
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data:')) continue

        const jsonStr = trimmed.slice(5).trim()
        if (!jsonStr) continue

        try {
          const event = JSON.parse(jsonStr)
          switch (event.type) {
            case 'snapshot':
              {
                const snapshot = (event.payload || event) as RouteGenerationJobSnapshot
                if (snapshot.content) {
                  fullText = snapshot.content
                }
                callbacks.onSnapshot?.(snapshot)
              }
              break
            case 'delta':
              {
                const text = event.text || event.delta || event.content || event.payload?.text || ''
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
                fullText = event.content || event.text || event.payload?.content || event.payload?.text || ''
                callbacks.onReplace?.(fullText)
              }
              break
            case 'done':
              callbacks.onDone?.(event.content || event.payload?.content || fullText)
              return
            case 'error':
              callbacks.onError?.(event.message || event.payload?.message || 'AI route generation failed')
              return
          }
        } catch {
          // Skip malformed SSE lines.
        }
      }
    }
  } catch (err: any) {
    if (err.name === 'AbortError') return
    callbacks.onError?.(err.message || 'Stream connection failed')
  } finally {
    reader.releaseLock()
  }
}

export async function generateRouteStream(
  requestBody: { days: number; budget: string; preference: string; locale?: string },
  callbacks: StreamCallbacks,
  abortSignal?: AbortSignal
): Promise<void> {
  const locale = requestBody.locale || localStorage.getItem('locale') || 'zh'
  const csrfToken = readCookie('XSRF-TOKEN')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    'Accept-Language': locale,
  }
  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken
  }

  const response = await fetch(`${apiBaseURL}/routes/generate/stream`, {
    method: 'POST',
    credentials: sameOriginApi ? 'include' : 'omit',
    headers,
    body: JSON.stringify({ ...requestBody, locale }),
    signal: abortSignal,
  })

  if (!response.ok) {
    let errorText = ''
    try {
      errorText = await response.text()
    } catch {
      errorText = `HTTP ${response.status}`
    }
    if (response.status === 401) {
      expireAuthSession('/login')
    }
    throw new Error(errorText || `HTTP ${response.status}`)
  }

  const reader = response.body!.getReader()
  const decoder = new TextDecoder()
  let fullText = ''
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data:')) continue

        const jsonStr = trimmed.slice(5).trim()
        if (!jsonStr) continue

        try {
          const event = JSON.parse(jsonStr)
          switch (event.type) {
            case 'meta':
              callbacks.onMeta?.(event as StreamMeta)
              break
            case 'delta':
              if (event.text) {
                // Handle both incremental and cumulative deltas from different API formats.
                // If the incoming text starts with what we already have, it's cumulative —
                // replace instead of append to avoid duplication.
                if (fullText && event.text.startsWith(fullText)) {
                  fullText = event.text
                } else {
                  fullText += event.text
                }
                callbacks.onDelta?.(event.text, fullText)
              }
              break
            case 'replace':
              fullText = event.text || ''
              callbacks.onReplace?.(fullText)
              break
            case 'done':
              callbacks.onDone?.(fullText)
              return
            case 'error':
              callbacks.onError?.(event.message || 'Unknown stream error')
              return
          }
        } catch {
          // Skip unparseable JSON lines
        }
      }
    }
    callbacks.onDone?.(fullText)
  } catch (err: any) {
    if (err.name === 'AbortError') return
    callbacks.onError?.(err.message || 'Stream connection failed')
  } finally {
    reader.releaseLock()
  }
}
