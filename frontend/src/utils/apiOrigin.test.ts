import { describe, expect, it } from 'vitest'
import { isSameOriginApi, normalizedApiBaseURL, resolveApiBaseURL } from './apiOrigin'

describe('api origin handling', () => {
  const appOrigin = 'https://travel.example.test'

  it('keeps relative API bases on the current origin', () => {
    expect(resolveApiBaseURL('/api/', appOrigin)).toBe('/api')
    expect(resolveApiBaseURL('api', appOrigin)).toBe('/api')
    expect(isSameOriginApi('/api', appOrigin)).toBe(true)
  })

  it('keeps same-origin absolute API bases', () => {
    expect(resolveApiBaseURL('https://travel.example.test/api/', appOrigin)).toBe('https://travel.example.test/api')
    expect(isSameOriginApi('https://travel.example.test/api', appOrigin)).toBe(true)
  })

  it('blocks cross-origin absolute API bases to preserve cookie and CSRF auth', () => {
    expect(resolveApiBaseURL('https://api.example.test/api', appOrigin)).toBe('/api')
    expect(isSameOriginApi('https://api.example.test/api', appOrigin)).toBe(false)
  })

  it('normalizes resolved API bases without exposing trailing slash variants', () => {
    expect(normalizedApiBaseURL('/api///')).toBe('/api')
  })
})
