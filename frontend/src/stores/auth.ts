import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { clearAllRoutePlannerDrafts } from '../composables/useRoutePlannerDraft'

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
const AUTH_SESSION_VERSION_KEY = 'auth-session-version'
const AUTH_SESSION_EVENT = 'auth-session-changed'
const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const hasStorage = () => typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'

const normalizedApiBaseURL = () => String(apiBaseURL).replace(/\/+$/, '')

const notifySessionChanged = () => {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_EVENT))
}

const bumpSessionVersion = () => {
  if (!hasStorage()) {
    notifySessionChanged()
    return
  }

  const currentVersion = Number.parseInt(localStorage.getItem(AUTH_SESSION_VERSION_KEY) || '0', 10)
  const nextVersion = Number.isFinite(currentVersion) ? currentVersion + 1 : 1
  localStorage.setItem(AUTH_SESSION_VERSION_KEY, String(nextVersion))
  notifySessionChanged()
}

const SAFE_USER_FIELDS = new Set(['id', 'username', 'nickname', 'avatar', 'createdAt'])

const stripAuthTokens = (userData: AuthUser): AuthUser => {
  const { token: _token, accessToken: _accessToken, jwt: _jwt, data: _data, ...userWithoutToken } = userData
  return userWithoutToken
}

const sanitizeForStorage = (userData: AuthUser): AuthUser => {
  const safe: AuthUser = {}
  for (const key of SAFE_USER_FIELDS) {
    if (key in userData) safe[key] = userData[key]
  }
  return safe
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
  const nextUser = JSON.stringify(sanitizeForStorage(userData))
  if (localStorage.getItem(USER_STORAGE_KEY) === nextUser) return
  localStorage.setItem(USER_STORAGE_KEY, nextUser)
  bumpSessionVersion()
}

export const clearStoredAuth = () => {
  if (hasStorage()) {
    const hadStoredAuth = Boolean(localStorage.getItem(USER_STORAGE_KEY) || localStorage.getItem('token'))
    localStorage.removeItem(USER_STORAGE_KEY)
    localStorage.removeItem('token')
    if (!hadStoredAuth) return
  }
  bumpSessionVersion()
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
  const sessionChecked = ref(false)
  const sessionLoading = ref(false)
  let sessionRefreshPromise: Promise<boolean> | null = null

  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => isLoggedIn.value && user.value?.role === 'ADMIN')

  function applySession(userData: AuthUser) {
    const nextUser = stripAuthTokens(userData)
    user.value = nextUser
    sessionChecked.value = true
    persistUser(nextUser)
    return true
  }

  function login(userData: AuthUser, _authToken?: string | null) {
    return applySession(userData)
  }

  function logout() {
    user.value = null
    sessionChecked.value = true
    clearStoredAuth()
    clearAllRoutePlannerDrafts()
  }

  async function refreshSession() {
    if (sessionRefreshPromise) {
      return sessionRefreshPromise
    }

    sessionLoading.value = true
    sessionRefreshPromise = (async () => {
      try {
        const currentUser = await fetchCurrentUser()
        return applySession(currentUser)
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
    if (isLoggedIn.value && user.value?.role) {
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
    applySession({ ...user.value, ...patch })
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
