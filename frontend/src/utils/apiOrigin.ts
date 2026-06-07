const DEFAULT_API_BASE_URL = '/api'
const ABSOLUTE_URL_PATTERN = /^[a-z][a-z\d+\-.]*:\/\//i

const configuredApiBaseURL = import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL

function getBrowserOrigin() {
  return typeof window === 'undefined' ? '' : window.location.origin
}

function normalizeApiBasePath(baseURL: string) {
  const trimmed = String(baseURL || DEFAULT_API_BASE_URL).trim()
  if (!trimmed) return DEFAULT_API_BASE_URL

  const withoutTrailingSlash = trimmed === '/' ? trimmed : trimmed.replace(/\/+$/, '')
  if (ABSOLUTE_URL_PATTERN.test(withoutTrailingSlash)) return withoutTrailingSlash
  if (withoutTrailingSlash.startsWith('/')) return withoutTrailingSlash
  if (withoutTrailingSlash.startsWith('./')) return `/${withoutTrailingSlash.slice(2)}`
  if (/^[\w.-]+(?:\/|$)/.test(withoutTrailingSlash)) return `/${withoutTrailingSlash}`

  return DEFAULT_API_BASE_URL
}

export function resolveApiBaseURL(baseURL = configuredApiBaseURL, currentOrigin = getBrowserOrigin()) {
  const normalizedBaseURL = normalizeApiBasePath(baseURL)
  if (!ABSOLUTE_URL_PATTERN.test(normalizedBaseURL) || !currentOrigin) {
    return normalizedBaseURL
  }

  try {
    const resolvedBaseURL = new URL(normalizedBaseURL)
    if (resolvedBaseURL.origin !== currentOrigin) {
      return DEFAULT_API_BASE_URL
    }
    return normalizedBaseURL
  } catch {
    return DEFAULT_API_BASE_URL
  }
}

const resolvedApiBaseURL = resolveApiBaseURL()

if (
  import.meta.env.DEV
  && resolvedApiBaseURL !== normalizeApiBasePath(configuredApiBaseURL)
  && typeof console !== 'undefined'
) {
  console.warn('Ignoring cross-origin VITE_API_BASE_URL because cookie and CSRF authentication require same-origin /api proxying.')
}

export const apiBaseURL = resolvedApiBaseURL

export function isSameOriginApi(baseURL = apiBaseURL, currentOrigin = getBrowserOrigin()) {
  if (!currentOrigin) return true
  try {
    return new URL(baseURL, currentOrigin).origin === currentOrigin
  } catch {
    return false
  }
}

export function normalizedApiBaseURL(baseURL = apiBaseURL) {
  return normalizeApiBasePath(resolveApiBaseURL(baseURL))
}
