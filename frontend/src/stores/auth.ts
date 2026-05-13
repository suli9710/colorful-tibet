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
const TOKEN_STORAGE_KEY = 'token'

const hasStorage = () => typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'

const resolveToken = (userData: AuthUser | null, explicitToken?: string | null) =>
  explicitToken ||
  userData?.token ||
  userData?.accessToken ||
  userData?.jwt ||
  userData?.data?.token ||
  userData?.data?.accessToken ||
  null

const readStoredUser = (): AuthUser | null => {
  if (!hasStorage()) return null

  const storedUser = localStorage.getItem(USER_STORAGE_KEY)
  if (!storedUser) return null

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    clearStoredAuth()
    return null
  }
}

const readStoredToken = () => {
  if (!hasStorage()) return null
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export const clearStoredAuth = () => {
  if (!hasStorage()) return
  localStorage.removeItem(USER_STORAGE_KEY)
  localStorage.removeItem(TOKEN_STORAGE_KEY)
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

  const payload = decodeJwtPayload(authToken)
  if (!payload?.exp) return false

  return payload.exp * 1000 < Date.now()
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const token = ref<string | null>(null)

  const isLoggedIn = computed(() => !!token.value && !tokenHasExpired(token.value))
  const isAdmin = computed(() => isLoggedIn.value && user.value?.role === 'ADMIN')

  function restoreFromStorage() {
    const storedUser = readStoredUser()
    const storedToken = readStoredToken()
    const resolvedToken = resolveToken(storedUser, storedToken)

    if (!storedUser || !resolvedToken || tokenHasExpired(resolvedToken)) {
      logout()
      return false
    }

    user.value = { ...storedUser, token: resolvedToken }
    token.value = resolvedToken
    return true
  }

  function login(userData: AuthUser, authToken?: string | null) {
    const resolvedToken = resolveToken(userData, authToken)
    if (!resolvedToken) {
      logout()
      return false
    }

    const nextUser = { ...userData, token: resolvedToken }
    user.value = nextUser
    token.value = resolvedToken

    if (hasStorage()) {
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nextUser))
      localStorage.setItem(TOKEN_STORAGE_KEY, resolvedToken)
    }

    return true
  }

  function logout() {
    user.value = null
    token.value = null
    clearStoredAuth()
  }

  function hasValidSession() {
    if (!token.value || tokenHasExpired(token.value)) {
      return restoreFromStorage()
    }
    return true
  }

  function syncFromStorage() {
    restoreFromStorage()
  }

  restoreFromStorage()

  if (typeof window !== 'undefined') {
    window.addEventListener('auth-expired', logout)
    window.addEventListener('storage', event => {
      if (event.key === USER_STORAGE_KEY || event.key === TOKEN_STORAGE_KEY) {
        syncFromStorage()
      }
    })
  }

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    login,
    logout,
    restoreFromStorage,
    hasValidSession
  }
})
