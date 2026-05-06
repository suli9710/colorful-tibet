const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

function getStoredToken(): string {
  const token = localStorage.getItem('token')
  if (token) return token

  const userStr = localStorage.getItem('user')
  if (!userStr) return ''

  try {
    const user = JSON.parse(userStr)
    return user?.token || user?.accessToken || user?.jwt || user?.data?.token || user?.data?.accessToken || ''
  } catch {
    return ''
  }
}

export interface StreamMeta {
  model: string
  days: string
  budget: string
  preference: string
}

export interface StreamCallbacks {
  onMeta?: (meta: StreamMeta) => void
  onDelta?: (text: string) => void
  onDone?: (fullText: string) => void
  onError?: (message: string) => void
}

export async function generateRouteStream(
  requestBody: { days: number; budget: string; preference: string },
  callbacks: StreamCallbacks,
  abortSignal?: AbortSignal
): Promise<void> {
  const token = getStoredToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${apiBaseURL}/routes/generate/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify(requestBody),
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
                fullText += event.text
                callbacks.onDelta?.(event.text)
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
