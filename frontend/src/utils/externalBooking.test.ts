import { afterEach, describe, expect, it, vi } from 'vitest'
import { getExternalBookingUrl } from './externalBooking'

afterEach(() => {
  vi.unstubAllEnvs()
})

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

  it('prefers a configured direct supplier URL when present', () => {
    vi.stubEnv('VITE_EXTERNAL_BOOKING_ALLOWED_HOSTS', 'supplier.example.com')

    const url = new URL(getExternalBookingUrl({
      kind: 'route',
      name: '林芝线路',
      officialUrl: 'https://supplier.example.com/route/1'
    }))

    expect(url.href).toBe('https://supplier.example.com/route/1?utm_source=colorful_tibet')
  })

  it('falls back when a direct supplier URL is not allowlisted', () => {
    const url = new URL(getExternalBookingUrl({
      kind: 'route',
      name: '林芝线路',
      officialUrl: 'https://supplier.example.com/route/1'
    }))

    expect(url.hostname).toBe('you.ctrip.com')
    expect(url.searchParams.get('utm_source')).toBe('colorful_tibet')
    expect(decodeURIComponent(url.search)).toContain('林芝线路')
  })

  it('rejects plaintext or credentialed direct URLs and falls back to a safe provider search', () => {
    const url = new URL(getExternalBookingUrl({
      kind: 'hotel',
      name: '拉萨酒店',
      officialUrl: 'http://supplier.example.com/hotel/1',
      externalUrl: 'https://user:pass@supplier.example.com/hotel/1'
    }, 'fliggy'))

    expect(url.protocol).toBe('https:')
    expect(url.hostname).toBe('travelsearch.fliggy.com')
    expect(url.searchParams.get('utm_source')).toBe('colorful_tibet')
    expect(decodeURIComponent(url.search)).toContain('拉萨酒店')
  })

  it('continues to the next direct URL candidate when a provider-specific URL is unsafe', () => {
    vi.stubEnv('VITE_EXTERNAL_BOOKING_ALLOWED_HOSTS', 'tickets.example.com')

    const url = new URL(getExternalBookingUrl({
      kind: 'spot',
      name: '纳木错',
      providerUrls: {
        ctrip: 'http://unsafe.example.com/ticket'
      },
      officialUrl: 'https://tickets.example.com/namtso'
    }, 'ctrip'))

    expect(url.href).toBe('https://tickets.example.com/namtso?utm_source=colorful_tibet')
  })

  it('rejects provider URL host suffix lookalikes', () => {
    const url = new URL(getExternalBookingUrl({
      kind: 'spot',
      name: '布达拉宫',
      providerUrls: {
        ctrip: 'https://you.ctrip.com.evil.example/phish'
      }
    }, 'ctrip'))

    expect(url.hostname).toBe('you.ctrip.com')
    expect(url.pathname).toBe('/SearchSite/Default/Destination')
  })
})
