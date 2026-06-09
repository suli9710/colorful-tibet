const DEFAULT_DEV_PROXY_TARGET = 'http://localhost:8080'

function normalizeAbsoluteOrigin(value?: string) {
  const trimmed = String(value || '').trim().replace(/\/+$/, '')
  if (!trimmed) return ''

  let url: URL
  try {
    url = new URL(trimmed)
  } catch {
    return ''
  }

  assertAllowedDevProxyTarget(url)
  return url.origin
}

function assertAllowedDevProxyTarget(url: URL) {
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw new Error(`Unsupported Vite dev proxy protocol: ${url.protocol}`)
  }

  if (url.protocol === 'http:' && !isLocalHttpTarget(url.hostname)) {
    throw new Error(
      'Vite dev proxy target must use HTTPS unless it points to localhost, 127.0.0.1, or [::1].'
    )
  }
}

function isLocalHttpTarget(hostname: string) {
  const normalizedHost = hostname.trim().toLowerCase()

  return (
    normalizedHost === 'localhost'
    || normalizedHost === '127.0.0.1'
    || normalizedHost === '[::1]'
    || normalizedHost === '::1'
  )
}

export function resolveDevProxyTarget(
  configuredApiBaseURL?: string,
  explicitProxyTarget?: string,
  fallback = DEFAULT_DEV_PROXY_TARGET
) {
  return (
    normalizeAbsoluteOrigin(explicitProxyTarget)
    || normalizeAbsoluteOrigin(configuredApiBaseURL)
    || normalizeAbsoluteOrigin(fallback)
    || DEFAULT_DEV_PROXY_TARGET
  )
}
