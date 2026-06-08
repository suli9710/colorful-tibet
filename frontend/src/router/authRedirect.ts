export const LOGIN_PATH = '/login'

const FALLBACK_ORIGIN = 'http://localhost'
const CONTROL_CHARACTER_PATTERN = /[\u0000-\u001F\u007F]/

function firstRedirectValue(value: unknown): string {
  if (Array.isArray(value)) {
    return firstRedirectValue(value[0])
  }

  return typeof value === 'string' ? value.trim() : ''
}

function getCurrentOrigin(): string {
  if (typeof window === 'undefined') return FALLBACK_ORIGIN
  return window.location?.origin || FALLBACK_ORIGIN
}

function normalizeOrigin(origin: string): string {
  try {
    return new URL(origin).origin
  } catch {
    return FALLBACK_ORIGIN
  }
}

function isLoginRedirect(path: string): boolean {
  return path === LOGIN_PATH
    || path.startsWith(`${LOGIN_PATH}?`)
    || path.startsWith(`${LOGIN_PATH}#`)
}

export function getDefaultPostLoginPath(role: unknown): string {
  return role === 'ADMIN' ? '/admin' : '/'
}

export function sanitizeAuthRedirect(value: unknown, origin = getCurrentOrigin()): string {
  const rawRedirect = firstRedirectValue(value)
  if (!rawRedirect || CONTROL_CHARACTER_PATTERN.test(rawRedirect) || rawRedirect.includes('\\')) {
    return ''
  }

  const currentOrigin = normalizeOrigin(origin)
  try {
    const redirectUrl = new URL(rawRedirect, `${currentOrigin}/`)
    if (redirectUrl.origin !== currentOrigin) return ''

    const redirectPath = `${redirectUrl.pathname}${redirectUrl.search}${redirectUrl.hash}`
    if (!redirectPath.startsWith('/') || redirectPath.startsWith('//') || isLoginRedirect(redirectPath)) return ''

    return redirectPath
  } catch {
    return ''
  }
}

export function resolvePostLoginRedirect(redirect: unknown, role: unknown, origin = getCurrentOrigin()): string {
  return sanitizeAuthRedirect(redirect, origin) || getDefaultPostLoginPath(role)
}

export function createLoginRedirect(fullPath: string) {
  const redirect = sanitizeAuthRedirect(fullPath)
  return redirect
    ? { path: LOGIN_PATH, query: { redirect } }
    : LOGIN_PATH
}
