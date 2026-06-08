import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  getBrowserStorage,
  readBrowserStorage,
  removeBrowserStorage,
  writeBrowserStorage
} from './browserStorage'

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

describe('browserStorage utilities', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('reads writes and removes browser storage values when available', () => {
    const localStorage = new MemoryStorage()
    vi.stubGlobal('window', { localStorage })

    expect(writeBrowserStorage('localStorage', 'locale', 'bo')).toBe(true)
    expect(readBrowserStorage('localStorage', 'locale', 'zh')).toBe('bo')
    expect(removeBrowserStorage('localStorage', 'locale')).toBe(true)
    expect(readBrowserStorage('localStorage', 'locale', 'zh')).toBe('zh')
  })

  it('returns safe fallbacks when the storage getter is blocked', () => {
    vi.stubGlobal('window', Object.defineProperty({}, 'localStorage', {
      get() {
        throw new Error('storage blocked')
      }
    }))

    expect(getBrowserStorage('localStorage')).toBeNull()
    expect(readBrowserStorage('localStorage', 'locale', 'zh')).toBe('zh')
    expect(writeBrowserStorage('localStorage', 'locale', 'bo')).toBe(false)
    expect(removeBrowserStorage('localStorage', 'locale')).toBe(false)
  })

  it('returns safe fallbacks when storage methods fail', () => {
    const brokenStorage = {
      getItem: () => { throw new Error('get failed') },
      setItem: () => { throw new Error('set failed') },
      removeItem: () => { throw new Error('remove failed') },
      clear: vi.fn(),
      key: vi.fn(),
      length: 0
    } as unknown as Storage
    vi.stubGlobal('window', { localStorage: brokenStorage })

    expect(readBrowserStorage('localStorage', 'locale', 'zh')).toBe('zh')
    expect(writeBrowserStorage('localStorage', 'locale', 'bo')).toBe(false)
    expect(removeBrowserStorage('localStorage', 'locale')).toBe(false)
  })
})
