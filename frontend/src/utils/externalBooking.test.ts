import { describe, expect, it } from 'vitest'
import { getExternalBookingUrl } from './externalBooking'

describe('external booking links', () => {
  it('builds an allowlisted OTA search URL with campaign attribution', () => {
    const url = new URL(getExternalBookingUrl({
      kind: 'spot',
      name: '布达拉宫',
      location: '拉萨'
    }, 'ctrip'))

    expect(url.hostname).toContain('ctrip.com')
    expect(url.searchParams.get('utm_source')).toBe('colorful_tibet')
    expect(decodeURIComponent(url.search)).toContain('布达拉宫')
  })

  it('falls back to the default provider when provider id is unknown', () => {
    const url = new URL(getExternalBookingUrl({
      kind: 'hotel',
      name: '拉萨酒店'
    }, 'unknown-provider'))

    expect(url.hostname).toContain('ctrip.com')
  })

  it('prefers a safe direct supplier URL when present', () => {
    const url = new URL(getExternalBookingUrl({
      kind: 'route',
      name: '林芝线路',
      officialUrl: 'https://supplier.example.com/route/1'
    }))

    expect(url.href).toBe('https://supplier.example.com/route/1?utm_source=colorful_tibet')
  })
})
