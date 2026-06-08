import axios from 'axios'
import { clearStoredAuth } from '../stores/auth'
import { getDeviceFingerprint } from '../utils/deviceFingerprint'
import { apiBaseURL, isSameOriginApi } from '../utils/apiOrigin'
import { summarizeClientError } from '../utils/errorMonitoring'
import {
  clearTokenCache,
  DEFAULT_TIMEOUT_MS,
  getMemoizedLocale,
  installGetCache,
  withSpecialTimeout
} from './cache'

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthRedirect?: boolean
    skipGetCache?: boolean
  }
}

const sameOriginApi = isSameOriginApi(apiBaseURL)

const api = axios.create({
  baseURL: apiBaseURL,
  timeout: DEFAULT_TIMEOUT_MS,
  withCredentials: sameOriginApi,
  xsrfCookieName: sameOriginApi ? 'XSRF-TOKEN' : undefined,
  xsrfHeaderName: sameOriginApi ? 'X-XSRF-TOKEN' : undefined,
  headers: { 'Content-Type': 'application/json' }
})

installGetCache(api)

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
    console.error(`[401] ${summarizeClientError(error)}`)
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

api.interceptors.request.use(async config => {
  config.timeout = withSpecialTimeout(String(config.url || ''), config).timeout
  config.headers['Accept-Language'] = getMemoizedLocale()

  if (config.method?.toLowerCase() === 'get') {
    config.params = { ...(config.params || {}), locale: getMemoizedLocale() }
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
      clearTokenCache()
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

export default api
