export type BrowserStorageKind = 'localStorage' | 'sessionStorage'

export function getBrowserStorage(kind: BrowserStorageKind): Storage | null {
  if (typeof window === 'undefined') return null
  try {
    return window[kind] || null
  } catch {
    return null
  }
}

export function readBrowserStorage(kind: BrowserStorageKind, key: string, fallback = ''): string {
  const storage = getBrowserStorage(kind)
  if (!storage) return fallback
  try {
    return storage.getItem(key) ?? fallback
  } catch {
    return fallback
  }
}

export function writeBrowserStorage(kind: BrowserStorageKind, key: string, value: string): boolean {
  const storage = getBrowserStorage(kind)
  if (!storage) return false
  try {
    storage.setItem(key, value)
    return true
  } catch {
    return false
  }
}

export function removeBrowserStorage(kind: BrowserStorageKind, key: string): boolean {
  const storage = getBrowserStorage(kind)
  if (!storage) return false
  try {
    storage.removeItem(key)
    return true
  } catch {
    return false
  }
}
