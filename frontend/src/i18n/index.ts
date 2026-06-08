import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import bo from './locales/bo.json'
import extraMessages from './locales/extra.json'
import securityMessages from './locales/security.json'
import { readBrowserStorage } from '../utils/browserStorage'

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

// 从localStorage获取保存的语言设置，默认为中文
const normalizeLocale = (locale: string) => locale === 'bo' ? 'bo' : 'zh'
const savedLocale = normalizeLocale(readBrowserStorage('localStorage', 'locale', 'zh'))

// 设置HTML lang属性
if (typeof document !== 'undefined') {
  document.documentElement.lang = savedLocale
}

const i18n = createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'zh',
  messages: {
    zh: mergeMessages(mergeMessages(zh, extraMessages.zh), securityMessages.zh),
    bo: mergeMessages(mergeMessages(bo, extraMessages.bo), securityMessages.bo)
  }
})

export default i18n

