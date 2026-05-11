import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import bo from './locales/bo.json'
import extraMessages from './locales/extra'

const mergeMessages = <T extends Record<string, any>>(base: T, extra: Record<string, any>): T => {
  const merged: Record<string, any> = { ...base }
  Object.entries(extra).forEach(([key, value]) => {
    if (
      value &&
      typeof value === 'object' &&
      !Array.isArray(value) &&
      merged[key] &&
      typeof merged[key] === 'object' &&
      !Array.isArray(merged[key])
    ) {
      merged[key] = mergeMessages(merged[key], value)
    } else {
      merged[key] = value
    }
  })
  return merged as T
}

// 从localStorage获取保存的语言设置，默认为中文
const savedLocale = localStorage.getItem('locale') || 'zh'

// 设置HTML lang属性
document.documentElement.lang = savedLocale

const i18n = createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'zh',
  messages: {
    zh: mergeMessages(zh, extraMessages.zh),
    bo: mergeMessages(bo, extraMessages.bo)
  }
})

export default i18n

