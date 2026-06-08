import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  post: vi.fn(),
  getRecaptchaToken: vi.fn(),
  isRecaptchaV3Enabled: vi.fn()
}))

vi.mock('../api', () => ({
  default: { post: mocks.post },
  endpoints: { guide: { chat: '/api/ai/guide/chat' } }
}))

vi.mock('../utils/recaptcha', () => ({
  getRecaptchaToken: mocks.getRecaptchaToken,
  isRecaptchaV3Enabled: mocks.isRecaptchaV3Enabled
}))

vi.mock('../utils/errorMonitoring', () => ({
  summarizeClientError: vi.fn(() => 'client side verification failed')
}))

import {
  MAX_GUIDE_CHAT_MESSAGE_CHARS,
  coerceRetryAfter,
  createNetworkFallbackGuideMessage,
  createUserMessage,
  normalizeGuideChatInput,
  requestGuideChat,
  resolveGuideChatLocale,
  sanitizeGuideChatResponseContent,
  summarizeGuideChatError
} from './guideChat'

const axiosError = (status: number, data: Record<string, unknown> = {}, headers: Record<string, unknown> = {}) => ({
  isAxiosError: true,
  response: {
    status,
    data,
    headers
  }
})

describe('guide chat input safety', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    mocks.post.mockReset()
    mocks.getRecaptchaToken.mockReset()
    mocks.isRecaptchaV3Enabled.mockReset()
    mocks.isRecaptchaV3Enabled.mockReturnValue(false)
  })

  it('normalizes control characters, zero-width marks, and whitespace', () => {
    expect(normalizeGuideChatInput('  拉萨\u0000\n\n林芝\u200B\t高反  ')).toBe('拉萨 林芝 高反')
  })

  it('clamps long prompts without splitting surrogate pairs', () => {
    const normalized = normalizeGuideChatInput(`路线${'😀'.repeat(MAX_GUIDE_CHAT_MESSAGE_CHARS)}`)

    expect(Array.from(normalized)).toHaveLength(MAX_GUIDE_CHAT_MESSAGE_CHARS)
    expect(normalized.endsWith('😀')).toBe(true)
  })

  it('uses normalized text for visible user messages', () => {
    const message = createUserMessage('  帮我\n规划  西藏路线  ')

    expect(message.text).toBe('帮我 规划 西藏路线')
  })

  it('annotates local fallback messages that can be retried against the cloud guide', () => {
    const message = createNetworkFallbackGuideMessage('  拉萨  高原适应  ')

    expect(message.role).toBe('guide')
    expect(message.fallback).toBe(true)
    expect(message.networkFallback).toBe(true)
    expect(message.retryText).toBe('拉萨 高原适应')
  })

  it('coerces retry-after values into a bounded UI cooldown', () => {
    expect(coerceRetryAfter('12')).toBe(12)
    expect(coerceRetryAfter(0)).toBe(1)
    expect(coerceRetryAfter(9999)).toBe(3600)
    expect(coerceRetryAfter('not-a-number', 30)).toBe(30)
  })

  it('allows only supported locales for cloud guide requests', () => {
    expect(resolveGuideChatLocale({ getItem: () => 'bo' })).toBe('bo')
    expect(resolveGuideChatLocale({ getItem: () => 'zh' })).toBe('zh')
    expect(resolveGuideChatLocale({ getItem: () => '../../admin' })).toBe('zh')
    expect(resolveGuideChatLocale({ getItem: () => { throw new Error('storage denied') } })).toBe('zh')
    expect(resolveGuideChatLocale(null)).toBe('zh')
  })

  it('reads the default guide chat locale through safe browser storage', () => {
    const storage = {
      getItem: vi.fn(() => 'bo')
    }
    vi.stubGlobal('window', { localStorage: storage })

    expect(resolveGuideChatLocale()).toBe('bo')
  })

  it('summarizes request failures without leaking headers or query secrets', () => {
    const summary = summarizeGuideChatError({
      isAxiosError: true,
      message: 'Request failed with status code 500',
      response: { status: 500 },
      config: {
        url: '/api/ai/guide/chat?token=recaptcha-secret',
        headers: { 'X-Recaptcha-Token': 'recaptcha-secret' }
      }
    })

    expect(summary).toBe('guide chat request failed with status 500')
    expect(summary).not.toContain('recaptcha-secret')
    expect(summary).not.toContain('X-Recaptcha-Token')
  })

  it('sanitizes guide response content that looks like provider, token, or stack detail', () => {
    const sanitized = sanitizeGuideChatResponseContent(
      'provider=openai Authorization: Bearer secret-token stack trace at com.tibet.tourism.GuideChat(GuideChat.java:42)'
    )

    expect(sanitized).toContain('本地安全建议')
    expect(sanitized).not.toMatch(/provider|Bearer|stack|com\.tibet/i)
  })

  it('marks unsafe cloud content as fallback and keeps action labels safe', async () => {
    mocks.post.mockResolvedValue({
      data: {
        content: 'provider=openai token=sk-1234567890 stack trace at com.tibet.tourism.GuideChat(GuideChat.java:42)',
        action: 'navigate',
        actionLabel: 'model=gpt-internal'
      }
    })

    const message = await requestGuideChat('  拉萨\u0000路线  ', [
      { role: 'user', content: '   ' },
      { role: 'guide', content: '林芝\u200B行程' }
    ])

    expect(mocks.post).toHaveBeenCalledWith('/api/ai/guide/chat', {
      message: '拉萨 路线',
      history: [{ role: 'guide', content: '林芝行程' }],
      locale: 'zh'
    }, undefined)
    expect(message.fallback).toBe(true)
    expect(message.action).toBe('navigate')
    expect(message.actionLabel).toBe('去规划路线')
    expect(message.text).not.toMatch(/provider|sk-|stack|com\.tibet/i)
  })

  it('retries a reCAPTCHA challenge once and then shows a generic security message', async () => {
    mocks.isRecaptchaV3Enabled.mockReturnValue(true)
    mocks.getRecaptchaToken.mockResolvedValue('challenge-token')
    mocks.post.mockRejectedValue(axiosError(428, {
      content: 'provider=openai token=sk-1234567890 stack trace',
      challengeRequired: true
    }, { 'retry-after': '9' }))

    const message = await requestGuideChat('拉萨路线', [])

    expect(mocks.getRecaptchaToken).toHaveBeenCalledTimes(1)
    expect(mocks.post).toHaveBeenCalledTimes(2)
    expect(mocks.post.mock.calls[1]?.[2]).toEqual({
      headers: { 'X-Recaptcha-Token': 'challenge-token' }
    })
    expect(message.limited).toBe(true)
    expect(message.challengeRequired).toBe(true)
    expect(message.retryAfterSeconds).toBe(9)
    expect(message.text).toContain('安全校验')
    expect(message.text).not.toMatch(/provider|token|stack|recaptcha/i)
  })

  it('does not loop when reCAPTCHA token generation fails', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    mocks.isRecaptchaV3Enabled.mockReturnValue(true)
    mocks.getRecaptchaToken.mockRejectedValue(new Error('provider token stack'))
    mocks.post.mockRejectedValue(axiosError(428, {
      content: 'Exception: token leaked in stack trace',
      challengeRequired: true
    }, { 'retry-after': '7' }))

    const message = await requestGuideChat('拉萨路线', [])

    expect(mocks.getRecaptchaToken).toHaveBeenCalledTimes(1)
    expect(mocks.post).toHaveBeenCalledTimes(1)
    expect(message.challengeRequired).toBe(true)
    expect(message.retryAfterSeconds).toBe(7)
    expect(message.text).not.toMatch(/Exception|token|stack/i)
    warn.mockRestore()
  })

  it('uses generic rate-limit copy instead of backend technical detail', async () => {
    mocks.post.mockRejectedValue(axiosError(429, {
      content: 'NullPointerException provider=openai token=sk-1234567890',
      retryAfterSeconds: 12
    }))

    const message = await requestGuideChat('珠峰行程', [])

    expect(message.limited).toBe(true)
    expect(message.challengeRequired).toBe(false)
    expect(message.retryAfterSeconds).toBe(12)
    expect(message.text).toContain('连续请求过多')
    expect(message.text).not.toMatch(/NullPointerException|provider|token/i)
  })
})
