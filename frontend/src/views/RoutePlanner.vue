<template>
  <div class="route-planner-shell min-h-screen relative overflow-hidden py-16 md:py-24">
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute -top-24 left-1/2 h-72 w-72 -translate-x-1/2 rounded-full bg-sky-400/15 blur-3xl animate-float-slow"></div>
      <div class="absolute top-40 -left-20 h-80 w-80 rounded-full bg-indigo-400/15 blur-3xl animate-float-medium"></div>
      <div class="absolute bottom-0 right-0 h-96 w-96 rounded-full bg-fuchsia-400/10 blur-3xl animate-float-slower"></div>
    </div>

    <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="grid gap-10 xl:grid-cols-[minmax(0,1.1fr)_420px] items-start">
        <section class="space-y-8 animate-fade-in xl:col-span-2">
          <div v-if="statusMessage || errorMessage" class="rounded-[1.75rem] border px-5 py-4 shadow-lg backdrop-blur-md"
               :class="errorMessage ? 'border-rose-200 bg-rose-50/90 text-rose-700' : 'border-emerald-200 bg-emerald-50/90 text-emerald-700'">
            <div class="flex items-start gap-3">
              <span class="mt-0.5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full"
                    :class="errorMessage ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'">
                <svg v-if="errorMessage" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M4.93 19h14.14c1.54 0 2.49-1.67 1.72-3L14.72 4c-.77-1.33-2.69-1.33-3.46 0L3.21 16c-.77 1.33.18 3 1.72 3z" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </span>
              <div class="min-w-0">
                <p class="text-sm font-semibold">{{ errorMessage ? '生成失败' : '状态提示' }}</p>
                <p class="mt-1 text-sm leading-6">{{ errorMessage || statusMessage }}</p>
              </div>
            </div>
          </div>

          <div class="hero-card glass rounded-[2rem] p-8 md:p-10 border border-white/60 shadow-2xl shadow-slate-900/5 relative overflow-hidden animate-fade-in-up" style="animation-delay: 0.12s">
            <div class="absolute inset-0 pointer-events-none opacity-70">
              <div class="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-amber-300 via-rose-400 to-red-500"></div>
              <div class="absolute -right-8 top-8 h-32 w-32 rounded-full bg-amber-300/20 blur-2xl"></div>
              <div class="absolute -left-10 bottom-0 h-40 w-40 rounded-full bg-sky-400/10 blur-2xl"></div>
            </div>

            <div class="relative flex flex-wrap items-center gap-3 mb-6 animate-fade-in-up" style="animation-delay: 0.05s">
              <span class="inline-flex items-center gap-2 rounded-full bg-white/70 px-4 py-2 text-sm font-medium text-apple-gray-700 border border-white/80 animate-pulse-soft">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                AI 行程生成器
              </span>
              <span class="inline-flex items-center rounded-full bg-sky-50 px-4 py-2 text-sm font-medium text-sky-700 border border-sky-100">
                西藏路线 · 个性化定制
              </span>
              <span class="inline-flex items-center rounded-full bg-amber-50 px-4 py-2 text-sm font-medium text-amber-700 border border-amber-100">
                布达拉宫 / 雪山 / 经幡灵感
              </span>
            </div>

            <div class="relative max-w-3xl">
              <div class="mb-4 flex items-center gap-3 text-xs font-medium tracking-[0.28em] text-amber-700 uppercase">
                <span class="h-px w-10 bg-gradient-to-r from-transparent via-amber-400 to-transparent"></span>
                Tibetan Journey
                <span class="h-px w-10 bg-gradient-to-r from-transparent via-amber-400 to-transparent"></span>
              </div>
              <h1 class="text-4xl md:text-5xl font-bold text-apple-gray-900 leading-tight">
                {{ t('routePlanner.title') }}
              </h1>
              <p class="mt-4 text-lg md:text-xl text-apple-gray-600 leading-relaxed">
                {{ t('routePlanner.subtitle') }}
              </p>
            </div>

            <div class="relative mt-8 grid gap-4 sm:grid-cols-3">
              <div class="rounded-2xl bg-white/75 border border-white/80 p-5 backdrop-blur-sm animate-card-rise">
                <p class="text-sm text-apple-gray-500">快速生成</p>
                <p class="mt-2 text-xl font-semibold text-apple-gray-900">一键出方案</p>
              </div>
              <div class="rounded-2xl bg-white/75 border border-white/80 p-5 backdrop-blur-sm">
                <p class="text-sm text-apple-gray-500">灵活配置</p>
                <p class="mt-2 text-xl font-semibold text-apple-gray-900">天数 / 预算 / 偏好</p>
              </div>
              <div class="rounded-2xl bg-white/75 border border-white/80 p-5 backdrop-blur-sm">
                <p class="text-sm text-apple-gray-500">便捷分享</p>
                <p class="mt-2 text-xl font-semibold text-apple-gray-900">保存到社区</p>
              </div>
            </div>
          </div>


        </section>

        <section v-if="result" class="xl:col-span-2">
          <div class="glass rounded-[2rem] p-8 md:p-12 shadow-xl border border-white/50">
            <div class="flex flex-col gap-6 mb-6 md:flex-row md:items-start md:justify-between">
              <div class="rounded-2xl border border-sky-200 bg-sky-50/90 px-4 py-3 text-sky-700">
                <p class="text-sm font-semibold">路线已生成</p>
                <p class="mt-1 text-sm leading-6">下面直接显示完整的 AI 行程结果。</p>
              </div>
              <button @click="copyResult" class="inline-flex items-center justify-center gap-2 self-start rounded-2xl border border-gray-200 bg-white/80 px-4 py-2.5 font-medium text-gray-700 transition-colors hover:bg-gray-50" :disabled="copying">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16h8M8 12h8m-7-8h5a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V6a2 2 0 012-2h1z" />
                </svg>
                {{ copying ? '复制中' : '复制文本' }}
              </button>
            </div>

            <div class="tibet-frame prose prose-lg max-w-none prose-headings:text-apple-gray-900 prose-p:text-apple-gray-600 prose-strong:text-apple-blue">
              <div v-html="renderedResult"></div>
            </div>

            <div class="mt-8 pt-8 border-t border-gray-200 flex flex-wrap justify-end gap-4">
              <button @click="generateRoute" class="inline-flex items-center justify-center rounded-2xl border border-gray-200 bg-white/80 px-6 py-3 font-medium text-gray-700 transition-colors hover:bg-gray-50" :disabled="loading">
                重新生成
              </button>
              <button @click="saveRoute" class="inline-flex items-center justify-center rounded-2xl border border-gray-200 bg-white/80 px-6 py-3 font-medium text-gray-700 transition-colors hover:bg-gray-50" :disabled="saving || !result">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                </svg>
                {{ saving ? t('routePlanner.saving') : t('routePlanner.saveRoute') }}
              </button>
              <button @click="shareRoute" class="inline-flex items-center justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 px-6 py-3 font-medium text-white shadow-lg shadow-blue-500/30 transition-all hover:from-blue-700 hover:to-indigo-700" :disabled="sharing || !result">
                 <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                </svg>
                {{ sharing ? t('routePlanner.sharing') : t('routePlanner.shareToCommunity') }}
              </button>
            </div>
          </div>
        </section>

        <aside class="sticky top-24">
          <div class="glass rounded-[2rem] p-8 animate-slide-up animate-fade-in-up border border-white/60 shadow-2xl shadow-slate-900/5 relative overflow-hidden" style="animation-delay: 0.1s">
            <div class="absolute inset-0 pointer-events-none">
              <div class="absolute -top-10 right-0 h-24 w-24 rounded-full bg-red-400/10 blur-2xl"></div>
              <div class="absolute bottom-0 left-0 h-28 w-28 rounded-full bg-amber-300/10 blur-2xl"></div>
            </div>
            <div class="relative flex items-start justify-between gap-4 mb-6">
              <div>
                <div class="mb-3 flex items-center gap-2 text-xs font-medium tracking-[0.24em] text-amber-700 uppercase">
                  <span class="h-px w-8 bg-amber-400"></span>
                  Sacred Himalayas
                </div>
                <h2 class="text-2xl font-bold text-apple-gray-900">AI 路线生成</h2>
                <p class="text-sm text-apple-gray-500 mt-2">选好天数、预算和偏好，一键生成可直接分享的西藏行程。</p>
              </div>
              <div class="hidden md:flex items-center gap-2 text-xs text-apple-gray-500 bg-apple-gray-50 px-3 py-2 rounded-full">
                <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                生成后可保存 / 分享
              </div>
            </div>

            <div v-if="loading || statusMessage || errorMessage || result" class="mb-6 rounded-[1.5rem] border p-4 animate-fade-in-up" :class="errorMessage ? 'border-rose-200 bg-rose-50/90' : loading ? 'border-sky-200 bg-sky-50/90' : 'border-emerald-200 bg-emerald-50/90'">
              <div class="flex items-start gap-3">
                <div class="mt-0.5 flex h-10 w-10 items-center justify-center rounded-2xl" :class="errorMessage ? 'bg-rose-100 text-rose-600' : loading ? 'bg-sky-100 text-sky-600' : 'bg-emerald-100 text-emerald-600'">
                  <svg v-if="loading" class="h-5 w-5 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <svg v-else-if="errorMessage" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M4.93 19h14.14c1.54 0 2.49-1.67 1.72-3L14.72 4c-.77-1.33-2.69-1.33-3.46 0L3.21 16c-.77 1.33.18 3 1.72 3z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm font-semibold text-apple-gray-900">{{ loading ? '生成中...' : errorMessage ? '生成失败' : result ? '结果已生成' : '状态提示' }}</p>
                  <p class="mt-1 text-sm leading-6" :class="errorMessage ? 'text-rose-600' : 'text-apple-gray-600'">
                    {{ errorMessage || statusMessage || '生成结果会直接显示在这里。' }}
                  </p>
                  <div v-if="loading" class="mt-4 space-y-3 animate-breath">
                    <div class="flex items-center justify-between text-xs text-sky-700">
                      <span class="transition-all duration-300">{{ typingProgressMessage }}</span>
                      <span>{{ Math.round(progress) }}%</span>
                    </div>
                    <div class="h-3 overflow-hidden rounded-full bg-sky-100 ring-1 ring-sky-200/70">
                      <div class="h-full rounded-full bg-gradient-to-r from-sky-500 via-indigo-500 to-purple-500 transition-all duration-700 ease-out" :style="{ width: `${progress}%` }"></div>
                    </div>
                    <div class="flex items-center justify-between text-[11px] text-sky-700/75">
                      <div class="flex flex-wrap gap-2">
                        <span class="rounded-full bg-white/70 px-3 py-1" :class="progress < 28 ? 'bg-sky-100 text-sky-800' : ''">解析需求</span>
                        <span class="rounded-full bg-white/70 px-3 py-1" :class="progress >= 28 && progress < 62 ? 'bg-sky-100 text-sky-800' : ''">规划景点</span>
                        <span class="rounded-full bg-white/70 px-3 py-1" :class="progress >= 62 ? 'bg-sky-100 text-sky-800' : ''">整理 Markdown</span>
                      </div>
                      <span class="whitespace-nowrap rounded-full bg-white/70 px-3 py-1">预计剩余 {{ estimatedRemainingTime }}</span>
                    </div>
                  </div>
                  <div v-if="result" class="mt-3 rounded-xl bg-white/70 p-3 text-sm text-apple-gray-700 border border-white/80">
                    <p class="font-medium text-apple-gray-900 mb-1">预览</p>
                    <p class="line-clamp-4 whitespace-pre-wrap">{{ result }}</p>
                  </div>
                </div>
              </div>
            </div>

            <form @submit.prevent="generateRoute" class="space-y-8">
              <div class="space-y-8">
                <div>
                  <label class="block text-sm font-medium text-apple-gray-700 mb-3">{{ t('routePlanner.plannedDays') }}</label>
                  <div class="flex items-center gap-4 rounded-2xl border border-white/70 bg-white/70 p-3">
                    <button type="button" @click="adjustDays(-1)" :disabled="loading || form.days <= 1"
                            class="flex h-11 w-11 items-center justify-center rounded-xl bg-apple-gray-100 text-xl text-apple-gray-600 transition-colors hover:bg-apple-gray-200 disabled:cursor-not-allowed disabled:opacity-40">
                      −
                    </button>
                    <div class="min-w-0 flex-1 text-center">
                      <span class="block text-3xl font-bold text-apple-gray-900 leading-none">{{ form.days }}</span>
                      <span class="mt-1 block text-xs uppercase tracking-[0.24em] text-apple-gray-400">Days</span>
                    </div>
                    <button type="button" @click="adjustDays(1)" :disabled="loading || form.days >= 30"
                            class="flex h-11 w-11 items-center justify-center rounded-xl bg-apple-gray-100 text-xl text-apple-gray-600 transition-colors hover:bg-apple-gray-200 disabled:cursor-not-allowed disabled:opacity-40">
                      +
                    </button>
                  </div>
                  <p class="mt-2 text-xs text-apple-gray-400">建议 5-8 天游览更均衡</p>
                </div>

                <div class="grid gap-6 sm:grid-cols-2 lg:grid-cols-1">
                  <div>
                    <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('routePlanner.budgetRange') }}</label>
                    <select v-model="form.budget" :disabled="loading" class="w-full rounded-2xl border border-gray-200 bg-white/80 px-4 py-3 outline-none transition-all appearance-none focus:border-apple-blue focus:ring-4 focus:ring-blue-100 disabled:opacity-60">
                      <option value="economy">{{ t('routePlanner.budget.economy') }}</option>
                      <option value="comfort">{{ t('routePlanner.budget.comfort') }}</option>
                      <option value="luxury">{{ t('routePlanner.budget.luxury') }}</option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('routePlanner.preference') }}</label>
                    <select v-model="form.preference" :disabled="loading" class="w-full rounded-2xl border border-gray-200 bg-white/80 px-4 py-3 outline-none transition-all appearance-none focus:border-apple-blue focus:ring-4 focus:ring-blue-100 disabled:opacity-60">
                      <option value="natural">{{ t('routePlanner.preferenceOptions.natural') }}</option>
                      <option value="cultural">{{ t('routePlanner.preferenceOptions.cultural') }}</option>
                      <option value="photography">{{ t('routePlanner.preferenceOptions.photography') }}</option>
                      <option value="relaxation">{{ t('routePlanner.preferenceOptions.relaxation') }}</option>
                    </select>
                  </div>
                </div>
              </div>

              <div>
                <p class="mb-3 text-sm font-medium text-apple-gray-700">快速预设</p>
                <div class="flex flex-wrap gap-3">
                  <button type="button" v-for="preset in presets" :key="preset.label" @click="applyPreset(preset)" :disabled="loading"
                          class="rounded-full border border-gray-200 bg-white/70 px-4 py-2 text-sm text-apple-gray-700 transition-all hover:-translate-y-0.5 hover:border-apple-blue hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-60 animate-chip-pop">
                    {{ preset.label }}
                  </button>
                </div>
              </div>

              <button type="submit" :disabled="loading"
                      class="w-full rounded-2xl bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 px-6 py-4 font-bold text-white shadow-xl shadow-blue-500/25 transition-all duration-300 hover:scale-[1.01] hover:from-blue-700 hover:via-indigo-700 hover:to-purple-700 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50 flex items-center justify-center animate-press-glow">
                <span v-if="loading" class="flex items-center">
                  <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  生成中...
                </span>
                <span v-else>{{ t('routePlanner.generateRoute') }}</span>
              </button>
            </form>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { marked } from 'marked'
