export interface JsonSseMessage<T = unknown> {
  event?: string
  data: T
  rawData: string
}

export interface ReadJsonSseOptions<T = unknown> {
  onMessage: (message: JsonSseMessage<T>) => boolean | void | Promise<boolean | void>
  onMalformedMessage?: (rawData: string, error: unknown) => void
  maxBufferBytes?: number
  maxFrameBytes?: number
}

export interface ReadJsonSseResult {
  stopped: boolean
  eventCount: number
  malformedEventCount: number
}

interface SseFrame {
  event?: string
  data: string
}

export const DEFAULT_SSE_MAX_BUFFER_BYTES = 1024 * 1024
export const DEFAULT_SSE_MAX_FRAME_BYTES = 512 * 1024

const textEncoder = new TextEncoder()

export class SseStreamSizeError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'SseStreamSizeError'
  }
}

export function isAbortError(error: unknown): boolean {
  if (!error || typeof error !== 'object') return false
  const candidate = error as { name?: unknown; code?: unknown; message?: unknown }
  return candidate.name === 'AbortError'
    || candidate.code === 20
    || String(candidate.message || '').toLowerCase() === 'the operation was aborted.'
}

export async function readResponseText(response: Response): Promise<string> {
  try {
    return await response.text()
  } catch {
    return ''
  }
}

export async function readJsonSseStream<T = unknown>(
  response: Response,
  options: ReadJsonSseOptions<T>
): Promise<ReadJsonSseResult> {
  if (!response.body) {
    throw new Error('Stream response did not include a readable body.')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  const maxBufferBytes = normalizeByteLimit(options.maxBufferBytes, DEFAULT_SSE_MAX_BUFFER_BYTES)
  const maxFrameBytes = normalizeByteLimit(options.maxFrameBytes, DEFAULT_SSE_MAX_FRAME_BYTES)
  let buffer = ''
  let stopped = false
  let eventCount = 0
  let malformedEventCount = 0

  const byteLength = (value: string) => textEncoder.encode(value).byteLength
  const assertBufferSize = () => {
    if (byteLength(buffer) > maxBufferBytes) {
      throw new SseStreamSizeError('SSE stream exceeded the safe buffer size limit.')
    }
  }
  const assertFrameSize = (frameText: string) => {
    if (byteLength(frameText) > maxFrameBytes) {
      throw new SseStreamSizeError('SSE stream exceeded the safe frame size limit.')
    }
  }

  const processFrame = async (frameText: string) => {
    assertFrameSize(frameText)

    const frame = parseSseFrame(frameText)
    if (!frame) return

    const rawData = frame.data.trim()
    if (!rawData) return

    let data: T
    try {
      data = (rawData === '[DONE]' ? { type: 'done' } : JSON.parse(rawData)) as T
    } catch (error) {
      malformedEventCount += 1
      options.onMalformedMessage?.(rawData, error)
      return
    }

    eventCount += 1
    const shouldContinue = await options.onMessage({
      event: frame.event,
      data,
      rawData
    })
    if (shouldContinue === false) {
      stopped = true
    }
  }

  try {
    while (!stopped) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      assertBufferSize()

      let boundary = findFrameBoundary(buffer)
      while (boundary && !stopped) {
        const frameText = buffer.slice(0, boundary.index)
        buffer = buffer.slice(boundary.index + boundary.length)
        await processFrame(frameText)
        assertBufferSize()
        boundary = findFrameBoundary(buffer)
      }
    }

    buffer += decoder.decode()
    assertBufferSize()
    if (!stopped && buffer.trim()) {
      await processFrame(buffer)
    }
  } catch (error) {
    await reader.cancel(error).catch(() => undefined)
    throw error
  } finally {
    reader.releaseLock()
  }

  return {
    stopped,
    eventCount,
    malformedEventCount
  }
}

function normalizeByteLimit(value: number | undefined, fallback: number): number {
  return Number.isFinite(value) && Number(value) > 0 ? Math.floor(Number(value)) : fallback
}

function findFrameBoundary(buffer: string): { index: number; length: number } | null {
  const match = /(?:\r\n|\r|\n){2}/.exec(buffer)
  return match ? { index: match.index, length: match[0].length } : null
}

function parseSseFrame(frameText: string): SseFrame | null {
  let event: string | undefined
  const dataLines: string[] = []

  for (const rawLine of frameText.split(/\r\n|\r|\n/)) {
    if (!rawLine || rawLine.startsWith(':')) continue

    const separatorIndex = rawLine.indexOf(':')
    const field = separatorIndex === -1 ? rawLine : rawLine.slice(0, separatorIndex)
    let value = separatorIndex === -1 ? '' : rawLine.slice(separatorIndex + 1)
    if (value.startsWith(' ')) {
      value = value.slice(1)
    }

    if (field === 'event') {
      event = value
    } else if (field === 'data') {
      dataLines.push(value)
    }
  }

  if (dataLines.length === 0) return null
  return {
    event,
    data: dataLines.join('\n')
  }
}
