<template>
  <div class="route-planner-page tibet-page-shell min-h-screen relative overflow-hidden">
    <div class="planner-workspace relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 md:py-10">
      <header class="planner-heading">
        <div>
          <p class="planner-eyebrow">{{ t('plannerFlow.eyebrow') }}</p>
          <h1>{{ t('plannerFlow.title') }}</h1>
          <p class="planner-intro">{{ t('plannerFlow.intro') }}</p>
        </div>
        <router-link to="/spots" class="planner-back">{{ t('plannerFlow.browse') }} <span aria-hidden="true">↗</span></router-link>
      </header>
      <nav class="planner-steps" :aria-label="t('plannerFlow.stepsLabel')">
        <a href="#planner-preferences"><span>01</span>{{ t('plannerFlow.stepPreferences') }}</a>
        <a v-if="result" href="#planner-review"><span>02</span>{{ t('plannerFlow.stepReview') }}</a>
        <span v-else><span>02</span>{{ t('plannerFlow.stepReview') }}</span>
        <a v-if="result" href="#planner-check"><span>03</span>{{ t('plannerFlow.stepCheck') }}</a>
        <span v-else><span>03</span>{{ t('plannerFlow.stepCheck') }}</span>
      </nav>
      <p v-if="isOffline" class="planner-notice" role="status">{{ t('plannerFlow.offline') }}</p>

      <!-- ===== STATUS / ERROR BANNER ===== -->
      <AnimatePresence mode="popLayout">
        <motion.div
          v-if="statusMessage || errorMessage"
          key="route-planner-status"
          class="mb-8"
          :initial="{ opacity: 0, y: -12, scale: 0.98 }"
          :animate="{ opacity: 1, y: 0, scale: 1 }"
          :exit="{ opacity: 0, y: -8, scale: 0.98 }"
          :transition="{ duration: 0.36, ease: motionEase }"
        >
          <div
            class="mx-auto max-w-3xl rounded-2xl border px-5 py-4 backdrop-blur-md"
            :class="errorMessage ? 'border-rose-200 bg-rose-50/90 text-rose-700' : 'border-emerald-200 bg-emerald-50/90 text-emerald-700'"
            :role="routeStatusRole"
            :aria-live="routeStatusLive"
            aria-atomic="true"
          >
            <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex min-w-0 items-center gap-3">
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
                <p class="text-sm font-semibold">{{ routeStatusTitle }}</p>
                <p id="route-planner-status-message" class="mt-0.5 break-words text-sm">{{ routeStatusText }}</p>
              </div>
            </div>
            <div v-if="errorMessage || canResumePausedRouteJob" class="flex shrink-0 flex-wrap gap-2 sm:justify-end">
              <button
                v-if="canResumePausedRouteJob"
                type="button"
                class="min-h-11 rounded-xl bg-tibet-blue px-4 py-2 text-sm font-semibold text-white transition hover:bg-tibet-blue/90 disabled:cursor-not-allowed disabled:opacity-55 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-blue focus-visible:ring-offset-2"
                :disabled="routeResumeLoading"
                :aria-busy="routeResumeLoading"
                :aria-label="t('routePlanner.resumeAiGenerationAria')"
                @click="resumePausedRouteJob"
              >
                {{ routeResumeLoading ? t('routePlanner.resumingGeneration') : t('routePlanner.resumeGeneration') }}
              </button>
              <button
                v-if="errorMessage && !result"
                type="button"
                :disabled="loading || isOffline"
                class="min-h-11 rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 focus-visible:ring-offset-2"
                :aria-label="t('routePlanner.regenerate')"
                @click="generateRoute"
              >
                {{ t('routePlanner.regenerate') }}
              </button>
              <button
                v-if="errorMessage"
                type="button"
                class="min-h-11 rounded-xl border border-rose-200 bg-white/75 px-4 py-2 text-sm font-semibold text-rose-700 transition hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 focus-visible:ring-offset-2"
                :aria-label="t('routePlanner.dismissErrorAria')"
                @click="dismissRouteError"
              >
                {{ t('routePlanner.dismissError') }}
              </button>
            </div>
            </div>
          </div>
        </motion.div>
      </AnimatePresence>

      <!-- ===== MAIN CONTENT ===== -->
      <div class="planner-columns grid gap-6 lg:grid-cols-5 items-start">
        <!-- LEFT: Form (takes 2 cols on lg) -->
        <div class="lg:col-span-2">
          <div id="planner-preferences" class="planner-preferences space-y-4">
            <!-- Form Card -->
            <motion.div
              class="glass-card rounded-3xl border border-white/50 shadow-xl shadow-slate-900/3 overflow-hidden"
              :initial="cardInitial"
              :animate="cardInView"
              :transition="cardTransition(0, 0.06)"
            >
              <!-- Card header -->
              <div class="px-6 py-5 border-b border-white/60 bg-white/40">
                <div class="flex items-center gap-3">
                  <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-tibet-red/10 to-tibet-gold/10 text-tibet-red">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l5.447 2.724A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                    </svg>
                  </span>
                  <div>
                    <h2 class="text-lg font-bold text-tibet-dark">{{ t('plannerFlow.preferencesTitle') }}</h2>
                    <p class="text-xs text-tibet-brown/50">{{ t('plannerFlow.destination') }}</p>
                  </div>
                </div>
              </div>

              <form @submit.prevent="generateRoute" class="planner-form p-5 space-y-4" aria-describedby="route-planner-form-hint route-planner-status-message">
                <p class="planner-context">{{ t('plannerFlow.scope') }}</p>
                <!-- Days Selector -->
                <motion.div layout>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.plannedDays') }}</label>
                  <div class="flex items-stretch gap-0 rounded-2xl border border-tibet-gold/25 bg-white/70 overflow-hidden" role="group" :aria-label="t('routePlanner.plannedDays')">
                    <motion.button type="button" @click="adjustDays(-1)" :disabled="loading || form.days <= 1"
                            :aria-label="t('routePlanner.decreaseDaysAria')"
                            aria-controls="route-planner-days-value"
                            :whileHover="{ backgroundColor: 'rgba(255, 255, 255, 0.82)' }"
                            :whileTap="{ scale: 0.94 }"
                            class="flex items-center justify-center w-12 shrink-0 text-xl text-tibet-brown/70 hover:bg-gray-50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/></svg>
                    </motion.button>
                    <div class="flex-1 flex flex-col items-center justify-center py-3 border-x border-tibet-gold/20">
                      <motion.span
                        :key="form.days"
                        class="text-3xl font-bold text-tibet-dark leading-none tabular-nums"
                        :initial="{ opacity: 0, y: 8, scale: 0.94 }"
                        :animate="{ opacity: 1, y: 0, scale: 1 }"
                        :transition="{ duration: 0.22, ease: motionEase }"
                        id="route-planner-days-value"
                        role="status"
                        aria-live="polite"
                        :aria-label="daysValueLabel"
                      >{{ form.days }}</motion.span>
                      <span class="text-[10px] uppercase tracking-[0.2em] text-tibet-brown/50 mt-1">{{ t('routePlanner.daysCounterUnit') }}</span>
                    </div>
                    <motion.button type="button" @click="adjustDays(1)" :disabled="loading || form.days >= 30"
                            :aria-label="t('routePlanner.increaseDaysAria')"
                            aria-controls="route-planner-days-value"
                            :whileHover="{ backgroundColor: 'rgba(255, 255, 255, 0.82)' }"
                            :whileTap="{ scale: 0.94 }"
                            class="flex items-center justify-center w-12 shrink-0 text-xl text-tibet-brown/70 hover:bg-gray-50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                    </motion.button>
                  </div>
                  <p class="mt-2 text-xs text-tibet-brown/50">{{ t('routePlanner.suggestedDays') }}</p>
                </motion.div>

                <!-- Budget: card-style radio -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.budgetRange') }}</label>
                  <div class="planner-choice-grid grid gap-2">
                    <motion.label v-for="opt in budgetOptions" :key="opt.value"
                           class="planner-choice relative flex min-h-12 items-center gap-3 rounded-xl border-2 cursor-pointer transition-all duration-200 px-4 py-3 focus-within:ring-2 focus-within:ring-tibet-gold/60 focus-within:ring-offset-2"
                           layout
                           :whileHover="{ y: -2, scale: 1.01 }"
                           :whileTap="{ scale: 0.99 }"
                           :transition="softSpring"
                           :class="form.budget === opt.value
                             ? 'border-tibet-gold bg-amber-50/60 shadow-sm'
                             : 'border-tibet-gold/20 bg-white/60 hover:border-tibet-gold/25 hover:bg-white'">
                      <input
                        v-model="form.budget"
                        class="sr-only"
                        type="radio"
                        name="route-budget"
                        :value="opt.value"
                        :disabled="loading"
                        :aria-describedby="`route-budget-${opt.value}-desc`"
                      >
                      <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm"
                            :class="form.budget === opt.value ? 'bg-tibet-gold/15 text-tibet-gold' : 'bg-gray-100 text-gray-400'">
                        {{ opt.icon }}
                      </span>
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-semibold" :class="form.budget === opt.value ? 'text-tibet-dark' : 'text-tibet-brown/80'">{{ opt.label }}</p>
                        <p :id="`route-budget-${opt.value}-desc`" class="text-xs text-tibet-brown/50 mt-0.5">{{ opt.desc }}</p>
                      </div>
                      <motion.span v-if="form.budget === opt.value" class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-tibet-gold text-white"
                            :initial="{ opacity: 0, scale: 0.6, rotate: -20 }"
                            :animate="{ opacity: 1, scale: 1, rotate: 0 }"
                            :transition="softSpring">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                      </motion.span>
                    </motion.label>
                  </div>
                </div>

                <!-- Preference: card-style radio -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.preference') }}</label>
                  <div class="planner-choice-grid grid gap-2">
                    <motion.label v-for="opt in preferenceOptions" :key="opt.value"
                           class="planner-choice relative flex min-h-12 items-center gap-3 rounded-xl border-2 cursor-pointer transition-all duration-200 px-4 py-3 focus-within:ring-2 focus-within:ring-tibet-blue/60 focus-within:ring-offset-2"
                           layout
                           :whileHover="{ y: -2, scale: 1.01 }"
                           :whileTap="{ scale: 0.99 }"
                           :transition="softSpring"
                           :class="form.preference === opt.value
                             ? 'border-tibet-blue bg-blue-50/60 shadow-sm'
                             : 'border-tibet-gold/20 bg-white/60 hover:border-tibet-gold/25 hover:bg-white'">
                      <input
                        v-model="form.preference"
                        class="sr-only"
                        type="radio"
                        name="route-preference"
                        :value="opt.value"
                        :disabled="loading"
                        :aria-describedby="`route-preference-${opt.value}-desc`"
                      >
                      <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm"
                            :class="form.preference === opt.value ? 'bg-tibet-blue/15 text-tibet-blue' : 'bg-gray-100 text-gray-400'">
                        {{ opt.icon }}
                      </span>
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-semibold" :class="form.preference === opt.value ? 'text-tibet-dark' : 'text-tibet-brown/80'">{{ opt.label }}</p>
                        <p :id="`route-preference-${opt.value}-desc`" class="text-xs text-tibet-brown/50 mt-0.5">{{ opt.desc }}</p>
                      </div>
                      <motion.span v-if="form.preference === opt.value" class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-tibet-blue text-white"
                            :initial="{ opacity: 0, scale: 0.6, rotate: -20 }"
                            :animate="{ opacity: 1, scale: 1, rotate: 0 }"
                            :transition="softSpring">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                      </motion.span>
                    </motion.label>
                  </div>
                </div>

                <p v-if="form.days > 15" class="planner-notice">{{ t('plannerFlow.longTrip') }}</p>
                <!-- Generate Button -->
                <motion.button type="submit" :disabled="loading || isOffline || confirmingRegenerate"
                        :aria-busy="loading"
                        aria-describedby="route-planner-form-hint route-planner-status-message"
                        :whileHover="loading ? {} : { y: -2, scale: 1.01 }"
                        :whileTap="loading ? {} : { scale: 0.98 }"
                        class="planner-generate w-full rounded-2xl bg-tibet-red px-6 py-4 font-bold text-white shadow-lg shadow-tibet-red/20 transition-all duration-300 hover:shadow-xl hover:shadow-tibet-red/25 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100 flex items-center justify-center gap-3">
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
                </motion.button>
                <p id="route-planner-form-hint" class="text-xs leading-relaxed text-tibet-brown/55">
                  {{ t('routePlanner.formDraftHint') }}
                </p>
                <details v-if="result" class="border-t border-tibet-gold/20 pt-3">
                  <summary class="cursor-pointer py-2 text-sm font-semibold text-tibet-dark">{{ t('routePlanner.quickPresets') }}</summary>
                  <div class="mt-2 grid grid-cols-2 gap-2">
                    <button v-for="preset in presets" :key="preset.label" type="button" :disabled="loading"
                      :aria-label="getPresetAriaLabel(preset)" @click="applyPreset(preset)"
                      class="min-h-12 rounded-xl border border-tibet-gold/25 bg-white px-3 py-3 text-left disabled:opacity-50">
                      <span class="block text-xs font-semibold">{{ preset.label }}</span>
                      <span class="mt-1 block text-xs text-tibet-brown/70">{{ preset.days }}{{ t('routePlanner.daysUnit') }} · {{ getBudgetShort(preset.budget) }}</span>
                    </button>
                  </div>
                </details>
              </form>
            </motion.div>

            <!-- Streaming Indicator -->
            <AnimatePresence>
              <motion.div
                v-if="loading"
                key="route-planner-streaming"
                class="glass-card rounded-2xl p-5 border border-sky-200/60 bg-sky-50/70"
                role="status"
                aria-live="polite"
                :aria-busy="loading"
                :initial="{ opacity: 0, y: 14, scale: 0.98 }"
                :animate="{ opacity: 1, y: 0, scale: 1 }"
                :exit="{ opacity: 0, y: 8, scale: 0.98 }"
                :transition="{ duration: 0.34, ease: motionEase }"
              >
                <div class="mb-3 flex flex-wrap items-center gap-3">
                  <div class="flex min-w-0 flex-1 items-center gap-3">
                    <span class="relative flex h-3 w-3 shrink-0">
                      <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-sky-400 opacity-75"></span>
                      <span class="relative inline-flex rounded-full h-3 w-3 bg-tibet-turquoise"></span>
                    </span>
                    <span class="min-w-0 text-sm font-semibold text-tibet-blue">{{ routeGenerationStatusLabel }}</span>
                  </div>
                  <span v-if="charCount > 0" class="text-xs text-tibet-blue/70 font-mono">{{ charCount }} {{ t('routePlanner.charCountUnit') }}</span>
                  <button
                    type="button"
                    class="min-h-9 rounded-xl border border-sky-200 bg-white/80 px-3 py-1.5 text-xs font-semibold text-tibet-blue transition hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-blue/70 focus-visible:ring-offset-2"
                    :aria-label="t('routePlanner.pauseGenerationAria')"
                    @click="pauseRouteGeneration"
                  >
                    {{ t('routePlanner.pauseWaiting') }}
                  </button>
                </div>
                <div class="h-1.5 rounded-full bg-tibet-gold/15 overflow-hidden">
                  <div
                    class="h-full rounded-full bg-gradient-to-r from-tibet-blue via-tibet-turquoise to-tibet-gold relative overflow-hidden animate-shimmer-stream transition-[width] duration-700 ease-out"
                    :style="{ width: `${routeGenerationProgressPercent}%` }"
                    :class="{ 'animate-pulse': streaming }"
                    role="progressbar"
                    :aria-label="routeGenerationProgressLabel"
                    aria-valuemin="0"
                    aria-valuemax="100"
                    :aria-valuetext="t('plannerFlow.progressUnknown')"
                  ></div>
                </div>
                <p class="mt-2 text-xs text-tibet-brown/60">{{ t('plannerFlow.progressUnknown') }}</p>
                <p class="mt-1 text-xs leading-relaxed text-tibet-brown/55">
                  {{ t('routePlanner.pauseWaitingHint') }}
                </p>
              </motion.div>
            </AnimatePresence>


          </div>
        </div>

        <!-- RIGHT: Results (takes 3 cols on lg) -->
        <div id="planner-result" class="lg:col-span-3 min-w-0">
          <!-- Empty state -->
          <AnimatePresence mode="wait">
          <motion.div
            v-if="!result && !loading"
            key="route-planner-empty"
            class="planner-guide flex flex-col items-start p-6 md:p-8 text-left"
            :initial="{ opacity: 0, y: 18, scale: 0.98 }"
            :animate="{ opacity: 1, y: 0, scale: 1 }"
            :exit="{ opacity: 0, y: -10, scale: 0.98 }"
            :transition="revealTransition"
          >
            <div class="planner-guide-number" aria-hidden="true">01 — 03</div>
            <h2 class="text-2xl font-bold text-tibet-dark">{{ t('plannerFlow.guideTitle') }}</h2>
            <p class="mt-3 text-sm leading-relaxed">{{ t('plannerFlow.guideDesc') }}</p>
            <div class="mt-6 w-full text-left">
                <!-- Quick Presets -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.quickPresets') }}</label>
                  <div class="grid grid-cols-2 gap-2">
                    <motion.button type="button" v-for="preset in presets" :key="preset.label" @click="applyPreset(preset)" :disabled="loading"
                            :aria-label="getPresetAriaLabel(preset)"
                            :whileHover="{ y: -2, scale: 1.01 }"
                            :whileTap="{ scale: 0.98 }"
                            class="group flex min-h-12 flex-col items-start gap-0.5 rounded-xl border border-tibet-gold/20 bg-white/60 px-3.5 py-3 text-left transition-all hover:border-tibet-gold/40 hover:bg-amber-50/40 hover:shadow-sm disabled:opacity-40 disabled:cursor-not-allowed">
                      <span class="text-xs font-semibold text-tibet-dark/80 group-hover:text-tibet-dark">{{ preset.label }}</span>
                      <span class="text-[11px] text-tibet-brown/50">{{ preset.days }}{{ t('routePlanner.daysUnit') }} · {{ getBudgetShort(preset.budget) }}</span>
                    </motion.button>
                  </div>
                </div>

            </div>
            <ol class="planner-guide-list">
              <li><b>01</b><div><strong>{{ t('plannerFlow.stepPreferences') }}</strong><p>{{ t('plannerFlow.guideOne') }}</p></div></li>
              <li><b>02</b><div><strong>{{ t('plannerFlow.stepReview') }}</strong><p>{{ t('plannerFlow.guideTwo') }}</p></div></li>
              <li><b>03</b><div><strong>{{ t('plannerFlow.stepCheck') }}</strong><p>{{ t('plannerFlow.guideThree') }}</p></div></li>
            </ol>
            <p class="planner-guide-footnote">{{ t('plannerFlow.priceBoundary') }}</p>
          </motion.div>

            <!-- First-token wait state -->
            <motion.div
              v-if="loading && !result"
              key="route-planner-first-token"
              class="glass-card rounded-3xl border border-sky-100/80 p-6 shadow-xl shadow-sky-900/5 md:p-8"
              role="status"
              aria-live="polite"
              :initial="{ opacity: 0, y: 24, scale: 0.98 }"
              :animate="{ opacity: 1, y: 0, scale: 1 }"
              :exit="{ opacity: 0, y: -12, scale: 0.98 }"
              :transition="revealTransition"
            >
              <div class="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex items-center gap-4">
                  <span class="relative flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-sky-100 text-tibet-blue">
                    <span class="absolute inset-0 rounded-2xl bg-sky-200/70 animate-ping"></span>
                    <Sparkles class="relative h-6 w-6" />
                  </span>
                  <div>
                    <p class="text-base font-bold text-tibet-dark">{{ t('routePlanner.firstTokenTitle') }}</p>
                    <p class="mt-1 text-sm leading-relaxed text-tibet-brown/60">{{ routeGenerationStatusLabel }}</p>
                  </div>
                </div>
                <div class="text-left sm:text-right">
                  <p class="text-xs font-semibold text-tibet-brown/45">{{ t('routePlanner.firstTokenProgressLabel') }}</p>
                  <p class="mt-1 font-mono text-2xl font-bold text-tibet-blue">{{ routeGenerationProgressPercent }}%</p>
                </div>
              </div>

              <div class="mt-6">
                <div class="h-2 rounded-full bg-sky-100 overflow-hidden">
                  <div
                    class="h-full rounded-full bg-gradient-to-r from-tibet-blue via-tibet-turquoise to-tibet-gold relative overflow-hidden animate-shimmer-stream transition-[width] duration-700 ease-out"
                    :style="{ width: `${routeGenerationProgressPercent}%` }"
                  ></div>
                </div>
                <p class="mt-3 text-xs leading-relaxed text-tibet-brown/55">{{ t('routePlanner.firstTokenSubtitle') }}</p>
              </div>

              <div class="mt-6 grid gap-3">
                <div
                  v-for="step in routeGenerationProgressSteps"
                  :key="step.key"
                  class="rounded-2xl border px-4 py-3 transition-colors"
                  :class="step.active ? 'border-sky-200 bg-sky-50 text-tibet-blue' : 'border-tibet-gold/15 bg-white/70 text-tibet-brown/55'"
                >
                  <div class="flex items-center gap-2">
                    <span class="h-2.5 w-2.5 rounded-full" :class="step.active ? 'bg-tibet-turquoise animate-pulse' : 'bg-tibet-gold/40'"></span>
                    <p class="text-sm font-semibold">{{ step.label }}</p>
                  </div>
                  <p class="mt-1.5 text-xs leading-relaxed opacity-80">{{ step.description }}</p>
                </div>
              </div>
            </motion.div>

          <!-- Result card -->
            <motion.div
              v-if="result"
              key="route-planner-result"
              class="glass-card rounded-3xl border border-white/50 shadow-xl shadow-slate-900/3 overflow-hidden"
              :initial="{ opacity: 0, y: 24, scale: 0.98 }"
              :animate="{ opacity: 1, y: 0, scale: 1 }"
              :exit="{ opacity: 0, y: -12, scale: 0.98 }"
              :transition="revealTransition"
            >
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
                <div class="flex flex-wrap gap-1.5">
                  <motion.button v-if="resultShouldCollapse" @click="resultExpanded = !resultExpanded"
                          :aria-expanded="resultExpanded"
                          :aria-label="resultToggleLabel"
                          :whileHover="{ y: -1, scale: 1.02 }"
                          :whileTap="{ scale: 0.96 }"
                          class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-blue/20 bg-white/80 px-3.5 py-2 text-xs font-medium text-tibet-blue transition-all hover:bg-sky-50">
                    <ChevronUp v-if="resultExpanded" class="h-4 w-4" />
                    <ChevronDown v-else class="h-4 w-4" />
                    {{ resultToggleButtonLabel }}
                  </motion.button>
                  <motion.button @click="copyResult" :disabled="copying"
                          :aria-label="copying ? t('routePlanner.copying') : t('routePlanner.copyText')"
                          :whileHover="{ y: -1, scale: 1.02 }"
                          :whileTap="{ scale: 0.96 }"
                          class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-3.5 py-2 text-xs font-medium text-gray-600 transition-all hover:bg-gray-50 hover:border-gray-300 disabled:opacity-50">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16h8M8 12h8m-7-8h5a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V6a2 2 0 012-2h1z" />
                    </svg>
                    {{ copying ? t('routePlanner.copying') : t('routePlanner.copyText') }}
                  </motion.button>
                </div>
              </div>

              <div id="planner-review" class="planner-review-section px-5 py-5">
                <RouteReviewPanel :days="routeDaySections" :route-key="String(reviewRevision)" @update:notes="reviewNotes = $event" @jump="scrollToRouteDay" />
              </div>
              <section id="planner-check" class="planner-check px-5 py-5">
                <h2 class="text-lg font-bold">{{ t('plannerFlow.checkTitle') }}</h2>
                <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p class="text-xs">{{ t('plannerFlow.referenceTotal') }}</p>
                    <p class="mt-1 text-2xl font-bold">{{ formatCurrency(itineraryQuote?.totalEstimatedCost ?? bookableItinerary?.totalEstimatedCost, itineraryQuote?.currency) }}</p>
                  </div>
                  <a v-if="bookableItinerary || itineraryLoading || itineraryError" class="planner-secondary" href="#planner-budget">{{ t('plannerFlow.viewDetails') }}</a>
                </div>
                <p class="mt-2 text-sm leading-relaxed">{{ t('plannerFlow.priceBoundary') }}</p>
                <p class="mt-2 text-sm leading-relaxed">{{ t('plannerFlow.saveBoundary') }}</p>
                <div class="mt-4 flex flex-wrap gap-2">
                  <button type="button" class="planner-primary" :disabled="saving || loading || isOffline || currentAiRouteManuallySaved" @click="saveRoute">{{ currentAiRouteManuallySaved ? t('plannerFlow.saved') : saving ? t('routePlanner.saving') : t('routePlanner.saveRoute') }}</button>
                  <button type="button" class="planner-secondary" :disabled="loading" @click="downloadRoute">{{ t('plannerFlow.exportReview') }}</button>
                  <a class="planner-secondary" href="#planner-preferences">{{ t('plannerFlow.backPreferences') }}</a>
                </div>
              </section>
              <!-- Compact summary -->
              <div
                v-if="!isLongRoute"
                class="space-y-5 border-b border-white/60 bg-gradient-to-br from-white/70 via-amber-50/45 to-sky-50/40 p-5 md:p-6"
              >
                <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <div v-for="stat in resultStats" :key="stat.label" class="rounded-xl border border-white/70 bg-white/70 px-3 py-2.5 shadow-sm">
                    <p class="text-[11px] font-semibold text-tibet-brown/45">{{ stat.label }}</p>
                    <p class="mt-1 text-sm font-bold text-tibet-dark">{{ stat.value }}</p>
                  </div>
                </div>

                <div class="rounded-2xl border border-amber-200/80 bg-amber-50/75 px-4 py-3">
                  <div class="mb-1.5 flex items-center gap-2 text-amber-800">
                    <ShieldAlert class="h-4 w-4" />
                    <h3 class="text-sm font-bold">{{ t('routePlanner.preTripCheckTitle') }}</h3>
                  </div>
                  <p class="text-xs leading-relaxed text-amber-800/80">
                    {{ t('routePlanner.preTripCheckDesc') }}
                  </p>
                </div>

                <div v-if="routeOverviewText" class="rounded-2xl border border-tibet-gold/15 bg-white/75 px-4 py-3">
                  <div class="mb-2 flex items-center gap-2 text-tibet-dark">
                    <Sparkles class="h-4 w-4 text-tibet-gold" />
                      <h3 class="text-sm font-bold">{{ t('routePlanner.routeSummary') }}</h3>
                  </div>
                  <p class="text-sm leading-relaxed text-tibet-brown/70 line-clamp-3">{{ routeOverviewText }}</p>
                </div>

                <div v-if="routeHighlightItems.length" class="grid gap-2 sm:grid-cols-2">
                  <div v-for="item in routeHighlightItems" :key="item" class="flex gap-2 rounded-xl border border-tibet-gold/10 bg-white/70 px-3 py-2">
                    <span class="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-tibet-gold"></span>
                    <p class="text-xs leading-relaxed text-tibet-brown/70">{{ item }}</p>
                  </div>
                </div>
              </div>

              <!-- Day outline -->
              <div
                v-if="visibleRouteDaySections.length && loading"
                class="border-b border-white/60 bg-white/45 px-5 py-5 md:px-6"

              >
                <div class="mb-3 flex items-center justify-between gap-3">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <ListChecks class="h-4 w-4 text-tibet-blue" />
                        <h3 class="text-sm font-bold">{{ t('routePlanner.routeDailyPreview') }}</h3>
                  </div>
                      <span v-if="hiddenDayCount > 0" class="text-xs font-medium text-tibet-brown/50">{{ t('routePlanner.routeHiddenDays', { count: hiddenDayCount }) }}</span>
                </div>
                <div class="grid gap-3 sm:grid-cols-2">
                  <section
                    v-for="day in visibleRouteDaySections"
                    :key="day.day"
                    class="rounded-2xl border border-tibet-gold/15 bg-white/80 px-4 py-3"
                  >
                    <p class="text-xs font-bold text-tibet-blue">{{ day.day }}</p>
                    <h4 class="mt-1 line-clamp-1 text-sm font-bold text-tibet-dark">{{ day.title }}</h4>
                    <p class="mt-1.5 line-clamp-2 text-xs leading-relaxed text-tibet-brown/60">{{ day.summary }}</p>
                  </section>
                </div>
              </div>

              <!-- Markdown content -->
              <div
                class="px-6 pb-6 pt-5 md:px-8 md:pb-8 lg:px-10"

              >
                <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-bold text-tibet-dark">{{ t('routePlanner.routeFullText') }}</p>
                    <p class="text-xs text-tibet-brown/50">{{ t('routePlanner.routeShortReaderHint') }}</p>
                  </div>
                  <motion.button v-if="resultShouldCollapse" @click="resultExpanded = !resultExpanded"
                          :aria-expanded="resultExpanded"
                          :aria-label="resultToggleLabel"
                          :whileHover="{ y: -1, scale: 1.02 }"
                          :whileTap="{ scale: 0.96 }"
                          class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-blue/20 bg-white/80 px-3.5 py-2 text-xs font-medium text-tibet-blue transition-all hover:bg-sky-50">
                    <ChevronUp v-if="resultExpanded" class="h-4 w-4" />
                    <ChevronDown v-else class="h-4 w-4" />
                    {{ resultToggleButtonLabel }}
                  </motion.button>
                </div>
                <div class="relative">
                  <div
                    ref="routeResultReaderRef" class="prose prose-slate route-result-prose max-w-none prose-headings:text-tibet-dark prose-p:text-tibet-brown/80 prose-p:leading-relaxed prose-a:text-tibet-blue prose-strong:text-tibet-dark/90 prose-li:text-tibet-brown/80 prose-h2:border-b prose-h2:border-tibet-gold/20 prose-h2:pb-2 prose-h2:mt-8 prose-h2:mb-4 prose-img:rounded-2xl prose-img:shadow-md"
                    :class="{
                      'max-h-[420px] overflow-hidden': resultShouldCollapse && !resultExpanded,
                      'route-result-prose-compact': isLongRoute
                    }"
                    v-html="renderedResult"
                  ></div>
                  <div v-if="resultShouldCollapse && !resultExpanded" class="pointer-events-none absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-white via-white/95 to-transparent"></div>
                </div>
              </div>

              <!-- Result actions -->
              <div
                class="px-6 py-4 border-t border-white/60 bg-white/30 flex flex-wrap items-center justify-end gap-2"

              >
                <motion.button @click="generateRoute" :disabled="loading"
                        :aria-busy="loading"
                        :aria-label="t('routePlanner.regenerate')"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex min-h-11 items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  {{ t('routePlanner.regenerate') }}
                </motion.button>
                <motion.button @click="saveRoute" :disabled="saving || loading || isOffline || currentAiRouteManuallySaved || !result"
                        :aria-busy="saving"
                        :aria-label="saving ? t('routePlanner.saving') : t('routePlanner.saveRoute')"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex min-h-11 items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                  </svg>
                  {{ saving ? t('routePlanner.saving') : t('routePlanner.saveRoute') }}
                </motion.button>
                <motion.button @click="downloadRoute" :disabled="loading || !result"
                        :aria-label="t('routePlanner.downloadMarkdown')"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex min-h-11 items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <Download class="h-4 w-4" />
                  {{ t('routePlanner.downloadMarkdown') }}
                </motion.button>
                <motion.button @click="shareRoute" :disabled="sharing || loading || isOffline || !result"
                        :aria-busy="sharing"
                        :aria-label="sharing ? t('routePlanner.sharing') : t('routePlanner.shareToCommunity')"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex min-h-11 items-center gap-1.5 rounded-xl bg-tibet-blue px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-tibet-blue/20 transition-all hover:from-tibet-blue/90 hover:to-indigo-700 hover:shadow-lg disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                  </svg>
                  {{ sharing ? t('routePlanner.sharing') : t('routePlanner.shareToCommunity') }}
                </motion.button>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>
      </div>

      <div
        id="planner-budget"
        v-if="bookableItinerary || itineraryLoading || itineraryError || tibetTravelKit || tibetTravelKitLoading || travelKitError"
        class="mt-8 grid items-start gap-6 xl:grid-cols-2"
      >
          <motion.div
            v-if="bookableItinerary || itineraryLoading || itineraryError"
            class="glass-card rounded-3xl border border-white/50 shadow-xl shadow-slate-900/3 overflow-hidden"
            :class="{ 'xl:col-span-2': !tibetTravelKit && !tibetTravelKitLoading }"
            :initial="{ opacity: 0, y: 18, scale: 0.98 }"
            :animate="{ opacity: 1, y: 0, scale: 1 }"
            :transition="revealTransition"
          >
            <div class="px-6 py-5 border-b border-white/60 bg-white/40 flex flex-wrap items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-tibet-gold/15 text-tibet-gold">
                  <ReceiptText class="h-5 w-5" />
                </span>
                <div>
                  <p class="text-sm font-bold text-tibet-dark">{{ bookableItinerary?.title || t('routePlanner.bookableItineraryGeneratingTitle') }}</p>
                  <p class="text-xs text-tibet-brown/50">
                    {{ bookableItinerarySubtitle }}
                  </p>
                </div>
              </div>
              <div v-if="bookableItinerary" class="flex flex-wrap gap-1.5">
                <router-link
                  to="/orders"
                  class="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50/80 px-3 py-2 text-xs font-semibold text-emerald-700 transition-all hover:bg-emerald-100"
                >
                  <ReceiptText class="h-3.5 w-3.5" />
                  {{ t('routePlanner.consultationRecords') }}
                </router-link>
                <motion.button
                  v-for="option in itineraryVersionOptions"
                  :key="option.value"
                  @click="generateBookableItinerary(option.value)"
                  :disabled="!!itineraryVersionLoading"
                  :aria-busy="itineraryVersionLoading === option.value"
                  :aria-label="getItineraryVersionAriaLabel(option)"
                  :whileHover="{ y: -1, scale: 1.02 }"
                  :whileTap="{ scale: 0.96 }"
                  class="inline-flex min-h-11 items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-3 py-2 text-xs font-medium text-gray-600 transition-all hover:bg-amber-50 disabled:opacity-50"
                >
                  <Shuffle class="h-3.5 w-3.5" />
                  {{ itineraryVersionLoading === option.value ? t('routePlanner.itineraryVersionGenerating') : option.label }}
                </motion.button>
              </div>
            </div>

            <div v-if="itineraryLoading" class="p-8 text-center text-tibet-brown/60" role="status" aria-live="polite" aria-busy="true">
              <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-tibet-gold mx-auto mb-3"></div>
              <p class="text-sm font-medium">{{ t('routePlanner.bookableItineraryLoading') }}</p>
            </div>

            <div v-if="itineraryError" class="planner-notice m-5" role="alert">
              <p>{{ itineraryError }}</p><button class="planner-secondary mt-3" :disabled="itineraryLoading || !!itineraryVersionLoading || isOffline" @click="generateBookableItinerary()">{{ t('plannerFlow.retryDetails') }}</button>
            </div>
            <div v-if="bookableItinerary && !itineraryLoading" class="p-5 md:p-6 space-y-5">
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div class="rounded-2xl border border-tibet-gold/20 bg-white/70 px-4 py-3">
                  <p class="text-xs text-tibet-brown/50">{{ t('routePlanner.quoteTotalEstimated') }}</p>
                  <p class="mt-1 text-xl font-bold text-tibet-dark">{{ formatCurrency(itineraryQuote?.totalEstimatedCost ?? bookableItinerary.totalEstimatedCost, itineraryQuote?.currency) }}</p>
                </div>
                <div class="rounded-2xl border border-emerald-200 bg-emerald-50/70 px-4 py-3">
                  <p class="text-xs text-emerald-700/70">{{ t('routePlanner.quoteBookable') }}</p>
                  <p class="mt-1 text-xl font-bold text-emerald-700">{{ formatCurrency(itineraryQuote?.bookableTotal, itineraryQuote?.currency) }}</p>
                </div>
                <div class="rounded-2xl border border-sky-200 bg-sky-50/70 px-4 py-3">
                  <p class="text-xs text-sky-700/70">{{ t('routePlanner.quoteInformational') }}</p>
                  <p class="mt-1 text-xl font-bold text-sky-700">{{ formatCurrency(itineraryQuote?.informationalTotal, itineraryQuote?.currency) }}</p>
                </div>
              </div>

              <div class="space-y-4">
                <section
                  v-for="day in bookableItinerary.itineraryDays"
                  :key="day.id"
                  class="rounded-2xl border border-tibet-gold/15 bg-white/75 overflow-hidden"
                >
                  <div class="px-4 py-3 border-b border-tibet-gold/10 flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <h3 class="text-sm font-bold text-tibet-dark">{{ day.title }}</h3>
                      <p class="mt-0.5 text-xs text-tibet-brown/50">{{ formatItineraryDayMeta(day) }}</p>
                    </div>
                    <span class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold" :class="riskClass(day.altitudeRisk)">
                      {{ riskLabel(day.altitudeRisk) }}
                    </span>
                  </div>

                  <div class="divide-y divide-tibet-gold/10">
                    <div
                      v-for="item in day.items"
                      :key="item.id"
                      class="px-4 py-3 flex flex-col gap-3 md:flex-row md:items-center md:justify-between"
                    >
                      <div class="flex gap-3 min-w-0">
                        <span class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-tibet-gold/10 text-tibet-gold">
                          <component :is="itemIcon(item.itemType)" class="h-4 w-4" />
                        </span>
                        <div class="min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <p class="text-sm font-semibold text-tibet-dark">{{ item.startTime }} · {{ item.title }}</p>
                            <span class="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] text-gray-600">{{ itemTypeLabel(item.itemType) }}</span>
                            <span v-if="item.bookingStatus === 'BOOKED'" class="rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">{{ t('routePlanner.bookingSubmitted') }}</span>
                          </div>
                          <p class="mt-1 text-xs leading-relaxed text-tibet-brown/60 line-clamp-2">{{ item.description }}</p>
                          <p v-if="item.alternatives" class="mt-1 text-[11px] text-tibet-blue/70">{{ t('routePlanner.alternativesPrefix', { alternatives: item.alternatives }) }}</p>
                        </div>
                      </div>
                      <div class="flex shrink-0 items-center justify-between gap-3 md:justify-end">
                        <span class="text-sm font-bold text-tibet-dark">{{ formatCurrency(item.estimatedCost, itineraryQuote?.currency) }}</span>
                        <motion.button
                          v-if="isBookableItem(item)"
                          @click="bookItineraryItem(item)"
                          :disabled="itineraryBookingItemId === item.id"
                          :aria-busy="itineraryBookingItemId === item.id"
                          :aria-label="getExternalBookingAriaLabel(item)"
                          :whileHover="{ y: -1, scale: 1.02 }"
                          :whileTap="{ scale: 0.96 }"
                          class="inline-flex min-h-11 items-center gap-1.5 rounded-xl bg-tibet-red px-3.5 py-2 text-xs font-semibold text-tibet-yellow shadow-md shadow-tibet-red/20 disabled:opacity-50"
                        >
                          <CalendarCheck class="h-3.5 w-3.5" />
                          {{ itineraryBookingItemId === item.id ? t('routePlanner.bookingOpening') : t('routePlanner.thirdPartyBooking') }}
                        </motion.button>
                      </div>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </motion.div>

          <motion.div
            v-if="tibetTravelKit || tibetTravelKitLoading || travelKitError"
            class="glass-card rounded-3xl border border-white/50 shadow-xl shadow-slate-900/3 overflow-hidden"
            :class="{ 'xl:col-span-2': !bookableItinerary && !itineraryLoading }"
            :initial="{ opacity: 0, y: 18, scale: 0.98 }"
            :animate="{ opacity: 1, y: 0, scale: 1 }"
            :transition="revealTransition"
          >
            <div class="px-6 py-5 border-b border-white/60 bg-white/40 flex flex-wrap items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                  <Mountain class="h-5 w-5" />
                </span>
                <div>
                  <p class="text-sm font-bold text-tibet-dark">{{ t('routePlanner.travelKitTitle') }}</p>
                  <p class="text-xs text-tibet-brown/50">
                    {{ tibetTravelKitSubtitle }}
                  </p>
                </div>
              </div>
              <motion.button
                v-if="tibetTravelKit"
                @click="downloadOfflinePackage"
                :aria-label="t('routePlanner.travelKitDownloadAria')"
                :whileHover="{ y: -1, scale: 1.02 }"
                :whileTap="{ scale: 0.96 }"
                class="inline-flex min-h-11 items-center gap-1.5 rounded-xl border border-emerald-200 bg-white/80 px-3.5 py-2 text-xs font-semibold text-emerald-700 transition-all hover:bg-emerald-50"
              >
                <Download class="h-3.5 w-3.5" />
                {{ t('routePlanner.offlinePackageShort') }}
              </motion.button>
            </div>

            <div v-if="travelKitError" class="planner-notice m-5" role="alert">
              <p>{{ t('plannerFlow.kitFailed') }}</p><button class="planner-secondary mt-3" :disabled="tibetTravelKitLoading || isOffline" @click="bookableItinerary && fetchTibetTravelKit(bookableItinerary.id)">{{ t('plannerFlow.retryKit') }}</button>
            </div>
            <div v-if="tibetTravelKitLoading" class="p-8 text-center text-tibet-brown/60" role="status" aria-live="polite" aria-busy="true">
              <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-500 mx-auto mb-3"></div>
              <p class="text-sm font-medium">{{ t('routePlanner.travelKitLoading') }}</p>
            </div>

            <div v-else-if="tibetTravelKit" class="p-5 md:p-6 space-y-5">
              <div class="grid gap-3 md:grid-cols-3">
                <div class="rounded-2xl border px-4 py-3" :class="riskClass(tibetTravelKit.highlandAssessment.riskLevel)">
                  <div class="flex items-center gap-2">
                    <HeartPulse class="h-4 w-4" />
                    <p class="text-xs font-semibold">{{ t('routePlanner.highlandAdaptation') }}</p>
                  </div>
                  <p class="mt-2 text-2xl font-bold">{{ tibetTravelKit.highlandAssessment.riskScore }}</p>
                  <p class="mt-1 text-xs leading-relaxed">{{ tibetTravelKit.highlandAssessment.summary }}</p>
                </div>
                <div class="rounded-2xl border border-sky-200 bg-sky-50/70 px-4 py-3 text-sky-800">
                  <div class="flex items-center gap-2">
                    <MapPin class="h-4 w-4" />
                    <p class="text-xs font-semibold">{{ t('routePlanner.offlineTravelKit') }}</p>
                  </div>
                  <p class="mt-2 text-2xl font-bold">{{ formatInteger(tibetTravelKit.offlinePackage.mapPins.length) }}</p>
                  <p class="mt-1 text-xs leading-relaxed">{{ t('routePlanner.offlineTravelKitDesc') }}</p>
                </div>
                <div class="rounded-2xl border border-emerald-200 bg-emerald-50/70 px-4 py-3 text-emerald-800">
                  <div class="flex items-center gap-2">
                    <Leaf class="h-4 w-4" />
                    <p class="text-xs font-semibold">{{ t('routePlanner.sustainableTravel') }}</p>
                  </div>
                  <p class="mt-2 text-2xl font-bold">{{ formatInteger(tibetTravelKit.sustainableOptions.length) }}</p>
                  <p class="mt-1 text-xs leading-relaxed">{{ t('routePlanner.sustainableTravelDesc') }}</p>
                </div>
              </div>

              <div class="grid gap-4 xl:grid-cols-2">
                <section class="rounded-2xl border border-tibet-gold/15 bg-white/75 p-4">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <HeartPulse class="h-4 w-4 text-rose-600" />
                    <h3 class="text-sm font-bold">{{ t('routePlanner.dailyHighlandPace') }}</h3>
                  </div>
                  <div class="mt-3 space-y-3">
                    <div
                      v-for="advice in tibetTravelKit.highlandAssessment.dailyAdvice.slice(0, 3)"
                      :key="advice.dayNumber"
                      class="rounded-xl border border-tibet-gold/10 bg-white/80 px-3 py-2"
                    >
                      <div class="flex items-center justify-between gap-2">
                        <p class="text-xs font-semibold text-tibet-dark">{{ t('routePlanner.dayAltitudeMeta', { day: advice.dayNumber, altitude: formatAltitudeMeters(advice.maxAltitudeMeters) }) }}</p>
                        <span class="rounded-full border px-2 py-0.5 text-[11px] font-semibold" :class="riskClass(advice.riskLevel)">{{ riskLabel(advice.riskLevel) }}</span>
                      </div>
                      <p class="mt-1 text-xs leading-relaxed text-tibet-brown/60">{{ advice.paceAdvice }}</p>
                    </div>
                  </div>
                </section>

                <section class="rounded-2xl border border-tibet-gold/15 bg-white/75 p-4">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <Bell class="h-4 w-4 text-amber-600" />
                    <h3 class="text-sm font-bold">{{ t('routePlanner.realtimeAlerts') }}</h3>
                  </div>
                  <div class="mt-3 space-y-2">
                    <div
                      v-for="alert in tibetTravelKit.realtimeAlerts.slice(0, 3)"
                      :key="`${alert.type}-${alert.title}`"
                      class="rounded-xl border px-3 py-2"
                      :class="alertClass(alert.level)"
                    >
                      <div class="flex items-center justify-between gap-2">
                        <p class="text-xs font-semibold">{{ alert.title }}</p>
                        <span class="rounded-full bg-white/70 px-2 py-0.5 text-[11px]">{{ alertLabel(alert.level) }}</span>
                      </div>
                      <p class="mt-1 text-xs leading-relaxed opacity-80">{{ alert.action }}</p>
                    </div>
                  </div>
                </section>

                <section class="rounded-2xl border border-tibet-gold/15 bg-white/75 p-4">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <Landmark class="h-4 w-4 text-tibet-gold" />
                    <h3 class="text-sm font-bold">{{ t('routePlanner.culturalEtiquetteAssistant') }}</h3>
                  </div>
                  <div class="mt-3 space-y-3">
                    <div v-for="tip in tibetTravelKit.culturalTips.slice(0, 2)" :key="tip.scene" class="rounded-xl bg-amber-50/60 px-3 py-2">
                      <p class="text-xs font-semibold text-tibet-dark">{{ tip.title }}</p>
                      <p class="mt-1 text-xs leading-relaxed text-tibet-brown/60">{{ tip.doTips[0] }}</p>
                      <p class="mt-1 text-[11px] text-rose-700/80">{{ t('routePlanner.avoidTip', { tip: tip.avoidTips[0] }) }}</p>
                    </div>
                  </div>
                </section>

                <section class="rounded-2xl border border-tibet-gold/15 bg-white/75 p-4">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <Languages class="h-4 w-4 text-sky-600" />
                    <h3 class="text-sm font-bold">{{ t('routePlanner.phrasebookTitle') }}</h3>
                  </div>
                  <div class="mt-3 grid gap-2 sm:grid-cols-2">
                    <div v-for="phrase in tibetTravelKit.phrasebook.slice(0, 4)" :key="`${phrase.category}-${phrase.chinese}`" class="rounded-xl border border-sky-100 bg-sky-50/60 px-3 py-2">
                      <p class="text-xs font-semibold text-tibet-dark">{{ phrase.chinese }}</p>
                      <p class="mt-1 text-sm text-sky-900 tibetan-font">{{ phrase.tibetan }}</p>
                      <p class="mt-0.5 text-[11px] text-sky-700/70">{{ phrase.pronunciation }} · {{ phrase.english }}</p>
                    </div>
                  </div>
                </section>
              </div>

              <section class="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4">
                <div class="flex items-center gap-2 text-emerald-900">
                  <ShieldAlert class="h-4 w-4" />
                  <h3 class="text-sm font-bold">{{ t('routePlanner.offlineSustainableChecklist') }}</h3>
                </div>
                <div class="mt-3 grid gap-3 md:grid-cols-2">
                  <div>
                    <p class="text-xs font-semibold text-emerald-900/80">{{ t('routePlanner.offlinePreparation') }}</p>
                    <ul class="mt-2 space-y-1">
                      <li v-for="item in tibetTravelKit.offlinePackage.offlineChecklist.slice(0, 4)" :key="item" class="text-xs leading-relaxed text-emerald-900/70">
                        {{ item }}
                      </li>
                    </ul>
                  </div>
                  <div>
                    <p class="text-xs font-semibold text-emerald-900/80">{{ t('routePlanner.lowImpactTravel') }}</p>
                    <ul class="mt-2 space-y-1">
                      <li v-for="option in tibetTravelKit.sustainableOptions.slice(0, 3)" :key="option.title" class="text-xs leading-relaxed text-emerald-900/70">
                        {{ option.title }}：{{ option.localBenefit }}
                      </li>
                    </ul>
                  </div>
                </div>
              </section>
            </div>
          </motion.div>
      </div>
    </div>
  </div>

  <MobileStickyActionBar
    :show="true"
    :secondary-label="result ? t('routePlanner.copyText') : ''"
    :primary-label="result ? t('plannerFlow.checkTitle') : loading ? t('routePlanner.aiPlanning') : t('routePlanner.generateRoute')"
    :secondary-disabled="copying"
    :primary-disabled="loading || confirmingRegenerate || (!result && isOffline)"
    @secondary="copyResult"
    @primary="result ? goToReview() : generateRoute()"
  />