import api from '../api'

const { t } = useI18n()
const router = useRouter()

window.addEventListener('auth-expired', () => {
  if (window.location.pathname !== '/login') alert(t('routePlanner.authFailed'))
})

const form = ref({
  days: 7,
  budget: 'comfort',
  preference: 'natural'
})

const presets = [
  { label: '自然风光', days: 6, budget: 'comfort', preference: 'natural' },
  { label: '人文历史', days: 5, budget: 'economy', preference: 'cultural' },
  { label: '深度摄影', days: 8, budget: 'luxury', preference: 'photography' },
  { label: '休闲度假', days: 7, budget: 'comfort', preference: 'relaxation' }
]

const loading = ref(false)
const saving = ref(false)
const sharing = ref(false)
const copying = ref(false)
const progress = ref(0)
const progressTimer = ref<number | null>(null)
const typingTimer = ref<number | null>(null)
const typingProgressMessage = ref('正在生成路线')
const estimatedRemainingTime = computed(() => {
  if (!loading.value) return '0 秒'
  if (progress.value < 20) return '18 秒左右'
  if (progress.value < 40) return '14 秒左右'
  if (progress.value < 60) return '10 秒左右'
  if (progress.value < 80) return '6 秒左右'
  return '3 秒左右'
})
const result = ref('')
const statusMessage = ref('')
const errorMessage = ref('')

