import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export interface AuthUser {
  id?: number
  username?: string
  nickname?: string
  role?: string
  token?: string
  accessToken?: string
  jwt?: string
  data?: {
    token?: string
    accessToken?: string
  }
  [key: string]: unknown
}

const USER_STORAGE_KEY = 'user'
const COOKIE_SESSION_TOKEN = 'cookie-session'
const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const hasStorage = () => typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'

const normalizedApiBaseURL = () => String(apiBaseURL).replace(/\/+$/, '')

const resolveToken = (userData: AuthUser | null, explicitToken?: string | null) =>
  explicitToken ||
  userData?.token ||
  userData?.accessToken ||
  userData?.jwt ||
  userData?.data?.token ||
  userData?.data?.accessToken ||
  COOKIE_SESSION_TOKEN

const stripAuthTokens = (userData: AuthUser): AuthUser => {
  const { token: _token, accessToken: _accessToken, jwt: _jwt, data: _data, ...userWithoutToken } = userData
  return userWithoutToken
}

const readStoredUser = (): AuthUser | null => {
  if (!hasStorage()) return null

  localStorage.removeItem('token')
  const storedUser = localStorage.getItem(USER_STORAGE_KEY)
  if (!storedUser) return null

  try {
    return stripAuthTokens(JSON.parse(storedUser) as AuthUser)
  } catch {
    clearStoredAuth()
    return null
  }
}

const persistUser = (userData: AuthUser) => {
  if (!hasStorage()) return
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(stripAuthTokens(userData)))
}

export const clearStoredAuth = () => {
  if (!hasStorage()) return
  localStorage.removeItem(USER_STORAGE_KEY)
  localStorage.removeItem('token')
}

const decodeJwtPayload = (authToken: string): { exp?: number } | null => {
  const payload = authToken.split('.')[1]
  if (!payload) return null

  try {
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, '=')
    return JSON.parse(atob(padded))
  } catch {
    return null
  }
}

const tokenHasExpired = (authToken: string | null) => {
  if (!authToken) return true
  if (authToken === COOKIE_SESSION_TOKEN) return false

  const payload = decodeJwtPayload(authToken)
  if (!payload?.exp) return false

  return payload.exp * 1000 < Date.now()
}

const fetchCurrentUser = async (): Promise<AuthUser> => {
  const locale = hasStorage() ? localStorage.getItem('locale') || 'zh' : 'zh'
  const response = await fetch(`${normalizedApiBaseURL()}/auth/me`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Accept-Language': locale
    }
  })

  if (!response.ok) {
    throw new Error(`Session refresh failed with status ${response.status}`)
  }

  return response.json()
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const token = ref<string | null>(null)
  const sessionChecked = ref(false)
  const sessionLoading = ref(false)
  let sessionRefreshPromise: Promise<boolean> | null = null

  const isLoggedIn = computed(() => !!user.value && !!token.value && !tokenHasExpired(token.value))
  const isAdmin = computed(() => isLoggedIn.value && user.value?.role === 'ADMIN')

  function applySession(userData: AuthUser, authToken = COOKIE_SESSION_TOKEN) {
    const resolvedToken = resolveToken(userData, authToken)
    if (!resolvedToken || tokenHasExpired(resolvedToken)) {
      logout()
      return false
    }

    const nextUser = stripAuthTokens(userData)
    user.value = nextUser
    token.value = resolvedToken
    sessionChecked.value = true
    persistUser(nextUser)
    return true
  }

  function login(userData: AuthUser, authToken?: string | null) {
    return applySession(userData, resolveToken(userData, authToken))
  }

  function logout() {
    user.value = null
    token.value = null
    sessionChecked.value = true
    clearStoredAuth()
  }

  async function refreshSession() {
    if (sessionRefreshPromise) {
      return sessionRefreshPromise
    }

    sessionLoading.value = true
    sessionRefreshPromise = (async () => {
      try {
        const currentUser = await fetchCurrentUser()
        return applySession(currentUser, COOKIE_SESSION_TOKEN)
      } catch {
        logout()
        return false
      } finally {
        sessionChecked.value = true
        sessionLoading.value = false
        sessionRefreshPromise = null
      }
    })()

    return sessionRefreshPromise
  }

  async function ensureSession() {
    if (isLoggedIn.value) {
      return true
    }
    return refreshSession()
  }

  function hasValidSession() {
    return isLoggedIn.value
  }

  function restoreFromStorage() {
    return !!readStoredUser()
  }

  function updateUser(patch: Partial<AuthUser>) {
    if (!user.value) return
    applySession({ ...user.value, ...patch }, token.value || COOKIE_SESSION_TOKEN)
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('auth-expired', logout)
    window.addEventListener('storage', event => {
      if (event.key === USER_STORAGE_KEY) {
        if (event.newValue) {
          void refreshSession()
        } else {
          logout()
        }
      }
    })
  }

  return {
    user,
    token,
    sessionChecked,
    sessionLoading,
    isLoggedIn,
    isAdmin,
    login,
    logout,
    refreshSession,
    ensureSession,
    restoreFromStorage,
    hasValidSession,
    updateUser
  }
})
