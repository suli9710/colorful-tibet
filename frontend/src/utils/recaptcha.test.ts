import { JSDOM } from 'jsdom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { hasRecaptchaSiteKey, isRecaptchaV2Enabled, isRecaptchaV3Enabled } from './recaptcha'

afterEach(() => {
  vi.resetModules()
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
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

  it('adds CSP nonce and optional SRI attributes to the injected script', async () => {
    const dom = installDom('<meta name="csp-nonce" content="recaptcha-nonce">')
    vi.stubEnv('VITE_RECAPTCHA_ENABLED', 'true')
    vi.stubEnv('VITE_RECAPTCHA_MODE', 'v3')
    vi.stubEnv('VITE_RECAPTCHA_SITE_KEY', 'site-key')
    vi.stubEnv('VITE_RECAPTCHA_SCRIPT_INTEGRITY', 'sha384-recaptcha-hash')
    window.grecaptcha = {
      ready: callback => callback(),
      execute: vi.fn(),
      render: vi.fn(),
      getResponse: vi.fn(),
      reset: vi.fn()
    }

    const { loadRecaptcha } = await import('./recaptcha')
    const loaded = loadRecaptcha('site-key')
    const script = document.querySelector<HTMLScriptElement>('script[src*="recaptcha/api.js"]')

    expect(script).not.toBeNull()
    expect(script?.src).toBe('https://www.recaptcha.net/recaptcha/api.js?render=site-key')
    expect(script?.getAttribute('nonce')).toBe('recaptcha-nonce')
    expect(script?.integrity).toBe('sha384-recaptcha-hash')
    expect(script?.crossOrigin).toBe('anonymous')

    script?.dispatchEvent(new dom.window.Event('load'))

    await expect(loaded).resolves.toBeUndefined()
  })
})

function installDom(head = '') {
  const dom = new JSDOM(`<!doctype html><html><head>${head}</head><body></body></html>`, {
    url: 'https://example.com/'
  })
  vi.stubGlobal('window', dom.window)
  vi.stubGlobal('document', dom.window.document)
  return dom
}
