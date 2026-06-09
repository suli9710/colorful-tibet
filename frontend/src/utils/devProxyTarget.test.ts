import { describe, expect, it } from 'vitest'
import { resolveDevProxyTarget } from './devProxyTarget'

describe('dev proxy target handling', () => {
  it('keeps localhost as the default Vite backend target', () => {
    expect(resolveDevProxyTarget()).toBe('http://localhost:8080')
    expect(resolveDevProxyTarget('/api')).toBe('http://localhost:8080')
  })

  it('derives the Vite proxy target from remote API bases', () => {
    expect(resolveDevProxyTarget('https://travel.example.test/api/')).toBe('https://travel.example.test')
  })

  it('lets an explicit proxy target take priority over the browser API base', () => {
    expect(resolveDevProxyTarget('/api', 'https://travel.example.test')).toBe('https://travel.example.test')
  })

  it('allows HTTP only for loopback development targets', () => {
    expect(resolveDevProxyTarget('/api', 'http://127.0.0.1:6000')).toBe('http://127.0.0.1:6000')
    expect(resolveDevProxyTarget('/api', 'http://[::1]:6000')).toBe('http://[::1]:6000')
  })

  it('rejects plaintext HTTP targets outside localhost', () => {
    expect(() => resolveDevProxyTarget('http://1.15.29.168:6000/api')).toThrow(/must use HTTPS/)
    expect(() => resolveDevProxyTarget('/api', 'http://travel.example.test:6000')).toThrow(/must use HTTPS/)
  })
})
