import { JSDOM } from 'jsdom'
import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => {
  vi.resetModules()
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
})

describe('AMap script loader', () => {
  it('adds CSP nonce and optional SRI attributes to the injected script', async () => {
    const dom = installDom('<meta name="csp-nonce" content="amap-nonce">')
    vi.stubEnv('VITE_AMAP_KEY', 'amap-key')
    vi.stubEnv('VITE_AMAP_SCRIPT_INTEGRITY', 'sha384-amap-hash')

    const { loadAmap } = await import('./amap')
    const loaded = loadAmap()
    const script = document.querySelector<HTMLScriptElement>('script[data-amap-loader="colorful-tibet"]')

    expect(script).not.toBeNull()
    expect(script?.src).toBe('https://webapi.amap.com/maps?v=2.0&key=amap-key')
    expect(script?.getAttribute('nonce')).toBe('amap-nonce')
    expect(script?.integrity).toBe('sha384-amap-hash')
    expect(script?.crossOrigin).toBe('anonymous')

    const amapWindow = window as unknown as Window & { AMap: unknown }
    amapWindow.AMap = { ready: true }
    script?.dispatchEvent(new dom.window.Event('load'))

    await expect(loaded).resolves.toBe(amapWindow.AMap)
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