</template>

<script setup lang="ts">
import { nextTick, ref, computed, shallowRef, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AnimatePresence, motion } from 'motion-v'
import {
  Bell,
  CalendarCheck,
  ChevronDown,
  ChevronUp,
  Download,
  HeartPulse,
  Hotel,
  Landmark,
  Languages,
  Leaf,
  ListChecks,
  MapPin,
  Mountain,
  ReceiptText,
  ShieldAlert,
  Shuffle,
  Sparkles,
  Ticket
} from 'lucide-vue-next'
import { renderMarkdownToSafeHtml, sanitizeHtml } from '../utils/sanitize'
import {
  getRouteGenerationJob,
  startRouteGenerationJob,
  streamRouteGenerationJob,
  type RouteGenerationJobSnapshot
} from '../api/stream'
import api, { endpoints, type AiRouteRecordResponse } from '../api'
import RouteReviewPanel from '../components/RouteReviewPanel.vue'
import { showConfirm } from '../composables/useConfirm'
import MobileStickyActionBar from '../components/MobileStickyActionBar.vue'
import { useRoutePlannerDraft, type RoutePlannerFormState } from '../composables/useRoutePlannerDraft'
import { useAuthGuard } from '../composables/useAuthGuard'
import { showToast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'
import { useRouteGenerationStore } from '../stores/routeGeneration'
import { readBrowserStorage, removeBrowserStorage, writeBrowserStorage } from '../utils/browserStorage'
import { summarizeClientError } from '../utils/errorMonitoring'
import { openExternalBooking } from '../utils/externalBooking'
import { toFiniteAmount, toIntlLocale } from '../i18n/formatting'
import {
  cardInitial,
  cardInView,
  cardTransition,
  motionEase,
  revealInitial,
  revealInView,
  revealTransition,
  softSpring
} from '../motion/presets'

const { t, locale } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const generationStore = useRouteGenerationStore()
const { requireAuth } = useAuthGuard()
const PAUSED_ROUTE_JOB_STORAGE_KEY = 'colorful-tibet:route-planner:paused-job'
const SAFE_ROUTE_JOB_ID_PATTERN = /^[A-Za-z0-9:_-]{1,160}$/

interface RouteSummarySection {
  day: string
  dayNumber: number
  title: string
  summary: string
}

interface ItineraryItem {
  id: number
  itemType: string
  title: string
  description?: string
  startTime?: string
  durationMinutes?: number
  estimatedCost?: number
  altitudeMeters?: number
  riskLevel?: string
  alternatives?: string
  bookingAction?: string
  bookingStatus?: string
  bookingReferenceType?: string
  bookingReferenceId?: number
  scenicSpotId?: number
  scenicSpotName?: string
  hotelId?: number
  hotelName?: string
  roomTypeId?: number
  roomTypeName?: string
}

interface ItineraryDay {
  id: number
  dayNumber: number
  travelDate?: string
  title: string
  region?: string
  summary?: string
  estimatedCost?: number
  altitudeRisk?: string
  items: ItineraryItem[]
}

interface BookableItinerary {
  id: number
  title: string
  days: number
  startDate?: string
  budget: string
  preference: string
  versionType: string
  versionLabel: string
  totalEstimatedCost?: number
  itineraryDays: ItineraryDay[]
}

interface ItineraryQuote {
  itineraryId: number
  totalEstimatedCost: number
  bookableTotal: number
  informationalTotal: number
  currency: string
}

interface HighlandDayAdvice {
  dayNumber: number
  title: string
  maxAltitudeMeters: number
  riskLevel: string
  paceAdvice: string
  hydrationAdvice: string
  activityLimit: string
  warning: string
}

interface HighlandAssessment {
  riskScore: number
  riskLevel: string
  riskLabel: string
  maxAltitudeMeters: number
  highAltitudeDays: number
  summary: string
  dailyAdvice: HighlandDayAdvice[]
  adaptationChecklist: string[]
  warningSigns: string[]
  goSlowRules: string[]
}

interface CulturalTip {
  scene: string
  title: string
  context: string
  doTips: string[]
  avoidTips: string[]
}

interface PhraseGuideItem {
  category: string
  chinese: string
  tibetan: string
  english: string
  pronunciation: string
  usage: string
}

interface EmergencyContact {
  name: string
  phone: string
  description: string
}

interface OfflineMapPin {
  type: string
  name: string
  latitude?: number
  longitude?: number
  altitudeMeters?: number
  note?: string
}

interface OfflinePackage {
  itineraryId: number
  title: string
  generatedAt: string
  validUntil: string
  includedSections: string[]
  emergencyContacts: EmergencyContact[]
  mapPins: OfflineMapPin[]
  offlineChecklist: string[]
  voucherHints: string[]
  backupNotes: string[]
}

interface TravelAlert {
  level: string
  type: string
  title: string
  message: string
  action: string
  relatedDay?: number
  expiresAt?: string
}

interface SustainableOption {
  category: string
  title: string
  impact: string
  actions: string[]
  localBenefit: string
  carbonHint: string
}

interface TibetTravelKit {
  itineraryId: number
  title: string
  highlandAssessment: HighlandAssessment
  culturalTips: CulturalTip[]
  phrasebook: PhraseGuideItem[]
  offlinePackage: OfflinePackage
  realtimeAlerts: TravelAlert[]
  sustainableOptions: SustainableOption[]
}

const defaultForm: RoutePlannerFormState = {
  days: 7,
  budget: 'comfort',
  preference: 'natural'
}

const routeDraftPersistenceSuspended = ref(false)

const onAuthExpired = () => {
  suspendRouteDraftPersistence()
  if (window.location.pathname !== '/login') showToast(t('routePlanner.authFailed'), 'warning')
}

const form = ref<RoutePlannerFormState>({ ...defaultForm })
// Inputs describe the next request; generated content keeps its original parameters.
const generatedForm = ref<RoutePlannerFormState>({ ...defaultForm })

const presets = computed(() => [
  { label: t('routePlanner.presetNatural'), days: 6, budget: 'comfort', preference: 'natural' },
  { label: t('routePlanner.presetCultural'), days: 5, budget: 'economy', preference: 'cultural' },
  { label: t('routePlanner.presetPhotography'), days: 8, budget: 'luxury', preference: 'photography' },
  { label: t('routePlanner.presetRelaxation'), days: 7, budget: 'comfort', preference: 'relaxation' }
])

const budgetOptions = computed(() => [
  { value: 'economy', label: t('routePlanner.budget.economy'), desc: t('routePlanner.budgetDesc.economy'), icon: '💰' },
  { value: 'comfort', label: t('routePlanner.budget.comfort'), desc: t('routePlanner.budgetDesc.comfort'), icon: '⭐' },
  { value: 'luxury', label: t('routePlanner.budget.luxury'), desc: t('routePlanner.budgetDesc.luxury'), icon: '💎' }
])

const preferenceOptions = computed(() => [
  { value: 'natural', label: t('routePlanner.preferenceOptions.natural'), desc: t('routePlanner.preferenceDesc.natural'), icon: '🏔️' },
  { value: 'cultural', label: t('routePlanner.preferenceOptions.cultural'), desc: t('routePlanner.preferenceDesc.cultural'), icon: '🏛️' },
  { value: 'photography', label: t('routePlanner.preferenceOptions.photography'), desc: t('routePlanner.preferenceDesc.photography'), icon: '📷' },
  { value: 'relaxation', label: t('routePlanner.preferenceOptions.relaxation'), desc: t('routePlanner.preferenceDesc.relaxation'), icon: '🌿' }
])

const itineraryVersionOptions = computed(() => [
  { value: 'cheaper', label: t('routePlanner.itineraryVersionOptions.cheaper') },
  { value: 'relaxed', label: t('routePlanner.itineraryVersionOptions.relaxed') },
  { value: 'hidden', label: t('routePlanner.itineraryVersionOptions.hidden') },
  { value: 'family', label: t('routePlanner.itineraryVersionOptions.family') }
])

const getPresetAriaLabel = (preset: { label: string; days: number; budget: string; preference: string }) =>
  t('routePlanner.quickPresetAria', {
    label: preset.label,
    days: t('routePlanner.daysValueCompact', { count: preset.days }),
    budget: getBudgetShort(preset.budget)
  })

const getItineraryVersionAriaLabel = (option: { value: string; label: string }) =>
  itineraryVersionLoading.value === option.value
    ? t('routePlanner.itineraryVersionGeneratingAria', { label: option.label })
    : t('routePlanner.itineraryVersionGenerateAria', { label: option.label })

const getBudgetShort = (key: string) => {
  if (key === 'economy') return t('routePlanner.budgetShort.economy')
  if (key === 'comfort') return t('routePlanner.budgetShort.comfort')
  if (key === 'luxury') return t('routePlanner.budgetShort.luxury')
  return key
}

const formatInteger = (value?: number | string | null) =>
  new Intl.NumberFormat(toIntlLocale(locale.value), { maximumFractionDigits: 0 }).format(toFiniteAmount(value))

const formatCurrency = (value?: number | string | null, currency?: string | null) =>
  value == null || value === '' || !Number.isFinite(Number(value)) ? t('plannerFlow.amountPending') : new Intl.NumberFormat(toIntlLocale(locale.value), {
    style: 'currency',
    currency: currency || 'CNY',
    maximumFractionDigits: 0
  }).format(toFiniteAmount(value))

const formatDate = (value?: string | null) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(toIntlLocale(locale.value), {
    month: 'numeric',
    day: 'numeric'
  }).format(date)
}

