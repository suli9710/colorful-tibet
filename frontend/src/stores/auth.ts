import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { clearAllRoutePlannerDrafts } from '../composables/useRoutePlannerDraft'
import { apiBaseURL, isSameOriginApi, normalizedApiBaseURL } from '../utils/apiOrigin'

export interface AuthUser {
  nickname?: string
  avatar?: string
  avatarUrl?: string
  role?: string
  mustChangePassword?: boolean
}

type RawAuthUser = Partial<AuthUser> & Record<string, unknown> & {
  token?: string
  accessToken?: string
  jwt?: string
  refreshToken?: string
  authToken?: string
  idToken?: string
  jwtToken?: string
  userToken?: string
  bearerToken?: string
  data?: {
    token?: string
    accessToken?: string
  }
}
type AuthUserInput = Partial<AuthUser> | RawAuthUser

const USER_STORAGE_KEY = 'user'
const USER_STORAGE_VERSION = 2
const USER_STORAGE_MAX_CHARS = 2048
const USER_SNAPSHOT_TTL_MS = 7 * 24 * 60 * 60 * 1000
const AUTH_SESSION_VERSION_KEY = 'auth-session-version'
const AUTH_SESSION_EVENT = 'auth-session-changed'
const sameOriginApi = isSameOriginApi(apiBaseURL)
const LEGACY_AUTH_STORAGE_KEYS = [
  'token',
  'jwt',
  'accessToken',
  'refreshToken',
  'authToken',
  'idToken',
  'jwtToken',
  'userToken',
  'bearerToken'
]
const TOKEN_FIELD_PATTERN = /^(?:token|jwt|accessToken|refreshToken|authToken|idToken|jwtToken|userToken|bearerToken|authorization|session)$/i
const SAFE_STORED_USER_STRING_FIELDS = ['nickname', 'avatar', 'avatarUrl', 'role'] as const
const SAFE_STORED_USER_BOOLEAN_FIELDS = ['mustChangePassword'] as const

interface StoredUserSnapshot {
  version: number
  storedAt: number
  user: AuthUserInput
}

const getStorage = (kind: 'localStorage' | 'sessionStorage'): Storage | null => {
  if (typeof window === 'undefined') return null
  try {
    return window[kind] || null
  } catch {
    return null
  }
}

const getLocalStorage = () => getStorage('localStorage')
const getSessionStorage = () => getStorage('sessionStorage')
const hasStorage = () => Boolean(getLocalStorage())

const readStorage = (storage: Storage | null, key: string): string | null => {
  if (!storage) return null
  try {
    return storage.getItem(key)
  } catch {
    return null
  }
}

const writeStorage = (storage: Storage | null, key: string, value: string) => {
  if (!storage) return
  try {
    storage.setItem(key, value)
  } catch {
    // Ignore storage write failures in private browsing or locked-down contexts.
  }
}

const removeStorage = (storage: Storage | null, key: string): boolean => {
  if (!storage) return false
  const hadValue = readStorage(storage, key) !== null
  try {
    storage.removeItem(key)
  } catch {
    return false
  }
  return hadValue
}

const purgeLegacyAuthTokens = (storage: Storage | null): boolean => {
  let removed = false
  for (const key of LEGACY_AUTH_STORAGE_KEYS) {
    removed = removeStorage(storage, key) || removed
  }
  return removed
}

const purgeLegacyAuthArtifacts = () => {
  const localStorage = getLocalStorage()
  const sessionStorage = getSessionStorage()
  const purgedLocalStorage = purgeLegacyAuthTokens(localStorage)
  const purgedSessionStorage = purgeLegacyAuthTokens(sessionStorage)
  const purgedSessionUser = removeStorage(sessionStorage, USER_STORAGE_KEY)
  return purgedLocalStorage || purgedSessionStorage || purgedSessionUser
}

const notifySessionChanged = () => {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_EVENT))
}

const bumpSessionVersion = () => {
  if (!hasStorage()) {
    notifySessionChanged()
    return
  }

  const localStorage = getLocalStorage()
  const currentVersion = Number.parseInt(readStorage(localStorage, AUTH_SESSION_VERSION_KEY) || '0', 10)
  const nextVersion = Number.isFinite(currentVersion) ? currentVersion + 1 : 1
  writeStorage(localStorage, AUTH_SESSION_VERSION_KEY, String(nextVersion))
  notifySessionChanged()
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value)

const stripSensitiveAuthFields = (value: unknown, depth = 0): unknown => {
  if (!isRecord(value) || depth > 4) return value

  const sanitized: Record<string, unknown> = {}
  for (const [key, item] of Object.entries(value)) {
    if (TOKEN_FIELD_PATTERN.test(key)) continue
    const nextValue = stripSensitiveAuthFields(item, depth + 1)
    if (isRecord(nextValue) && Object.keys(nextValue).length === 0) continue
    sanitized[key] = nextValue
  }
  return sanitized
}

const toAuthRecord = (userData: AuthUserInput): Record<string, unknown> =>
  userData as Record<string, unknown>

const stripAuthTokens = (userData: AuthUserInput): RawAuthUser => {
  const sanitized = stripSensitiveAuthFields(userData)
  return isRecord(sanitized) ? sanitized as RawAuthUser : {}
}

