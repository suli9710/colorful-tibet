import type { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'

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

function installBrowserStorage() {
  const localStorage = new MemoryStorage()
  const sessionStorage = new MemoryStorage()
  vi.stubGlobal('window', {
    localStorage,
    sessionStorage,
    location: {
      origin: 'http://localhost'
    },
    addEventListener: vi.fn()
  })
  vi.stubGlobal('localStorage', localStorage)
  vi.stubGlobal('sessionStorage', sessionStorage)
  return { localStorage, sessionStorage }
}

type TestGet = (url: string, config?: AxiosRequestConfig) => Promise<AxiosResponse>
type TestGetMock = ReturnType<typeof vi.fn> & TestGet

function createRawGet() {
  return vi.fn() as TestGetMock
}

function createApi(rawGet: TestGet) {
  return {
    get: rawGet
  }
}

const response = (data: unknown): AxiosResponse => ({
  data,
  status: 200,
  statusText: 'OK',
  headers: {},
  config: { headers: {} } as InternalAxiosRequestConfig
})

describe('GET cache storage guardrails', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.resetModules()
  })

  it('clears malformed user snapshots and still caches anonymous public GETs', async () => {
    const { localStorage } = installBrowserStorage()
    localStorage.setItem('user', '{bad-json')
    const { installGetCache } = await import('./cache')
    const rawGet = createRawGet()
    rawGet.mockResolvedValue(response({ ok: true }))
    const api = createApi(rawGet)

    installGetCache(api)

    await api.get('/scenic-spots')
    await api.get('/scenic-spots')

    expect(localStorage.getItem('user')).toBeNull()
    expect(rawGet).toHaveBeenCalledTimes(1)
  })

  it('scopes public GETs by session state and version without user id or username', async () => {
    const { localStorage } = installBrowserStorage()
    localStorage.setItem('auth-session-version', '5')
    const { installGetCache } = await import('./cache')
    const rawGet = createRawGet()
    rawGet
      .mockResolvedValueOnce(response({ seq: 1 }))
      .mockResolvedValueOnce(response({ seq: 2 }))
      .mockResolvedValueOnce(response({ seq: 3 }))
    const api = createApi(rawGet)

    installGetCache(api)

    await expect(api.get('/scenic-spots')).resolves.toMatchObject({ data: { seq: 1 } })
    localStorage.setItem('user', JSON.stringify({
      version: 2,
      storedAt: Date.now(),
      user: {
        id: 42,
        username: 'traveler'
      }
    }))
    await expect(api.get('/scenic-spots')).resolves.toMatchObject({ data: { seq: 2 } })
    localStorage.setItem('user', JSON.stringify({
      version: 2,
      storedAt: Date.now(),
      user: {
        id: 99,
        username: 'another-traveler'
      }
    }))
    await expect(api.get('/scenic-spots')).resolves.toMatchObject({ data: { seq: 2 } })
    localStorage.setItem('auth-session-version', '6')
    await expect(api.get('/scenic-spots')).resolves.toMatchObject({ data: { seq: 3 } })

    expect(rawGet).toHaveBeenCalledTimes(3)
  })

  it('does not keep oversized responses in the in-memory GET cache', async () => {
    installBrowserStorage()
    const { installGetCache } = await import('./cache')
    const rawGet = createRawGet()
    rawGet
      .mockResolvedValueOnce(response({ text: 'x'.repeat(100_001) }))
      .mockResolvedValueOnce(response({ text: 'small' }))
    const api = createApi(rawGet)

    installGetCache(api)

    await api.get('/scenic-spots')
    await expect(api.get('/scenic-spots')).resolves.toMatchObject({ data: { text: 'small' } })

    expect(rawGet).toHaveBeenCalledTimes(2)
  })

  it('clears legacy hotel order details from local and session storage only', async () => {
    const { localStorage, sessionStorage } = installBrowserStorage()
    localStorage.setItem('hotel-orders', JSON.stringify({ guestName: 'Ada', phone: '19532458802' }))
    localStorage.setItem('hotel-orders:42', JSON.stringify({ phone: '19532458802' }))
    localStorage.setItem('colorful-tibet:hotel-orders:42', JSON.stringify({ note: 'late arrival' }))
    localStorage.setItem('locale', 'zh')
    sessionStorage.setItem('hotel-orders', JSON.stringify({ guestName: 'Lin', phone: '19532458803' }))
    sessionStorage.setItem('hotel-orders:42', JSON.stringify({ phone: '19532458803' }))
    sessionStorage.setItem('colorful-tibet:hotel-orders:42', JSON.stringify({ note: 'window room' }))
    sessionStorage.setItem('route-filter', 'family')

    const { clearHotelOrderClientStorage } = await import('./cache')
    clearHotelOrderClientStorage()

    expect(localStorage.getItem('hotel-orders')).toBeNull()
    expect(localStorage.getItem('hotel-orders:42')).toBeNull()
    expect(localStorage.getItem('colorful-tibet:hotel-orders:42')).toBeNull()
    expect(localStorage.getItem('locale')).toBe('zh')
    expect(sessionStorage.getItem('hotel-orders')).toBeNull()
    expect(sessionStorage.getItem('hotel-orders:42')).toBeNull()
    expect(sessionStorage.getItem('colorful-tibet:hotel-orders:42')).toBeNull()
    expect(sessionStorage.getItem('route-filter')).toBe('family')
  })

  it('does not cache order detail list endpoints for anonymous sessions', async () => {
    installBrowserStorage()
    const { installGetCache } = await import('./cache')
    const rawGet = createRawGet()
    rawGet
      .mockResolvedValueOnce(response({ seq: 1 }))
      .mockResolvedValueOnce(response({ seq: 2 }))
      .mockResolvedValueOnce(response({ seq: 3 }))
      .mockResolvedValueOnce(response({ seq: 4 }))
      .mockResolvedValueOnce(response({ seq: 5 }))
      .mockResolvedValueOnce(response({ seq: 6 }))
      .mockResolvedValueOnce(response({ seq: 7 }))
      .mockResolvedValueOnce(response({ seq: 8 }))
    const api = createApi(rawGet)

    installGetCache(api)

    await expect(api.get('/hotel-bookings')).resolves.toMatchObject({ data: { seq: 1 } })
    await expect(api.get('/hotel-bookings')).resolves.toMatchObject({ data: { seq: 2 } })
    await expect(api.get('/orders')).resolves.toMatchObject({ data: { seq: 3 } })
    await expect(api.get('/orders')).resolves.toMatchObject({ data: { seq: 4 } })
    await expect(api.get('/hotel-bookings/42')).resolves.toMatchObject({ data: { seq: 5 } })
    await expect(api.get('/hotel-bookings/42')).resolves.toMatchObject({ data: { seq: 6 } })
    await expect(api.get('/orders/42')).resolves.toMatchObject({ data: { seq: 7 } })
    await expect(api.get('/orders/42')).resolves.toMatchObject({ data: { seq: 8 } })

    expect(rawGet).toHaveBeenCalledTimes(8)
  })

  it('keeps anonymous public hotel catalog GETs eligible for short memory cache', async () => {
    installBrowserStorage()
    const { installGetCache } = await import('./cache')
    const rawGet = createRawGet()
    rawGet.mockResolvedValue(response({ hotels: [] }))
    const api = createApi(rawGet)

    installGetCache(api)

    await api.get('/hotel-bookings/hotels')
    await api.get('/hotel-bookings/hotels')

    expect(rawGet).toHaveBeenCalledTimes(1)
  })

  it('does not cache personalized spot recommendation endpoints', async () => {
    installBrowserStorage()
    const { installGetCache } = await import('./cache')
    const rawGet = createRawGet()
    rawGet
      .mockResolvedValueOnce(response({ seq: 1 }))
      .mockResolvedValueOnce(response({ seq: 2 }))
      .mockResolvedValueOnce(response({ seq: 3 }))
      .mockResolvedValueOnce(response({ seq: 4 }))
    const api = createApi(rawGet)

    installGetCache(api)

    await expect(api.get('/spots/recommendations/me')).resolves.toMatchObject({ data: { seq: 1 } })
    await expect(api.get('/spots/recommendations/me')).resolves.toMatchObject({ data: { seq: 2 } })
    await expect(api.get('/spots/recommendations', { params: { userId: 42 } })).resolves.toMatchObject({ data: { seq: 3 } })
    await expect(api.get('/spots/recommendations', { params: { userId: 42 } })).resolves.toMatchObject({ data: { seq: 4 } })

    expect(rawGet).toHaveBeenCalledTimes(4)
  })
})