const formatAltitudeMeters = (value?: number | string | null) =>
  t('routePlanner.altitudeMeters', { count: formatInteger(value) })

const riskLabel = (risk?: string) => {
  const labelKeys: Record<string, string> = {
    EXTREME: 'extreme',
    HIGH: 'high',
    MEDIUM: 'medium'
  }
  return t(`routePlanner.riskLabels.${labelKeys[risk || ''] || 'low'}`)
}

const riskClass = (risk?: string) => {
  if (risk === 'HIGH') return 'bg-rose-50 text-rose-700 border-rose-200'
  if (risk === 'EXTREME') return 'bg-red-50 text-red-800 border-red-200'
  if (risk === 'MEDIUM') return 'bg-amber-50 text-amber-700 border-amber-200'
  return 'bg-emerald-50 text-emerald-700 border-emerald-200'
}

const alertClass = (level?: string) => {
  if (level === 'HIGH') return 'border-rose-200 bg-rose-50 text-rose-800'
  if (level === 'MEDIUM') return 'border-amber-200 bg-amber-50 text-amber-800'
  return 'border-sky-200 bg-sky-50 text-sky-800'
}

const alertLabel = (level?: string) => {
  if (level === 'HIGH') return t('routePlanner.alertLabels.high')
  if (level === 'MEDIUM') return t('routePlanner.alertLabels.medium')
  return t('routePlanner.alertLabels.low')
}

