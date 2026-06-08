// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  clearStoredAuth: vi.fn(),
  clearTokenCache: vi.fn()
}))

vi.mock('../stores/auth', () => ({
  clearStoredAuth: mocks.clearStoredAuth
}))

vi.mock('../utils/deviceFingerprint', () => ({
  getDeviceFingerprint: vi.fn()
}))

vi.mock('../utils/apiOrigin', () => ({
  apiBaseURL: '/api',
  isSameOriginApi: () => true
}))

vi.mock('../utils/errorMonitoring', () => ({
  summarizeClientError: () => 'HTTP 401'
}))

vi.mock('./cache', () => ({
  clearTokenCache: mocks.clearTokenCache,
  DEFAULT_TIMEOUT_MS: 10_000,
  getMemoizedLocale: () => 'zh',
  installGetCache: vi.fn(),
  withSpecialTimeout: (_url: string, config: unknown) => config
}))

describe('API client unauthorized handling', () => {
  afterEach(() => {
    mocks.clearStoredAuth.mockClear()
    mocks.clearTokenCache.mockClear()
    vi.restoreAllMocks()
    window.history.pushState({}, '', '/')
  })

  it('expires the auth session for admin 401 responses by default', async () => {
    window.history.pushState({}, '', '/admin')
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent')
    const { handleUnauthorizedResponse } = await import('./client')

    handleUnauthorizedResponse({
      config: {
        method: 'get',
        url: '/admin/stats'
      }
    })

    expect(mocks.clearStoredAuth).toHaveBeenCalledOnce()
    expect(mocks.clearTokenCache).toHaveBeenCalledOnce()
    expect(dispatchEvent).toHaveBeenCalledWith(expect.objectContaining({
      type: 'auth-expired'
    }))
  })

  it('respects explicit skipAuthRedirect on selected 401 responses', async () => {
    window.history.pushState({}, '', '/admin')
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent')
    const { handleUnauthorizedResponse } = await import('./client')

    handleUnauthorizedResponse({
      config: {
        method: 'get',
        url: '/admin/stats',
        skipAuthRedirect: true
      }
    })

    expect(mocks.clearStoredAuth).not.toHaveBeenCalled()
    expect(mocks.clearTokenCache).not.toHaveBeenCalled()
    expect(dispatchEvent).not.toHaveBeenCalled()
  })
})
