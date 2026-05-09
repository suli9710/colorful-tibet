<template>
  <div class="route-planner-page min-h-screen relative overflow-hidden">
    <!-- Background ambient orbs -->
    <div class="absolute inset-0 pointer-events-none overflow-hidden">
      <div class="absolute -top-32 left-1/2 -translate-x-1/2 w-[600px] h-[600px] rounded-full bg-gradient-to-b from-sky-400/12 via-indigo-400/8 to-transparent blur-3xl"></div>
      <div class="absolute top-1/3 -right-32 w-80 h-80 rounded-full bg-amber-300/8 blur-3xl"></div>
      <div class="absolute bottom-0 left-0 w-96 h-96 rounded-full bg-rose-300/6 blur-3xl"></div>
    </div>

    <div class="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12 md:py-20">
      <!-- ===== HEADER ===== -->
      <header class="text-center mb-12 animate-fade-in">
        <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/60 border border-white/80 backdrop-blur text-xs font-semibold text-tibet-red tracking-widest uppercase mb-6">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
          {{ t('routePlanner.aiGeneratorLabel') }}
        </div>
        <h1 class="text-4xl md:text-5xl lg:text-6xl font-bold text-tibet-dark tracking-tight leading-tight">
          {{ t('routePlanner.title') }}
        </h1>
        <p class="mt-4 text-lg text-tibet-brown/70 max-w-2xl mx-auto leading-relaxed">
          {{ t('routePlanner.subtitle') }}
        </p>
        <!-- Tibetan ornament divider -->
        <div class="mt-6 flex items-center justify-center gap-3">
          <span class="h-px w-12 bg-gradient-to-r from-transparent to-tibet-gold/50"></span>
          <span class="text-tibet-gold/60 text-sm">✦</span>
          <span class="h-px w-12 bg-gradient-to-l from-transparent to-tibet-gold/50"></span>
        </div>
      </header>

      <!-- ===== STATUS / ERROR BANNER ===== -->
      <transition name="fade-slide">
        <div v-if="statusMessage || errorMessage" class="mb-8 animate-fade-in">
          <div class="mx-auto max-w-3xl rounded-2xl border px-5 py-4 backdrop-blur-md"
               :class="errorMessage ? 'border-rose-200 bg-rose-50/90 text-rose-700' : 'border-emerald-200 bg-emerald-50/90 text-emerald-700'">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl"
                    :class="errorMessage ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'">
                <svg v-if="errorMessage" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M4.93 19h14.14c1.54 0 2.49-1.67 1.72-3L14.72 4c-.77-1.33-2.69-1.33-3.46 0L3.21 16c-.77 1.33.18 3 1.72 3z" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </span>
              <div class="min-w-0">
                <p class="text-sm font-semibold">{{ errorMessage ? t('routePlanner.generationFailed') : t('routePlanner.statusHint') }}</p>
                <p class="mt-0.5 text-sm">{{ errorMessage || statusMessage }}</p>
              </div>
            </div>
          </div>
        </div>
      </transition>

      <!-- ===== MAIN CONTENT ===== -->
      <div class="grid gap-8 lg:grid-cols-5 items-start">
        <!-- LEFT: Form (takes 2 cols on lg) -->
        <div class="lg:col-span-2">
          <div class="sticky top-20 space-y-6">
            <!-- Form Card -->
            <div class="glass-card rounded-3xl border border-white/50 shadow-xl shadow-slate-900/3 overflow-hidden animate-fade-in-up">
              <!-- Card header -->
              <div class="px-6 py-5 border-b border-white/60 bg-white/40">
                <div class="flex items-center gap-3">
                  <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-tibet-red/10 to-tibet-gold/10 text-tibet-red">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l5.447 2.724A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                    </svg>
                  </span>
                  <div>
                    <h2 class="text-lg font-bold text-tibet-dark">{{ t('routePlanner.aiRouteGenTitle') }}</h2>
                    <p class="text-xs text-tibet-brown/50">{{ t('routePlanner.aiRouteGenDesc') }}</p>
                  </div>
                </div>
              </div>

              <form @submit.prevent="generateRoute" class="p-6 space-y-6">
                <!-- Days Selector -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.plannedDays') }}</label>
                  <div class="flex items-stretch gap-0 rounded-2xl border border-tibet-gold/25 bg-white/70 overflow-hidden">
                    <button type="button" @click="adjustDays(-1)" :disabled="loading || form.days <= 1"
                            class="flex items-center justify-center w-12 shrink-0 text-xl text-tibet-brown/70 hover:bg-gray-50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/></svg>
                    </button>
                    <div class="flex-1 flex flex-col items-center justify-center py-3 border-x border-tibet-gold/20">
                      <span class="text-4xl font-bold text-tibet-dark leading-none tabular-nums">{{ form.days }}</span>
                      <span class="text-[10px] uppercase tracking-[0.2em] text-tibet-brown/50 mt-1">Days</span>
                    </div>
                    <button type="button" @click="adjustDays(1)" :disabled="loading || form.days >= 30"
                            class="flex items-center justify-center w-12 shrink-0 text-xl text-tibet-brown/70 hover:bg-gray-50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                    </button>
                  </div>
                  <p class="mt-2 text-xs text-tibet-brown/50">{{ t('routePlanner.suggestedDays') }}</p>
                </div>

                <!-- Budget: card-style radio -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.budgetRange') }}</label>
                  <div class="grid gap-2">
                    <label v-for="opt in budgetOptions" :key="opt.value"
                           @click="form.budget = opt.value"
                           class="relative flex items-center gap-3 rounded-xl border-2 cursor-pointer transition-all duration-200 px-4 py-3"
                           :class="form.budget === opt.value
                             ? 'border-tibet-gold bg-amber-50/60 shadow-sm'
                             : 'border-tibet-gold/20 bg-white/60 hover:border-tibet-gold/25 hover:bg-white'">
                      <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm"
                            :class="form.budget === opt.value ? 'bg-tibet-gold/15 text-tibet-gold' : 'bg-gray-100 text-gray-400'">
                        {{ opt.icon }}
                      </span>
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-semibold" :class="form.budget === opt.value ? 'text-tibet-dark' : 'text-tibet-brown/80'">{{ opt.label }}</p>
                        <p class="text-xs text-tibet-brown/50 mt-0.5">{{ opt.desc }}</p>
                      </div>
                      <span v-if="form.budget === opt.value" class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-tibet-gold text-white">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                      </span>
                    </label>
                  </div>
                </div>

                <!-- Preference: card-style radio -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.preference') }}</label>
                  <div class="grid gap-2">
                    <label v-for="opt in preferenceOptions" :key="opt.value"
                           @click="form.preference = opt.value"
                           class="relative flex items-center gap-3 rounded-xl border-2 cursor-pointer transition-all duration-200 px-4 py-3"
                           :class="form.preference === opt.value
                             ? 'border-tibet-blue bg-blue-50/60 shadow-sm'
                             : 'border-tibet-gold/20 bg-white/60 hover:border-tibet-gold/25 hover:bg-white'">
                      <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm"
                            :class="form.preference === opt.value ? 'bg-tibet-blue/15 text-tibet-blue' : 'bg-gray-100 text-gray-400'">
                        {{ opt.icon }}
                      </span>
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-semibold" :class="form.preference === opt.value ? 'text-tibet-dark' : 'text-tibet-brown/80'">{{ opt.label }}</p>
                        <p class="text-xs text-tibet-brown/50 mt-0.5">{{ opt.desc }}</p>
                      </div>
                      <span v-if="form.preference === opt.value" class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-tibet-blue text-white">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                      </span>
                    </label>
                  </div>
                </div>

                <!-- Quick Presets -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.quickPresets') }}</label>
                  <div class="grid grid-cols-2 gap-2">
                    <button type="button" v-for="preset in presets" :key="preset.label" @click="applyPreset(preset)" :disabled="loading"
                            class="group flex flex-col items-start gap-0.5 rounded-xl border border-tibet-gold/20 bg-white/60 px-3.5 py-3 text-left transition-all hover:border-tibet-gold/40 hover:bg-amber-50/40 hover:shadow-sm disabled:opacity-40 disabled:cursor-not-allowed">
                      <span class="text-xs font-semibold text-tibet-dark/80 group-hover:text-tibet-dark">{{ preset.label }}</span>
                      <span class="text-[11px] text-tibet-brown/50">{{ preset.days }}天 · {{ getBudgetShort(preset.budget) }}</span>
                    </button>
                  </div>
                </div>

                <!-- Generate Button -->
                <button type="submit" :disabled="loading"
                        class="w-full rounded-2xl bg-gradient-to-r from-tibet-red via-rose-600 to-tibet-gold px-6 py-4 font-bold text-white shadow-lg shadow-tibet-red/20 transition-all duration-300 hover:shadow-xl hover:shadow-tibet-red/25 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100 flex items-center justify-center gap-3">
                  <span v-if="loading" class="flex items-center gap-2">
                    <svg class="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    {{ t('routePlanner.aiPlanning') }}
                  </span>
                  <span v-else class="flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                    {{ t('routePlanner.generateRoute') }}
                  </span>
                </button>
              </form>
            </div>

            <!-- Streaming Indicator -->
            <transition name="fade-slide">
              <div v-if="loading" class="glass-card rounded-2xl p-5 border border-sky-200/60 bg-sky-50/70 animate-fade-in-up">
                <div class="flex items-center gap-3 mb-3">
                  <span class="relative flex h-3 w-3">
                    <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-sky-400 opacity-75"></span>
                    <span class="relative inline-flex rounded-full h-3 w-3 bg-sky-500"></span>
                  </span>
                  <span class="text-sm font-semibold text-sky-700">{{ streaming ? t('routePlanner.aiStreamingLabel') : t('routePlanner.preparingLabel') }}</span>
                  <span v-if="charCount > 0" class="ml-auto text-xs text-sky-500 font-mono">{{ charCount }} {{ t('routePlanner.charCountUnit') }}</span>
                </div>
                <div class="h-1.5 rounded-full bg-sky-100 overflow-hidden">
                  <div class="h-full rounded-full bg-gradient-to-r from-sky-400 via-indigo-400 to-purple-400 animate-shimmer-stream" :class="{ 'w-full': !streaming, 'animate-pulse': streaming }"></div>
                </div>
                <p class="mt-2 text-xs text-sky-600/70">{{ t('routePlanner.waitingTime') }}</p>
              </div>
            </transition>
          </div>
        </div>

        <!-- RIGHT: Results (takes 3 cols on lg) -->
        <div class="lg:col-span-3">
          <!-- Empty state -->
          <div v-if="!result && !loading" class="flex flex-col items-center justify-center py-20 text-center animate-fade-in">
            <div class="relative mb-8">
              <div class="w-32 h-32 rounded-3xl bg-gradient-to-br from-tibet-red/5 via-tibet-gold/5 to-tibet-blue/5 border border-tibet-gold/10 flex items-center justify-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-tibet-gold/30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l5.447 2.724A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                </svg>
              </div>
              <div class="absolute -bottom-2 -right-2 w-10 h-10 rounded-xl bg-tibet-gold/10 flex items-center justify-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-tibet-gold/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
            </div>
            <h3 class="text-xl font-bold text-tibet-brown/50 mb-2">定制您的西藏之旅</h3>
            <p class="text-sm text-tibet-brown/40 max-w-sm">在左侧选择天数、预算和偏好，然后点击生成按钮，AI 将为您规划专属行程。</p>
          </div>

          <!-- Result card -->
          <transition name="fade-slide">
            <div v-if="result" class="glass-card rounded-3xl border border-white/50 shadow-xl shadow-slate-900/3 overflow-hidden animate-fade-in-up">
              <!-- Result header -->
              <div class="px-6 py-5 border-b border-white/60 bg-white/40 flex flex-wrap items-center justify-between gap-3">
                <div class="flex items-center gap-3">
                  <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </span>
                  <div>
                    <p class="text-sm font-bold text-tibet-dark">{{ t('routePlanner.routeGenerated') }}</p>
                    <p class="text-xs text-tibet-brown/50">{{ t('routePlanner.routeGeneratedDesc') }}</p>
                  </div>
                </div>
                <div class="flex gap-1.5">
                  <button @click="copyResult" :disabled="copying"
                          class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-3.5 py-2 text-xs font-medium text-gray-600 transition-all hover:bg-gray-50 hover:border-gray-300 disabled:opacity-50">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16h8M8 12h8m-7-8h5a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V6a2 2 0 012-2h1z" />
                    </svg>
                    {{ copying ? t('routePlanner.copying') : t('routePlanner.copyText') }}
                  </button>
                </div>
              </div>

              <!-- Markdown content -->
              <div class="p-6 md:p-8 lg:p-10">
                <div class="prose prose-slate max-w-none prose-headings:text-tibet-dark prose-p:text-tibet-brown/80 prose-p:leading-relaxed prose-a:text-tibet-blue prose-strong:text-tibet-dark/90 prose-li:text-tibet-brown/80 prose-h2:border-b prose-h2:border-tibet-gold/20 prose-h2:pb-2 prose-h2:mt-8 prose-h2:mb-4 prose-img:rounded-2xl prose-img:shadow-md" v-html="renderedResult"></div>
              </div>

              <!-- Result actions -->
              <div class="px-6 py-4 border-t border-white/60 bg-white/30 flex flex-wrap items-center justify-end gap-2">
                <button @click="generateRoute" :disabled="loading"
                        class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  {{ t('routePlanner.regenerate') }}
                </button>
                <button @click="saveRoute" :disabled="saving || !result"
                        class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                  </svg>
                  {{ saving ? t('routePlanner.saving') : t('routePlanner.saveRoute') }}
                </button>
                <button @click="shareRoute" :disabled="sharing || !result"
                        class="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-tibet-blue to-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-tibet-blue/20 transition-all hover:from-tibet-blue/90 hover:to-indigo-700 hover:shadow-lg disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                  </svg>
                  {{ sharing ? t('routePlanner.sharing') : t('routePlanner.shareToCommunity') }}
                </button>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, shallowRef, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { marked } from 'marked'
