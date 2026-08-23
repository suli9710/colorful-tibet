import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import heritageContentZh from './locales/heritage-content.zh.json'
import extraZh from './locales/extra.zh.json'
import securityZh from './locales/security.zh.json'
import { readBrowserStorage } from '../utils/browserStorage'

export type SupportedLocale = 'zh' | 'bo'

const isI18nMessageAst = (value: unknown): value is Record<string, any> =>
  !!value &&
  typeof value === 'object' &&
  !Array.isArray(value) &&
  (value as Record<string, any>).t === 0 &&
  ('b' in (value as Record<string, any>) || 'body' in (value as Record<string, any>))

const mergeMessages = <T extends Record<string, any>>(base: T, extra: Record<string, any>): T => {
  const merged: Record<string, any> = { ...base }
  Object.entries(extra).forEach(([key, value]) => {
    if (
      value &&
      typeof value === 'object' &&
      !Array.isArray(value) &&
      !isI18nMessageAst(value) &&
      merged[key] &&
      typeof merged[key] === 'object' &&
      !Array.isArray(merged[key]) &&
      !isI18nMessageAst(merged[key])
    ) {
      merged[key] = mergeMessages(merged[key], value)
    } else {
      merged[key] = value
    }
  })
  return merged as T
}

export const normalizeLocale = (locale: string): SupportedLocale => locale === 'bo' ? 'bo' : 'zh'
export const initialLocale = normalizeLocale(readBrowserStorage('localStorage', 'locale', 'zh'))

const zhMessages = mergeMessages(
  mergeMessages(mergeMessages(zh, heritageContentZh), extraZh),
  securityZh
)

const i18n = createI18n({
  legacy: false,
  locale: 'zh',
  fallbackLocale: 'zh',
  messages: { zh: zhMessages, bo: {} }
})

const loadedLocales = new Set<SupportedLocale>(['zh'])
const pendingLocaleLoads = new Map<SupportedLocale, Promise<SupportedLocale>>()

const loadTibetanMessages = async () => {
  const [base, heritage, extra, security] = await Promise.all([
    import('./locales/bo.json'),
    import('./locales/heritage-content.bo.json'),
    import('./locales/extra.bo.json'),
    import('./locales/security.bo.json'),
  ])

  return mergeMessages(
    mergeMessages(mergeMessages(base.default, heritage.default), extra.default),
    security.default
  )
}

export const loadLocaleMessages = async (value: string): Promise<SupportedLocale> => {
  const locale = normalizeLocale(value)
  if (loadedLocales.has(locale)) return locale

  const existing = pendingLocaleLoads.get(locale)
  if (existing) return existing

  const pending = loadTibetanMessages()
    .then(messages => {
      i18n.global.setLocaleMessage(locale, messages)
      loadedLocales.add(locale)
      return locale
    })
    .finally(() => pendingLocaleLoads.delete(locale))

  pendingLocaleLoads.set(locale, pending)
  return pending
}

export const setAppLocale = async (value: string): Promise<SupportedLocale> => {
  const locale = await loadLocaleMessages(value)
  i18n.global.locale.value = locale
  if (typeof document !== 'undefined') {
    document.documentElement.lang = locale
  }
  return locale
}

export default i18n