const itemTypeLabel = (type: string) => {
  const labelKeys: Record<string, string> = {
    SCENIC_SPOT: 'scenicSpot',
    HOTEL: 'hotel',
    TRANSPORT: 'transport',
    MEAL: 'meal',
    EXPERIENCE: 'experience',
    NOTE: 'note'
  }
  const labelKey = labelKeys[type]
  return labelKey ? t(`routePlanner.itemTypes.${labelKey}`) : type
}

const itemIcon = (type: string) => {
  if (type === 'HOTEL') return Hotel
  if (type === 'SCENIC_SPOT') return Ticket
  if (type === 'TRANSPORT') return CalendarCheck
  return Sparkles
}

const isBookableItem = (item: ItineraryItem) =>
  (item.bookingAction === 'BOOK_SPOT' || item.bookingAction === 'BOOK_HOTEL') && item.bookingStatus !== 'BOOKED'

const getExternalBookingAriaLabel = (item: ItineraryItem) =>
  itineraryBookingItemId.value === item.id
    ? t('routePlanner.externalBookingOpeningAria', { title: item.title })
    : t('routePlanner.externalBookingOpenAria', { title: item.title })

const loading = ref(false)
const saving = ref(false)
const sharing = ref(false)
const copying = ref(false)
const itineraryLoading = ref(false)
const itineraryVersionLoading = ref<string | null>(null)
const itineraryBookingItemId = ref<number | null>(null)
const tibetTravelKitLoading = ref(false)
const streaming = ref(false)
const waitingForFirstToken = ref(false)
const generationElapsedSeconds = ref(0)
const charCount = ref(0)
const streamAbortController = ref<AbortController | null>(null)
const activeRouteJobId = ref('')
const pausedRouteJobId = ref('')
const routeResumeLoading = ref(false)
const currentAiRouteRecordId = ref<number | null>(null)
const currentAiRouteManuallySaved = ref(false)
const result = shallowRef('')
const renderedResult = shallowRef('')
const resultExpanded = ref(false)
const routeResultReaderRef = ref<HTMLElement | null>(null)
const bookableItinerary = ref<BookableItinerary | null>(null)
const itineraryQuote = ref<ItineraryQuote | null>(null)
const tibetTravelKit = ref<TibetTravelKit | null>(null)
const statusMessage = ref('')
const errorMessage = ref('')
const itineraryError = ref('')
const travelKitError = ref(false)
const isOffline = ref(!navigator.onLine)
const confirmingRegenerate = ref(false)
const reviewNotes = ref<Record<string, string>>({})
const reviewRevision = ref(0)
const updateConnection = () => { isOffline.value = !navigator.onLine }
const goToReview = () => document.getElementById('planner-check')?.scrollIntoView({ block: 'start', behavior: 'smooth' })
const markdownRenderDebounceMs = 120
let markdownWorker: Worker | null = null
let markdownRenderTimer: number | null = null
let firstTokenProgressTimer: number | null = null
let latestRenderJobId = 0
let latestAppliedRenderId = 0
let latestMarkdownSnapshot = ''
let routeGenerationRequestId = 0
let itineraryRequestId = 0
let travelKitRequestId = 0