import { generateRouteStream } from '../api/stream'
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

const presets = computed(() => [
  { label: t('routePlanner.presetNatural'), days: 6, budget: 'comfort', preference: 'natural' },
  { label: t('routePlanner.presetCultural'), days: 5, budget: 'economy', preference: 'cultural' },
  { label: t('routePlanner.presetPhotography'), days: 8, budget: 'luxury', preference: 'photography' },
  { label: t('routePlanner.presetRelaxation'), days: 7, budget: 'comfort', preference: 'relaxation' }
])

const budgetOptions = computed(() => [
  { value: 'economy', label: t('routePlanner.budget.economy'), desc: '经济实惠的住宿与交通', icon: '💰' },
  { value: 'comfort', label: t('routePlanner.budget.comfort'), desc: '性价比高的舒适体验', icon: '⭐' },
  { value: 'luxury', label: t('routePlanner.budget.luxury'), desc: '高端酒店与专车服务', icon: '💎' }
])

const preferenceOptions = computed(() => [
  { value: 'natural', label: t('routePlanner.preferenceOptions.natural'), desc: '雪山圣湖 · 自然奇观', icon: '🏔️' },
  { value: 'cultural', label: t('routePlanner.preferenceOptions.cultural'), desc: '寺庙古迹 · 文化深度', icon: '🏛️' },
  { value: 'photography', label: t('routePlanner.preferenceOptions.photography'), desc: '日照金山 · 光影秘境', icon: '📷' },
  { value: 'relaxation', label: t('routePlanner.preferenceOptions.relaxation'), desc: '林芝氧吧 · 身心放松', icon: '🌿' }
])

