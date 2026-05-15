import axios, { type AxiosResponse } from 'axios'
import { clearStoredAuth } from '../stores/auth'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const DEFAULT_TIMEOUT_MS = 15000
const LONG_TIMEOUT_MS = 180000
const UPLOAD_TIMEOUT_MS = 60000
const GET_CACHE_TTL_MS = 15000

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
    locale: memoizedLocale
  })
}

function withSpecialTimeout(url: string, config: any = {}) {
  const nextConfig = { ...config }
  const requestUrl = String(url || '')
  const isUpload = nextConfig.data instanceof FormData
    || requestUrl.includes('/upload-image')
    || requestUrl.includes('/upload-avatar')
  const isAiGenerate = requestUrl.includes('/routes/generate')

  if (isAiGenerate && (!nextConfig.timeout || nextConfig.timeout === DEFAULT_TIMEOUT_MS)) {
    nextConfig.timeout = LONG_TIMEOUT_MS
  } else if (isUpload && (!nextConfig.timeout || nextConfig.timeout === DEFAULT_TIMEOUT_MS)) {
    nextConfig.timeout = UPLOAD_TIMEOUT_MS
  }

  return nextConfig
}

const rawGet = api.get.bind(api)
api.get = ((url: string, config?: any) => {
  const nextConfig = withSpecialTimeout(url, config)
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

api.interceptors.request.use(config => {
  config.timeout = withSpecialTimeout(String(config.url || ''), config).timeout
  config.headers['Accept-Language'] = memoizedLocale

  if (config.method?.toLowerCase() === 'get') {
    config.params = { ...(config.params || {}), locale: memoizedLocale }
  }

  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
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
      const method = String(error.config?.method || '').toLowerCase()
      const requestUrl = String(error.config?.url || '')
      console.error(`[401] ${method.toUpperCase()} ${requestUrl}`, error.response.data)

      // 对于 /admin 路径的请求，不自动跳转，由各页面自己处理
      const isAdminRequest = requestUrl.includes('/admin')
      if (isAdminRequest) {
        return Promise.reject(error)
      }

      const isBookingCreate = method === 'post' && requestUrl.includes('/bookings')
      const isAiRouteGenerate = method === 'post' && requestUrl.includes('/routes/generate')
      const isRouteShare = method === 'post' && requestUrl.includes('/routes/share')

      if (!isBookingCreate && !isAiRouteGenerate && !isRouteShare) {
        const currentPath = window.location.pathname
        if (currentPath !== '/login') {
          clearStoredAuth()
          clearTokenCache()
          window.dispatchEvent(new CustomEvent('auth-expired', { detail: { redirectTo: '/login' } }))
        }
      }
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

export const endpoints = {
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    me: '/auth/me',
    meStats: '/auth/me/stats'
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
    myRoutes: '/routes/my-routes'
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
    detail: (id: number) => `/heritage/${id}`
  },
  admin: {
    stats: '/admin/stats',
    users: '/admin/users',
    updateRole: (id: number) => `/admin/users/${id}/role`,
    deleteUser: (id: number) => `/admin/users/${id}`,
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
