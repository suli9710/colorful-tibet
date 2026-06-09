import axios from 'axios'
import type { AxiosError } from 'axios'
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

const isAxiosClientError = (error: unknown): error is AxiosError => axios.isAxiosError(error)
const readSkipAuthRedirect = (error: unknown) => {
  if (isAxiosClientError(error)) {
    return Boolean(error.config?.skipAuthRedirect)
  }

  if (!error || typeof error !== 'object') return false
  const config = (error as { config?: unknown }).config
  if (!config || typeof config !== 'object') return false
  return Boolean((config as { skipAuthRedirect?: unknown }).skipAuthRedirect)
}

export function handleUnauthorizedResponse(error: unknown) {
  if (import.meta.env.DEV) {
    console.error(`[401] ${summarizeClientError(error)}`)
  }

  const skipAuthRedirect = readSkipAuthRedirect(error)

  if (!skipAuthRedirect) {
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