const getBudgetShort = (key: string) => {
  if (key === 'economy') return '经济型'
  if (key === 'comfort') return '舒适型'
  if (key === 'luxury') return '豪华型'
  return key
}

const loading = ref(false)
const saving = ref(false)
const sharing = ref(false)
const copying = ref(false)
const streaming = ref(false)
const charCount = ref(0)
const streamAbortController = ref<AbortController | null>(null)
const result = shallowRef('')
const statusMessage = ref('')
const errorMessage = ref('')

const renderedResult = computed(() => marked.parse(result.value || ''))

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
  streaming.value = true
  result.value = ''
  charCount.value = 0
  copying.value = false
  errorMessage.value = ''
  statusMessage.value = t('routePlanner.aiWritingHint')

  const controller = new AbortController()
  streamAbortController.value = controller

  try {
    await generateRouteStream(
      form.value,
      {
        onMeta: (meta) => {
          statusMessage.value = t('routePlanner.aiGeneratingRoute', { days: meta.days, pref: meta.preference })
        },
        onDelta: (text: string) => {
          if (result.value && text.startsWith(result.value)) {
            result.value = text
          } else {
            result.value += text
          }
          charCount.value = result.value.length
        },
        onDone: (fullText) => {
          if (fullText && fullText.length > result.value.length) {
            result.value = fullText
          }
          charCount.value = result.value.length
          streaming.value = false
          loading.value = false
          if (fullText && fullText.trim().length > 0) {
            statusMessage.value = t('routePlanner.routeGenComplete', { chars: fullText.trim().length })
          } else {
            errorMessage.value = t('routePlanner.aiEmptyResult')
            statusMessage.value = t('routePlanner.routeGenFailedStatus')
          }
        },
        onError: (message) => {
          errorMessage.value = message
          statusMessage.value = '路线生成失败，请检查后重试。'
          streaming.value = false
          loading.value = false
        },
      },
      controller.signal
    )
  } catch (error: any) {
    console.error('Failed to generate route:', error)
    let message = t('routePlanner.generateFailed')
    if (error.name === 'AbortError') {
      message = t('routePlanner.generationCancelled')
    } else if (error.message?.includes('timeout')) {
      message = t('routePlanner.timeoutError')
    }
    errorMessage.value = message
    statusMessage.value = t('routePlanner.routeGenFailedStatus')
  } finally {
    loading.value = false
    streaming.value = false
    streamAbortController.value = null
  }
}

