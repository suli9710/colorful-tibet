import { describe, expect, it } from 'vitest'
import { redactForErrorReport, safeClientErrorMessage, summarizeClientError } from './errorMonitoring'

describe('error monitoring redaction', () => {
  it('redacts query string, JSON, and header-shaped secrets', () => {
    const input = [
      'https://example.test/api?token=abc123&safe=value',
      'https://example.test/api?jwt=jwt-secret&id_token=id-secret&authToken=auth-secret',
      '{"password":"plain","authorization":"Bearer abc","apiKey":"k-123","X-XSRF-TOKEN":"xsrf-123","code":123456,"jwt":"json-jwt","refresh_token":"json-refresh"}',
      'Authorization: Bearer top-secret',
      'X-Api-Key: key-123',
      'raw Bearer loose-secret',
    ].join('\n')

    const redacted = redactForErrorReport(input)

    expect(redacted).toContain('token=<redacted>')
    expect(redacted).toContain('jwt=<redacted>')
    expect(redacted).toContain('id_token=<redacted>')
    expect(redacted).toContain('authToken=<redacted>')
    expect(redacted).toContain('"password":"<redacted>"')
    expect(redacted).toContain('"authorization":"<redacted>"')
    expect(redacted).toContain('"apiKey":"<redacted>"')
    expect(redacted).toContain('"X-XSRF-TOKEN":"<redacted>"')
    expect(redacted).toContain('"code":<redacted>')
    expect(redacted).toContain('"jwt":"<redacted>"')
    expect(redacted).toContain('"refresh_token":"<redacted>"')
    expect(redacted).toContain('Authorization: <redacted>')
    expect(redacted).toContain('X-Api-Key: <redacted>')
    expect(redacted).toContain('raw Bearer <redacted>')
    expect(redacted).not.toContain('abc123')
    expect(redacted).not.toContain('jwt-secret')
    expect(redacted).not.toContain('id-secret')
    expect(redacted).not.toContain('auth-secret')
    expect(redacted).not.toContain('json-jwt')
    expect(redacted).not.toContain('json-refresh')
    expect(redacted).not.toContain('plain')
    expect(redacted).not.toContain('Bearer abc')
    expect(redacted).not.toContain('top-secret')
    expect(redacted).not.toContain('xsrf-123')
    expect(redacted).not.toContain('loose-secret')
  })

  it('summarizes client request errors without leaking response data or request secrets', () => {
    const summary = summarizeClientError({
      message: 'Request failed with raw Bearer message-secret',
      config: {
        method: 'post',
        url: '/api/profile?token=query-secret',
        headers: {
          Authorization: 'Bearer header-secret',
          'X-XSRF-TOKEN': 'xsrf-secret'
        },
        data: {
          password: 'plain-password'
        }
      },
      response: {
        status: 401,
        data: {
          token: 'response-token',
          password: 'response-password'
        }
      }
    })

    expect(summary).toContain('status=401')
    expect(summary).toContain('method=POST')
    expect(summary).toContain('token=<redacted>')
    expect(summary).toContain('Bearer <redacted>')
    expect(summary).not.toContain('query-secret')
    expect(summary).not.toContain('message-secret')
    expect(summary).not.toContain('header-secret')
    expect(summary).not.toContain('xsrf-secret')
    expect(summary).not.toContain('plain-password')
    expect(summary).not.toContain('response-token')
    expect(summary).not.toContain('response-password')
  })

  it('keeps user-facing client errors on local fallback text', () => {
    const message = safeClientErrorMessage({
      message: 'SQL failed at /var/app/private with token=error-message-secret',
      response: {
        status: 500,
        data: {
          error: 'raw server error token=response-secret',
          message: 'stack trace /srv/app/UserService.java:42'
        }
      }
    }, '操作失败，请稍后重试')

    expect(message).toBe('操作失败，请稍后重试')
    expect(message).not.toContain('response-secret')
    expect(message).not.toContain('/srv/app')
    expect(message).not.toContain('error-message-secret')
  })

  it('redacts fallback text before displaying it', () => {
    const message = safeClientErrorMessage(null, 'Retry with token=abc123')

    expect(message).toBe('Retry with token=<redacted>')
  })
})
