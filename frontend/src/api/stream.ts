const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

function readCookie(name: string): string {
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
  onDone?: (fullText: string) => void
  onError?: (message: string) => void
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
    credentials: 'include',
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