const copyResult = async () => {
  if (!result.value || copying.value) return
  copying.value = true
  try {
    await navigator.clipboard.writeText(result.value)
    statusMessage.value = t('routePlanner.copiedToClipboard')
    errorMessage.value = ''
  } catch (error) {
    console.error('Copy failed:', error)
    errorMessage.value = t('routePlanner.copyFailed')
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
  if (streamAbortController.value) {
    streamAbortController.value.abort()
  }
})
</script>

<style scoped>
/* ===== Animations ===== */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes shimmerStream {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.animate-fade-in {
  animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.animate-fade-in-up {
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.animate-shimmer-stream {
  background-size: 200% 100%;
  animation: shimmerStream 2s linear infinite;
}

/* ===== Transitions ===== */
.fade-slide-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.fade-slide-leave-active {
  transition: all 0.25s ease-in;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ===== Glass Card ===== */
.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}

/* ===== Page Background ===== */
.route-planner-page {
  background:
    radial-gradient(ellipse at top, rgba(14, 165, 233, 0.06), transparent 50%),
    radial-gradient(ellipse at bottom left, rgba(245, 158, 11, 0.04), transparent 40%),
    linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
}

/* ===== Prose overrides ===== */
.prose {
  --tw-prose-body: #4b5563;
  --tw-prose-headings: #121212;
  --tw-prose-links: #2D5F8A;
  --tw-prose-bold: #1d1d1f;
}

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .route-planner-page {
    background:
      radial-gradient(ellipse at top, rgba(14, 165, 233, 0.04), transparent 40%),
      linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  }
}
</style>