const invalidateItineraryRequests = () => {
  itineraryRequestId += 1
  travelKitRequestId += 1
  itineraryLoading.value = false
  itineraryVersionLoading.value = null
  tibetTravelKitLoading.value = false
}

const isCurrentItineraryRequest = (requestId: number, itineraryId?: number) =>
  requestId === itineraryRequestId &&
  (itineraryId === undefined || bookableItinerary.value?.id === itineraryId)

const routeStatusRole = computed(() => errorMessage.value ? 'alert' : 'status')
const routeStatusLive = computed(() => errorMessage.value ? 'assertive' : 'polite')
const routeStatusTitle = computed(() => errorMessage.value ? t('plannerFlow.actionFailed') : t('routePlanner.statusHint'))
const routeStatusText = computed(() => errorMessage.value || statusMessage.value)
const daysValueLabel = computed(() => t('routePlanner.daysValueAria', { count: form.value.days }))
const routeGenerationProgressLabel = computed(() => t('plannerFlow.progressUnknown'))
const resultToggleButtonLabel = computed(() =>
  resultExpanded.value ? t('routePlanner.collapseFullText') : t('routePlanner.expandFullText')
)
const resultToggleLabel = computed(() =>
  resultExpanded.value ? t('routePlanner.collapseRouteFullTextAria') : t('routePlanner.expandRouteFullTextAria')
)
const canResumePausedRouteJob = computed(() =>
  Boolean(pausedRouteJobId.value && !loading.value && !streaming.value && !activeRouteJobId.value)
)

const bookableItinerarySubtitle = computed(() => {
  if (!bookableItinerary.value) return t('routePlanner.bookableItineraryGeneratingMeta')
  return t('routePlanner.bookableItineraryMeta', {
    version: bookableItinerary.value.versionLabel,
    days: t('routePlanner.daysValueCompact', { count: bookableItinerary.value.days }),
    price: formatCurrency(
      itineraryQuote.value?.totalEstimatedCost ?? bookableItinerary.value.totalEstimatedCost,
      itineraryQuote.value?.currency
    )
  })
})

const tibetTravelKitSubtitle = computed(() => {
  if (!tibetTravelKit.value) return t('routePlanner.travelKitGeneratingMeta')
  return t('routePlanner.travelKitMeta', {
    risk: riskLabel(tibetTravelKit.value.highlandAssessment.riskLevel),
    altitude: formatAltitudeMeters(tibetTravelKit.value.highlandAssessment.maxAltitudeMeters),
    pins: formatInteger(tibetTravelKit.value.offlinePackage.mapPins.length)
  })
})

const formatItineraryDayMeta = (day: ItineraryDay) =>
  [formatDate(day.travelDate), day.region].filter(Boolean).join(t('routePlanner.metaSeparator'))

const getRouteDayAriaLabel = (day: RouteSummarySection) =>
  t('routePlanner.routeDayJumpAria', { day: day.day, title: day.title })

const dismissRouteError = () => {
  errorMessage.value = ''
}

const normalizeRouteJobId = (jobId: unknown) => {
  if (typeof jobId !== 'string') return ''
  const normalized = jobId.trim()
  return SAFE_ROUTE_JOB_ID_PATTERN.test(normalized) ? normalized : ''
}

const rememberPausedRouteJob = (jobId: string) => {
  const normalized = normalizeRouteJobId(jobId)
  if (!normalized) return
  pausedRouteJobId.value = normalized
  writeBrowserStorage('sessionStorage', PAUSED_ROUTE_JOB_STORAGE_KEY, normalized)
}

const clearPausedRouteJob = (jobId = '') => {
  if (jobId && pausedRouteJobId.value && pausedRouteJobId.value !== jobId) return
  pausedRouteJobId.value = ''
  removeBrowserStorage('sessionStorage', PAUSED_ROUTE_JOB_STORAGE_KEY)
}

const restorePausedRouteJob = () => {
  pausedRouteJobId.value = normalizeRouteJobId(readBrowserStorage('sessionStorage', PAUSED_ROUTE_JOB_STORAGE_KEY, ''))
  return pausedRouteJobId.value
}

const safeRouteFailureMessage = (error: unknown) => {
  const summary = summarizeClientError(error)
  if (/abort|cancel/i.test(summary)) return t('routePlanner.generationCancelled')
  if (/timeout/i.test(summary)) return t('routePlanner.timeoutError')
  return t('routePlanner.generateFailed')
}

const responseStatus = (error: unknown) => {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return null
  }

  const response = (error as { response?: { status?: unknown } }).response
  return typeof response?.status === 'number' ? response.status : null
}

const stopFirstTokenProgress = () => {
  if (firstTokenProgressTimer !== null) {
    window.clearInterval(firstTokenProgressTimer)
    firstTokenProgressTimer = null
  }
  waitingForFirstToken.value = false
}

const startFirstTokenProgress = () => {
  stopFirstTokenProgress()
  waitingForFirstToken.value = true
  generationElapsedSeconds.value = 0
  firstTokenProgressTimer = window.setInterval(() => {
    generationElapsedSeconds.value += 1
  }, 1000)
}

const markFirstTokenReceived = () => {
  if (!waitingForFirstToken.value) return
  stopFirstTokenProgress()
}

