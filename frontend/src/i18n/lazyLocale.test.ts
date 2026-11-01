// @vitest-environment jsdom

import { afterEach, describe, expect, it } from 'vitest'
import mainSource from '../main.ts?raw'
import indexSource from './index.ts?raw'
import i18n, { initialLocale, loadLocaleMessages, setAppLocale } from './index'

describe('lazy locale loading', () => {
  afterEach(async () => {
    await setAppLocale('zh')
  })

  it('keeps the Tibetan bundles behind dynamic imports', () => {
    expect(indexSource).not.toContain("import bo from './locales/bo.json'")
    expect(indexSource).not.toContain("import heritageContentBo from './locales/heritage-content.bo.json'")
    expect(indexSource).toContain("import('./locales/bo.json')")
    expect(indexSource).toContain("import('./locales/heritage-content.bo.json')")
    expect(initialLocale === 'zh' || initialLocale === 'bo').toBe(true)
    expect(mainSource).toContain('await setAppLocale(initialLocale)')
  })

  it('loads Tibetan messages before switching the active locale', async () => {
    await loadLocaleMessages('bo')
    await setAppLocale('bo')

    expect(i18n.global.locale.value).toBe('bo')
    expect(document.documentElement.lang).toBe('bo')
    expect(i18n.global.te('common.home', 'bo')).toBe(true)
    expect(i18n.global.t('common.home')).not.toBe('common.home')
  })
})
