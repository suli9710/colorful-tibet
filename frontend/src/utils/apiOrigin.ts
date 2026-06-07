export const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export function isSameOriginApi(baseURL = apiBaseURL) {
  if (typeof window === 'undefined') return true
  try {
    return new URL(baseURL, window.location.origin).origin === window.location.origin
  } catch {
    return false
  }
}

export function normalizedApiBaseURL(baseURL = apiBaseURL) {
  return String(baseURL).replace(/\/+$/, '')
}
