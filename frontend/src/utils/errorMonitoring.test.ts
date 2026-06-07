import { describe, expect, it } from 'vitest'
import { redactForErrorReport } from './errorMonitoring'

describe('error monitoring redaction', () => {
  it('redacts query string, JSON, and header-shaped secrets', () => {
    const input = [
      'https://example.test/api?token=abc123&safe=value',
      '{"password":"plain","authorization":"Bearer abc","apiKey":"k-123","X-XSRF-TOKEN":"xsrf-123","code":123456}',
      'Authorization: Bearer top-secret',
      'X-Api-Key: key-123',
      'raw Bearer loose-secret',
    ].join('\n')

    const redacted = redactForErrorReport(input)

    expect(redacted).toContain('token=<redacted>')
    expect(redacted).toContain('"password":"<redacted>"')
    expect(redacted).toContain('"authorization":"<redacted>"')
    expect(redacted).toContain('"apiKey":"<redacted>"')
    expect(redacted).toContain('"X-XSRF-TOKEN":"<redacted>"')
    expect(redacted).toContain('"code":<redacted>')
    expect(redacted).toContain('Authorization: <redacted>')
    expect(redacted).toContain('X-Api-Key: <redacted>')
    expect(redacted).toContain('raw Bearer <redacted>')
    expect(redacted).not.toContain('abc123')
    expect(redacted).not.toContain('plain')
    expect(redacted).not.toContain('Bearer abc')
    expect(redacted).not.toContain('top-secret')
    expect(redacted).not.toContain('xsrf-123')
    expect(redacted).not.toContain('loose-secret')
  })
})
