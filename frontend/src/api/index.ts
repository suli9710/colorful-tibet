import axios, { type AxiosResponse } from 'axios'
import { clearStoredAuth } from '../stores/auth'
import { getDeviceFingerprint } from '../utils/deviceFingerprint'

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthRedirect?: boolean
    skipGetCache?: boolean
  }
}

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const DEFAULT_TIMEOUT_MS = 15000
const LONG_TIMEOUT_MS = 180000
const GUIDE_CHAT_TIMEOUT_MS = 60000
const UPLOAD_TIMEOUT_MS = 60000
const PRICE_TIMEOUT_MS = 300000
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

const api = axios.create({
  baseURL: apiBaseURL,
  timeout: DEFAULT_TIMEOUT_MS,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { 'Content-Type': 'application/json' }
})

let memoizedLocale: string = localStorage.getItem('locale') || 'zh'

export const updateMemoizedLocale = (locale: string) => {
  memoizedLocale = locale
  localStorage.setItem('locale', locale)
  clearGetCache()
}

export const clearTokenCache = () => {
  clearGetCache()
}

const pendingGets = new Map<string, Promise<AxiosResponse>>()
const getResponseCache = new Map<string, { expiresAt: number; response: AxiosResponse }>()

function clearGetCache() {
  pendingGets.clear()
  getResponseCache.clear()
}

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

function withSpecialTimeout(url: string, config: any = {}) {
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

export const expireAuthSession = (redirectTo = '/login') => {
  clearStoredAuth()
  clearTokenCache()
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('auth-expired', { detail: { redirectTo } }))
  }
}

export function handleUnauthorizedResponse(error: any) {
  const method = String(error.config?.method || '').toLowerCase()
  const requestUrl = String(error.config?.url || '')

  if (import.meta.env.DEV) {
    console.error(`[401] ${method.toUpperCase()} ${requestUrl}`, error.response?.data)
  }

  if (requestUrl.includes('/admin')) {
    return
  }

  const isBookingCreate = method === 'post' && requestUrl.includes('/bookings')
  const isAiRouteGenerate = method === 'post' && requestUrl.includes('/routes/generate')
  const isRouteShare = method === 'post' && requestUrl.includes('/routes/share')
  const skipAuthRedirect = Boolean(error.config?.skipAuthRedirect)

  if (!skipAuthRedirect && !isBookingCreate && !isAiRouteGenerate && !isRouteShare) {
    const currentPath = window.location.pathname
    if (currentPath !== '/login') {
      expireAuthSession('/login')
    }
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener(AUTH_SESSION_EVENT, clearGetCache)
  window.addEventListener('storage', event => {
    if (event.key === USER_STORAGE_KEY || event.key === AUTH_SESSION_VERSION_KEY) {
      clearGetCache()
    }
  })
}

api.interceptors.request.use(async config => {
  config.timeout = withSpecialTimeout(String(config.url || ''), config).timeout
  config.headers['Accept-Language'] = memoizedLocale

  if (config.method?.toLowerCase() === 'get') {
    config.params = { ...(config.params || {}), locale: memoizedLocale }
  }

  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }

  try {
    const fp = await getDeviceFingerprint()
    if (fp) config.headers['X-Device-Fingerprint'] = fp
  } catch { /* ignore */ }

  return config
})

api.interceptors.response.use(
  response => {
    if (response.config.method?.toLowerCase() !== 'get') {
      clearGetCache()
    }
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      handleUnauthorizedResponse(error)
      return Promise.reject(error)
    }
    return Promise.reject(error)
  }
)

export interface AiRouteGenerateRequest {
  days: number
  budget: string
  preference: string
  locale?: string
}

export interface AiRouteGenerateResponse {
  content: string
  model: string
  budget: string
  preference: string
  days: number
  prompt: string
}

export interface AiRouteRecordResponse {
  id: number
  jobId?: string | null
  title: string
  content: string
  days: number
  budget: string
  preference: string
  locale?: string | null
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  manuallySaved: boolean
  errorMessage?: string | null
  createdAt: string
  updatedAt: string
}

export interface HeritageItem {
  id: number
  name: string
  nameTibetan?: string
  description?: string
  descriptionTibetan?: string
  category?: string
  imageUrl?: string
  videoUrl?: string
  originStory?: string
  significance?: string
  baikeUrl?: string
  region?: string
  protectionLevel?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  createdAt?: string
}

export interface HeritageCommentItem {
  id: number
  content: string
  imageUrl?: string
  rating?: number
  userId: number
  username: string
  nickname?: string
  avatar?: string
  createdAt: string
}

export interface HeritageInheritorItem {
  id: number
  name: string
  nameTibetan?: string
  avatarUrl?: string
  level?: string
  bio?: string
  bioTibetan?: string
  story?: string
  region?: string
  heritageItemId: number
  createdAt?: string
}

export interface HeritageEventItem {
  id: number
  title: string
  titleTibetan?: string
  description?: string
  descriptionTibetan?: string
  eventDate?: string
  endDate?: string
  location?: string
  imageUrl?: string
  contactInfo?: string
  heritageItemId?: number
  createdAt?: string
}

