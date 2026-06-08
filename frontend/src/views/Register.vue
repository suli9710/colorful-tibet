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
        <h2 class="text-2xl font-bold text-tibet-dark mb-2 font-display sm:text-3xl">{{ t('register.createAccount') }}</h2>
        <div class="tibet-divider w-24 mx-auto mt-3 mb-3"></div>
        <p class="text-tibet-brown/70 text-sm">{{ t('register.subtitle') }}</p>
      </motion.div>

      <motion.form
        class="mt-6 space-y-5 sm:mt-8"
        :initial="authItemInitial"
        :animate="authItemAnimate"
        :transition="authItemTransition(0.18)"
        :aria-busy="loading"
        :aria-describedby="registerErrorMessage ? registerErrorId : undefined"
        @submit.prevent="handleRegister"
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
                   :aria-invalid="registerFieldInvalid"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-70 sm:text-sm input-focus"
                   :placeholder="t('login.username')"
                   @input="clearRegisterError">
            <p id="register-username-help" class="sr-only">{{ t('login.username') }}</p>
          </motion.div>
          <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.34)">
            <label for="nickname" class="sr-only">{{ t('register.nickname') }}</label>
            <input id="nickname" name="nickname" type="text" required v-model="form.nickname"
                   autocomplete="nickname"
                   :disabled="loading"
                   :aria-describedby="nicknameDescription"
                   :aria-invalid="registerFieldInvalid"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-70 sm:text-sm input-focus"
                   :placeholder="t('register.nicknamePlaceholder')"
                   @input="clearRegisterError">
            <p id="register-nickname-help" class="sr-only">{{ t('register.nickname') }}</p>
          </motion.div>
          <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.4)">
            <label for="password" class="sr-only">{{ t('login.password') }}</label>
            <input id="password" name="password" type="password" required v-model="form.password"
                   autocomplete="new-password"
                   minlength="6"
                   :disabled="loading"
                   :aria-describedby="passwordDescription"
                   :aria-invalid="registerFieldInvalid"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-70 sm:text-sm input-focus"
                   :placeholder="t('login.password')"
                   @input="clearRegisterError">
            <p id="register-password-help" class="sr-only">{{ t('login.password') }}</p>
          </motion.div>
          <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.46)">
            <label for="confirmPassword" class="sr-only">{{ t('register.confirmPassword') }}</label>
            <input id="confirmPassword" name="confirmPassword" type="password" required v-model="confirmPassword"
                   autocomplete="new-password"
                   minlength="6"
                   :disabled="loading"
                   :aria-describedby="confirmPasswordDescription"
                   :aria-invalid="registerFieldInvalid"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-tibet-gold/25 placeholder-tibet-brown/40 text-tibet-dark bg-tibet-white focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-70 sm:text-sm input-focus"
                   :placeholder="t('register.confirmPassword')"
                   @input="clearRegisterError">
            <p id="register-confirm-password-help" class="sr-only">{{ t('register.confirmPassword') }}</p>
          </motion.div>
        </div>

        <div
          v-if="registerErrorMessage"
          :id="registerErrorId"
          role="alert"
          aria-live="assertive"
          class="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700"
        >
          {{ registerErrorMessage }}
        </div>

        <motion.div
          v-if="isRecaptchaV2Enabled()"
          :initial="authItemInitial"
          :animate="authItemAnimate"
          :transition="authItemTransition(0.5)"
          class="min-h-[78px]"
        >
          <div ref="recaptchaContainer" class="flex justify-center"></div>
        </motion.div>

        <motion.div :initial="authItemInitial" :animate="authItemAnimate" :transition="authItemTransition(0.54)">
          <motion.button type="submit" :disabled="loading"
                  :whileHover="loading ? {} : authSubmitHover"
                  :whileTap="loading ? {} : authSubmitPress"
                  class="tibet-btn relative flex w-full min-w-0 items-center justify-center px-4 py-3 text-center text-base leading-snug disabled:cursor-not-allowed disabled:opacity-50 disabled:transform-none">
            <span v-if="loading" class="absolute left-0 inset-y-0 flex items-center pl-3 z-10">
              <svg class="animate-spin h-5 w-5 text-tibet-yellow" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            <span class="relative z-10 min-w-0 whitespace-normal break-words px-6 sm:whitespace-nowrap">{{ loading ? t('register.registering') : t('register.registerNow') }}</span>
          </motion.button>
        </motion.div>
      </motion.form>

      <motion.div
        class="text-center mt-4"
        :initial="authItemInitial"
        :animate="authItemAnimate"
        :transition="authItemTransition(0.62)"
      >
        <p class="text-sm text-tibet-brown/70">
          {{ t('register.hasAccount') }}
          <router-link to="/login" class="tibet-link">
            {{ t('register.goToLogin') }}
          </router-link>
        </p>
      </motion.div>
    </motion.div>
  </motion.div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { motion, useReducedMotion } from 'motion-v'
