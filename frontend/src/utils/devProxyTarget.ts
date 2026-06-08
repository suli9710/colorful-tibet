const DEFAULT_DEV_PROXY_TARGET = 'http://localhost:8080'

function normalizeAbsoluteOrigin(value?: string) {
  const trimmed = String(value || '').trim().replace(/\/+$/, '')
  if (!trimmed) return ''

  try {
    return new URL(trimmed).origin
  } catch {
    return ''
  }
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
