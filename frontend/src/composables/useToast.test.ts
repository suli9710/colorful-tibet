import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearToasts, dismissToast, normalizeToastMessage, showToast, useToast } from './useToast'

const currentToasts = () => useToast().toasts.value

describe('useToast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    clearToasts()
  })

  afterEach(() => {
    clearToasts()
    vi.useRealTimers()
  })

  it('adds normalized toast messages and supports manual dismissal', () => {
    const id = showToast('  Saved  ', 'success', 0)

    expect(id).toEqual(expect.any(Number))
    expect(currentToasts()).toEqual([{ id, message: 'Saved', type: 'success' }])

    dismissToast(id!)

    expect(currentToasts()).toEqual([])
  })

  it('ignores empty messages', () => {
    expect(showToast('   ', 'info')).toBeUndefined()
    expect(currentToasts()).toEqual([])
  })

  it('automatically dismisses toast messages after the configured duration', () => {
    showToast('Try again', 'error', 1000)

    expect(currentToasts()).toHaveLength(1)

    vi.advanceTimersByTime(999)
    expect(currentToasts()).toHaveLength(1)

    vi.advanceTimersByTime(1)
    expect(currentToasts()).toEqual([])
  })

  it('redacts secrets and suppresses stack-shaped server details in visible toast text', () => {
    const id = showToast('SQLSyntaxErrorException token=secret\n    at UserService.java:42', 'error', 0)

    expect(currentToasts()).toEqual([{ id, message: '操作失败，请稍后重试', type: 'error' }])
    expect(currentToasts()[0].message).not.toContain('secret')
    expect(currentToasts()[0].message).not.toContain('UserService')
  })

  it('keeps actionable non-internal messages after redaction', () => {
    expect(normalizeToastMessage('请重新登录后继续 token=abc123', 'warning')).toBe('请重新登录后继续 token=<redacted>')
  })
})
