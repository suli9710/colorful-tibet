import axios from 'axios'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: apiBaseURL,
  timeout: 180000,
  headers: { 'Content-Type': 'application/json' }
})

// Module-level memoization to avoid localStorage reads on every request
let memoizedToken: string | null = null
let tokenMemoExpiry = 0

let memoizedLocale: string = localStorage.getItem('locale') || 'zh'

export const updateMemoizedLocale = (locale: string) => {
  memoizedLocale = locale
  localStorage.setItem('locale', locale)
}

export const clearTokenCache = () => {
  memoizedToken = null
  tokenMemoExpiry = 0
}

const getToken = (): string => {
  const now = Date.now()
  if (now < tokenMemoExpiry && memoizedToken !== null) {
    return memoizedToken
  }

  const token = localStorage.getItem('token')
  if (token) {
    memoizedToken = token
    tokenMemoExpiry = now + 60_000
    return token
  }

  const userStr = localStorage.getItem('user')
  if (!userStr) {
    memoizedToken = ''
    tokenMemoExpiry = now + 60_000
    return ''
  }

  try {
    const user = JSON.parse(userStr)
    const resolved = user?.token || user?.accessToken || user?.jwt || user?.data?.token || user?.data?.accessToken || ''
    memoizedToken = resolved
    tokenMemoExpiry = now + 60_000
    return resolved
  } catch {
    memoizedToken = ''
    tokenMemoExpiry = now + 60_000
    return ''
  }
}

api.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

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
  response => response,
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
          localStorage.removeItem('user')
          localStorage.removeItem('token')
          clearTokenCache()
          window.dispatchEvent(new CustomEvent('auth-expired'))
          if (!window.location.pathname.startsWith('/login')) {
            window.location.href = '/login'
          }
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
    myRoutes: '/routes/my-routes'
  },
  spots: {
    list: '/spots',
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
    auditLogs: '/admin/audit-logs/list',
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
    cancel: (id: number) => `/bookings/${id}/cancel`
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
    cancel: (id: number) => `/hotel-bookings/${id}`
  },
  adminRoutes: {
    list: '/admin/routes',
    create: '/admin/routes',
    update: (id: number) => `/admin/routes/${id}`,
    delete: (id: number) => `/admin/routes/${id}`
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
    uploadImage: '/comments/upload-image'
  }
}

export default api
