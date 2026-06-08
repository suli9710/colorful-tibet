import { afterEach, describe, expect, it, vi } from 'vitest'
import { hasRecaptchaSiteKey, isRecaptchaV2Enabled, isRecaptchaV3Enabled } from './recaptcha'

afterEach(() => {
  vi.unstubAllEnvs()
})

describe('recaptcha configuration', () => {
  it('defaults to v3 when enabled with a site key', () => {
    vi.stubEnv('VITE_RECAPTCHA_ENABLED', 'true')
    vi.stubEnv('VITE_RECAPTCHA_MODE', '')
    vi.stubEnv('VITE_RECAPTCHA_SITE_KEY', 'site-key')

    expect(hasRecaptchaSiteKey()).toBe(true)
    expect(isRecaptchaV3Enabled()).toBe(true)
    expect(isRecaptchaV2Enabled()).toBe(false)
  })

  it('uses v2 only when explicitly configured', () => {
    vi.stubEnv('VITE_RECAPTCHA_ENABLED', 'true')
    vi.stubEnv('VITE_RECAPTCHA_MODE', 'v2')
    vi.stubEnv('VITE_RECAPTCHA_SITE_KEY', 'site-key')

    expect(isRecaptchaV2Enabled()).toBe(true)
    expect(isRecaptchaV3Enabled()).toBe(false)
  })

  it('honors an explicit disabled flag even when a site key is present', () => {
    vi.stubEnv('VITE_RECAPTCHA_ENABLED', 'false')
    vi.stubEnv('VITE_RECAPTCHA_MODE', 'v3')
    vi.stubEnv('VITE_RECAPTCHA_SITE_KEY', 'site-key')

    expect(hasRecaptchaSiteKey()).toBe(false)
    expect(isRecaptchaV2Enabled()).toBe(false)
    expect(isRecaptchaV3Enabled()).toBe(false)
  })
})
