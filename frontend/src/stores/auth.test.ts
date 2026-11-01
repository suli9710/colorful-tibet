import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { clearStoredAuth, useAuthStore } from './auth'

class MemoryStorage implements Storage {
  private values = new Map<string, string>()

  get length() {
    return this.values.size
  }

  clear() {
    this.values.clear()
  }

  getItem(key: string) {
    return this.values.get(key) ?? null
  }

  key(index: number) {
    return Array.from(this.values.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.values.delete(key)
  }

  setItem(key: string, value: string) {
    this.values.set(key, String(value))
  }
}

class TestCustomEvent {
  type: string
  detail: unknown

  constructor(type: string, init?: { detail?: unknown }) {
    this.type = type
    this.detail = init?.detail
  }
}

function installBrowserStorage() {
  const localStorage = new MemoryStorage()
  const sessionStorage = new MemoryStorage()
  const windowStub = {
    localStorage,
    sessionStorage,
    location: { origin: 'http://localhost' },
    addEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }

  vi.stubGlobal('window', windowStub)
  vi.stubGlobal('localStorage', localStorage)
  vi.stubGlobal('sessionStorage', sessionStorage)
  vi.stubGlobal('CustomEvent', TestCustomEvent)

  return { localStorage, sessionStorage }
}

const legacyAuthKeys = [
  'token',
  'jwt',
  'accessToken',
  'refreshToken',
  'authToken',
  'idToken',
  'jwtToken',
  'userToken',
  'bearerToken'
]

const dumpStorage = (storage: Storage) =>
  Array.from({ length: storage.length }, (_, index) => {
    const key = storage.key(index)
    return `${key}:${key ? storage.getItem(key) : ''}`
  }).join('|')

const expectStorageToOmitSensitiveAuthData = (storage: Storage, values: string[] = []) => {
  const dump = dumpStorage(storage)
  for (const marker of [
    '"id"',
    '"username"',
    '"token"',
    '"accessToken"',
    '"refreshToken"',
    '"authToken"',
    '"phone"',
    '"mobile"',
    '"orders"',
    '"invoices"',
    ...values
  ]) {
    expect(dump, marker).not.toContain(marker)
  }
}

describe('auth store sensitive storage handling', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('clears legacy auth tokens from localStorage and sessionStorage', () => {
    const { localStorage, sessionStorage } = installBrowserStorage()
    localStorage.setItem('user', JSON.stringify({ id: 1, username: 'legacy', token: 'stored-token' }))
    sessionStorage.setItem('user', JSON.stringify({ id: 2, username: 'session-legacy', token: 'session-token' }))

    for (const key of legacyAuthKeys) {
      localStorage.setItem(key, `local-${key}`)
      sessionStorage.setItem(key, `session-${key}`)
    }

    clearStoredAuth()

    expect(localStorage.getItem('user')).toBeNull()
    for (const key of legacyAuthKeys) {
      expect(localStorage.getItem(key), `localStorage ${key}`).toBeNull()
      expect(sessionStorage.getItem(key), `sessionStorage ${key}`).toBeNull()
    }
    expect(sessionStorage.getItem('user')).toBeNull()
    expect(localStorage.getItem('auth-session-version')).toBe('1')
  })

  it('keeps login identifiers and tokens out of Pinia state and persisted user storage', () => {
    const { localStorage, sessionStorage } = installBrowserStorage()
    localStorage.setItem('jwt', 'old-local-jwt')
    sessionStorage.setItem('accessToken', 'old-session-access-token')

    const auth = useAuthStore()
    auth.login({
      id: 7,
      username: 'traveler',
      nickname: 'Traveler',
      avatarUrl: 'https://example.test/avatar.png',
      role: 'ADMIN',
      mustChangePassword: true,
      phone: '13800138000',
      orders: [{ orderNo: 'ORDER-SECRET-1' }],
      invoices: [{ title: 'INVOICE-SECRET-1' }],
      token: 'login-token',
      accessToken: 'login-access-token',
      refreshToken: 'login-refresh-token',
      authToken: 'login-auth-token',
      data: {
        token: 'nested-token',
        accessToken: 'nested-access-token'
      }
    }, 'argument-session-token')

    expect(auth.user).toEqual({
      nickname: 'Traveler',
      avatarUrl: 'https://example.test/avatar.png',
      role: 'ADMIN',
      mustChangePassword: true
    })
    expect(auth.user).not.toHaveProperty('id')
    expect(auth.user).not.toHaveProperty('username')
    expect(JSON.stringify(auth.user)).not.toContain('traveler')
    expect(JSON.stringify(auth.user)).not.toContain('13800138000')
    expect(JSON.stringify(auth.user)).not.toContain('ORDER-SECRET-1')
    expect(JSON.stringify(auth.user)).not.toContain('login-token')
    expect(JSON.stringify(auth.user)).not.toContain('login-refresh-token')
    expect(JSON.stringify(auth.user)).not.toContain('nested-token')
    expect(dumpStorage(localStorage)).not.toContain('argument-session-token')
    expect(dumpStorage(sessionStorage)).not.toContain('argument-session-token')
    expect(localStorage.getItem('jwt')).toBeNull()
    expect(sessionStorage.getItem('accessToken')).toBeNull()

    const storedUser = JSON.parse(localStorage.getItem('user') || '{}')
    expect(storedUser).toEqual({
      version: 2,
      storedAt: expect.any(Number),
      user: {
        nickname: 'Traveler',
        avatarUrl: 'https://example.test/avatar.png',
        role: 'ADMIN',
        mustChangePassword: true
      }
    })
    expectStorageToOmitSensitiveAuthData(localStorage, [
      'traveler',
      '13800138000',
      'ORDER-SECRET-1',
      'INVOICE-SECRET-1',
      'login-token'
    ])
    expectStorageToOmitSensitiveAuthData(sessionStorage, [
      'traveler',
      '13800138000',
      'ORDER-SECRET-1',
      'INVOICE-SECRET-1',
      'login-token'
    ])
  })

  it('keeps sessions usable when nickname is omitted without exposing account identifiers', () => {
    const { localStorage, sessionStorage } = installBrowserStorage()

    const auth = useAuthStore()
    auth.login({
      id: 11,
      username: 'private-account-name',
      nickname: '   ',
      role: 'USER',
      token: 'privacy-token'
    })

    expect(auth.isLoggedIn).toBe(true)
    expect(auth.user).toEqual({ role: 'USER' })
    expect(auth.user).not.toHaveProperty('nickname')
    expect(auth.user).not.toHaveProperty('id')
    expect(auth.user).not.toHaveProperty('username')

    const storedUser = JSON.parse(localStorage.getItem('user') || '{}')
    expect(storedUser).toEqual({
      version: 2,
      storedAt: expect.any(Number),
      user: {
        role: 'USER'
      }
    })
    expectStorageToOmitSensitiveAuthData(localStorage, [
      'private-account-name',
      'privacy-token'
    ])
    expectStorageToOmitSensitiveAuthData(sessionStorage, [
      'private-account-name',
      'privacy-token'
    ])
  })

  it('refreshes the current session through the registered auth/me endpoint', async () => {
    const { localStorage } = installBrowserStorage()
    localStorage.setItem('locale', 'bo')
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      nickname: 'Fresh Traveler',
      role: 'USER'
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    }))
    vi.stubGlobal('fetch', fetchMock)

