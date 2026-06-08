<template>
  <div class="min-h-screen bg-tibet-white py-12 pt-24 sm:py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <MotionBlock
        class="text-center mb-8 sm:mb-12"
      >
        <h1 class="text-3xl font-bold text-tibet-dark mb-3 sm:text-4xl sm:mb-4">{{ t('createRoute.title') }}</h1>
        <p class="text-base text-tibet-brown/70 sm:text-lg">
          {{ t('createRoute.subtitle') }}
        </p>
      </MotionBlock>

      <MotionBlock
        class="glass-card rounded-2xl p-4 sm:p-8 md:p-12 mb-8 sm:mb-12 shadow-xl border border-white/50"
        variant="card"
        :delay="0.08"
      >
        <form @submit.prevent="submitRoute" class="space-y-6 sm:space-y-8">
          <MotionBlock :delay="0.16">
            <label class="block text-sm font-medium text-tibet-dark/80 mb-2">
              {{ t('createRoute.routeTitle') }} <span class="text-red-500">*</span>
            </label>
            <input 
              v-model="form.title" 
              type="text" 
              :placeholder="t('createRoute.titlePlaceholder')"
              class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 outline-none transition-all"
              required
              maxlength="200"
            />
            <p class="mt-1 text-xs text-gray-500">{{ form.title.length }}/200</p>
          </MotionBlock>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <MotionBlock :index="1" :delay="0.16">
              <label class="block text-sm font-medium text-tibet-dark/80 mb-2">
                {{ t('createRoute.plannedDays') }} <span class="text-red-500">*</span>
              </label>
              <div class="flex items-center space-x-4">
                <motion.button
                  type="button" 
                  @click="form.days > 1 && form.days--" 
                  class="w-10 h-10 rounded-full bg-tibet-gold/5 hover:bg-tibet-gold/15 flex items-center justify-center text-tibet-brown/80 transition-colors"
                  :whileHover="{ y: -2, scale: 1.06 }"
                  :whileTap="{ scale: 0.9 }"
                >
                  -
                </motion.button>
                <motion.span
                  :key="form.days"
                  class="text-xl font-bold text-tibet-dark w-8 text-center"
                  :initial="{ opacity: 0, y: -8 }"
                  :animate="{ opacity: 1, y: 0 }"
                  :transition="{ duration: 0.2, ease: motionEase }"
                >
                  {{ form.days }}
                </motion.span>
                <motion.button
                  type="button" 
                  @click="form.days < 30 && form.days++" 
                  class="w-10 h-10 rounded-full bg-tibet-gold/5 hover:bg-tibet-gold/15 flex items-center justify-center text-tibet-brown/80 transition-colors"
                  :whileHover="{ y: -2, scale: 1.06 }"
                  :whileTap="{ scale: 0.9 }"
                >
                  +
                </motion.button>
              </div>
            </MotionBlock>

            <MotionBlock :index="2" :delay="0.16">
              <label class="block text-sm font-medium text-tibet-dark/80 mb-2">
                {{ t('createRoute.budgetRange') }} <span class="text-red-500">*</span>
              </label>
              <select 
                v-model="form.budget" 
                class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 outline-none transition-all appearance-none"
                required
              >
                <option value="">{{ t('createRoute.selectBudget') }}</option>
                <option v-for="opt in budgetOptions" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
              </select>
            </MotionBlock>

            <MotionBlock :index="3" :delay="0.16">
              <label class="block text-sm font-medium text-tibet-dark/80 mb-2">
                {{ t('createRoute.preference') }} <span class="text-red-500">*</span>
              </label>
              <select 
                v-model="form.preference" 
                class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 outline-none transition-all appearance-none"
                required
              >
                <option value="">{{ t('createRoute.selectPreference') }}</option>
                <option v-for="opt in preferenceOptions" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
              </select>
            </MotionBlock>
          </div>

          <MotionBlock :index="4" :delay="0.16">
            <label class="block text-sm font-medium text-tibet-dark/80 mb-2">
              {{ t('createRoute.routeContent') }} <span class="text-red-500">*</span>
            </label>
            <textarea 
              v-model="form.content" 
              rows="15"
              :placeholder="t('createRoute.contentPlaceholder')"
              class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 outline-none transition-all resize-y font-mono text-sm"
              required
            ></textarea>
          </MotionBlock>

          <MotionBlock
            class="flex flex-col-reverse justify-end gap-3 pt-4 border-t border-tibet-gold/25 sm:flex-row sm:gap-4"
            :index="5"
            :delay="0.16"
          >
            <motion.button
              type="button"
              @click="router.back()"
              class="px-6 py-3 rounded-xl border border-tibet-gold/25 hover:bg-gray-50 text-gray-700 font-medium transition-colors"
              :whileHover="{ y: -2 }"
              :whileTap="{ scale: 0.98 }"
            >
              {{ t('common.cancel') }}
            </motion.button>
            <motion.button
              type="submit" 
              :disabled="submitting"
              class="justify-center px-6 py-3 rounded-xl bg-tibet-gold hover:bg-tibet-gold/80 text-white font-medium transition-colors shadow-lg shadow-tibet-gold/20 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              :whileHover="submitting ? {} : { y: -2, scale: 1.01 }"
              :whileTap="submitting ? {} : { scale: 0.98 }"
            >
              <AnimatePresence mode="wait">
                <motion.span
                  v-if="submitting"
                  key="publishing"
                  class="flex items-center"
                  :initial="{ opacity: 0, y: 6 }"
                  :animate="{ opacity: 1, y: 0 }"
                  :exit="{ opacity: 0, y: -6 }"
                  :transition="{ duration: 0.18, ease: motionEase }"
                >
                  <motion.svg
                    class="-ml-1 mr-2 h-5 w-5 text-white"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    :animate="{ rotate: 360 }"
                    :transition="{ duration: 1, repeat: Infinity, ease: 'linear' }"
                  >
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </motion.svg>
                  {{ t('createRoute.publishing') }}
                </motion.span>
                <motion.span
                  v-else
                  key="publish"
                  :initial="{ opacity: 0, y: 6 }"
                  :animate="{ opacity: 1, y: 0 }"
                  :exit="{ opacity: 0, y: -6 }"
                  :transition="{ duration: 0.18, ease: motionEase }"
                >
                  {{ t('createRoute.publishToCommunity') }}
                </motion.span>
              </AnimatePresence>
            </motion.button>
          </MotionBlock>
        </form>
      </MotionBlock>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '../api'