const renderedResult = computed(() => marked.parse(result.value || ''))

const clearProgressTimer = () => {
  if (progressTimer.value !== null) {
    window.clearInterval(progressTimer.value)
    progressTimer.value = null
  }
}

const startTypingProgress = () => {
  if (typingTimer.value !== null) {
    window.clearInterval(typingTimer.value)
  }
  const messages = [
    '正在解析预算和偏好',
    '正在规划景点和路线',
    '正在整理 Markdown 结果',
    '即将完成生成'
  ]
  let index = 0
  typingProgressMessage.value = messages[index]
  typingTimer.value = window.setInterval(() => {
    if (!loading.value) {
      if (typingTimer.value !== null) {
        window.clearInterval(typingTimer.value)
        typingTimer.value = null
      }
      return
    }
    index = (index + 1) % messages.length
    typingProgressMessage.value = messages[index]
  }, 1800)
}

const startProgress = () => {
  clearProgressTimer()
  typingProgressMessage.value = '正在解析预算和偏好'
  startTypingProgress()
  progress.value = 6
  progressTimer.value = window.setInterval(() => {
    if (!loading.value) {
      clearProgressTimer()
      return
    }
    const increment = progress.value < 30 ? 2 : progress.value < 65 ? 1.5 : 0.8
    progress.value = Math.min(94, Number((progress.value + increment).toFixed(1)))
  }, 700)
}

