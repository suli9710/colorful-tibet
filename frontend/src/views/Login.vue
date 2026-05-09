<template>
  <div class="min-h-screen flex items-center justify-center bg-cover bg-center py-12 px-4 sm:px-6 lg:px-8 relative overflow-hidden"
       style="background-image: url('/heritage/布达拉宫3.jpg')">

    <!-- Overlay -->
    <div class="absolute inset-0 bg-gradient-to-b from-tibet-dark/60 via-tibet-dark/40 to-tibet-dark/70 backdrop-blur-[1px]"></div>

    <!-- Falling particles -->
    <div class="auth-particles">
      <div class="auth-particle"></div>
      <div class="auth-particle"></div>
      <div class="auth-particle"></div>
      <div class="auth-particle"></div>
      <div class="auth-particle"></div>
      <div class="auth-particle"></div>
      <div class="auth-particle"></div>
      <div class="auth-particle"></div>
    </div>

    <!-- Sacred Gate Card -->
    <div class="max-w-md w-full space-y-8 relative z-10 bg-tibet-white/95 backdrop-blur-xl p-10 rounded-2xl shadow-2xl animate-scale-in gpu-accelerated tibet-top-ornament tibet-four-corners">

      <!-- Auth arch decoration -->
      <div class="auth-arch"></div>

      <div class="text-center animate-slide-up pt-4" style="animation-delay: 0.1s">
        <h2 class="text-3xl font-bold text-tibet-dark mb-2 font-display">{{ t('login.welcomeBack') }}</h2>
        <div class="tibet-divider w-24 mx-auto mt-3 mb-3"></div>
        <p class="text-tibet-brown/70 animate-fade-in text-sm" style="animation-delay: 0.2s">{{ t('login.subtitle') }}</p>
      </div>

      <form class="mt-8 space-y-6 animate-slide-up" style="animation-delay: 0.2s" @submit.prevent="handleLogin">
        <div class="space-y-4">
          <div class="animate-slide-up" style="animation-delay: 0.3s">
            <label for="username" class="sr-only">{{ t('login.username') }}</label>
            <input id="username" name="username" type="text" required v-model="form.username"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('login.username')">
          </div>
          <div class="animate-slide-up" style="animation-delay: 0.4s">
            <label for="password" class="sr-only">{{ t('login.password') }}</label>
            <input id="password" name="password" type="password" required v-model="form.password"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('login.password')">
          </div>
        </div>

        <div class="animate-slide-up" style="animation-delay: 0.5s">
          <button type="submit" :disabled="loading"
                  class="tibet-btn w-full flex justify-center py-3 text-base disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none">
            <span v-if="loading" class="absolute left-0 inset-y-0 flex items-center pl-3 z-10">
              <svg class="animate-spin h-5 w-5 text-tibet-yellow" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            <span class="relative z-10">{{ loading ? t('login.loggingIn') : t('common.login') }}</span>
          </button>
        </div>
      </form>

      <div class="text-center mt-4 animate-fade-in" style="animation-delay: 0.6s">
        <p class="text-sm text-tibet-brown/70">
          {{ t('login.noAccount') }}
          <router-link to="/register" class="tibet-link">
            {{ t('login.registerNow') }}
          </router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api, { clearTokenCache } from '../api'
import { useAuthStore } from '../stores/auth'

const { t } = useI18n()

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  loading.value = true
  try {
    const { data: user } = await api.post('/auth/login', form.value)

    if (!user.token) {
      alert(t('login.loginFailed') + ' (缺少token)')
      return
    }

    clearTokenCache()
    auth.login(user, user.token)

    router.push(user.role === 'ADMIN' ? '/admin' : '/')
  } catch (error: any) {
    alert(error.response?.data?.message || error.response?.data?.error || t('login.loginFailed'))
  } finally {
    loading.value = false
  }
}
</script>