import MotionBlock from '../components/motion/MotionBlock.vue'
import { motionEase } from '../motion/presets'
import { useAuthGuard } from '../composables/useAuthGuard'
import { showToast } from '../composables/useToast'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'

const { t } = useI18n()

const router = useRouter()
const { requireAuth } = useAuthGuard()

const form = ref({
  title: '',
  content: '',
  days: 7,
  budget: '',
  preference: ''
})

const submitting = ref(false)

const budgetOptions = computed(() => [
  { key: '经济型', label: t('routePlanner.budget.economy') },
  { key: '舒适型', label: t('routePlanner.budget.comfort') },
  { key: '豪华型', label: t('routePlanner.budget.luxury') }
])

const preferenceOptions = computed(() => [
  { key: '自然风光', label: t('routePlanner.preferenceOptions.natural') },
  { key: '人文历史', label: t('routePlanner.preferenceOptions.cultural') },
  { key: '深度摄影', label: t('routePlanner.preferenceOptions.photography') },
  { key: '休闲度假', label: t('routePlanner.preferenceOptions.relaxation') }
])

const submitRoute = async () => {
  if (!(await requireAuth())) return

  submitting.value = true
  try {
    await api.post('/routes/share', {
      title: form.value.title,
      content: form.value.content,
      days: form.value.days,
      budget: form.value.budget,
      preference: form.value.preference
    })
    showToast(t('createRoute.publishSuccess'), 'success')
    router.push('/community')
  } catch (error: any) {
    console.error('Failed to share route:', summarizeClientError(error))
    if (error.response && error.response.status === 401) {
      showToast(t('createRoute.loginExpired'), 'warning')
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    } else {
      const errorMsg = safeClientErrorMessage(error, t('createRoute.publishFailed'))
      showToast(errorMsg, 'error')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!(await requireAuth())) return
})
</script>
