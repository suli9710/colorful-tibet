import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { registerErrorMonitoring } from './utils/errorMonitoring'
import { useAuthStore } from './stores/auth'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
void useAuthStore(pinia).refreshSession()
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
