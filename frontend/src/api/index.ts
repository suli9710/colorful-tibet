import axios from 'axios'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: apiBaseURL,
  timeout: 180000,
  headers: { 'Content-Type': 'application/json' }
})

const getStoredToken = () => {
  const token = localStorage.getItem('token')
  if (token) return token

  const userStr = localStorage.getItem('user')
  if (!userStr) return ''

  try {
    const user = JSON.parse(userStr)
    return user?.token || user?.accessToken || user?.jwt || user?.data?.token || user?.data?.accessToken || ''
  } catch {
    return ''
  }
}

api.interceptors.request.use(config => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
    localStorage.setItem('token', token)
  }

  if (config.method?.toLowerCase() === 'get') {
    config.params = { ...(config.params || {}), locale: localStorage.getItem('locale') || 'zh' }
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
      const isBookingCreate = method === 'post' && requestUrl.includes('/bookings')
      const isAiRouteGenerate = method === 'post' && requestUrl.includes('/routes/generate')
      const isRouteShare = method === 'post' && requestUrl.includes('/routes/share')

      if (!isBookingCreate && !isAiRouteGenerate && !isRouteShare) {
        const currentPath = window.location.pathname
        if (currentPath !== '/login') {
          localStorage.removeItem('user')
          localStorage.removeItem('token')
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
    list: '/heritage'
  },
  admin: {
    stats: '/admin/stats',
    users: '/admin/users',
    updateRole: (id: number) => `/admin/users/${id}/role`,
    deleteUser: (id: number) => `/admin/users/${id}`,
    decryptPassword: (id: number) => `/admin/users/${id}/decrypt-password`,
    auditLogs: '/admin/audit-logs/list',
    spots: '/admin/spots',
    updateSpot: (id: number) => `/admin/spots/${id}`,
    news: '/admin/news',
    createNews: '/admin/news',
    updateNews: (id: number) => `/admin/news/${id}`,
    deleteNews: (id: number) => `/admin/news/${id}`
  },
  bookings: {
    create: '/bookings',
    my: '/bookings/my',
    cancel: (id: number) => `/bookings/${id}/cancel`
  },
  hotelBookings: {
    create: '/hotel-bookings',
    my: '/hotel-bookings/my',
    all: '/hotel-bookings',
    updateStatus: (id: number) => `/hotel-bookings/${id}/status`,
    cancel: (id: number) => `/hotel-bookings/${id}`
  },
  comments: {
    list: (spotId: number) => `/comments/spot/${spotId}`,
    create: '/comments',
    uploadImage: '/comments/upload-image'
  }
}

export default api