const finishProgress = () => {
  clearProgressTimer()
  if (typingTimer.value !== null) {
    window.clearInterval(typingTimer.value)
    typingTimer.value = null
  }
  progress.value = 100
  typingProgressMessage.value = '已完成生成'
  window.setTimeout(() => {
    if (!loading.value) {
      progress.value = 0
      typingProgressMessage.value = '正在生成路线'
    }
  }, 600)
}

const adjustDays = (delta: number) => {
  form.value.days = Math.min(30, Math.max(1, form.value.days + delta))
}

const applyPreset = (preset: { label: string; days: number; budget: string; preference: string }) => {
  if (loading.value) return
  form.value = {
    days: preset.days,
    budget: preset.budget,
    preference: preset.preference
  }
}

const generateRoute = async () => {
  loading.value = true
  result.value = ''
  copying.value = false
  errorMessage.value = ''
  statusMessage.value = '正在生成路线，请稍候…'
  startProgress()

  try {
    const response = await api.post('/routes/generate', form.value)
    const content = response.data?.content ?? response.data
    result.value = typeof content === 'string' ? content : JSON.stringify(content, null, 2)
    statusMessage.value = '路线生成成功，可以继续保存或分享。'
  } catch (error: any) {
    console.error('Failed to generate route:', error)

    let message = t('routePlanner.generateFailed')

    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      message = t('routePlanner.timeoutError')
    } else if (error.response) {
      const status = error.response.status
      const backendError = error.response.data?.detail || error.response.data?.error || error.response.data?.message || ''
      if (status === 500) {
        message = backendError || t('routePlanner.serviceUnavailable')
      } else if (status === 401) {
        const serverMessage = String(backendError).toLowerCase()
        if (serverMessage.includes('expired') || serverMessage.includes('invalid') || serverMessage.includes('authentication')) {
          message = t('routePlanner.authFailed')
        } else {
          message = backendError || t('routePlanner.authFailed')
        }
      } else {
        message = backendError || t('routePlanner.serverError', { status })
      }
    } else if (error.request) {
      message = t('routePlanner.connectionError')
    }

    errorMessage.value = message
    statusMessage.value = '路线生成失败，请检查后重试。'
  } finally {
    loading.value = false
    finishProgress()
  }
}