const normalizeStoredString = (value: unknown, limit: number) => {
  if (typeof value !== 'string') return undefined
  const normalized = value.trim()
  return normalized && normalized.length <= limit ? normalized : undefined
}

const sanitizeForStorage = (userData: AuthUserInput): AuthUser => {
  const userRecord = toAuthRecord(userData)
  const safe: AuthUser = {}
  for (const key of SAFE_STORED_USER_STRING_FIELDS) {
    if (!(key in userRecord)) continue
    const normalized = normalizeStoredString(userRecord[key], key === 'avatar' || key === 'avatarUrl' ? 512 : 80)
    if (normalized) safe[key] = normalized
  }

  for (const key of SAFE_STORED_USER_BOOLEAN_FIELDS) {
    if (typeof userRecord[key] === 'boolean') {
      safe[key] = userRecord[key]
    }
  }

  return safe
}

const sanitizeSessionProfile = (userData: AuthUserInput): AuthUser => {
  return sanitizeForStorage(stripAuthTokens(userData))
}

const createStoredUserSnapshot = (userData: AuthUserInput): StoredUserSnapshot | null => {
  const user = sanitizeForStorage(userData)
  if (Object.keys(user).length === 0) return null
  return {
    version: USER_STORAGE_VERSION,
    storedAt: Date.now(),
    user
  }
}

const readSnapshotUser = (parsed: unknown): AuthUser | null => {
  if (!isRecord(parsed)) return null

  const candidate = parsed.version === USER_STORAGE_VERSION && isRecord(parsed.user)
    ? parsed as unknown as StoredUserSnapshot
    : { version: USER_STORAGE_VERSION, storedAt: Date.now(), user: parsed as RawAuthUser }

  if (candidate.version !== USER_STORAGE_VERSION) return null
  if (!Number.isFinite(candidate.storedAt) || Date.now() - candidate.storedAt > USER_SNAPSHOT_TTL_MS) return null

  const user = sanitizeForStorage(stripAuthTokens(candidate.user))
  return Object.keys(user).length > 0 ? user : null
}

const readStoredUser = (): AuthUser | null => {
  const localStorage = getLocalStorage()
  purgeLegacyAuthArtifacts()
  if (!localStorage) return null
  const storedUser = readStorage(localStorage, USER_STORAGE_KEY)
  if (!storedUser) return null
  if (storedUser.length > USER_STORAGE_MAX_CHARS) {
    removeStorage(localStorage, USER_STORAGE_KEY)
    return null
  }

  try {
    const sanitizedUser = readSnapshotUser(JSON.parse(storedUser))
    if (!sanitizedUser || Object.keys(sanitizedUser).length === 0) {
      removeStorage(localStorage, USER_STORAGE_KEY)
      return null
    }

    const nextSnapshot = createStoredUserSnapshot(sanitizedUser)
    const nextUser = nextSnapshot ? JSON.stringify(nextSnapshot) : ''
    if (!nextSnapshot || nextUser.length > USER_STORAGE_MAX_CHARS) {
      removeStorage(localStorage, USER_STORAGE_KEY)
      return null
    }
    if (nextUser !== storedUser) {
      writeStorage(localStorage, USER_STORAGE_KEY, nextUser)
    }

    return sanitizedUser
  } catch {
    clearStoredAuth()
    return null
  }
}

const persistUser = (userData: AuthUserInput) => {
  const purgedLegacyAuth = purgeLegacyAuthArtifacts()
  const localStorage = getLocalStorage()
  if (!localStorage) {
    if (purgedLegacyAuth) bumpSessionVersion()
    return
  }
  const snapshot = createStoredUserSnapshot(userData)
  if (!snapshot) {
    removeStorage(localStorage, USER_STORAGE_KEY)
    if (purgedLegacyAuth) bumpSessionVersion()
    return
  }
  const nextUser = JSON.stringify(snapshot)
  if (nextUser.length > USER_STORAGE_MAX_CHARS) {
    removeStorage(localStorage, USER_STORAGE_KEY)
    if (purgedLegacyAuth) bumpSessionVersion()
    return
  }
  if (readStorage(localStorage, USER_STORAGE_KEY) === nextUser) {
    if (purgedLegacyAuth) bumpSessionVersion()
    return
  }
  writeStorage(localStorage, USER_STORAGE_KEY, nextUser)
  bumpSessionVersion()
}

export const clearStoredAuth = () => {
  const localStorage = getLocalStorage()
  let hadStoredAuth = purgeLegacyAuthArtifacts()
  if (localStorage) {
    hadStoredAuth = removeStorage(localStorage, USER_STORAGE_KEY) || hadStoredAuth
    if (!hadStoredAuth) return
  }
  bumpSessionVersion()
}

const fetchCurrentUser = async (): Promise<RawAuthUser> => {
  const locale = readStorage(getLocalStorage(), 'locale') || 'zh'
  const response = await fetch(`${normalizedApiBaseURL()}/auth/me`, {
    credentials: sameOriginApi ? 'include' : 'omit',
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

  function applySession(userData: AuthUserInput) {
    const nextUser = sanitizeSessionProfile(userData)
    user.value = nextUser
    sessionChecked.value = true
    persistUser(nextUser)
    return true
  }

  function login(userData: RawAuthUser, _authToken?: string | null) {
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
    purgeLegacyAuthArtifacts()
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
