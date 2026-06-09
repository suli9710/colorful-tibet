import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { apiBaseURL } from '../utils/apiOrigin'

export const DEFAULT_TIMEOUT_MS = 15000
export const LONG_TIMEOUT_MS = 180000
export const GUIDE_CHAT_TIMEOUT_MS = 60000
export const UPLOAD_TIMEOUT_MS = 60000
export const PRICE_TIMEOUT_MS = 300000

const GET_CACHE_TTL_MS = 15000
const GET_RESPONSE_CACHE_MAX_CHARS = 100_000
const USER_STORAGE_KEY = 'user'
const USER_STORAGE_VERSION = 2
const USER_STORAGE_MAX_CHARS = 2048
const AUTH_SESSION_VERSION_KEY = 'auth-session-version'
const AUTH_SESSION_EVENT = 'auth-session-changed'
const HOTEL_ORDER_STORAGE_KEY = 'hotel-orders'
const HOTEL_ORDER_STORAGE_KEY_PREFIXES = [
  `${HOTEL_ORDER_STORAGE_KEY}:`,
  'colorful-tibet:hotel-orders'
]
const PRIVATE_GET_EXACT_PATHS = [
  '/bookings',
  '/hotel-bookings',
  '/orders'
]
const PRIVATE_GET_PATH_MARKERS = [
  '/admin',
  '/auth/me',
  '/bookings/my',
  '/favorites',
  '/hotel-bookings/my',
  '/itineraries/my',
  '/orders/my',
  '/routes/ai',
  '/routes/my-routes',
  '/spots/recommendations'
]

const pendingGets = new Map<string, Promise<AxiosResponse>>()
const getResponseCache = new Map<string, { expiresAt: number; response: AxiosResponse }>()

const getBrowserStorage = (kind: 'localStorage' | 'sessionStorage'): Storage | null => {
  if (typeof window === 'undefined') return null
  try {
    return window[kind] || null
  } catch {
    return null
  }
}

function readLocalStorage(key: string): string {
  const localStorage = getBrowserStorage('localStorage')
  if (!localStorage) return ''
  try {
    return localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

function removeStorageItem(storage: Storage | null, key: string) {
  if (!storage) return
  try {
    storage.removeItem(key)
  } catch {
    // Ignore storage cleanup failures in private browsing or locked-down contexts.
  }
}

function removeLocalStorage(key: string) {
  removeStorageItem(getBrowserStorage('localStorage'), key)
}

function writeLocalStorage(key: string, value: string) {
  const localStorage = getBrowserStorage('localStorage')
  if (!localStorage) return
  try {
    localStorage.setItem(key, value)
  } catch {
    // Ignore storage write failures in private browsing or locked-down contexts.
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
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

const isHotelOrderStorageKey = (key: string | null) =>
  key === HOTEL_ORDER_STORAGE_KEY
  || Boolean(key && HOTEL_ORDER_STORAGE_KEY_PREFIXES.some(prefix => key.startsWith(prefix)))

export function clearHotelOrderClientStorage() {
  for (const kind of ['localStorage', 'sessionStorage'] as const) {
    const storage = getBrowserStorage(kind)
    if (!storage) continue
    try {
      for (let index = storage.length - 1; index >= 0; index -= 1) {
        const key = storage.key(index)
        if (key && isHotelOrderStorageKey(key)) {
          removeStorageItem(storage, key)
        }
      }
    } catch {
      removeStorageItem(storage, HOTEL_ORDER_STORAGE_KEY)
    }
  }
}

function hasStoredAuthenticatedSession(): boolean {
  const storedUser = readLocalStorage(USER_STORAGE_KEY)
  if (!storedUser) return false
  if (storedUser.length > USER_STORAGE_MAX_CHARS) {
    removeLocalStorage(USER_STORAGE_KEY)
    return false
  }

  try {
    const parsed = JSON.parse(storedUser) as unknown
    if (!isRecord(parsed)) {
      removeLocalStorage(USER_STORAGE_KEY)
      return false
    }

    if (parsed.version === USER_STORAGE_VERSION) {
      if (!isRecord(parsed.user)) {
        removeLocalStorage(USER_STORAGE_KEY)
        return false
      }
      return Object.keys(parsed.user).length > 0
    }

    return Object.keys(parsed).length > 0
  } catch {
    removeLocalStorage(USER_STORAGE_KEY)
    return false
  }
}

function getSessionCacheScope(): string {
  const version = readLocalStorage(AUTH_SESSION_VERSION_KEY) || '0'
  const sessionScope = hasStoredAuthenticatedSession() ? 'authenticated' : 'anon'
  return `${sessionScope}:v${version}`
}

function getRequestPath(url: string): string {
  try {
    const origin = typeof window !== 'undefined' && window.location?.origin
      ? window.location.origin
      : 'http://localhost'
    return new URL(String(url || ''), origin).pathname.replace(/^\/api(?=\/|$)/, '')
  } catch {
    return String(url || '').split('?')[0].replace(/^\/api(?=\/|$)/, '')
  }
}

function isPrivateGetUrl(url: string): boolean {
  const requestUrl = String(url || '')
  const requestPath = getRequestPath(requestUrl)
  const isOrderDetailPath = /^\/(?:bookings|orders)\/[^/]+/.test(requestPath)
  const isHotelOrderDetailPath = /^\/hotel-bookings\/(?!hotels(?:\/|$)|room-types(?:\/|$))[^/]+/.test(requestPath)
  return PRIVATE_GET_EXACT_PATHS.includes(requestPath)
    || isOrderDetailPath
    || isHotelOrderDetailPath
    || PRIVATE_GET_PATH_MARKERS.some(marker => requestUrl.includes(marker) || requestPath.includes(marker))
}

type CacheableGetConfig = AxiosRequestConfig & {
  skipGetCache?: boolean
}
type CacheableGet = (url: string, config?: CacheableGetConfig) => Promise<AxiosResponse>
interface GetCacheClient {
  get: CacheableGet
}

function shouldUseGetCache(url: string, config: CacheableGetConfig = {}) {
  if (config.skipGetCache) return false
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

function getRequestKey(url: string, config: CacheableGetConfig = {}) {
  return stableStringify({
    baseURL: config.baseURL || apiBaseURL,
    url,
    params: config.params || {},
    locale: memoizedLocale,
    session: getSessionCacheScope()
  })
}

function isCacheableGetResponse(response: AxiosResponse): boolean {
  try {
    return JSON.stringify(response.data).length <= GET_RESPONSE_CACHE_MAX_CHARS
  } catch {
    return false
  }
}

export function withSpecialTimeout(url: string, config: CacheableGetConfig = {}) {
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

export function installGetCache(api: GetCacheClient) {
  const rawGet = api.get.bind(api)
  api.get = ((url: string, config?: CacheableGetConfig) => {
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
        if (isCacheableGetResponse(response)) {
          getResponseCache.set(key, { expiresAt: Date.now() + GET_CACHE_TTL_MS, response })
        } else {
          getResponseCache.delete(key)
        }
        return response
      })
      .finally(() => {
        pendingGets.delete(key)
      })

    pendingGets.set(key, request)
    return request
  })
}

if (typeof window !== 'undefined') {
  clearHotelOrderClientStorage()
  window.addEventListener(AUTH_SESSION_EVENT, clearGetCache)
  window.addEventListener('storage', event => {
    if (event.key === USER_STORAGE_KEY || event.key === AUTH_SESSION_VERSION_KEY) {
      clearGetCache()
    }
  })
}