const copyResult = async () => {
  if (!result.value || copying.value) return
  copying.value = true
  try {
    await navigator.clipboard.writeText(result.value)
    statusMessage.value = '路线内容已复制到剪贴板。'
    errorMessage.value = ''
  } catch (error) {
    console.error('Copy failed:', error)
    errorMessage.value = '复制失败，请手动选择文本复制。'
  } finally {
    copying.value = false
  }
}

const saveRoute = () => {
  const blob = new Blob([result.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = t('routePlanner.downloadFilename', { days: form.value.days })
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const getPreferenceText = (key: string) => {
  return t(`routePlanner.preferenceOptions.${key}`)
}

const shareRoute = async () => {
  if (!result.value) return

  const userStr = localStorage.getItem('user')
  let hasToken = false
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      hasToken = !!(user?.token || user?.accessToken || user?.jwt || user?.data?.token || user?.data?.accessToken)
    } catch (e) {
      console.error('Failed to parse user info:', e)
    }
  }

  if (!hasToken) {
    if (confirm(t('routePlanner.loginRequired'))) {
      router.push('/login')
    }
    return
  }

  sharing.value = true
  try {
    await api.post('/routes/share', {
      title: t('routePlanner.routeTitle', { days: form.value.days, preference: getPreferenceText(form.value.preference) }),
      content: result.value,
      days: form.value.days,
      budget: form.value.budget,
      preference: form.value.preference
    })
    alert(t('routePlanner.shareSuccess'))
    router.push('/community')
  } catch (error: any) {
    console.error('Share failed:', error)
    if (error.response && error.response.status === 401) {
      if (confirm(t('routePlanner.loginExpired'))) {
        router.push('/login')
      }
    } else {
      alert(t('routePlanner.shareFailed'))
    }
  } finally {
    sharing.value = false
  }
}

onBeforeUnmount(() => {
  clearProgressTimer()
})
</script>

<style scoped>
@keyframes floatSlow {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(0, -18px, 0) scale(1.03); }
}

