<template>
  <AnimatePresence>
    <motion.div
      v-if="modelValue"
      key="contact-modal-backdrop"
      @click="close"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-md"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.24, ease: motionEase }"
    >
        <motion.div
             key="contact-modal-panel"
             @click.stop
             class="relative w-full max-w-lg overflow-hidden rounded-3xl shadow-2xl"
             :initial="{ opacity: 0, y: 24, scale: 0.96 }"
             :animate="{ opacity: 1, y: 0, scale: 1 }"
             :exit="{ opacity: 0, y: 16, scale: 0.97 }"
             :transition="{ duration: 0.32, ease: motionEase }">
          
          <div class="absolute inset-0 bg-gradient-to-br from-white/95 via-tibet-white/90 to-white/80 backdrop-blur-2xl"></div>
          <div class="absolute inset-0 tibet-cloud-pattern opacity-55"></div>
          
          <div class="relative z-10 p-8">
            <motion.button @click="close"
                    :whileHover="{ scale: 1.08, rotate: 4 }"
                    :whileTap="{ scale: 0.9 }"
                    class="absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-full bg-gray-200/50 hover:bg-gray-300/70 backdrop-blur-sm transition-all duration-200 active:scale-90 group">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-600 group-hover:text-gray-800 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </motion.button>

            <motion.div
              class="text-center mb-8"
              :initial="{ opacity: 0, y: 16 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="cardTransition(0, 0.08)"
            >
              <motion.div
                class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gradient-to-br from-tibet-red via-tibet-gold to-tibet-yellow mb-4 shadow-lg"
                :animate="prefersReducedMotion ? { scale: 1 } : { scale: [1, 1.04, 1] }"
                :transition="prefersReducedMotion ? { duration: 0 } : { duration: 2.4, repeat: Infinity, ease: 'easeInOut' }"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-tibet-dark" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </motion.div>
              <h2 class="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-tibet-red via-tibet-gold to-tibet-blue mb-2">
                {{ t('contact.title') }}
              </h2>
              <p class="text-sm text-gray-600">{{ t('contact.subtitle') }}</p>
            </motion.div>

            <div class="space-y-4">
              <motion.div
                class="group relative overflow-hidden rounded-2xl bg-white/60 backdrop-blur-sm border border-tibet-gold/25 p-5 shadow-sm hover:shadow-lg transition-all duration-300"
                :initial="{ opacity: 0, y: 18, scale: 0.98 }"
                :animate="{ opacity: 1, y: 0, scale: 1 }"
                :transition="cardTransition(0, 0.16)"
                :whileHover="{ y: -3, scale: 1.012 }"
              >
                <div class="absolute inset-0 tibet-prayer-flag opacity-0 group-hover:opacity-10 transition-opacity duration-300"></div>
                <div class="relative flex items-start space-x-4">
                  <div class="flex-shrink-0">
                    <motion.div class="w-12 h-12 rounded-xl bg-tibet-blue/10 text-tibet-blue border border-tibet-blue/20 flex items-center justify-center shadow-md"
                      :whileHover="{ scale: 1.1, rotate: -3 }">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                      </svg>
                    </motion.div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-sm font-medium text-gray-500 mb-1">{{ t('contact.phoneConsult') }}</h3>
                    <p class="text-lg font-semibold text-gray-900 mb-1">{{ t('footer.phone') }}</p>
                    <p class="text-xs text-gray-500">{{ t('contact.phoneHours') }}</p>
                  </div>
                  <motion.button @click="copyToClipboard(t('footer.phone'))"
                          :whileHover="{ scale: 1.08 }"
                          :whileTap="{ scale: 0.9 }"
                          class="flex-shrink-0 w-8 h-8 rounded-lg bg-gray-100/50 hover:bg-gray-200/70 flex items-center justify-center transition-all duration-200 active:scale-90">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                  </motion.button>
                </div>
              </motion.div>

              <motion.div
                class="group relative overflow-hidden rounded-2xl bg-white/60 backdrop-blur-sm border border-tibet-gold/25 p-5 shadow-sm hover:shadow-lg transition-all duration-300"
                :initial="{ opacity: 0, y: 18, scale: 0.98 }"
                :animate="{ opacity: 1, y: 0, scale: 1 }"
                :transition="cardTransition(1, 0.16)"
                :whileHover="{ y: -3, scale: 1.012 }"
              >
                <div class="absolute inset-0 tibet-prayer-flag opacity-0 group-hover:opacity-10 transition-opacity duration-300"></div>
                <div class="relative flex items-start space-x-4">
                  <div class="flex-shrink-0">
                    <motion.div class="w-12 h-12 rounded-xl bg-tibet-red/10 text-tibet-red border border-tibet-red/20 flex items-center justify-center shadow-md"
                      :whileHover="{ scale: 1.1, rotate: 3 }">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                    </motion.div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-sm font-medium text-gray-500 mb-1">{{ t('contact.emailContact') }}</h3>
                    <p class="text-lg font-semibold text-gray-900 mb-1 break-all">{{ t('footer.email') }}</p>
                    <p class="text-xs text-gray-500">{{ t('contact.emailResponse') }}</p>
                  </div>
                  <motion.button @click="copyToClipboard(t('footer.email'))"
                          :whileHover="{ scale: 1.08 }"
                          :whileTap="{ scale: 0.9 }"
                          class="flex-shrink-0 w-8 h-8 rounded-lg bg-gray-100/50 hover:bg-gray-200/70 flex items-center justify-center transition-all duration-200 active:scale-90">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                  </motion.button>
                </div>
              </motion.div>

              <motion.div
                class="group relative overflow-hidden rounded-2xl bg-white/60 backdrop-blur-sm border border-tibet-gold/25 p-5 shadow-sm hover:shadow-lg transition-all duration-300"
                :initial="{ opacity: 0, y: 18, scale: 0.98 }"
                :animate="{ opacity: 1, y: 0, scale: 1 }"
                :transition="cardTransition(2, 0.16)"
                :whileHover="{ y: -3, scale: 1.012 }"
              >
                <div class="absolute inset-0 tibet-prayer-flag opacity-0 group-hover:opacity-10 transition-opacity duration-300"></div>
                <div class="relative flex items-start space-x-4">
                  <div class="flex-shrink-0">
                    <motion.div class="w-12 h-12 rounded-xl bg-tibet-turquoise/10 text-tibet-turquoise border border-tibet-turquoise/20 flex items-center justify-center shadow-md"
                      :whileHover="{ scale: 1.1, rotate: -3 }">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </motion.div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-sm font-medium text-gray-500 mb-1">{{ t('contact.companyAddress') }}</h3>
                    <p class="text-lg font-semibold text-gray-900 mb-1">{{ t('footer.addressLine1') }}</p>
                    <p class="text-xs text-gray-500">{{ t('footer.addressLine2') }}</p>
                  </div>
                </div>
              </motion.div>
            </div>

            <motion.div
              class="mt-6 pt-6 border-t border-tibet-gold/25 text-center"
              :initial="{ opacity: 0, y: 10 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="cardTransition(3, 0.16)"
            >
              <p class="text-xs text-gray-500">{{ t('contact.closingMessage') }}</p>
            </motion.div>
          </div>

          <div class="absolute bottom-0 left-0 right-0 h-1 tibet-prayer-flag opacity-80"></div>
        </motion.div>
    </motion.div>
  </AnimatePresence>

  <AnimatePresence>
    <motion.div
         v-if="showToast"
         key="contact-toast"
         class="fixed bottom-8 right-8 z-[60] bg-gray-900/90 backdrop-blur-xl text-white px-6 py-3 rounded-2xl shadow-2xl flex items-center space-x-3"
      :initial="{ opacity: 0, y: 18, scale: 0.96 }"
      :animate="{ opacity: 1, y: 0, scale: 1 }"
      :exit="{ opacity: 0, y: 12, scale: 0.96 }"
      :transition="{ duration: 0.26, ease: motionEase }"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
      </svg>
      <span class="text-sm font-medium">{{ t('contact.copiedToClipboard') }}</span>
    </motion.div>
  </AnimatePresence>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { AnimatePresence, motion, useReducedMotion } from 'motion-v'
import { cardTransition, motionEase } from '../motion/presets'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const { t } = useI18n()
const showToast = ref(false)
const prefersReducedMotion = useReducedMotion()

const close = () => {
  emit('update:modelValue', false)
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    showToast.value = true
    setTimeout(() => {
      showToast.value = false
    }, 2000)
  } catch (err) {
    console.error('Failed to copy:', err)
  }
}
</script>
