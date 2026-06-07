import type { AxiosInstance, AxiosResponse } from 'axios'
import { apiBaseURL } from '../utils/apiOrigin'

export const DEFAULT_TIMEOUT_MS = 15000
export const LONG_TIMEOUT_MS = 180000
export const GUIDE_CHAT_TIMEOUT_MS = 60000
export const UPLOAD_TIMEOUT_MS = 60000
export const PRICE_TIMEOUT_MS = 300000

const GET_CACHE_TTL_MS = 15000
const USER_STORAGE_KEY = 'user'
const AUTH_SESSION_VERSION_KEY = 'auth-session-version'
const AUTH_SESSION_EVENT = 'auth-session-changed'
const PRIVATE_GET_PATH_MARKERS = [
  '/admin',
  '/auth/me',
  '/bookings/my',
  '/favorites',
  '/hotel-bookings/my',
  '/itineraries/my',
  '/orders/my',
  '/routes/ai',
  '/routes/my-routes'
]

const pendingGets = new Map<string, Promise<AxiosResponse>>()
const getResponseCache = new Map<string, { expiresAt: number; response: AxiosResponse }>()

const canUseLocalStorage = () =>
  typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'

function readLocalStorage(key: string): string {
  if (!canUseLocalStorage()) return ''
  try {
    return window.localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

function writeLocalStorage(key: string, value: string) {
  if (!canUseLocalStorage()) return
  try {
    window.localStorage.setItem(key, value)
  } catch {
    // Ignore storage write failures in private browsing or locked-down contexts.
  }
}

let memoizedLocale: string = readLocalStorage('locale') || 'zh'

export const getMemoizedLocale = () => memoizedLocale

export const updateMemoizedLocale = (locale: string) => {
  memoizedLocale = locale
  writeLocalStorage('locale', locale)
  clearGetCache()
}

export const clearTokenCache = () => {
  clearGetCache()
}

export function clearGetCache() {
  pendingGets.clear()
  getResponseCache.clear()
}

function getStoredSessionUserId(): string {
  const storedUser = readLocalStorage(USER_STORAGE_KEY)
  if (!storedUser) return ''

  try {
    const user = JSON.parse(storedUser) as { id?: unknown; username?: unknown }
    return String(user.id ?? user.username ?? 'authenticated')
  } catch {
    return 'authenticated'
  }
}

function getSessionCacheScope(): string {
  const version = readLocalStorage(AUTH_SESSION_VERSION_KEY) || '0'
  const userId = getStoredSessionUserId()
  return userId ? `user:${userId}:v${version}` : `anon:v${version}`
}

function isPrivateGetUrl(url: string): boolean {
  const requestUrl = String(url || '')
  return PRIVATE_GET_PATH_MARKERS.some(marker => requestUrl.includes(marker))
}

function shouldUseGetCache(url: string, config: any = {}) {
  if (config.skipGetCache) return false
  if (getStoredSessionUserId()) return false
  return !isPrivateGetUrl(url)
}

function stableStringify(value: unknown): string {
  if (value === null || typeof value !== 'object') return JSON.stringify(value)
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`
  return `{${Object.entries(value as Record<string, unknown>)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, item]) => `${JSON.stringify(key)}:${stableStringify(item)}`)
    .join(',')}}`
}

function getRequestKey(url: string, config: any = {}) {
  return stableStringify({
    baseURL: config.baseURL || apiBaseURL,
    url,
    params: config.params || {},
    locale: memoizedLocale,
    session: getSessionCacheScope()
  })
}

export function withSpecialTimeout(url: string, config: any = {}) {
  const nextConfig = { ...config }
  const requestUrl = String(url || '')
  const isUpload = nextConfig.data instanceof FormData
    || requestUrl.includes('/upload-image')
    || requestUrl.includes('/upload-avatar')
  const isAiGenerate = requestUrl.includes('/routes/generate')
  const isGuideChat = requestUrl.includes('/guide/chat')
  const isPriceFetch = requestUrl.includes('/prices/')

  if (isAiGenerate && (!nextConfig.timeout || nextConfig.timeout === DEFAULT_TIMEOUT_MS)) {
    nextConfig.timeout = LONG_TIMEOUT_MS
  } else if (isGuideChat && (!nextConfig.timeout || nextConfig.timeout === DEFAULT_TIMEOUT_MS)) {
    nextConfig.timeout = GUIDE_CHAT_TIMEOUT_MS
  } else if (isPriceFetch && (!nextConfig.timeout || nextConfig.timeout === DEFAULT_TIMEOUT_MS)) {
    nextConfig.timeout = PRICE_TIMEOUT_MS
  } else if (isUpload && (!nextConfig.timeout || nextConfig.timeout === DEFAULT_TIMEOUT_MS)) {
    nextConfig.timeout = UPLOAD_TIMEOUT_MS
  }

  return nextConfig
}

export function installGetCache(api: AxiosInstance) {
  const rawGet = api.get.bind(api)
  api.get = ((url: string, config?: any) => {
    const nextConfig = withSpecialTimeout(url, config)
    if (!shouldUseGetCache(url, nextConfig)) {
      return rawGet(url, nextConfig)
    }

    const key = getRequestKey(url, nextConfig)
    const cached = getResponseCache.get(key)

    if (cached && cached.expiresAt > Date.now()) {
      return Promise.resolve(cached.response)
    }

    const pending = pendingGets.get(key)
    if (pending) {
      return pending
    }

    const request = rawGet(url, nextConfig)
      .then(response => {
        getResponseCache.set(key, { expiresAt: Date.now() + GET_CACHE_TTL_MS, response })
        return response
      })
      .finally(() => {
        pendingGets.delete(key)
      })

    pendingGets.set(key, request)
    return request
  }) as typeof api.get
}

if (typeof window !== 'undefined') {
  window.addEventListener(AUTH_SESSION_EVENT, clearGetCache)
  window.addEventListener('storage', event => {
    if (event.key === USER_STORAGE_KEY || event.key === AUTH_SESSION_VERSION_KEY) {
      clearGetCache()
    }
  })
}