import api, { endpoints } from '../api'
import { useToast } from '../composables/useToast'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'
import {
  RecaptchaError,
  getRecaptchaToken,
  getRecaptchaWidgetResponse,
  isRecaptchaError,
  isRecaptchaV2Enabled,
  isRecaptchaV3Enabled,
  renderRecaptchaCheckbox,
  resetRecaptchaWidget
} from '../utils/recaptcha'
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
const { showToast } = useToast()

const router = useRouter()
const prefersReducedMotion = useReducedMotion()
const loading = ref(false)
const registerErrorMessage = ref('')
const registerErrorId = 'register-form-error'
const confirmPassword = ref('')
const recaptchaContainer = ref<HTMLElement | null>(null)
const recaptchaWidgetId = ref<number | null>(null)
const form = ref({
  username: '',
  nickname: '',
  password: ''
})
const registerFieldInvalid = computed(() => registerErrorMessage.value ? 'true' : undefined)
const describedBy = (helpId: string) => computed(() => [
  helpId,
  registerErrorMessage.value ? registerErrorId : ''
].filter(Boolean).join(' '))
const usernameDescription = describedBy('register-username-help')
const nicknameDescription = describedBy('register-nickname-help')
const passwordDescription = describedBy('register-password-help')
const confirmPasswordDescription = describedBy('register-confirm-password-help')

const clearRegisterError = () => {
  registerErrorMessage.value = ''
}

const handleRegister = async () => {
  if (loading.value) return

  registerErrorMessage.value = ''

  if (form.value.password !== confirmPassword.value) {
    const message = t('register.passwordMismatch')
    registerErrorMessage.value = message
    showToast(message, 'warning')
    return
  }

  loading.value = true
  try {
    let recaptchaToken = ''
    if (isRecaptchaV3Enabled()) {
      recaptchaToken = await getRecaptchaToken('register')
    } else if (isRecaptchaV2Enabled()) {
      recaptchaToken = getRecaptchaWidgetResponse(recaptchaWidgetId.value)
      if (!recaptchaToken) {
        throw new RecaptchaError()
      }
    }
    await api.post(endpoints.auth.register, form.value, {
      headers: recaptchaToken ? { 'X-Recaptcha-Token': recaptchaToken } : {}
    })
    showToast(t('register.registerSuccess'), 'success')
    router.push('/login')
  } catch (error: any) {
    if (isRecaptchaError(error)) {
      const message = t('security.recaptchaFailed')
      registerErrorMessage.value = message
      showToast(message, 'error')
      resetRecaptchaWidget(recaptchaWidgetId.value)
      return
    }
    console.error('Register failed:', summarizeClientError(error))
    const message = safeClientErrorMessage(error, t('register.registerFailed'))
    registerErrorMessage.value = message
    showToast(message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!isRecaptchaV2Enabled()) return
  await nextTick()
  if (!recaptchaContainer.value) return
  recaptchaWidgetId.value = await renderRecaptchaCheckbox(recaptchaContainer.value, {
    onExpired: () => resetRecaptchaWidget(recaptchaWidgetId.value),
    onError: () => resetRecaptchaWidget(recaptchaWidgetId.value)
  })
})
</script>
