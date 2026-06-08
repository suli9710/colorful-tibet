<template>
  <motion.div
    class="min-h-[calc(100dvh-5rem)] flex items-start justify-center bg-cover bg-center px-4 pb-8 pt-8 sm:items-center sm:px-6 sm:py-12 lg:px-8 relative overflow-y-auto overflow-x-hidden"
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
      class="max-w-md w-full space-y-6 relative z-10 bg-tibet-white/95 backdrop-blur-xl p-6 rounded-2xl shadow-2xl gpu-accelerated tibet-top-ornament tibet-four-corners sm:space-y-8 sm:p-10"
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
        <h2 class="text-2xl font-bold text-tibet-dark mb-2 font-display sm:text-3xl">{{ t('login.welcomeBack') }}</h2>
        <div class="tibet-divider w-24 mx-auto mt-3 mb-3"></div>
        <p class="text-tibet-brown/70 text-sm">{{ t('login.subtitle') }}</p>
      </motion.div>

      <motion.form
        class="mt-6 space-y-5 sm:mt-8 sm:space-y-6"
        :initial="authItemInitial"
        :animate="authItemAnimate"
        :transition="authItemTransition(0.18)"
        :aria-busy="loading"
        :aria-describedby="errorMessage ? loginErrorId : undefined"
        @submit.prevent="handleLogin"
      >
        <div class="space-y-4">
          <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.28)">
            <label for="username" class="sr-only">{{ t('login.username') }}</label>
            <input id="username" name="username" type="text" required v-model="form.username"
                   autocomplete="username"
                   autocapitalize="none"
                   spellcheck="false"
                   :disabled="loading"
                   :aria-describedby="usernameDescription"
                   :aria-invalid="loginFieldInvalid"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-70 sm:text-sm input-focus"
                   :placeholder="t('login.username')"
                   @input="clearError">
            <p id="login-username-help" class="sr-only">{{ t('login.username') }}</p>
          </motion.div>
          <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.36)">
            <label for="password" class="sr-only">{{ t('login.password') }}</label>
            <input id="password" name="password" type="password" required v-model="form.password"
                   autocomplete="current-password"
                   :disabled="loading"
                   :aria-describedby="passwordDescription"
                   :aria-invalid="loginFieldInvalid"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-70 sm:text-sm input-focus"
                   :placeholder="t('login.password')"
                   @input="clearError">
            <p id="login-password-help" class="sr-only">{{ t('login.password') }}</p>
          </motion.div>
          <motion.div
            v-if="requiresSecondaryPassword"
            :initial="authItemInitial"
            :animate="authItemAnimate"
            :transition="authItemTransition(0.4)"
          >
            <label for="secondaryPassword" class="sr-only">{{ t('login.secondaryPassword') }}</label>
            <input id="secondaryPassword" name="secondaryPassword" type="password" required v-model="form.secondaryPassword"
                   autocomplete="one-time-code"
                   inputmode="numeric"
                   maxlength="6"
                   pattern="[0-9]{6}"
                   :disabled="loading"
                   :aria-describedby="secondaryPasswordDescription"
                   :aria-invalid="loginFieldInvalid"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-70 sm:text-sm input-focus"
                   :placeholder="t('login.secondaryPassword')"
                   @input="clearError">
            <p id="login-secondary-password-help" class="sr-only">{{ t('login.secondaryPassword') }}</p>
          </motion.div>
        </div>

        <div
          v-if="errorMessage"
          :id="loginErrorId"
          role="alert"
          aria-live="assertive"
          class="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700"
        >
          {{ errorMessage }}<span v-if="lockCountdown > 0">（{{ lockCountdown }}秒后可重试）</span>
        </div>

        <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.44)">
          <motion.button type="submit" :disabled="isLoginSubmitDisabled"
                  :whileHover="isLoginSubmitDisabled ? {} : authSubmitHover"
                  :whileTap="isLoginSubmitDisabled ? {} : authSubmitPress"
                  class="tibet-btn relative flex w-full min-w-0 items-center justify-center px-4 py-3 text-center text-base leading-snug disabled:cursor-not-allowed disabled:opacity-50 disabled:transform-none">
            <span v-if="loading" class="absolute left-0 inset-y-0 flex items-center pl-3 z-10">
              <svg class="animate-spin h-5 w-5 text-tibet-yellow" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            <span class="relative z-10 min-w-0 whitespace-normal break-words px-6 sm:whitespace-nowrap">{{ loginSubmitLabel }}</span>
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
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { motion, useReducedMotion } from 'motion-v'
import api, { clearTokenCache, endpoints } from '../api'
import { useAuthStore } from '../stores/auth'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'
import { getRecaptchaToken, isRecaptchaError, isRecaptchaV3Enabled } from '../utils/recaptcha'
import { resolvePostLoginRedirect } from '../router/authRedirect'
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

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const prefersReducedMotion = useReducedMotion()
const loading = ref(false)
const errorMessage = ref('')
const lockCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
const loginErrorId = 'login-form-error'
const form = ref({
  username: '',
  password: '',
  secondaryPassword: ''
})
const requiresSecondaryPassword = ref(false)
const loginFieldInvalid = computed(() => errorMessage.value ? 'true' : undefined)
const describedBy = (helpId: string) => computed(() => [
  helpId,
  errorMessage.value ? loginErrorId : ''
].filter(Boolean).join(' '))
const usernameDescription = describedBy('login-username-help')
const passwordDescription = describedBy('login-password-help')
const secondaryPasswordDescription = describedBy('login-secondary-password-help')
const isLoginSubmitDisabled = computed(() => loading.value || lockCountdown.value > 0)
const loginSubmitLabel = computed(() => {
  if (lockCountdown.value > 0) return `请等待 ${lockCountdown.value} 秒`
  return loading.value ? t('login.loggingIn') : t('common.login')
})

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

