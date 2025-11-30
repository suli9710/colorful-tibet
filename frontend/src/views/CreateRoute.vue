<template>
  <div class="min-h-screen bg-apple-gray-50 py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="text-center mb-12 animate-fade-in">
        <h1 class="text-4xl font-bold text-apple-gray-900 mb-4">{{ t('createRoute.title') }}</h1>
        <p class="text-lg text-apple-gray-500">
          {{ t('createRoute.subtitle') }}
        </p>
      </div>

      <!-- Form Section -->
      <div class="glass rounded-3xl p-8 md:p-12 mb-12 animate-slide-up shadow-xl border border-white/50">
        <form @submit.prevent="submitRoute" class="space-y-8">
          <!-- Title -->
          <div>
            <label class="block text-sm font-medium text-apple-gray-700 mb-2">
              {{ t('createRoute.routeTitle') }} <span class="text-red-500">*</span>
            </label>
            <input 
              v-model="form.title" 
              type="text" 
              :placeholder="t('createRoute.titlePlaceholder')"
              class="w-full px-4 py-3 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none transition-all"
              required
              maxlength="200"
            />
            <p class="mt-1 text-xs text-gray-500">{{ form.title.length }}/200</p>
          </div>

          <!-- Days, Budget, Preference -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <!-- Days -->
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">
                {{ t('createRoute.plannedDays') }} <span class="text-red-500">*</span>
              </label>
              <div class="flex items-center space-x-4">
                <button 
                  type="button" 
                  @click="form.days > 1 && form.days--" 
                  class="w-10 h-10 rounded-full bg-apple-gray-100 hover:bg-apple-gray-200 flex items-center justify-center text-apple-gray-600 transition-colors"
                >
                  -
                </button>
                <span class="text-xl font-bold text-apple-gray-900 w-8 text-center">{{ form.days }}</span>
                <button 
                  type="button" 
                  @click="form.days < 30 && form.days++" 
                  class="w-10 h-10 rounded-full bg-apple-gray-100 hover:bg-apple-gray-200 flex items-center justify-center text-apple-gray-600 transition-colors"
                >
                  +
                </button>
              </div>
            </div>

            <!-- Budget -->
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">
                {{ t('createRoute.budgetRange') }} <span class="text-red-500">*</span>
              </label>
              <select 
                v-model="form.budget" 
                class="w-full px-4 py-3 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none transition-all appearance-none"
                required
              >
                <option value="">{{ t('createRoute.selectBudget') }}</option>
                <option value="经济型">{{ t('routePlanner.budget.economy') }}</option>
                <option value="舒适型">{{ t('routePlanner.budget.comfort') }}</option>
                <option value="豪华型">{{ t('routePlanner.budget.luxury') }}</option>
              </select>
            </div>

            <!-- Preference -->
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">
                {{ t('createRoute.preference') }} <span class="text-red-500">*</span>
              </label>
              <select 
                v-model="form.preference" 
                class="w-full px-4 py-3 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none transition-all appearance-none"
                required
              >
                <option value="">{{ t('createRoute.selectPreference') }}</option>
                <option value="自然风光">{{ t('routePlanner.preferenceOptions.natural') }}</option>
                <option value="人文历史">{{ t('routePlanner.preferenceOptions.cultural') }}</option>
                <option value="深度摄影">{{ t('routePlanner.preferenceOptions.photography') }}</option>
                <option value="休闲度假">{{ t('routePlanner.preferenceOptions.relaxation') }}</option>
              </select>
            </div>
          </div>

          <!-- Content -->
          <div>
            <label class="block text-sm font-medium text-apple-gray-700 mb-2">
              {{ t('createRoute.routeContent') }} <span class="text-red-500">*</span>
            </label>
            <textarea 
              v-model="form.content" 
              rows="15"
              :placeholder="t('createRoute.contentPlaceholder')"
              class="w-full px-4 py-3 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none transition-all resize-y font-mono text-sm"
              required
            ></textarea>
            <p class="mt-1 text-xs text-gray-500">
              {{ t('createRoute.markdownHint') }}
            </p>
          </div>

          <!-- Buttons -->
          <div class="flex justify-end gap-4 pt-4 border-t border-gray-200">
            <button 
              type="button"
              @click="router.back()"
              class="px-6 py-3 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-700 font-medium transition-colors"
            >
              {{ t('common.cancel') }}
            </button>
            <button 
              type="submit" 
              :disabled="submitting"
              class="px-6 py-3 rounded-xl bg-apple-blue hover:bg-apple-blue-hover text-white font-medium transition-colors shadow-lg shadow-blue-500/30 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
            >
              <span v-if="submitting" class="flex items-center">
                <svg class="animate-spin -ml-1 mr-2 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ t('createRoute.publishing') }}
              </span>
              <span v-else>{{ t('createRoute.publishToCommunity') }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '../api'

const { t } = useI18n()

const router = useRouter()

const form = ref({
  title: '',
  content: '',
  days: 7,
  budget: '',
  preference: ''
})

const submitting = ref(false)

const submitRoute = async () => {
  // 检查是否登录
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    if (confirm(t('createRoute.loginRequired'))) {
      router.push('/login')
    }
    return
  }

  submitting.value = true
  try {
    await api.post('/routes/share', {
      title: form.value.title,
      content: form.value.content,
      days: form.value.days,
      budget: form.value.budget,
      preference: form.value.preference
    })
    alert(t('createRoute.publishSuccess'))
    router.push('/community')
  } catch (error: any) {
    console.error('Failed to share route:', error)
    if (error.response && error.response.status === 401) {
      if (confirm(t('createRoute.loginExpired'))) {
        router.push('/login')
      }
    } else {
      const errorMsg = error.response?.data?.error || t('createRoute.publishFailed')
      alert(errorMsg)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 检查登录状态
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    if (confirm(t('createRoute.loginRequired'))) {
      router.push('/login')
    }
  }
})
</script>