    const auth = useAuthStore()

    await expect(auth.refreshSession()).resolves.toBe(true)

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/me', {
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Accept-Language': 'bo'
      },
      signal: expect.any(Object)
    })
    expect(auth.user).toEqual({
      nickname: 'Fresh Traveler',
      role: 'USER'
    })
  })

  it('clears the local session only when the server explicitly returns 401', async () => {
    const { localStorage } = installBrowserStorage()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 401 })))
    const auth = useAuthStore()
    auth.login({ nickname: 'Traveler', role: 'USER' })

    await expect(auth.refreshSession()).resolves.toBe(false)

    expect(auth.user).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it.each([
    ['network failure', () => Promise.reject(new TypeError('offline'))],
    ['server failure', () => Promise.resolve(new Response('', { status: 503 }))]
  ])('preserves the current session on %s', async (_label, responseFactory) => {
    const { localStorage } = installBrowserStorage()
    vi.stubGlobal('fetch', vi.fn().mockImplementation(responseFactory))
    const auth = useAuthStore()
    auth.login({ nickname: 'Traveler', role: 'USER' })

    await expect(auth.refreshSession()).resolves.toBe(false)

    expect(auth.user).toEqual({ nickname: 'Traveler', role: 'USER' })
    expect(localStorage.getItem('user')).not.toBeNull()
  })

  it('sanitizes legacy stored user records when restoring from storage', () => {
    const { localStorage, sessionStorage } = installBrowserStorage()
    localStorage.setItem('user', JSON.stringify({
      id: 8,
      username: 'legacy-user',
      nickname: 'Legacy Traveler',
      role: 'ADMIN',
      phone: '13900139000',
      orders: [{ orderNo: 'LEGACY-ORDER-1' }],
      invoices: [{ title: 'LEGACY-INVOICE-1' }],
      token: 'stored-token',
      accessToken: 'stored-access-token',
      refreshToken: 'stored-refresh-token',
      unsafe: 'not-for-storage',
      data: {
        token: 'nested-token'
      }
    }))
    sessionStorage.setItem('jwt', 'session-jwt')
    sessionStorage.setItem('user', JSON.stringify({
      id: 9,
      username: 'session-legacy-user',
      phone: '13700137000',
      token: 'session-user-token'
    }))

    const auth = useAuthStore()

    expect(auth.restoreFromStorage()).toBe(true)
    expect(JSON.parse(localStorage.getItem('user') || '{}')).toEqual({
      version: 2,
      storedAt: expect.any(Number),
      user: {
        nickname: 'Legacy Traveler',
        role: 'ADMIN'
      }
    })
    expectStorageToOmitSensitiveAuthData(localStorage, [
      'legacy-user',
      '13900139000',
      'LEGACY-ORDER-1',
      'LEGACY-INVOICE-1',
      'stored-token',
      'unsafe'
    ])
    expect(sessionStorage.getItem('jwt')).toBeNull()
    expect(sessionStorage.getItem('user')).toBeNull()
  })

  it('clears expired or oversized stored user snapshots instead of restoring them', () => {
    const { localStorage } = installBrowserStorage()
    localStorage.setItem('user', JSON.stringify({
      version: 2,
      storedAt: Date.now() - 8 * 24 * 60 * 60 * 1000,
      user: {
        id: 9,
        username: 'stale-user'
      }
    }))

    const auth = useAuthStore()

    expect(auth.restoreFromStorage()).toBe(false)
    expect(localStorage.getItem('user')).toBeNull()

    localStorage.setItem('user', JSON.stringify({
      version: 2,
      storedAt: Date.now(),
      user: {
        id: 10,
        username: 'x'.repeat(3000)
      }
    }))

    expect(auth.restoreFromStorage()).toBe(false)
    expect(localStorage.getItem('user')).toBeNull()
  })
})