function parseRetryAfter(value: unknown): number {
  const retryAfter = Array.isArray(value) ? value[0] : value
  if (typeof retryAfter !== 'string' && typeof retryAfter !== 'number') {
    return 0
  }
  const parsed = Number.parseInt(String(retryAfter), 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0
}

function readRetrySecondsFromBody(value: unknown): number {
  if (typeof value !== 'object' || value === null) {
    return 0
  }

  const retrySeconds = (value as Record<string, unknown>).retryAfterSeconds
    ?? (value as Record<string, unknown>).retryAfter
    ?? (value as Record<string, unknown>).lockRemainingSeconds

  return parseRetryAfter(retrySeconds)
}

const handleLogin = async () => {
  if (isLoginSubmitDisabled.value) return

  errorMessage.value = ''
  loading.value = true
  try {
    const payload = {
      username: form.value.username,
      password: form.value.password,
      ...(requiresSecondaryPassword.value ? { secondaryPassword: form.value.secondaryPassword } : {})
    }
    const recaptchaToken = isRecaptchaV3Enabled() ? await getRecaptchaToken('login') : ''
    const { data: user } = await api.post(endpoints.auth.login, payload, {
      headers: recaptchaToken ? { 'X-Recaptcha-Token': recaptchaToken } : {}
    })

    clearTokenCache()
    auth.login(user)

    if (user.mustChangePassword) {
      router.push({ path: '/profile', query: { changePassword: '1' } })
      return
    }

    router.push(resolvePostLoginRedirect(route.query.redirect, user.role))
  } catch (error: any) {
    if (isRecaptchaError(error)) {
      errorMessage.value = t('security.recaptchaFailed')
      return
    }
    if (error.response?.status === 449 && error.response?.data?.requiresSecondaryAuth) {
      requiresSecondaryPassword.value = true
      errorMessage.value = `${t('profile.fillAllFields')}: ${t('login.secondaryPassword')}`
      return
    }
    console.error('Login failed:', summarizeClientError(error))
    const msg = safeClientErrorMessage(error, t('login.loginFailed'))
    errorMessage.value = msg

    const lockSeconds = parseRetryAfter(error.response?.headers?.['retry-after'])
      || readRetrySecondsFromBody(error.response?.data)
    if (lockSeconds > 0) {
      startCountdown(lockSeconds)
    }
  } finally {
    loading.value = false
  }
}
</script>
