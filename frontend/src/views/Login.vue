<template>
  <motion.div
    class="min-h-screen flex items-center justify-center bg-cover bg-center py-12 px-4 sm:px-6 lg:px-8 relative overflow-hidden"
    style="background-image: url('/heritage/布达拉宫3.jpg')"
    :initial="authPageInitial"
    :animate="authPageAnimate"
    :transition="authPageTransition"
  >

    <!-- Overlay -->
    <motion.div
      class="absolute inset-0 bg-gradient-to-b from-tibet-dark/60 via-tibet-dark/40 to-tibet-dark/70 backdrop-blur-[1px]"
      :initial="authPageInitial"
      :animate="authPageAnimate"
      :transition="authOverlayTransition"
    ></motion.div>

    <!-- Falling particles -->
    <div v-if="!prefersReducedMotion" class="auth-particles">
      <div v-for="index in 8" :key="index" class="auth-particle"></div>
    </div>

    <!-- Sacred Gate Card -->
    <motion.div
      class="max-w-md w-full space-y-8 relative z-10 bg-tibet-white/95 backdrop-blur-xl p-10 rounded-2xl shadow-2xl gpu-accelerated tibet-top-ornament tibet-four-corners"
      :initial="authCardInitial"
      :animate="authCardAnimate"
      :transition="authCardTransition"
    >

      <!-- Auth arch decoration -->
      <div class="auth-arch"></div>

      <motion.div
        class="text-center pt-4"
        :initial="authItemInitial"
        :animate="authItemAnimate"
        :transition="authItemTransition(0.1)"
      >
        <h2 class="text-3xl font-bold text-tibet-dark mb-2 font-display">{{ t('login.welcomeBack') }}</h2>
        <div class="tibet-divider w-24 mx-auto mt-3 mb-3"></div>
        <p class="text-tibet-brown/70 text-sm">{{ t('login.subtitle') }}</p>
      </motion.div>

      <motion.form
        class="mt-8 space-y-6"
        :initial="authItemInitial"
        :animate="authItemAnimate"
        :transition="authItemTransition(0.18)"
        @submit.prevent="handleLogin"
      >
        <div class="space-y-4">
          <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.28)">
            <label for="username" class="sr-only">{{ t('login.username') }}</label>
            <input id="username" name="username" type="text" required v-model="form.username"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('login.username')"
                   @input="clearError">
          </motion.div>
          <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.36)">
            <label for="password" class="sr-only">{{ t('login.password') }}</label>
            <input id="password" name="password" type="password" required v-model="form.password"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('login.password')"
                   @input="clearError">
          </motion.div>
          <motion.div
            v-if="requiresSecondaryPassword"
            :initial="authItemInitial"
            :animate="authItemAnimate"
            :transition="authItemTransition(0.4)"
          >
            <label for="secondaryPassword" class="sr-only">{{ t('login.secondaryPassword') }}</label>
            <input id="secondaryPassword" name="secondaryPassword" type="text" required v-model="form.secondaryPassword"
                   autocomplete="one-time-code"
                   inputmode="numeric"
                   maxlength="6"
                   pattern="[0-9]{6}"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent sm:text-sm input-focus"
                   :placeholder="t('login.secondaryPassword')"
                   @input="clearError">
          </motion.div>
        </div>

        <div v-if="errorMessage" class="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {{ errorMessage }}<span v-if="lockCountdown > 0">（{{ lockCountdown }}秒后可重试）</span>
        </div>

        <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.44)">
          <motion.button type="submit" :disabled="loading || lockCountdown > 0"
                  :whileHover="(loading || lockCountdown > 0) ? {} : authSubmitHover"
                  :whileTap="(loading || lockCountdown > 0) ? {} : authSubmitPress"
                  class="tibet-btn w-full flex justify-center py-3 text-base disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none">
            <span v-if="loading" class="absolute left-0 inset-y-0 flex items-center pl-3 z-10">
              <svg class="animate-spin h-5 w-5 text-tibet-yellow" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            <span class="relative z-10">{{ lockCountdown > 0 ? `请等待 ${lockCountdown} 秒` : loading ? t('login.loggingIn') : t('common.login') }}</span>
          </motion.button>
        </motion.div>
      </motion.form>

      <motion.div
        class="text-center mt-4"
        :initial="authItemInitial"
        :animate="authItemAnimate"
        :transition="authItemTransition(0.54)"
      >
        <p class="text-sm text-tibet-brown/70">
          {{ t('login.noAccount') }}
          <router-link to="/register" class="tibet-link">
            {{ t('login.registerNow') }}
          </router-link>
        </p>
      </motion.div>
    </motion.div>
  </motion.div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { motion, useReducedMotion } from 'motion-v'
import api, { clearTokenCache } from '../api'
import { useAuthStore } from '../stores/auth'
import { getRecaptchaToken, isRecaptchaV3Enabled } from '../utils/recaptcha'
import {
  authCardAnimate,
  authCardInitial,
  authCardTransition,
  authItemAnimate,
  authItemInitial,
  authItemTransition,
  authOverlayTransition,
  authPageAnimate,
  authPageInitial,
  authPageTransition,
  authSubmitHover,
  authSubmitPress
} from '../motion/presets'

const { t } = useI18n()

const router = useRouter()
const auth = useAuthStore()
const prefersReducedMotion = useReducedMotion()
const loading = ref(false)
const errorMessage = ref('')
const lockCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
const form = ref({
  username: '',
  password: '',
  secondaryPassword: ''
})
const superAdminUsername = 'lzh'
const requiresSecondaryPassword = computed(() => form.value.username.trim().toLowerCase() === superAdminUsername)

function clearError() {
  errorMessage.value = ''
}

function startCountdown(seconds: number) {
  stopCountdown()
  lockCountdown.value = seconds
  countdownTimer = setInterval(() => {
    lockCountdown.value--
    if (lockCountdown.value <= 0) {
      stopCountdown()
      errorMessage.value = ''
    }
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer !== null) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  lockCountdown.value = 0
}

function parseLockSeconds(message: string): number {
  const match = message.match(/(\d+)\s*秒/)
  return match ? parseInt(match[1], 10) : 0
}

function parseRetryAfter(value: unknown): number {
  const retryAfter = Array.isArray(value) ? value[0] : value
  if (typeof retryAfter !== 'string' && typeof retryAfter !== 'number') {
    return 0
  }
  const parsed = Number.parseInt(String(retryAfter), 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0
}

const handleLogin = async () => {
  errorMessage.value = ''
  loading.value = true
  try {
    const payload = {
      username: form.value.username,
      password: form.value.password,
      ...(requiresSecondaryPassword.value ? { secondaryPassword: form.value.secondaryPassword } : {})
    }
    const recaptchaToken = isRecaptchaV3Enabled() ? await getRecaptchaToken('login') : ''
    const { data: user } = await api.post('/auth/login', payload, {
      headers: recaptchaToken ? { 'X-Recaptcha-Token': recaptchaToken } : {}
    })

    clearTokenCache()
    auth.login(user)

    if (user.mustChangePassword) {
      router.push({ path: '/profile', query: { changePassword: '1' } })
      return
    }

    router.push(user.role === 'ADMIN' ? '/admin' : '/')
  } catch (error: any) {
    const msg = error.response?.data?.message || error.response?.data?.error || t('login.loginFailed')
    errorMessage.value = msg

    const lockSeconds = parseRetryAfter(error.response?.headers?.['retry-after']) || parseLockSeconds(msg)
    if (lockSeconds > 0) {
      startCountdown(lockSeconds)
    }
  } finally {
    loading.value = false
  }
}
</script>
