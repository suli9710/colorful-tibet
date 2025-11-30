import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import bo from './locales/bo.json'

// 从localStorage获取保存的语言设置，默认为中文
const savedLocale = localStorage.getItem('locale') || 'zh'

// 设置HTML lang属性
document.documentElement.lang = savedLocale

const i18n = createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'zh',
  messages: {
    zh,
    bo
  }
})

export default i18n