@keyframes floatMedium {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(10px, -12px, 0) scale(1.02); }
}

@keyframes floatSlower {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-8px, -10px, 0) scale(1.04); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translate3d(0, 18px, 0); }
  to { opacity: 1; transform: translate3d(0, 0, 0); }
}

@keyframes cardRise {
  from { opacity: 0; transform: translate3d(0, 14px, 0) scale(0.985); }
  to { opacity: 1; transform: translate3d(0, 0, 0) scale(1); }
}

@keyframes chipPop {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes pressGlow {
  0%, 100% { box-shadow: 0 18px 40px rgba(59, 130, 246, 0.24); }
  50% { box-shadow: 0 20px 48px rgba(79, 70, 229, 0.34); }
}

@keyframes pulseSoft {
  0%, 100% { opacity: 0.82; }
  50% { opacity: 1; }
}

.animate-float-slow { animation: floatSlow 9s ease-in-out infinite; }
.animate-float-medium { animation: floatMedium 8s ease-in-out infinite; }
.animate-float-slower { animation: floatSlower 11s ease-in-out infinite; }
.animate-fade-in-up { animation: fadeInUp 0.7s cubic-bezier(0.22, 1, 0.36, 1) both; }
.animate-card-rise { animation: cardRise 0.75s cubic-bezier(0.22, 1, 0.36, 1) both; }
.animate-chip-pop { animation: chipPop 0.65s cubic-bezier(0.16, 1, 0.3, 1) both; }
.animate-press-glow { animation: pressGlow 3.5s ease-in-out infinite; }
.animate-pulse-soft { animation: pulseSoft 2.4s ease-in-out infinite; }

.route-planner-shell {
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.route-planner-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(135deg, rgba(180, 83, 9, 0.04) 25%, transparent 25%, transparent 50%, rgba(180, 83, 9, 0.04) 50%, rgba(180, 83, 9, 0.04) 75%, transparent 75%, transparent),
    linear-gradient(45deg, rgba(14, 165, 233, 0.025) 25%, transparent 25%, transparent 50%, rgba(14, 165, 233, 0.025) 50%, rgba(14, 165, 233, 0.025) 75%, transparent 75%, transparent);
  background-size: 64px 64px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.35), transparent 75%);
}

.hero-card {
  position: relative;
  border-radius: 2rem;
}

.hero-card::after {
  content: '';
  position: absolute;
  left: 1.5rem;
  right: 1.5rem;
  bottom: 1rem;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(180, 83, 9, 0.18), rgba(220, 38, 38, 0.12), rgba(14, 165, 233, 0.18));
  filter: blur(8px);
  opacity: 0.75;
}

.tibet-frame {
  position: relative;
  padding: 1.25rem;
  border-radius: 1.75rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.72));
  border: 1px solid rgba(251, 191, 36, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.tibet-frame::before,
.tibet-frame::after {
  content: '';
  position: absolute;
  inset: 0.75rem;
  border-radius: 1.3rem;
  border: 1px solid rgba(180, 83, 9, 0.12);
  pointer-events: none;
}

.tibet-frame::after {
  inset: 1.15rem;
  border-style: dashed;
  opacity: 0.45;
}

@media (max-width: 640px) {
  .route-planner-shell::before {
    background-size: 48px 48px;
  }

  .tibet-frame {
    padding: 1rem;
  }
}
</style>
