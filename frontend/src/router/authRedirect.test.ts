import { describe, expect, it } from 'vitest'
import {
  createLoginRedirect,
  resolvePostLoginRedirect,
  sanitizeAuthRedirect
} from './authRedirect'

describe('auth redirect handling', () => {
  it('preserves a protected in-app target when redirecting to login', () => {
    expect(createLoginRedirect('/orders?status=pending#latest')).toEqual({
      path: '/login',
      query: {
        redirect: '/orders?status=pending#latest'
      }
    })
  })

  it('keeps same-origin and in-app post-login redirects', () => {
    expect(resolvePostLoginRedirect('/hotel-booking/42?room=standard', 'USER')).toBe('/hotel-booking/42?room=standard')
    expect(resolvePostLoginRedirect('https://app.example.test/profile#security', 'USER', 'https://app.example.test')).toBe('/profile#security')
  })

  it('ignores external or looping post-login redirects and falls back by role', () => {
    expect(resolvePostLoginRedirect('https://evil.example.test/profile', 'ADMIN', 'https://app.example.test')).toBe('/admin')
    expect(resolvePostLoginRedirect('//evil.example.test/orders', 'USER', 'https://app.example.test')).toBe('/')
    expect(resolvePostLoginRedirect('/login?redirect=/orders', 'USER', 'https://app.example.test')).toBe('/')
  })

  it('rejects unsafe redirect input before router consumption', () => {
    expect(sanitizeAuthRedirect(['https://evil.example.test/orders'], 'https://app.example.test')).toBe('')
    expect(sanitizeAuthRedirect('\u0000/orders', 'https://app.example.test')).toBe('')
    expect(sanitizeAuthRedirect('/\\evil.example.test/orders', 'https://app.example.test')).toBe('')
  })
})