const cleanMarkdownLine = (value: string) =>
  value
    .replace(/!\[[^\]]*]\([^)]+\)/g, '')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/^[-*+]\s+/, '')
    .replace(/^#{1,6}\s*/, '')
    .replace(/^>\s*/, '')
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim()

const stripMarkdown = (markdown: string) =>
  markdown
    .split(/\r?\n/)
    .map(cleanMarkdownLine)
    .filter(Boolean)
    .join(' ')

const getMarkdownSectionLines = (markdown: string, keywords: string[]) => {
  const lines = markdown.split(/\r?\n/)
  const startIndex = lines.findIndex(line => {
    const clean = cleanMarkdownLine(line)
    return /^#{1,3}\s+/.test(line.trim()) && keywords.some(keyword => clean.includes(keyword))
  })
  if (startIndex < 0) return []

  const endIndex = lines.findIndex((line, index) => index > startIndex && /^##\s+/.test(line.trim()))
  return lines.slice(startIndex + 1, endIndex > startIndex ? endIndex : lines.length)
}

const routeOverviewText = computed(() => {
  const section = getMarkdownSectionLines(result.value, ['路线概览', 'Overview'])
    .map(cleanMarkdownLine)
    .filter(Boolean)
    .join(' ')

  if (section) return section
  return stripMarkdown(result.value).slice(0, 160)
})

const routeResultMobileTitle = computed(() => {
  if (!result.value) return ''
  return t('routePlanner.routeTitle', {
    days: formatInteger(generatedForm.value.days),
    preference: getPreferenceText(generatedForm.value.preference)
  })
})

const routeResultMobileMeta = computed(() => {
  if (!result.value) return ''
  return t('routePlanner.routeGenComplete', { chars: formatInteger(result.value.trim().length) })
})

const routeHighlightItems = computed(() => {
  const section = getMarkdownSectionLines(result.value, ['行程亮点', '亮点', 'Highlights'])
  const items = section
    .filter(line => /^[-*+]\s+/.test(line.trim()))
    .map(cleanMarkdownLine)
    .filter(Boolean)

  if (items.length > 0) return items.slice(0, 4)

  return stripMarkdown(result.value)
    .split(/[。！？.!?]/)
    .map(item => item.trim())
    .filter(item => item.length > 10)
    .slice(0, 3)
})

const parseChineseRouteDayNumber = (value: string) => {
  const normalized = value.trim()
  if (/^\d+$/.test(normalized)) return Number(normalized)

  const digits: Record<string, number> = {
    一: 1,
    二: 2,
    三: 3,
    四: 4,
    五: 5,
    六: 6,
    七: 7,
    八: 8,
    九: 9
  }

  if (normalized.length === 1 && digits[normalized]) return digits[normalized]

  const tenIndex = normalized.indexOf('十')
  if (tenIndex >= 0) {
    const tens = tenIndex === 0 ? 1 : digits[normalized[tenIndex - 1]] || 0
    const ones = tenIndex === normalized.length - 1 ? 0 : digits[normalized[tenIndex + 1]] || 0
    const parsed = tens * 10 + ones
    return parsed > 0 ? parsed : null
  }

  return null
}

const getRouteDayIndex = (label: string) => {
  const chineseMatch = label.match(/第\s*([一二三四五六七八九十0-9]+)\s*天/i)
  if (chineseMatch) return parseChineseRouteDayNumber(chineseMatch[1])

  const dayMatch = label.match(/(?:Day|D)\s*(\d+)/i)
  if (dayMatch) return Number(dayMatch[1])

  return null
}

const expectedRouteDayCount = computed(() => {
  const days = Math.round(Number(generatedForm.value.days) || defaultForm.days)
  return Math.min(30, Math.max(1, days))
})

const routeDaySections = computed<RouteSummarySection[]>(() => {
  const lines = result.value.split(/\r?\n/)
  const sections: RouteSummarySection[] = []
  const dayHeadingPattern = /^#{1,4}\s*(第[一二三四五六七八九十0-9]+天|Day\s*\d+)[：:\s-]*(.*)$/i
  const seenDayNumbers = new Set<number>()

  let current: { day: string; title: string; lines: string[] } | null = null

  const flushCurrent = () => {
    if (!current) return
    const dayNumber = getRouteDayIndex(current.day)
    if (!dayNumber || dayNumber > expectedRouteDayCount.value || seenDayNumbers.has(dayNumber)) return
    seenDayNumbers.add(dayNumber)

    const summary = current.lines
      .map(cleanMarkdownLine)
      .filter(Boolean)
      .filter(line => !/^(第[一二三四五六七八九十0-9]+天|Day\s*\d+)/i.test(line))
      .slice(0, 2)
      .join('；')

    sections.push({
      day: current.day,
      dayNumber,
      title: current.title || t('routePlanner.routeDayFallbackTitle'),
      summary: summary || t('routePlanner.routeDayFallbackSummary')
    })
  }

  for (const line of lines) {
    const match = line.trim().match(dayHeadingPattern)
    if (match) {
      flushCurrent()
      current = {
        day: match[1].replace(/\s+/g, ' '),
        title: cleanMarkdownLine(match[2] || ''),
        lines: []
      }
      continue
    }

    if (current) {
      if (/^##\s+/.test(line.trim()) && !dayHeadingPattern.test(line.trim())) {
        flushCurrent()
        current = null
      } else {
        current.lines.push(line)
      }
    }
  }

  flushCurrent()
  return sections.sort((left, right) => left.dayNumber - right.dayNumber)
})

const visibleRouteDaySections = computed(() => routeDaySections.value.slice(0, 6))
const hiddenDayCount = computed(() => Math.max(routeDaySections.value.length - visibleRouteDaySections.value.length, 0))
const isLongRoute = computed(() => result.value.length >= 2600 || routeDaySections.value.length >= 6)
const resultShouldCollapse = computed(() => result.value.length > 1200 || routeDaySections.value.length > 4)
const resultStats = computed(() => [
  { label: t('routePlanner.statDays'), value: t('routePlanner.daysValueSpaced', { count: generatedForm.value.days }) },
  { label: t('routePlanner.statBudget'), value: getBudgetShort(generatedForm.value.budget) },
  { label: t('routePlanner.statPreference'), value: getPreferenceText(generatedForm.value.preference) },
  { label: t('routePlanner.statLength'), value: t('routePlanner.charsValue', { count: formatInteger(charCount.value || result.value.length) }) }
])
const scrollToRouteDay = async (dayLabel: string) => {
  resultExpanded.value = true
  await nextTick()
  const root = routeResultReaderRef.value
  if (!root || !dayLabel) return

  const normalize = (value: string) => value.replace(/\s+/g, '').toLowerCase()
  const targetLabel = normalize(dayLabel)
  const headings = Array.from(root.querySelectorAll('h2, h3, h4'))
  const target = headings.find(heading => normalize(heading.textContent || '').includes(targetLabel))

  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const routeGenerationProgressPercent = computed(() => loading.value ? 40 : 0)
const routeGenerationStatusLabel = computed(() =>
  result.value ? t('routePlanner.aiStreamingLabel') : t('plannerFlow.waitingResponse')
)
const routeGenerationProgressSteps = computed(() => [{
  key: 'waiting', label: routeGenerationStatusLabel.value,
  description: t('plannerFlow.elapsed', { seconds: generationElapsedSeconds.value }), active: true
}])
const routeDraftStorageKey = computed(() => {
  return auth.user ? 'colorful-tibet:route-planner:draft:session' : 'colorful-tibet:route-planner:draft:anonymous'
})

const {
  persistRouteDraft: persistRoutePlannerDraft,
  restoreRouteDraft,
  clearRouteDraft
} = useRoutePlannerDraft(
  {
    form,
    result,
    jobId: activeRouteJobId,
    statusMessage,
    errorMessage,
    loading,
    streaming
  },
  {
    defaultForm,
    storageKey: routeDraftStorageKey,
    version: 2,
    getRestoredStatusMessage: draft => t('routePlanner.routeGenComplete', { chars: formatInteger(draft.result.trim().length) }),
    onRestoreResult: restoredResult => {
      charCount.value = restoredResult.length
      resultExpanded.value = false
      scheduleMarkdownRender(restoredResult, true)
    },
    onPersistError: error => console.warn('Failed to persist route planner draft:', summarizeClientError(error)),
    onRestoreError: error => console.warn('Failed to restore route planner draft:', summarizeClientError(error))
  }
)

const persistRouteDraft = (overrides: Parameters<typeof persistRoutePlannerDraft>[0] = {}) => {
  if (routeDraftPersistenceSuspended.value) return
  persistRoutePlannerDraft(overrides)
}

function suspendRouteDraftPersistence() {
  invalidateItineraryRequests()
  bookableItinerary.value = null
  itineraryQuote.value = null
  tibetTravelKit.value = null
  reviewRevision.value++
  reviewNotes.value = {}
  routeDraftPersistenceSuspended.value = true
  clearRouteDraft()
}

const renderMarkdownSync = (markdown: string) => {
  renderedResult.value = renderMarkdownToSafeHtml(markdown)
}

const ensureMarkdownWorker = () => {
  if (markdownWorker || typeof Worker === 'undefined') return markdownWorker

  markdownWorker = new Worker(new URL('../workers/routeMarkdown.worker.ts', import.meta.url), { type: 'module' })
  markdownWorker.onmessage = (event: MessageEvent<{ id: number; html: string }>) => {
    const { id, html } = event.data
    if (id < latestAppliedRenderId) return
    latestAppliedRenderId = id
    renderedResult.value = sanitizeHtml(html)
  }
  markdownWorker.onerror = () => {
    markdownWorker?.terminate()
    markdownWorker = null
    renderMarkdownSync(latestMarkdownSnapshot)
  }

  return markdownWorker
}

const dispatchMarkdownRender = (markdown: string) => {
  latestMarkdownSnapshot = markdown
  const worker = ensureMarkdownWorker()
  if (!worker) {
    renderMarkdownSync(markdown)
    return
  }

  const nextJobId = ++latestRenderJobId
  worker.postMessage({ id: nextJobId, markdown })
}

const scheduleMarkdownRender = (markdown: string, immediate = false) => {
  latestMarkdownSnapshot = markdown

  if (immediate) {
    if (markdownRenderTimer !== null) {
      window.clearTimeout(markdownRenderTimer)
      markdownRenderTimer = null
    }
    dispatchMarkdownRender(markdown)
    return
  }

  if (markdownRenderTimer !== null) return

  markdownRenderTimer = window.setTimeout(() => {
    markdownRenderTimer = null
    dispatchMarkdownRender(latestMarkdownSnapshot)
  }, markdownRenderDebounceMs)
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

const fetchItineraryQuote = async (itineraryId: number, requestId = itineraryRequestId) => {
  const { data } = await api.get<ItineraryQuote>(endpoints.itineraries.quote(itineraryId))
  if (!isCurrentItineraryRequest(requestId, itineraryId)) return
  itineraryQuote.value = data
}

const fetchTibetTravelKit = async (itineraryId: number, requestId = itineraryRequestId) => {
  if (!isCurrentItineraryRequest(requestId, itineraryId)) return
  const kitRequestId = ++travelKitRequestId
  tibetTravelKitLoading.value = true
  travelKitError.value = false
  tibetTravelKit.value = null
  const isCurrent = () => kitRequestId === travelKitRequestId && isCurrentItineraryRequest(requestId, itineraryId)
  try {
    const { data } = await api.get<TibetTravelKit>(endpoints.tibetSpecialty.travelKit(itineraryId))
    if (!isCurrent()) return
    tibetTravelKit.value = data
  } catch (error: unknown) {
    if (!isCurrent()) return
    console.error('Failed to fetch Tibet travel kit:', summarizeClientError(error))
    tibetTravelKit.value = null
    travelKitError.value = true
  } finally {
    if (isCurrent()) tibetTravelKitLoading.value = false
  }
}

const refreshBookableItinerary = async () => {
  if (!bookableItinerary.value) return
  const itineraryId = bookableItinerary.value.id
  const requestId = itineraryRequestId
  const { data } = await api.get<BookableItinerary>(endpoints.itineraries.detail(itineraryId))
  if (!isCurrentItineraryRequest(requestId, itineraryId)) return
  bookableItinerary.value = data
}

const generateBookableItinerary = async (versionType = 'default') => {
  if (loading.value || streaming.value || itineraryLoading.value || itineraryVersionLoading.value || isOffline.value) return
  const existingItinerary = bookableItinerary.value
  const parameters = { ...generatedForm.value }
  if (!existingItinerary && parameters.days > 15) { itineraryError.value = t('plannerFlow.longTrip'); return }
  const requestId = ++itineraryRequestId
  travelKitRequestId += 1
  itineraryLoading.value = true
  itineraryError.value = ''
  itineraryQuote.value = null
  tibetTravelKit.value = null
  tibetTravelKitLoading.value = false
  travelKitError.value = false

  const isVersion = versionType !== 'default' && !!existingItinerary
  if (isVersion) {
    itineraryVersionLoading.value = versionType
  }

  try {
    if (!(await auth.ensureSession())) { await requireAuth(); return }
    if (!isCurrentItineraryRequest(requestId)) return
    const locale = readBrowserStorage('localStorage', 'locale', 'zh')
    // Retrying missing prices must not generate another itinerary or discard a selected version.
    let itinerary = existingItinerary
    if (isVersion) {
      const response = await api.post<BookableItinerary>(endpoints.itineraries.createVersion(existingItinerary!.id), { versionType })
      itinerary = response.data
    } else if (!itinerary) {
      const response = await api.post<BookableItinerary>(endpoints.itineraries.generate, {
        ...parameters,
        versionType,
        travelers: 2,
        locale
      })
      itinerary = response.data
    }
    if (!isCurrentItineraryRequest(requestId)) return
    bookableItinerary.value = itinerary
    const [quoteResult] = await Promise.allSettled([
      fetchItineraryQuote(itinerary.id, requestId),
      fetchTibetTravelKit(itinerary.id, requestId)
    ])
    if (!isCurrentItineraryRequest(requestId, itinerary.id)) return
    if (quoteResult.status === 'rejected') throw quoteResult.reason
    statusMessage.value = t('routePlanner.bookableItineraryGeneratedStatus', {
      version: itinerary.versionLabel || t('routePlanner.bookableVersionFallback')
    })
    errorMessage.value = ''
  } catch (error: unknown) {
    if (!isCurrentItineraryRequest(requestId)) return
    console.error('Failed to generate bookable itinerary:', summarizeClientError(error))
    itineraryError.value = t('plannerFlow.detailsFailed')
  } finally {
    if (isCurrentItineraryRequest(requestId)) {
      itineraryLoading.value = false
      itineraryVersionLoading.value = null
    }
  }
}

const updateGeneratedRouteContent = (markdown: string, immediate = false) => {
  result.value = markdown
  charCount.value = markdown.length
  scheduleMarkdownRender(markdown, immediate)
}

const fetchLatestAiRouteRecord = async () => {
  const response = await api.get<AiRouteRecordResponse | ''>(endpoints.routes.aiLatest, {
    validateStatus: status => status === 200 || status === 204
  })
  return response.status === 204 ? null : response.data as AiRouteRecordResponse
}

const applyAiRouteRecord = (record: AiRouteRecordResponse) => {
  if (currentAiRouteRecordId.value !== record.id) {
    invalidateItineraryRequests()
    bookableItinerary.value = null
    itineraryQuote.value = null
    tibetTravelKit.value = null
    reviewRevision.value++
    reviewNotes.value = {}
  }
  currentAiRouteRecordId.value = record.id
  currentAiRouteManuallySaved.value = Boolean(record.manuallySaved)
  generatedForm.value = {
    days: Number(record.days) || defaultForm.days,
    budget: record.budget || defaultForm.budget,
    preference: record.preference || defaultForm.preference
  }
  form.value = { ...generatedForm.value }

  if (record.content?.trim()) {
    updateGeneratedRouteContent(record.content, true)
    resultExpanded.value = true
  }

  if (record.status === 'RUNNING' && record.jobId) {
    if (pausedRouteJobId.value === record.jobId) {
      activeRouteJobId.value = ''
      loading.value = false
      streaming.value = false
      stopFirstTokenProgress()
      statusMessage.value = record.content?.trim()
        ? t('routePlanner.pausedWithSavedContent', { chars: formatInteger(record.content.trim().length) })
        : t('routePlanner.pausedWithoutContent')
      errorMessage.value = ''
      persistRouteDraft({ jobId: '', completed: false })
      return true
    }

    activeRouteJobId.value = record.jobId
    loading.value = true
    streaming.value = true
    errorMessage.value = ''
    statusMessage.value = t('routePlanner.aiGeneratingRoute', {
      days: formatInteger(record.days),
      pref: getPreferenceText(generatedForm.value.preference)
    })
    if (!record.content?.trim()) {
      startFirstTokenProgress()
    }
    persistRouteDraft({ jobId: record.jobId, completed: false })
    return true
  }

  activeRouteJobId.value = ''
  loading.value = false
  streaming.value = false
  stopFirstTokenProgress()
  if (record.jobId) {
    clearPausedRouteJob(record.jobId)
  }

  if (record.content?.trim()) {
    errorMessage.value = record.status === 'FAILED' ? t('routePlanner.generateFailed') : ''
    statusMessage.value = t('routePlanner.routeGenComplete', { chars: formatInteger(record.content.trim().length) })
    persistRouteDraft({ jobId: '', completed: record.status === 'COMPLETED' })
    return true
  }

  if (record.status === 'FAILED') {
    errorMessage.value = t('routePlanner.generateFailed')
    statusMessage.value = t('routePlanner.routeGenFailedStatus')
    persistRouteDraft({ jobId: '', completed: false })
    return true
  }

  return false
}

const restoreLatestRouteFromServer = async () => {
  try {
    const latest = await fetchLatestAiRouteRecord()
    if (!latest) return false
    return applyAiRouteRecord(latest)
  } catch (error) {
    console.warn('Failed to restore latest AI route:', summarizeClientError(error))
    return false
  }
}

const failRouteJob = (message: string) => {
  stopFirstTokenProgress()
  errorMessage.value = message
  statusMessage.value = t('routePlanner.finalFailure')
  streaming.value = false
  loading.value = false
  activeRouteJobId.value = ''
  generationStore.cancelGeneration()
  persistRouteDraft({ jobId: '', completed: false })
}

const finishRouteJob = async (jobId: string, content: string) => {
  if (!activeRouteJobId.value || activeRouteJobId.value !== jobId) return

  stopFirstTokenProgress()
  const finalContent = content || result.value
  if (finalContent.trim().length === 0) {
    failRouteJob(t('routePlanner.aiEmptyResult'))
    return
  }

  updateGeneratedRouteContent(finalContent, true)
  resultExpanded.value = true
  streaming.value = false
  loading.value = false
  activeRouteJobId.value = ''
  errorMessage.value = ''
  statusMessage.value = t('routePlanner.routeGenComplete', { chars: formatInteger(finalContent.trim().length) })
  generationStore.completeGeneration()
  persistRouteDraft({ jobId: '', completed: true })
  await generateBookableItinerary()
}

const interruptRouteJob = (jobId: string, content: string) => {
  if (!activeRouteJobId.value || activeRouteJobId.value !== jobId) return

  stopFirstTokenProgress()
  if (content.trim()) {
    updateGeneratedRouteContent(content)
  }
  rememberPausedRouteJob(jobId)
  streaming.value = false
  loading.value = false
  activeRouteJobId.value = ''
  errorMessage.value = ''
  statusMessage.value = t('routePlanner.generationInterrupted')
  generationStore.cancelGeneration()
  persistRouteDraft({ jobId, completed: false })
}

const applyRouteJobSnapshot = (snapshot: RouteGenerationJobSnapshot) => {
  if (!snapshot || (activeRouteJobId.value && snapshot.jobId !== activeRouteJobId.value)) return
  if (pausedRouteJobId.value === snapshot.jobId && !loading.value && !streaming.value) return

  activeRouteJobId.value = snapshot.jobId
  generatedForm.value = {
    days: snapshot.days,
    budget: snapshot.budget,
    preference: snapshot.preference
  }
  if (snapshot.routeRecordId) {
    currentAiRouteRecordId.value = snapshot.routeRecordId
  }
  currentAiRouteManuallySaved.value = false
  if (snapshot.content?.trim()) {
    markFirstTokenReceived()
    updateGeneratedRouteContent(snapshot.content, snapshot.status !== 'RUNNING')
  }

  if (snapshot.status === 'RUNNING') {
    loading.value = true
    streaming.value = true
    errorMessage.value = ''
    statusMessage.value = t('routePlanner.aiGeneratingRoute', {
      days: formatInteger(snapshot.days),
      pref: getPreferenceText(generatedForm.value.preference)
    })
    persistRouteDraft({ jobId: snapshot.jobId, completed: false })
  } else if (snapshot.status === 'FAILED') {
    failRouteJob(t('routePlanner.generateFailed'))
  } else if (snapshot.status === 'COMPLETED') {
    void finishRouteJob(snapshot.jobId, snapshot.content || result.value)
  }
}

const pauseRouteGeneration = () => {
  const jobId = activeRouteJobId.value
  if (!loading.value && !streaming.value && !jobId) return

  routeGenerationRequestId += 1
  streamAbortController.value?.abort()
  streamAbortController.value = null
  stopFirstTokenProgress()
  loading.value = false
  streaming.value = false
  activeRouteJobId.value = ''
  errorMessage.value = ''
  generationStore.cancelGeneration()

  if (jobId) {
    rememberPausedRouteJob(jobId)
  }

  const savedChars = result.value.trim().length
  statusMessage.value = savedChars > 0
    ? t('routePlanner.pausedWithSavedContentAndActions', { chars: formatInteger(savedChars) })
    : t('routePlanner.pausedWithoutContentServerProcessing')
  persistRouteDraft({ jobId: '', completed: false })
}

const subscribeToRouteJob = async (jobId: string) => {
  const controller = new AbortController()
  streamAbortController.value?.abort()
  streamAbortController.value = controller

  await streamRouteGenerationJob(
    jobId,
    {
      onSnapshot: applyRouteJobSnapshot,
      onDelta: (_text, fullText) => {
        markFirstTokenReceived()
        updateGeneratedRouteContent(fullText)
      },
      onReplace: content => {
        if (!content.trim()) return
        markFirstTokenReceived()
        updateGeneratedRouteContent(content, true)
      },
      onDone: content => void finishRouteJob(jobId, content),
      onInterrupted: content => interruptRouteJob(jobId, content),
      onError: () => failRouteJob(t('routePlanner.generateFailed'))
    },
    controller.signal
  )

  if (streamAbortController.value === controller) {
    streamAbortController.value = null
  }
}

const resumePausedRouteJob = async () => {
  const jobId = normalizeRouteJobId(pausedRouteJobId.value)
  if (!jobId || loading.value || streaming.value || activeRouteJobId.value || routeResumeLoading.value) return

  const requestId = ++routeGenerationRequestId
  routeResumeLoading.value = true
  loading.value = true
  streaming.value = true
  activeRouteJobId.value = jobId
  errorMessage.value = ''
  statusMessage.value = t('routePlanner.resumeGenerationStarting')
  generationStore.startGeneration()
  if (!result.value.trim()) {
    startFirstTokenProgress()
  }

  try {
    const snapshot = await getRouteGenerationJob(jobId)
    if (requestId !== routeGenerationRequestId || !loading.value || !streaming.value) {
      rememberPausedRouteJob(jobId)
      return
    }
    clearPausedRouteJob(jobId)
    applyRouteJobSnapshot(snapshot)
    if (snapshot.status === 'RUNNING') {
      await subscribeToRouteJob(snapshot.jobId)
    }
  } catch (error: unknown) {
    if (requestId !== routeGenerationRequestId && !loading.value && !streaming.value) {
      rememberPausedRouteJob(jobId)
      return
    }
    console.error('Failed to resume route job:', summarizeClientError(error))
    stopFirstTokenProgress()
    loading.value = false
    streaming.value = false
    activeRouteJobId.value = ''
    errorMessage.value = safeRouteFailureMessage(error)
    statusMessage.value = t('routePlanner.resumeGenerationFailed')
    generationStore.cancelGeneration()
    persistRouteDraft({ jobId: '', completed: false })
  } finally {
    routeResumeLoading.value = false
  }
}

const resumeRouteJobFromDraft = async () => {
  if (!activeRouteJobId.value) return

  loading.value = true
  streaming.value = true
  errorMessage.value = ''
  if (!result.value) {
    startFirstTokenProgress()
  }

  try {
    const snapshot = await getRouteGenerationJob(activeRouteJobId.value)
    applyRouteJobSnapshot(snapshot)
    if (snapshot.status === 'RUNNING') {
      await subscribeToRouteJob(snapshot.jobId)
    }
  } catch (error: unknown) {
    console.error('Failed to resume route job:', summarizeClientError(error))
    if (result.value.trim()) {
      stopFirstTokenProgress()
      loading.value = false
      streaming.value = false
      activeRouteJobId.value = ''
      statusMessage.value = t('routePlanner.routeGenComplete', { chars: formatInteger(result.value.trim().length) })
      errorMessage.value = ''
      generationStore.completeGeneration()
      persistRouteDraft({ jobId: '', completed: true })
      return
    }
    failRouteJob(t('routePlanner.generateFailed'))
  }
}

const generateRoute = async () => {
  if (loading.value || streaming.value || activeRouteJobId.value) return
  if (confirmingRegenerate.value || isOffline.value) return
  if (result.value) {
    confirmingRegenerate.value = true
    const confirmed = await showConfirm({ title: t('plannerFlow.replaceTitle'), message: t('plannerFlow.replaceMessage'), confirmLabel: t('routePlanner.regenerate'), cancelLabel: t('plannerFlow.keepCurrent') })
    confirmingRegenerate.value = false
    if (!confirmed) return
  }

  const requestId = ++routeGenerationRequestId
  const parameters = { ...form.value }
  loading.value = true
  let hasSession = false
  try {
    hasSession = await auth.ensureSession()
  } catch (error) {
    console.warn('Failed to verify route generation session:', summarizeClientError(error))
  }
  if (!hasSession) {
    loading.value = false
    await requireAuth()
    return
  }

  streaming.value = true
  invalidateItineraryRequests()
  generatedForm.value = parameters
  streamAbortController.value?.abort()
  streamAbortController.value = null
  clearPausedRouteJob()
  reviewRevision.value++
  reviewNotes.value = {}
  itineraryError.value = ''
  travelKitError.value = false
  result.value = ''
  renderedResult.value = ''
  resultExpanded.value = false
  bookableItinerary.value = null
  itineraryQuote.value = null
  tibetTravelKit.value = null
  charCount.value = 0
  copying.value = false
  errorMessage.value = ''
  activeRouteJobId.value = ''
  currentAiRouteRecordId.value = null
  currentAiRouteManuallySaved.value = false
  statusMessage.value = t('routePlanner.aiWritingHint')
  startFirstTokenProgress()
  scheduleMarkdownRender('', true)
  persistRouteDraft({ jobId: '', completed: false })
  generationStore.startGeneration()

  try {
    const snapshot = await startRouteGenerationJob(parameters)
    if (requestId !== routeGenerationRequestId || !loading.value || !streaming.value) {
      if (snapshot.jobId) {
        rememberPausedRouteJob(snapshot.jobId)
      }
      activeRouteJobId.value = ''
      persistRouteDraft({ jobId: '', completed: false })
      return
    }
    activeRouteJobId.value = snapshot.jobId
    applyRouteJobSnapshot(snapshot)
    if (snapshot.status === 'RUNNING') {
      await subscribeToRouteJob(snapshot.jobId)
    }
  } catch (error: unknown) {
    if (requestId !== routeGenerationRequestId && !loading.value && !streaming.value) {
      persistRouteDraft({ jobId: '', completed: false })
      return
    }
    console.error('Failed to generate route:', summarizeClientError(error))
    stopFirstTokenProgress()
    errorMessage.value = safeRouteFailureMessage(error)
    statusMessage.value = t('routePlanner.routeGenFailedStatus')
    generationStore.cancelGeneration()
    loading.value = false
    streaming.value = false
    activeRouteJobId.value = ''
    streamAbortController.value = null
    persistRouteDraft({ jobId: '', completed: false })
  }
}

const copyResult = async () => {
  if (!result.value || copying.value) return
  copying.value = true
  try {
    await navigator.clipboard.writeText(result.value)
    statusMessage.value = t('routePlanner.copiedToClipboard')
    errorMessage.value = ''
    persistRouteDraft()
  } catch (error) {
    console.error('Copy failed:', summarizeClientError(error))
    errorMessage.value = t('routePlanner.copyFailed')
    persistRouteDraft()
  } finally {
    copying.value = false
  }
}

const saveRoute = async () => {
  if (!result.value || saving.value || loading.value || isOffline.value || currentAiRouteManuallySaved.value) return
  saving.value = true
  const revision = reviewRevision.value
  const content = result.value
  const parameters = { ...generatedForm.value }
  const isCurrent = () => revision === reviewRevision.value && content === result.value
  try {
    if (!(await auth.ensureSession())) { await requireAuth(); return }
    if (!isCurrent()) return
    let recordId = currentAiRouteRecordId.value
    if (!recordId) {
      const latest = await fetchLatestAiRouteRecord()
      if (!isCurrent()) return
      if (latest?.content === content && latest.days === parameters.days &&
          latest.budget === parameters.budget && latest.preference === parameters.preference) {
        recordId = latest.id
      }
    }
    if (!recordId) {
      throw new Error('AI route record is missing')
    }

    const response = await api.post<AiRouteRecordResponse>(endpoints.routes.saveAiRoute(recordId))
    if (!isCurrent()) return
    currentAiRouteRecordId.value = response.data.id
    currentAiRouteManuallySaved.value = true
    statusMessage.value = t('routePlanner.routeSavedPrivate')
    errorMessage.value = ''
    persistRouteDraft()
  } catch (error: unknown) {
    if (!isCurrent()) return
    console.error('Failed to save AI route record:', summarizeClientError(error))
    errorMessage.value = t('routePlanner.saveFailed')
    persistRouteDraft()
  } finally {
    saving.value = false
  }
}

const downloadRoute = () => {
  if (!result.value) return
  const notes = Object.entries(reviewNotes.value).filter(([, note]) => note.trim()).map(([day, note]) => `### ${day}\n${note}`).join('\n\n')
  const exportText = `${result.value}\n\n---\n${t('plannerFlow.priceBoundary')}${notes ? `\n\n## ${t('plannerFlow.notesTitle')}\n${t('plannerFlow.notesBoundary')}\n\n${notes}` : ''}`
  const blob = new Blob([exportText], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = t('routePlanner.downloadFilename', { days: formatInteger(generatedForm.value.days) })
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const getPreferenceText = (key: string) => {
  return t(`routePlanner.preferenceOptions.${key}`)
}

const communityBudgetValue = (key: string) => {
  const valueKeys: Record<string, string> = {
    economy: 'economy',
    comfort: 'comfort',
    luxury: 'luxury'
  }
  const valueKey = valueKeys[key]
  return valueKey ? t(`routePlanner.communityBudget.${valueKey}`) : key
}

const communityPreferenceValue = (key: string) => {
  const valueKeys: Record<string, string> = {
    natural: 'natural',
    cultural: 'cultural',
    photography: 'photography',
    relaxation: 'relaxation'
  }
  const valueKey = valueKeys[key]
  return valueKey ? t(`routePlanner.communityPreference.${valueKey}`) : key
}

const shareRoute = async () => {
  if (!result.value || sharing.value) return

  sharing.value = true
  const content = result.value
  const parameters = { ...generatedForm.value }
  let hasSession = false
  try {
    hasSession = await auth.ensureSession()
  } catch (error) {
    console.warn('Failed to verify route share session:', summarizeClientError(error))
  }
  if (!hasSession) {
    sharing.value = false
    await requireAuth()
    return
  }

  try {
    await api.post(endpoints.routes.share, {
      title: t('routePlanner.routeTitle', { days: formatInteger(parameters.days), preference: getPreferenceText(parameters.preference) }),
      content,
      days: parameters.days,
      budget: communityBudgetValue(parameters.budget),
      preference: communityPreferenceValue(parameters.preference)
    })
    showToast(t('routePlanner.shareSuccess'), 'success')
    router.push('/community')
  } catch (error: unknown) {
    console.error('Share failed:', summarizeClientError(error))
    if (responseStatus(error) === 401) {
      await requireAuth()
    } else {
      showToast(t('routePlanner.shareFailed'), 'error')
    }
  } finally {
    sharing.value = false
  }
}

const bookItineraryItem = (item: ItineraryItem) => {
  if (!bookableItinerary.value || !isBookableItem(item)) return

  itineraryBookingItemId.value = item.id
  const opened = openExternalBooking({
    kind: item.bookingAction === 'BOOK_HOTEL' ? 'hotel' : 'spot',
    name: item.hotelName || item.scenicSpotName || item.title
  })
  if (opened) {
    statusMessage.value = t('routePlanner.externalBookingOpened')
    errorMessage.value = ''
  } else {
    errorMessage.value = t('routePlanner.externalBookingBlocked')
  }
  window.setTimeout(() => {
    if (itineraryBookingItemId.value === item.id) {
      itineraryBookingItemId.value = null
    }
  }, 600)
}

const downloadOfflinePackage = () => {
  if (!tibetTravelKit.value) return
  const payload = JSON.stringify(tibetTravelKit.value, null, 2)
  const blob = new Blob([payload], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `tibet-travel-kit-${tibetTravelKit.value.itineraryId}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

watch(
  () => auth.isLoggedIn,
  (isLoggedIn, wasLoggedIn) => {
    if (wasLoggedIn && !isLoggedIn) {
      suspendRouteDraftPersistence()
    } else if (isLoggedIn) {
      routeDraftPersistenceSuspended.value = false
    }
  },
  { flush: 'sync' }
)

watch(form, () => persistRouteDraft(), { deep: true })

onMounted(async () => {
  window.addEventListener('auth-expired', onAuthExpired)
  window.addEventListener('online', updateConnection)
  window.addEventListener('offline', updateConnection)
  generationStore.acknowledgeResult()
  restorePausedRouteJob()
  const hasSession = await auth.ensureSession()
  const restoredFromServer = hasSession ? await restoreLatestRouteFromServer() : false
  if (!restoredFromServer) {
    restoreRouteDraft()
  }
  if (activeRouteJobId.value) {
    void resumeRouteJobFromDraft()
  }
})

onBeforeUnmount(() => {
  invalidateItineraryRequests()
  window.removeEventListener('auth-expired', onAuthExpired)
  window.removeEventListener('online', updateConnection)
  window.removeEventListener('offline', updateConnection)
  if (streamAbortController.value) {
    streamAbortController.value.abort()
    streamAbortController.value = null
  }
  stopFirstTokenProgress()

  persistRouteDraft()
  if (markdownRenderTimer !== null) {
    window.clearTimeout(markdownRenderTimer)
  }
  if (firstTokenProgressTimer !== null) {
    window.clearInterval(firstTokenProgressTimer)
  }
  markdownWorker?.terminate()
})
</script>

<style scoped>
.planner-workspace { padding-bottom: 9rem; container-type: inline-size; }
@container (max-width: 850px) {
  .planner-columns { display: flex; flex-direction: column; }
  .planner-columns > * { width: 100%; }
}
.planner-heading { display: flex; align-items: end; justify-content: space-between; gap: 1.5rem; margin-bottom: 1.75rem; }
.planner-eyebrow { color: #8b2e3a; font-size: .75rem; font-weight: 700; letter-spacing: .12em; margin-bottom: .75rem; }
.planner-heading h1 { font-size: clamp(1.75rem, 3vw, 2.5rem); line-height: 1.35; }
.planner-intro { color: #685b51; margin-top: .75rem; line-height: 1.7; }
.planner-back { color: #71313c; font-size: .875rem; padding: .75rem 0; flex-shrink: 0; }
.planner-steps { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border-block: 1px solid #dcd3c7; margin-bottom: 1.5rem; }
.planner-steps > * { display: flex; align-items: center; gap: .6rem; padding: 1rem .75rem; font-size: .875rem; color: #6b6058; }
.planner-steps > a { color: #802b38; font-weight: 700; }
.planner-steps > * > span { font-family: Georgia, serif; font-size: 1rem; }
.planner-guide { background: #ece8dd; border-radius: 1rem; color: #564d45; border: 1px solid #ddd5c8; }
.planner-guide-number { color: #856c45; font-family: Georgia, serif; letter-spacing: .2em; margin-bottom: 1.25rem; }
.planner-guide-list { margin-top: 2rem; width: 100%; }
.planner-guide-list li { display: flex; gap: 1rem; border-top: 1px solid #d6cebf; padding: 1rem 0; }
.planner-guide-list b { color: #8b2e3a; font-family: Georgia, serif; }
.planner-guide-list strong { font-size: .9rem; }
.planner-guide-list p { font-size: .8125rem; margin-top: .35rem; line-height: 1.7; }
.planner-guide-footnote { border-top: 1px solid #d6cebf; padding-top: 1rem; font-size: .8125rem; line-height: 1.8; }
.planner-context { font-size: .8125rem; line-height: 1.7; color: #62564d; }
.planner-choice { padding: .7rem; border-width: 1px; gap: .65rem; }
.planner-choice-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.planner-choice > span:first-of-type { display: none; }
.planner-choice > span:last-child { width: 1rem; height: 1rem; }
.planner-choice p { line-height: 1.6; }
.planner-form label { margin-bottom: .5rem; }
.planner-generate { background: #8b2e3a; box-shadow: none; }
.planner-check { background: #f3eee4; border-block: 1px solid #ded4c4; color: #554b43; }
.planner-primary, .planner-secondary { min-height: 44px; border-radius: .65rem; padding: .65rem 1rem; display: inline-flex; align-items: center; justify-content: center; font-size: .875rem; font-weight: 600; }
.planner-primary { background: #8b2e3a; color: white; }
.planner-secondary { border: 1px solid #c9bfb1; background: #fff; color: #5d493f; }
.planner-notice { padding: 1rem; background: #fff5df; border: 1px solid #d9bc78; border-radius: .75rem; color: #6c4e1b; font-size: .875rem; line-height: 1.7; margin-bottom: 1rem; }
.route-planner-page :is(button, a, input):focus-visible { outline: 3px solid #2d5f8a; outline-offset: 3px; }
.route-planner-page button:disabled { cursor: not-allowed; opacity: .55; }
.route-planner-page :is([id^="planner-"], h2, h3, h4) { scroll-margin-top: 7rem; }
.route-planner-page :deep([class*="text-tibet-brown/"]) { color: #716257; }
.route-planner-page .glass-card { border: 1px solid #e1d9cd; border-radius: 1rem; box-shadow: 0 4px 18px #33291f05; }
@media (max-width: 1023px) {
  .planner-columns { display: flex; flex-direction: column; }
  .planner-columns > * { width: 100%; }
}
@media (max-width: 639px) {
  .planner-review-section { padding: 0; }
  .planner-review-section :deep(.route-review) { border: 0; border-radius: 0; }
  .planner-heading { display: block; margin-bottom: 1.25rem; }
  .planner-back { display: inline-flex; margin-top: .5rem; }
  .planner-steps > * { padding: .85rem .25rem; gap: .3rem; font-size: .75rem; }
  .planner-choice { align-items: flex-start; }
  .planner-guide { padding: 1.25rem; }
  .planner-intro { font-size: .875rem; }
}
@media (prefers-reduced-motion: reduce) {
  .route-planner-page * { scroll-behavior: auto !important; }
}
/* ===== Animations ===== */
.animate-shimmer-stream::after {
  content: '';
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.45) 50%,
    transparent 100%
  );
  animation: shimmerStream 2s linear infinite;
  will-change: transform;
}

@keyframes shimmerStream {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* ===== Glass Card ===== */
.glass-card {
  background: rgba(255, 255, 255, 0.92);
}

/* ===== Page Background ===== */
.route-planner-page {
  background-color: #F7F3EE;
}

/* ===== Prose overrides ===== */
.prose {
  --tw-prose-body: #4b5563;
  --tw-prose-headings: #121212;
  --tw-prose-links: #2D5F8A;
  --tw-prose-bold: #1d1d1f;
}

.route-result-prose {
  transition: max-height 0.28s ease;
  min-width: 0;
  overflow-wrap: anywhere;
}

.route-long-reader,
.route-long-flow {
  min-width: 0;
}

.route-long-flow::after {
  display: block;
  clear: both;
  content: "";
}

.route-long-markdown {
  display: contents;
}

.route-long-nav {
  max-width: min(260px, 45%);
}

.route-day-nav {
  scrollbar-width: thin;
  scrollbar-color: rgba(45, 95, 138, 0.28) transparent;
}

.route-day-nav::-webkit-scrollbar {
  width: 6px;
}

.route-day-nav::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(45, 95, 138, 0.28);
}

.route-result-prose :deep(h1) {
  font-size: 1.45rem;
  line-height: 1.25;
  margin-bottom: 0.8rem;
  scroll-margin-top: 6rem;
}

.route-result-prose :deep(h2) {
  font-size: 1.05rem;
  line-height: 1.35;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  scroll-margin-top: 6rem;
}

.route-result-prose :deep(h3) {
  font-size: 0.98rem;
  line-height: 1.4;
  margin-top: 1.15rem;
  margin-bottom: 0.45rem;
  scroll-margin-top: 6rem;
}

.route-result-prose :deep(p),
.route-result-prose :deep(li) {
  font-size: 0.92rem;
  line-height: 1.72;
  margin-top: 0.28rem;
  margin-bottom: 0.28rem;
}

.route-result-prose :deep(ul),
.route-result-prose :deep(ol) {
  margin-top: 0.45rem;
  margin-bottom: 0.85rem;
}

.route-result-prose :deep(a),
.route-result-prose :deep(code) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.route-result-prose :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
}

.route-result-prose-compact :deep(h1) {
  font-size: 1.35rem;
}

.route-result-prose-compact :deep(h2) {
  margin-top: 1.15rem;
  margin-bottom: 0.55rem;
}

.route-result-prose-compact :deep(h3) {
  margin-top: 0.9rem;
  margin-bottom: 0.35rem;
}

.route-result-prose-compact :deep(p),
.route-result-prose-compact :deep(li) {
  line-height: 1.62;
  margin-top: 0.18rem;
  margin-bottom: 0.18rem;
}

.route-result-prose-compact :deep(ul),
.route-result-prose-compact :deep(ol) {
  margin-top: 0.3rem;
  margin-bottom: 0.62rem;
}

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .route-planner-page {
    background-color: #F7F3EE;
  }

  .route-planner-page :deep(.sticky) {
    position: static;
  }

  .route-result-prose :deep(h1) {
    font-size: 1.18rem;
  }

  .route-result-prose :deep(h2) {
    font-size: 1rem;
    margin-top: 1.05rem;
  }

  .route-result-prose :deep(p),
  .route-result-prose :deep(li) {
    font-size: 0.88rem;
    line-height: 1.65;
  }

  .route-result-prose :deep(table) {
    display: block;
    max-width: 100%;
    overflow-x: auto;
    white-space: nowrap;
  }
}
</style>