export type SecurityPostureStatus = 'READY' | 'DEGRADED' | 'BLOCKED'
export type SecurityFindingStatus = 'PASS' | 'WARN' | 'FAIL' | 'INFO'
export type DependencyHealthStatus = 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN' | 'DISABLED'

export interface SecurityPostureResponse {
  status: SecurityPostureStatus
  score: number
  generatedAt: string
  environment: {
    activeProfiles: string[]
    strictSecretsRequired: boolean
  }
  exposure: {
    publicDocsEnabled: boolean
    publicMetricsEnabled: boolean
    actuatorHealthPublic: boolean
    actuatorInfoAdminOnly: boolean
    corsConfigured: boolean
    trustedProxyHeadersEnabled: boolean
  }
  authentication: {
    jwtConfigured: boolean
    jwtIssuerConfigured: boolean
    jwtAudienceConfigured: boolean
    jwtExpirationMs: number
    cookieSecure: boolean
    cookieSameSite: string
    csrfConfigured: boolean
    superAdminTotpConfigured: boolean
    tokenRevocationRedisEnabled: boolean
  }
  protections: {
    rateLimitEnabled: boolean
    rateLimitRedisEnabled: boolean
    bruteForceEnabled: boolean
    bruteForceRedisEnabled: boolean
    antibotEnabled: boolean
    recaptchaConfigured: boolean
  }
  dataProtection: {
    piiKeysConfigured: boolean
    piiActiveKeyConfigured: boolean
    legacyPiiKeyConfigured: boolean
    piiMigrationEnabled: boolean
  }
  dependencies: {
    database: DependencyHealthStatus
    redis: DependencyHealthStatus
    scrapling: DependencyHealthStatus
    aiProviderConfigured: boolean
    paymentCallbackSecretConfigured: boolean
  }
  findings: Array<{
    id: string
    severity: 'HIGH' | 'MEDIUM' | 'LOW'
    status: SecurityFindingStatus
    message: string
  }>
}

