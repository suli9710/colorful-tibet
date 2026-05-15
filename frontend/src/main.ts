import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { registerErrorMonitoring } from './utils/errorMonitoring'
import './style.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
registerErrorMonitoring(app, router)

if (typeof window !== 'undefined') {
  window.addEventListener('auth-expired', () => {
    if (router.currentRoute.value.path !== '/login') {
      router.push('/login')
    }
  })
}

app.mount('#app')
