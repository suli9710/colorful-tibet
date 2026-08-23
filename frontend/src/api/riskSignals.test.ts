import { describe, expect, it } from 'vitest'
import { shouldAttachDeviceFingerprint } from './riskSignals'

describe('risk signal request scoping', () => {
  it.each([
    '/orders',
    '/bookings',
    '/hotel-bookings',
    '/itineraries/12/items/34/bookings',
    'https://travel.example/api/orders?source=checkout',
  ])('attaches a fingerprint only to risk-assessed mutations: %s', url => {
    expect(shouldAttachDeviceFingerprint({ method: 'post', url })).toBe(true)
  })

  it.each([
    { method: 'get', url: '/orders' },
    { method: 'post', url: '/orders/12/cancel' },
    { method: 'post', url: '/routes/generate' },
    { method: 'post', url: '/orders-extra' },
    { method: undefined, url: '/orders' },
    { method: 'post', url: undefined },
  ])('does not attach a fingerprint to unrelated requests: %o', request => {
    expect(shouldAttachDeviceFingerprint(request)).toBe(false)
  })
})
