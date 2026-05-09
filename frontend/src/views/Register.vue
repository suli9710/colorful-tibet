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

      <div class="text-center animate-slide-up pt-4">
        <h2 class="text-3xl font-bold text-tibet-dark mb-2 font-display">{{ t('register.createAccount') }}</h2>
        <div class="tibet-divider w-24 mx-auto mt-3 mb-3"></div>
        <p class="text-tibet-brown/70 text-sm">{{ t('register.subtitle') }}</p>
      </div>

      <form class="mt-8 space-y-5" @submit.prevent="handleRegister">
        <div class="space-y-4">
          <div>
            <label for="username" class="sr-only">{{ t('login.username') }}</label>
            <input id="username" name="username" type="text" required v-model="form.username"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('login.username')">
          </div>
          <div>
            <label for="nickname" class="sr-only">{{ t('register.nickname') }}</label>
            <input id="nickname" name="nickname" type="text" required v-model="form.nickname"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('register.nicknamePlaceholder')">
          </div>
          <div>
            <label for="password" class="sr-only">{{ t('login.password') }}</label>
            <input id="password" name="password" type="password" required v-model="form.password"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('login.password')">
          </div>
          <div>
            <label for="confirmPassword" class="sr-only">{{ t('register.confirmPassword') }}</label>
            <input id="confirmPassword" name="confirmPassword" type="password" required v-model="confirmPassword"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('register.confirmPassword')">
          </div>
        </div>

        <div>
          <button type="submit" :disabled="loading"
                  class="tibet-btn w-full flex justify-center py-3 text-base disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none">
            <span v-if="loading" class="absolute left-0 inset-y-0 flex items-center pl-3 z-10">
              <svg class="animate-spin h-5 w-5 text-tibet-yellow" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            <span class="relative z-10">{{ loading ? t('register.registering') : t('register.registerNow') }}</span>
          </button>
        </div>
      </form>

      <div class="text-center mt-4">
        <p class="text-sm text-tibet-brown/70">
          {{ t('register.hasAccount') }}
          <router-link to="/login" class="tibet-link">
            {{ t('register.goToLogin') }}
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
import api from '../api'

const { t } = useI18n()

const router = useRouter()
const loading = ref(false)
const confirmPassword = ref('')
const form = ref({
  username: '',
  nickname: '',
  password: ''
})

const handleRegister = async () => {
  if (form.value.password !== confirmPassword.value) {
    alert(t('register.passwordMismatch'))
    return
  }

  loading.value = true
  try {
    await api.post('/auth/register', form.value)
    alert(t('register.registerSuccess'))
    router.push('/login')
  } catch (error: any) {
    console.error('Register failed:', error)
    const serverMessage = error?.response?.data?.message || error?.response?.data?.error
    alert(serverMessage || t('register.registerFailed'))
  } finally {
    loading.value = false
  }
}
</script>
