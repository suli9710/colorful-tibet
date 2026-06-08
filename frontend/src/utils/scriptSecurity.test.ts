import { JSDOM } from 'jsdom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  applyThirdPartyScriptSecurity,
  thirdPartyScriptSecurityConfig
} from './scriptSecurity'

afterEach(() => {
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
})

describe('third-party script security attributes', () => {
  it('applies a CSP nonce from the document and SRI from env', () => {
    installDom('<meta name="csp-nonce" content="nonce-from-meta">')
    vi.stubEnv('VITE_AMAP_SCRIPT_INTEGRITY', 'sha384-amap-hash')

    const script = document.createElement('script')
    const config = applyThirdPartyScriptSecurity(script, 'amap')

    expect(config).toEqual({
      nonce: 'nonce-from-meta',
      integrity: 'sha384-amap-hash',
      crossOrigin: 'anonymous'
    })
    expect(script.getAttribute('nonce')).toBe('nonce-from-meta')
    expect(script.integrity).toBe('sha384-amap-hash')
    expect(script.crossOrigin).toBe('anonymous')
  })

  it('allows provider meta tags to override script integrity and crossorigin', () => {
    installDom(`
      <meta name="recaptcha-script-integrity" content="sha384-recaptcha-hash">
      <meta name="recaptcha-script-crossorigin" content="use-credentials">
    `)

    expect(thirdPartyScriptSecurityConfig('recaptcha')).toEqual({
      nonce: '',
      integrity: 'sha384-recaptcha-hash',
      crossOrigin: 'use-credentials'
    })
  })

  it('can read a nonce from a trusted runtime global', () => {
    installDom()
    window.__CSP_NONCE__ = 'runtime-nonce'

    const script = document.createElement('script')
    applyThirdPartyScriptSecurity(script, 'recaptcha')

    expect(script.getAttribute('nonce')).toBe('runtime-nonce')
    expect(script.getAttribute('integrity')).toBeNull()
  })
})

function installDom(head = '') {
  const dom = new JSDOM(`<!doctype html><html><head>${head}</head><body></body></html>`)
  vi.stubGlobal('window', dom.window)
  vi.stubGlobal('document', dom.window.document)
  return dom
}