export const endpoints = {
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    me: '/auth/me',
    meStats: '/auth/me/stats'
  },
  guide: {
    chat: '/guide/chat'
  },
  routes: {
    generate: '/routes/generate',
    share: '/routes/share',
    shared: '/routes/shared',
    sharedDetail: (id: number) => `/routes/shared/${id}`,
    sharedLike: (id: number) => `/routes/shared/${id}/like`,
    sharedLikeStatus: (id: number) => `/routes/shared/${id}/like-status`,
    sharedComments: (id: number) => `/routes/shared/${id}/comments`,
    deleteSharedComment: (routeId: number, commentId: number) => `/routes/shared/${routeId}/comments/${commentId}`,
    myRoutes: '/routes/my-routes',
    aiLatest: '/routes/ai/latest',
    aiSaved: '/routes/ai/saved',
    saveAiRoute: (id: number) => `/routes/ai/${id}/save`
  },
  itineraries: {
    generate: '/itineraries/generate',
    my: '/itineraries/my',
    detail: (id: number) => `/itineraries/${id}`,
    quote: (id: number) => `/itineraries/${id}/quote`,
    createVersion: (id: number) => `/itineraries/${id}/versions`,
    bookItem: (id: number, itemId: number) => `/itineraries/${id}/items/${itemId}/bookings`
  },
  orders: {
    create: '/orders',
    my: '/orders/my',
    detail: (id: number) => `/orders/${id}`,
    cancel: (id: number) => `/orders/${id}/cancel`,
    delete: (id: number) => `/orders/${id}`,
    refunds: (id: number) => `/orders/${id}/refunds`,
    invoice: (id: number) => `/orders/${id}/invoice`
  },
  payments: {
    mockCallback: '/payments/callbacks/mock'
  },
  tibetSpecialty: {
    travelKit: (itineraryId: number) => `/tibet-specialty/itineraries/${itineraryId}/travel-kit`,
    highlandAssessment: '/tibet-specialty/highland-assessment',
    cultureTips: '/tibet-specialty/culture-tips',
    phrasebook: '/tibet-specialty/phrasebook',
    sustainableOptions: '/tibet-specialty/sustainable-options'
  },
  spots: {
    list: '/spots',
    heatmap: '/spots/heatmap',
    detail: (id: number) => `/spots/${id}`,
    search: '/spots/search',
    recommendations: '/spots/recommendations',
    recommendationsDebug: '/spots/recommendations/debug'
  },
  news: {
    list: '/news'
  },
  heritage: {
    list: '/heritage',
    detail: (id: number) => `/heritage/${id}`,
    like: (id: number) => `/heritage/${id}/like`,
    likeStatus: (id: number) => `/heritage/${id}/like-status`,
    comments: (id: number) => `/heritage/${id}/comments`,
    deleteComment: (heritageId: number, commentId: number) => `/heritage/${heritageId}/comments/${commentId}`,
    inheritors: (id: number) => `/heritage/${id}/inheritors`,
    events: (id: number) => `/heritage/${id}/events`,
    upcomingEvents: '/heritage/events/upcoming'
  },
  adminHeritage: {
    list: '/admin/heritage',
    create: '/admin/heritage',
    update: (id: number) => `/admin/heritage/${id}`,
    delete: (id: number) => `/admin/heritage/${id}`,
    inheritors: (itemId: number) => `/admin/heritage/${itemId}/inheritors`,
    updateInheritor: (id: number) => `/admin/heritage/inheritors/${id}`,
    deleteInheritor: (id: number) => `/admin/heritage/inheritors/${id}`,
    events: (itemId: number) => `/admin/heritage/${itemId}/events`,
    updateEvent: (id: number) => `/admin/heritage/events/${id}`,
    deleteEvent: (id: number) => `/admin/heritage/events/${id}`
  },
  admin: {
    stats: '/admin/stats',
    securityPosture: '/admin/security-posture',
    users: '/admin/users',
    updateRole: (id: number) => `/admin/users/${id}/role`,
    deleteUser: (id: number) => `/admin/users/${id}`,
    unlockUser: (id: number) => `/admin/users/${id}/unlock`,
    spots: '/admin/spots',
    updateSpot: (id: number) => `/admin/spots/${id}`,
    news: '/admin/news',
    createNews: '/admin/news',
    updateNews: (id: number) => `/admin/news/${id}`,
    deleteNews: (id: number) => `/admin/news/${id}`,
    uploadImage: '/admin/upload-image'
  },
  carousels: {
    list: '/carousels',
    adminList: '/admin/carousels',
    adminCreate: '/admin/carousels',
    adminUpdate: (id: number) => `/admin/carousels/${id}`,
    adminDelete: (id: number) => `/admin/carousels/${id}`
  },
  prices: {
    fetch: (spotId: number) => `/prices/fetch/${spotId}`,
    update: (spotId: number) => `/prices/update/${spotId}`,
    batchUpdate: '/prices/batch-update',
    batchUpdateJob: '/prices/batch-update/jobs',
    batchUpdateJobStatus: (jobId: string) => `/prices/batch-update/jobs/${jobId}`
  },
  favorites: {
    list: '/favorites',
    add: (routeId: number) => `/favorites/${routeId}`,
    remove: (routeId: number) => `/favorites/${routeId}`,
    status: (routeId: number) => `/favorites/${routeId}/status`
  },
  bookings: {
    create: '/bookings',
    my: '/bookings/my',
    cancel: (id: number) => `/bookings/${id}/cancel`,
    delete: (id: number) => `/bookings/${id}`
  },
  hotels: {
    list: '/hotel-bookings/hotels',
    detail: (id: number) => `/hotel-bookings/hotels/${id}`,
    roomTypes: (hotelId: number) => `/hotel-bookings/room-types/${hotelId}`
  },
  hotelBookings: {
    create: '/hotel-bookings',
    my: '/hotel-bookings/my',
    all: '/hotel-bookings',
    roomTypes: (hotelId: number) => `/hotel-bookings/room-types/${hotelId}`,
    updateStatus: (id: number) => `/hotel-bookings/${id}/status`,
    cancel: (id: number) => `/hotel-bookings/${id}`,
    delete: (id: number) => `/hotel-bookings/${id}/permanent`
  },
  adminRoutes: {
    list: '/admin/routes',
    create: '/admin/routes',
    update: (id: number) => `/admin/routes/${id}`,
    delete: (id: number) => `/admin/routes/${id}`
  },
  adminCommunity: {
    routes: '/admin/community/routes',
    updateRoute: (id: number) => `/admin/community/routes/${id}`,
    deleteRoute: (id: number) => `/admin/community/routes/${id}`,
    questions: '/admin/community/questions',
    updateQuestion: (id: number) => `/admin/community/questions/${id}`,
    deleteQuestion: (id: number) => `/admin/community/questions/${id}`,
    comments: '/admin/community/comments',
    updateComment: (id: number) => `/admin/community/comments/${id}`,
    deleteComment: (id: number) => `/admin/community/comments/${id}`,
    spotComments: '/admin/community/spot-comments',
    updateSpotComment: (id: number) => `/admin/community/spot-comments/${id}`,
    deleteSpotComment: (id: number) => `/admin/community/spot-comments/${id}`,
    answers: '/admin/community/answers',
    updateAnswer: (id: number) => `/admin/community/answers/${id}`,
    deleteAnswer: (id: number) => `/admin/community/answers/${id}`
  },
  adminHotels: {
    list: '/admin/hotels',
    create: '/admin/hotels',
    update: (id: number) => `/admin/hotels/${id}`,
    delete: (id: number) => `/admin/hotels/${id}`,
    roomTypes: (hotelId: number) => `/admin/hotels/${hotelId}/room-types`,
    updateRoomType: (id: number) => `/admin/room-types/${id}`,
    deleteRoomType: (id: number) => `/admin/room-types/${id}`
  },
  comments: {
    list: (spotId: number) => `/comments/spot/${spotId}`,
    create: '/comments',
    delete: (id: number) => `/comments/${id}`,
    uploadImage: '/comments/upload-image'
  }
}

export default api
