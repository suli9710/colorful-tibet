<template>
  <div class="route-planner-page tibet-page-shell min-h-screen relative overflow-hidden">
    <div class="absolute inset-0 pointer-events-none overflow-hidden opacity-45 tibet-cloud-pattern"></div>

    <div class="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12 md:py-20">
      <!-- ===== HEADER ===== -->
      <motion.header
        class="text-center mb-12"
        :initial="revealInitial"
        :animate="revealInView"
        :transition="revealTransition"
      >
        <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/70 border border-tibet-gold/30 backdrop-blur text-xs font-semibold text-tibet-red tracking-widest uppercase mb-6">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
          {{ t('routePlanner.aiGeneratorLabel') }}
        </div>
        <h1 class="tibet-heading inline-flex justify-center text-4xl md:text-5xl lg:text-6xl font-bold text-tibet-dark leading-tight">
          {{ t('routePlanner.title') }}
        </h1>
        <p class="mt-4 text-lg text-tibet-brown/70 max-w-2xl mx-auto leading-relaxed">
          {{ t('routePlanner.subtitle') }}
        </p>
        <!-- Tibetan ornament divider -->
        <div class="mt-6 flex items-center justify-center gap-3">
          <span class="h-px w-12 bg-gradient-to-r from-transparent to-tibet-gold/50"></span>
          <span class="h-2.5 w-2.5 rotate-45 border border-tibet-gold/70 bg-tibet-gold/20"></span>
          <span class="h-px w-12 bg-gradient-to-l from-transparent to-tibet-gold/50"></span>
        </div>
      </motion.header>

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
        </motion.div>
      </AnimatePresence>

      <!-- ===== MAIN CONTENT ===== -->
      <div class="grid gap-8 lg:grid-cols-5 items-start">
        <!-- LEFT: Form (takes 2 cols on lg) -->
        <div class="lg:col-span-2">
          <div class="sticky top-20 space-y-6">
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
                    <h2 class="text-lg font-bold text-tibet-dark">{{ t('routePlanner.aiRouteGenTitle') }}</h2>
                    <p class="text-xs text-tibet-brown/50">{{ t('routePlanner.aiRouteGenDesc') }}</p>
                  </div>
                </div>
              </div>

              <form @submit.prevent="generateRoute" class="p-6 space-y-6">
                <!-- Days Selector -->
                <motion.div layout>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.plannedDays') }}</label>
                  <div class="flex items-stretch gap-0 rounded-2xl border border-tibet-gold/25 bg-white/70 overflow-hidden">
                    <motion.button type="button" @click="adjustDays(-1)" :disabled="loading || form.days <= 1"
                            :whileHover="{ backgroundColor: 'rgba(255, 255, 255, 0.82)' }"
                            :whileTap="{ scale: 0.94 }"
                            class="flex items-center justify-center w-12 shrink-0 text-xl text-tibet-brown/70 hover:bg-gray-50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/></svg>
                    </motion.button>
                    <div class="flex-1 flex flex-col items-center justify-center py-3 border-x border-tibet-gold/20">
                      <motion.span
                        :key="form.days"
                        class="text-4xl font-bold text-tibet-dark leading-none tabular-nums"
                        :initial="{ opacity: 0, y: 8, scale: 0.94 }"
                        :animate="{ opacity: 1, y: 0, scale: 1 }"
                        :transition="{ duration: 0.22, ease: motionEase }"
                      >{{ form.days }}</motion.span>
                      <span class="text-[10px] uppercase tracking-[0.2em] text-tibet-brown/50 mt-1">Days</span>
                    </div>
                    <motion.button type="button" @click="adjustDays(1)" :disabled="loading || form.days >= 30"
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
                  <div class="grid gap-2">
                    <motion.label v-for="opt in budgetOptions" :key="opt.value"
                           @click="form.budget = opt.value"
                           class="relative flex items-center gap-3 rounded-xl border-2 cursor-pointer transition-all duration-200 px-4 py-3"
                           layout
                           :whileHover="{ y: -2, scale: 1.01 }"
                           :whileTap="{ scale: 0.99 }"
                           :transition="softSpring"
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
                  <div class="grid gap-2">
                    <motion.label v-for="opt in preferenceOptions" :key="opt.value"
                           @click="form.preference = opt.value"
                           class="relative flex items-center gap-3 rounded-xl border-2 cursor-pointer transition-all duration-200 px-4 py-3"
                           layout
                           :whileHover="{ y: -2, scale: 1.01 }"
                           :whileTap="{ scale: 0.99 }"
                           :transition="softSpring"
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
                      <motion.span v-if="form.preference === opt.value" class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-tibet-blue text-white"
                            :initial="{ opacity: 0, scale: 0.6, rotate: -20 }"
                            :animate="{ opacity: 1, scale: 1, rotate: 0 }"
                            :transition="softSpring">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                      </motion.span>
                    </motion.label>
                  </div>
                </div>

                <!-- Quick Presets -->
                <div>
                  <label class="block text-sm font-semibold text-tibet-dark/80 mb-3">{{ t('routePlanner.quickPresets') }}</label>
                  <div class="grid grid-cols-2 gap-2">
                    <motion.button type="button" v-for="preset in presets" :key="preset.label" @click="applyPreset(preset)" :disabled="loading"
                            :whileHover="{ y: -2, scale: 1.01 }"
                            :whileTap="{ scale: 0.98 }"
                            class="group flex flex-col items-start gap-0.5 rounded-xl border border-tibet-gold/20 bg-white/60 px-3.5 py-3 text-left transition-all hover:border-tibet-gold/40 hover:bg-amber-50/40 hover:shadow-sm disabled:opacity-40 disabled:cursor-not-allowed">
                      <span class="text-xs font-semibold text-tibet-dark/80 group-hover:text-tibet-dark">{{ preset.label }}</span>
                      <span class="text-[11px] text-tibet-brown/50">{{ preset.days }}{{ t('routePlanner.daysUnit') }} · {{ getBudgetShort(preset.budget) }}</span>
                    </motion.button>
                  </div>
                </div>

                <!-- Generate Button -->
                <motion.button type="submit" :disabled="loading"
                        :whileHover="loading ? {} : { y: -2, scale: 1.01 }"
                        :whileTap="loading ? {} : { scale: 0.98 }"
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
                </motion.button>
              </form>
            </motion.div>

            <!-- Streaming Indicator -->
            <AnimatePresence>
              <motion.div
                v-if="loading"
                key="route-planner-streaming"
                class="glass-card rounded-2xl p-5 border border-sky-200/60 bg-sky-50/70"
                :initial="{ opacity: 0, y: 14, scale: 0.98 }"
                :animate="{ opacity: 1, y: 0, scale: 1 }"
                :exit="{ opacity: 0, y: 8, scale: 0.98 }"
                :transition="{ duration: 0.34, ease: motionEase }"
              >
                <div class="flex items-center gap-3 mb-3">
                  <span class="relative flex h-3 w-3">
                    <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-sky-400 opacity-75"></span>
                    <span class="relative inline-flex rounded-full h-3 w-3 bg-tibet-turquoise"></span>
                  </span>
                  <span class="text-sm font-semibold text-tibet-blue">{{ routeGenerationStatusLabel }}</span>
                  <span v-if="charCount > 0" class="ml-auto text-xs text-tibet-blue/70 font-mono">{{ charCount }} {{ t('routePlanner.charCountUnit') }}</span>
                </div>
                <div class="h-1.5 rounded-full bg-tibet-gold/15 overflow-hidden">
                  <div
                    class="h-full rounded-full bg-gradient-to-r from-tibet-blue via-tibet-turquoise to-tibet-gold relative overflow-hidden animate-shimmer-stream transition-[width] duration-700 ease-out"
                    :style="{ width: `${routeGenerationProgressPercent}%` }"
                    :class="{ 'animate-pulse': streaming }"
                  ></div>
                </div>
                <p class="mt-2 text-xs text-tibet-brown/60">{{ t('routePlanner.waitingTime') }}</p>
              </motion.div>
            </AnimatePresence>

            <motion.div
              class="glass-card rounded-2xl border border-white/50 p-5 shadow-lg shadow-slate-900/3"
              :initial="cardInitial"
              :animate="cardInView"
              :transition="cardTransition(0, 0.1)"
            >
              <div class="mb-4 flex items-center gap-3">
                <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-tibet-blue/10 text-tibet-blue">
                  <Sparkles class="h-4 w-4" />
                </span>
                <div>
                  <h3 class="text-sm font-bold text-tibet-dark">当前方案</h3>
                  <p class="text-xs text-tibet-brown/50">{{ result ? '已生成路线摘要' : '待生成路线参数' }}</p>
                </div>
              </div>

              <div class="grid grid-cols-3 gap-2">
                <div v-for="stat in plannerSnapshotStats" :key="stat.label" class="rounded-xl border border-tibet-gold/15 bg-white/70 px-3 py-2">
                  <p class="text-[11px] font-semibold text-tibet-brown/45">{{ stat.label }}</p>
                  <p class="mt-1 truncate text-sm font-bold text-tibet-dark">{{ stat.value }}</p>
                </div>
              </div>

              <div class="mt-4 space-y-2">
                <div v-for="item in planningPulseItems" :key="item" class="flex gap-2 rounded-xl border border-tibet-gold/10 bg-white/65 px-3 py-2">
                  <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-tibet-gold"></span>
                  <p class="text-xs leading-relaxed text-tibet-brown/65">{{ item }}</p>
                </div>
              </div>
            </motion.div>
          </div>
        </div>

        <!-- RIGHT: Results (takes 3 cols on lg) -->
        <div class="lg:col-span-3">
          <!-- Empty state -->
          <AnimatePresence mode="wait">
          <motion.div
            v-if="!result && !loading"
            key="route-planner-empty"
            class="flex flex-col items-center justify-center py-20 text-center"
            :initial="{ opacity: 0, y: 18, scale: 0.98 }"
            :animate="{ opacity: 1, y: 0, scale: 1 }"
            :exit="{ opacity: 0, y: -10, scale: 0.98 }"
            :transition="revealTransition"
          >
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
            <h3 class="text-xl font-bold text-tibet-brown/50 mb-2">{{ t('routePlanner.emptyTitle') }}</h3>
            <p class="text-sm text-tibet-brown/40 max-w-sm">{{ t('routePlanner.emptyDescription') }}</p>
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

              <div class="mt-6 grid gap-3 sm:grid-cols-3">
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
                  <motion.button v-if="!isLongRoute && resultShouldCollapse" @click="resultExpanded = !resultExpanded"
                          :whileHover="{ y: -1, scale: 1.02 }"
                          :whileTap="{ scale: 0.96 }"
                          class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-blue/20 bg-white/80 px-3.5 py-2 text-xs font-medium text-tibet-blue transition-all hover:bg-sky-50">
                    <ChevronUp v-if="resultExpanded" class="h-4 w-4" />
                    <ChevronDown v-else class="h-4 w-4" />
                    {{ resultExpanded ? '收起全文' : '展开全文' }}
                  </motion.button>
                  <motion.button @click="copyResult" :disabled="copying"
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
                v-if="visibleRouteDaySections.length"
                class="border-b border-white/60 bg-white/45 px-5 py-5 md:px-6"
                :class="{ 'xl:hidden': isLongRoute }"
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

              <!-- Long-route desktop navigation -->
              <aside
                v-if="isLongRoute"
                class="hidden border-b border-white/60 bg-gradient-to-br from-white/80 via-amber-50/45 to-sky-50/35 p-4 xl:block"
              >
                <div class="route-long-reader rounded-2xl border border-white/70 bg-white/65 p-5">
                  <div class="mb-4 flex items-center justify-between gap-3">
                    <div>
                      <p class="text-sm font-bold text-tibet-dark">{{ t('routePlanner.routeFullText') }}</p>
                      <p class="text-xs text-tibet-brown/50">{{ t('routePlanner.routeLongReaderHint') }}</p>
                    </div>
                  </div>

                  <div
                    ref="routeResultReaderRef"
                    class="prose prose-slate route-result-prose route-result-prose-compact route-long-flow flow-root max-w-none prose-headings:text-tibet-dark prose-p:text-tibet-brown/80 prose-p:leading-relaxed prose-a:text-tibet-blue prose-strong:text-tibet-dark/90 prose-li:text-tibet-brown/80 prose-h2:border-b prose-h2:border-tibet-gold/20 prose-h2:pb-2 prose-h2:mt-8 prose-h2:mb-4 prose-img:rounded-2xl prose-img:shadow-md"
                  >
                    <div class="not-prose route-long-nav float-left mb-4 mr-5 w-[260px] rounded-2xl border border-white/70 bg-white/75 p-4 shadow-sm">
                      <div class="mb-3 flex items-center gap-2 text-tibet-dark">
                        <ListChecks class="h-4 w-4 text-tibet-blue" />
                        <h3 class="text-sm font-bold">{{ t('routePlanner.routeNavigation') }}</h3>
                      </div>

                      <div class="grid grid-cols-2 gap-2">
                        <div v-for="stat in resultStats" :key="stat.label" class="rounded-xl border border-tibet-gold/10 bg-white/75 px-3 py-2">
                          <p class="text-[10px] font-semibold text-tibet-brown/45">{{ stat.label }}</p>
                          <p class="mt-1 truncate text-xs font-bold text-tibet-dark">{{ stat.value }}</p>
                        </div>
                      </div>

                      <div v-if="routeOverviewText" class="mt-3 rounded-2xl border border-tibet-gold/15 bg-white/75 px-3 py-3">
                        <div class="mb-2 flex items-center gap-2 text-tibet-dark">
                          <Sparkles class="h-3.5 w-3.5 text-tibet-gold" />
                          <h4 class="text-xs font-bold">{{ t('routePlanner.routeOverview') }}</h4>
                        </div>
                        <p class="line-clamp-5 text-xs leading-relaxed text-tibet-brown/65">{{ routeOverviewText }}</p>
                      </div>

                      <div v-if="routeHighlightItems.length" class="mt-3 space-y-2">
                        <p class="text-xs font-bold text-tibet-dark">{{ t('routePlanner.routeHighlights') }}</p>
                        <div v-for="item in routeHighlightItems" :key="item" class="flex gap-2 rounded-xl border border-tibet-gold/10 bg-white/70 px-3 py-2">
                          <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-tibet-gold"></span>
                          <p class="line-clamp-2 text-[11px] leading-relaxed text-tibet-brown/65">{{ item }}</p>
                        </div>
                      </div>

                      <div v-if="routeDaySections.length" class="mt-3 space-y-2">
                        <div class="flex items-center justify-between gap-2">
                          <p class="text-xs font-bold text-tibet-dark">{{ t('routePlanner.routeDailyDirectory') }}</p>
                          <span class="text-[11px] font-medium text-tibet-brown/45">{{ t('routePlanner.routeAllDays', { count: routeDaySections.length }) }}</span>
                        </div>
                        <div class="max-h-[42vh] space-y-1.5 overflow-y-auto pr-1 route-day-nav">
                          <button
                            v-for="day in routeDaySections"
                            :key="day.day"
                            type="button"
                            @click="scrollToRouteDay(day.day)"
                            class="w-full rounded-xl border border-tibet-gold/10 bg-white/70 px-3 py-2 text-left transition hover:border-tibet-blue/25 hover:bg-sky-50"
                          >
                            <p class="text-[11px] font-bold text-tibet-blue">{{ day.day }}</p>
                            <p class="mt-0.5 line-clamp-1 text-xs font-semibold text-tibet-dark">{{ day.title }}</p>
                          </button>
                        </div>
                      </div>

                      <div class="mt-4 grid grid-cols-2 gap-1.5">
                        <button
                          type="button"
                          @click="copyResult"
                          :disabled="copying"
                          class="truncate rounded-xl border border-tibet-gold/20 bg-white/80 px-2 py-2 text-xs font-medium text-gray-600 transition hover:bg-gray-50 disabled:opacity-50"
                        >
                          {{ t('routePlanner.copyText') }}
                        </button>
                        <button
                          type="button"
                          @click="saveRoute"
                          :disabled="saving || !result"
                          class="truncate rounded-xl border border-tibet-gold/20 bg-white/80 px-2 py-2 text-xs font-medium text-gray-600 transition hover:bg-gray-50 disabled:opacity-50"
                        >
                          {{ t('routePlanner.saveRoute') }}
                        </button>
                        <button
                          type="button"
                          @click="downloadRoute"
                          :disabled="!result"
                          class="truncate rounded-xl border border-tibet-gold/20 bg-white/80 px-2 py-2 text-xs font-medium text-gray-600 transition hover:bg-gray-50 disabled:opacity-50"
                        >
                          {{ t('routePlanner.downloadMarkdown') }}
                        </button>
                        <button
                          type="button"
                          @click="shareRoute"
                          :disabled="sharing || !result"
                          class="truncate rounded-xl bg-tibet-blue px-2 py-2 text-xs font-semibold text-white transition hover:bg-tibet-blue/90 disabled:opacity-50"
                        >
                          {{ t('routePlanner.shareToCommunity') }}
                        </button>
                      </div>
                    </div>

                    <div class="route-long-markdown contents" v-html="renderedResult"></div>
                  </div>
                </div>
              </aside>

              <!-- Markdown content -->
              <div
                class="px-6 pb-6 pt-5 md:px-8 md:pb-8 lg:px-10"
                :class="{ 'xl:hidden': isLongRoute }"
              >
                <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-bold text-tibet-dark">{{ t('routePlanner.routeFullText') }}</p>
                    <p class="text-xs text-tibet-brown/50">{{ t('routePlanner.routeShortReaderHint') }}</p>
                  </div>
                  <motion.button v-if="!isLongRoute && resultShouldCollapse" @click="resultExpanded = !resultExpanded"
                          :whileHover="{ y: -1, scale: 1.02 }"
                          :whileTap="{ scale: 0.96 }"
                          class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-blue/20 bg-white/80 px-3.5 py-2 text-xs font-medium text-tibet-blue transition-all hover:bg-sky-50">
                    <ChevronUp v-if="resultExpanded" class="h-4 w-4" />
                    <ChevronDown v-else class="h-4 w-4" />
                    {{ resultExpanded ? '收起全文' : '展开全文' }}
                  </motion.button>
                </div>
                <div class="relative">
                  <div
                    class="prose prose-slate route-result-prose max-w-none prose-headings:text-tibet-dark prose-p:text-tibet-brown/80 prose-p:leading-relaxed prose-a:text-tibet-blue prose-strong:text-tibet-dark/90 prose-li:text-tibet-brown/80 prose-h2:border-b prose-h2:border-tibet-gold/20 prose-h2:pb-2 prose-h2:mt-8 prose-h2:mb-4 prose-img:rounded-2xl prose-img:shadow-md"
                    :class="{
                      'max-h-[420px] overflow-hidden': !isLongRoute && resultShouldCollapse && !resultExpanded,
                      'route-result-prose-compact': isLongRoute
                    }"
                    v-html="renderedResult"
                  ></div>
                  <div v-if="!isLongRoute && resultShouldCollapse && !resultExpanded" class="pointer-events-none absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-white via-white/95 to-transparent"></div>
                </div>
              </div>

              <!-- Result actions -->
              <div
                class="px-6 py-4 border-t border-white/60 bg-white/30 flex flex-wrap items-center justify-end gap-2"
                :class="{ 'xl:hidden': isLongRoute }"
              >
                <motion.button @click="generateRoute" :disabled="loading"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  {{ t('routePlanner.regenerate') }}
                </motion.button>
                <motion.button @click="saveRoute" :disabled="saving || !result"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                  </svg>
                  {{ saving ? t('routePlanner.saving') : t('routePlanner.saveRoute') }}
                </motion.button>
                <motion.button @click="downloadRoute" :disabled="!result"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-medium text-gray-600 transition-all hover:bg-gray-50 disabled:opacity-50">
                  <Download class="h-4 w-4" />
                  {{ t('routePlanner.downloadMarkdown') }}
                </motion.button>
                <motion.button @click="shareRoute" :disabled="sharing || !result"
                        :whileHover="{ y: -1, scale: 1.02 }"
                        :whileTap="{ scale: 0.96 }"
                        class="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-tibet-blue to-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-tibet-blue/20 transition-all hover:from-tibet-blue/90 hover:to-indigo-700 hover:shadow-lg disabled:opacity-50">
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
        v-if="bookableItinerary || itineraryLoading || tibetTravelKit || tibetTravelKitLoading"
        class="mt-8 grid items-start gap-6 xl:grid-cols-2"
      >
          <motion.div
            v-if="bookableItinerary || itineraryLoading"
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
                  <p class="text-sm font-bold text-tibet-dark">{{ bookableItinerary?.title || '正在生成可预订行程' }}</p>
                  <p class="text-xs text-tibet-brown/50">
                    {{ bookableItinerary ? `${bookableItinerary.versionLabel} · ${bookableItinerary.days}天 · ${formatCurrency(itineraryQuote?.totalEstimatedCost || bookableItinerary.totalEstimatedCost)}` : '正在匹配景点、酒店、门票和预算' }}
                  </p>
                </div>
              </div>
              <div v-if="bookableItinerary" class="flex flex-wrap gap-1.5">
                <router-link
                  to="/orders"
                  class="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50/80 px-3 py-2 text-xs font-semibold text-emerald-700 transition-all hover:bg-emerald-100"
                >
                  <ReceiptText class="h-3.5 w-3.5" />
                  订单中心
                </router-link>
                <motion.button
                  v-for="option in itineraryVersionOptions"
                  :key="option.value"
                  @click="generateBookableItinerary(option.value)"
                  :disabled="!!itineraryVersionLoading"
                  :whileHover="{ y: -1, scale: 1.02 }"
                  :whileTap="{ scale: 0.96 }"
                  class="inline-flex items-center gap-1.5 rounded-xl border border-tibet-gold/25 bg-white/80 px-3 py-2 text-xs font-medium text-gray-600 transition-all hover:bg-amber-50 disabled:opacity-50"
                >
                  <Shuffle class="h-3.5 w-3.5" />
                  {{ itineraryVersionLoading === option.value ? '生成中' : option.label }}
                </motion.button>
              </div>
            </div>

            <div v-if="itineraryLoading" class="p-8 text-center text-tibet-brown/60">
              <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-tibet-gold mx-auto mb-3"></div>
              <p class="text-sm font-medium">正在把 AI 灵感转成可报价行程</p>
            </div>

            <div v-else-if="bookableItinerary" class="p-5 md:p-6 space-y-5">
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div class="rounded-2xl border border-tibet-gold/20 bg-white/70 px-4 py-3">
                  <p class="text-xs text-tibet-brown/50">总预估</p>
                  <p class="mt-1 text-xl font-bold text-tibet-dark">{{ formatCurrency(itineraryQuote?.totalEstimatedCost || bookableItinerary.totalEstimatedCost) }}</p>
                </div>
                <div class="rounded-2xl border border-emerald-200 bg-emerald-50/70 px-4 py-3">
                  <p class="text-xs text-emerald-700/70">可直接预订</p>
                  <p class="mt-1 text-xl font-bold text-emerald-700">{{ formatCurrency(itineraryQuote?.bookableTotal) }}</p>
                </div>
                <div class="rounded-2xl border border-sky-200 bg-sky-50/70 px-4 py-3">
                  <p class="text-xs text-sky-700/70">交通餐饮预估</p>
                  <p class="mt-1 text-xl font-bold text-sky-700">{{ formatCurrency(itineraryQuote?.informationalTotal) }}</p>
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
                      <p class="mt-0.5 text-xs text-tibet-brown/50">{{ day.travelDate }} · {{ day.region }}</p>
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
                            <span v-if="item.bookingStatus === 'BOOKED'" class="rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">已预订</span>
                          </div>
                          <p class="mt-1 text-xs leading-relaxed text-tibet-brown/60 line-clamp-2">{{ item.description }}</p>
                          <p v-if="item.alternatives" class="mt-1 text-[11px] text-tibet-blue/70">可替换：{{ item.alternatives }}</p>
                        </div>
                      </div>
                      <div class="flex shrink-0 items-center justify-between gap-3 md:justify-end">
                        <span class="text-sm font-bold text-tibet-dark">{{ formatCurrency(item.estimatedCost) }}</span>
                        <motion.button
                          v-if="isBookableItem(item)"
                          @click="bookItineraryItem(item)"
                          :disabled="itineraryBookingItemId === item.id"
                          :whileHover="{ y: -1, scale: 1.02 }"
                          :whileTap="{ scale: 0.96 }"
                          class="inline-flex items-center gap-1.5 rounded-xl bg-tibet-red px-3.5 py-2 text-xs font-semibold text-tibet-yellow shadow-md shadow-tibet-red/20 disabled:opacity-50"
                        >
                          <CalendarCheck class="h-3.5 w-3.5" />
                          {{ itineraryBookingItemId === item.id ? '预订中' : '立即预订' }}
                        </motion.button>
                      </div>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </motion.div>

          <motion.div
            v-if="tibetTravelKit || tibetTravelKitLoading"
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
                  <p class="text-sm font-bold text-tibet-dark">西藏深度旅行包</p>
                  <p class="text-xs text-tibet-brown/50">
                    {{ tibetTravelKit ? `${tibetTravelKit.highlandAssessment.riskLabel} · 最高海拔 ${tibetTravelKit.highlandAssessment.maxAltitudeMeters}m · ${tibetTravelKit.offlinePackage.mapPins.length} 个离线点位` : '正在生成高原、礼仪、短语和提醒' }}
                  </p>
                </div>
              </div>
              <motion.button
                v-if="tibetTravelKit"
                @click="downloadOfflinePackage"
                :whileHover="{ y: -1, scale: 1.02 }"
                :whileTap="{ scale: 0.96 }"
                class="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-white/80 px-3.5 py-2 text-xs font-semibold text-emerald-700 transition-all hover:bg-emerald-50"
              >
                <Download class="h-3.5 w-3.5" />
                离线包
              </motion.button>
            </div>

            <div v-if="tibetTravelKitLoading" class="p-8 text-center text-tibet-brown/60">
              <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-500 mx-auto mb-3"></div>
              <p class="text-sm font-medium">正在生成西藏目的地服务</p>
            </div>

            <div v-else-if="tibetTravelKit" class="p-5 md:p-6 space-y-5">
              <div class="grid gap-3 md:grid-cols-3">
                <div class="rounded-2xl border px-4 py-3" :class="riskClass(tibetTravelKit.highlandAssessment.riskLevel)">
                  <div class="flex items-center gap-2">
                    <HeartPulse class="h-4 w-4" />
                    <p class="text-xs font-semibold">高原适应</p>
                  </div>
                  <p class="mt-2 text-2xl font-bold">{{ tibetTravelKit.highlandAssessment.riskScore }}</p>
                  <p class="mt-1 text-xs leading-relaxed">{{ tibetTravelKit.highlandAssessment.summary }}</p>
                </div>
                <div class="rounded-2xl border border-sky-200 bg-sky-50/70 px-4 py-3 text-sky-800">
                  <div class="flex items-center gap-2">
                    <MapPin class="h-4 w-4" />
                    <p class="text-xs font-semibold">离线旅行包</p>
                  </div>
                  <p class="mt-2 text-2xl font-bold">{{ tibetTravelKit.offlinePackage.mapPins.length }}</p>
                  <p class="mt-1 text-xs leading-relaxed">点位、紧急电话、凭证提示和离线清单已打包。</p>
                </div>
                <div class="rounded-2xl border border-emerald-200 bg-emerald-50/70 px-4 py-3 text-emerald-800">
                  <div class="flex items-center gap-2">
                    <Leaf class="h-4 w-4" />
                    <p class="text-xs font-semibold">可持续旅行</p>
                  </div>
                  <p class="mt-2 text-2xl font-bold">{{ tibetTravelKit.sustainableOptions.length }}</p>
                  <p class="mt-1 text-xs leading-relaxed">本地向导、顺路合并、生态零遗留建议。</p>
                </div>
              </div>

              <div class="grid gap-4 xl:grid-cols-2">
                <section class="rounded-2xl border border-tibet-gold/15 bg-white/75 p-4">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <HeartPulse class="h-4 w-4 text-rose-600" />
                    <h3 class="text-sm font-bold">每日高原节奏</h3>
                  </div>
                  <div class="mt-3 space-y-3">
                    <div
                      v-for="advice in tibetTravelKit.highlandAssessment.dailyAdvice.slice(0, 3)"
                      :key="advice.dayNumber"
                      class="rounded-xl border border-tibet-gold/10 bg-white/80 px-3 py-2"
                    >
                      <div class="flex items-center justify-between gap-2">
                        <p class="text-xs font-semibold text-tibet-dark">D{{ advice.dayNumber }} · {{ advice.maxAltitudeMeters }}m</p>
                        <span class="rounded-full border px-2 py-0.5 text-[11px] font-semibold" :class="riskClass(advice.riskLevel)">{{ riskLabel(advice.riskLevel) }}</span>
                      </div>
                      <p class="mt-1 text-xs leading-relaxed text-tibet-brown/60">{{ advice.paceAdvice }}</p>
                    </div>
                  </div>
                </section>

                <section class="rounded-2xl border border-tibet-gold/15 bg-white/75 p-4">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <Bell class="h-4 w-4 text-amber-600" />
                    <h3 class="text-sm font-bold">实时提醒</h3>
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
                    <h3 class="text-sm font-bold">文化礼仪助手</h3>
                  </div>
                  <div class="mt-3 space-y-3">
                    <div v-for="tip in tibetTravelKit.culturalTips.slice(0, 2)" :key="tip.scene" class="rounded-xl bg-amber-50/60 px-3 py-2">
                      <p class="text-xs font-semibold text-tibet-dark">{{ tip.title }}</p>
                      <p class="mt-1 text-xs leading-relaxed text-tibet-brown/60">{{ tip.doTips[0] }}</p>
                      <p class="mt-1 text-[11px] text-rose-700/80">避免：{{ tip.avoidTips[0] }}</p>
                    </div>
                  </div>
                </section>

                <section class="rounded-2xl border border-tibet-gold/15 bg-white/75 p-4">
                  <div class="flex items-center gap-2 text-tibet-dark">
                    <Languages class="h-4 w-4 text-sky-600" />
                    <h3 class="text-sm font-bold">藏汉英导览短语</h3>
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
                  <h3 class="text-sm font-bold">离线和可持续清单</h3>
                </div>
                <div class="mt-3 grid gap-3 md:grid-cols-2">
                  <div>
                    <p class="text-xs font-semibold text-emerald-900/80">离线准备</p>
                    <ul class="mt-2 space-y-1">
                      <li v-for="item in tibetTravelKit.offlinePackage.offlineChecklist.slice(0, 4)" :key="item" class="text-xs leading-relaxed text-emerald-900/70">
                        {{ item }}
                      </li>
                    </ul>
                  </div>
                  <div>
                    <p class="text-xs font-semibold text-emerald-900/80">低影响旅行</p>
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

  <PaymentModal
    :show="showPaymentModal"
    :amount="pendingPaymentItem?.estimatedCost"
    recaptcha-action="itinerary_booking"
    @close="resetPendingItineraryBooking"
    @status-check="handleItineraryPaymentStatusCheck"
  />

  <MobileStickyActionBar
    :show="Boolean(result)"
    :eyebrow="t('routePlanner.routeGenerated')"
    :title="routeResultMobileTitle"
    :meta="routeResultMobileMeta"
    :secondary-label="t('routePlanner.copyText')"
    :primary-label="sharing ? t('routePlanner.sharing') : t('routePlanner.shareToCommunity')"
    :secondary-disabled="copying"
    :primary-disabled="sharing || !result"
    @secondary="copyResult"
    @primary="shareRoute"
  />
</template>

<script setup lang="ts">
import { ref, computed, shallowRef, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AnimatePresence, motion } from 'motion-v'
import { marked } from 'marked'
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
import { sanitizeHtml } from '../utils/sanitize'
import {
  getRouteGenerationJob,
  startRouteGenerationJob,
  streamRouteGenerationJob,
  type RouteGenerationJobSnapshot
} from '../api/stream'
import api, { endpoints, type AiRouteRecordResponse } from '../api'
import PaymentModal from '../components/PaymentModal.vue'
import MobileStickyActionBar from '../components/MobileStickyActionBar.vue'
import { useRoutePlannerDraft, type RoutePlannerFormState } from '../composables/useRoutePlannerDraft'
import { useBehaviorTracker } from '../composables/useBehaviorTracker'
import { useAuthGuard } from '../composables/useAuthGuard'
import { useAuthStore } from '../stores/auth'
import { useRouteGenerationStore } from '../stores/routeGeneration'
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

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const generationStore = useRouteGenerationStore()
const { encodeBehaviorData, reset: resetBehavior } = useBehaviorTracker()
const { requireAuth } = useAuthGuard()

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

const onAuthExpired = () => {
  if (window.location.pathname !== '/login') alert(t('routePlanner.authFailed'))
}

const form = ref<RoutePlannerFormState>({ ...defaultForm })

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

const itineraryVersionOptions = [
  { value: 'cheaper', label: '更省钱' },
  { value: 'relaxed', label: '更轻松' },
  { value: 'hidden', label: '更小众' },
  { value: 'family', label: '老人小孩' }
]

const getBudgetShort = (key: string) => {
  if (key === 'economy') return t('routePlanner.budgetShort.economy')
  if (key === 'comfort') return t('routePlanner.budgetShort.comfort')
  if (key === 'luxury') return t('routePlanner.budgetShort.luxury')
  return key
}

const formatCurrency = (value?: number | string | null) => {
  const amount = Number(value || 0)
  return amount.toLocaleString('zh-CN', { style: 'currency', currency: 'CNY', maximumFractionDigits: 0 })
}

const riskLabel = (risk?: string) => {
  if (risk === 'EXTREME') return '极高风险'
  if (risk === 'HIGH') return '高海拔'
  if (risk === 'MEDIUM') return '中海拔'
  return '低海拔'
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
  if (level === 'HIGH') return '重要'
  if (level === 'MEDIUM') return '提醒'
  return '提示'
}

const itemTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    SCENIC_SPOT: '景点',
    HOTEL: '酒店',
    TRANSPORT: '交通',
    MEAL: '餐饮',
    EXPERIENCE: '体验',
    NOTE: '提示'
  }
  return labels[type] || type
}

const itemIcon = (type: string) => {
  if (type === 'HOTEL') return Hotel
  if (type === 'SCENIC_SPOT') return Ticket
  if (type === 'TRANSPORT') return CalendarCheck
  return Sparkles
}

const isBookableItem = (item: ItineraryItem) =>
  (item.bookingAction === 'BOOK_SPOT' || item.bookingAction === 'BOOK_HOTEL') && item.bookingStatus !== 'BOOKED'

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
const markdownRenderDebounceMs = 120
let markdownWorker: Worker | null = null
let markdownRenderTimer: number | null = null
let firstTokenProgressTimer: number | null = null
let latestRenderJobId = 0
let latestAppliedRenderId = 0
let latestMarkdownSnapshot = ''

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
    days: form.value.days,
    preference: getPreferenceText(form.value.preference)
  })
})

const routeResultMobileMeta = computed(() => {
  if (!result.value) return ''
  return t('routePlanner.routeGenComplete', { chars: result.value.trim().length })
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
  const days = Math.round(Number(form.value.days) || defaultForm.days)
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
      title: current.title || '当日行程',
      summary: summary || '查看完整路线了解当天细节。'
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
  { label: '天数', value: `${form.value.days} 天` },
  { label: '预算', value: getBudgetShort(form.value.budget) },
  { label: '偏好', value: getPreferenceText(form.value.preference) },
  { label: '篇幅', value: `${charCount.value || result.value.length} 字` }
])
const plannerSnapshotStats = computed(() => [
  { label: '天数', value: `${form.value.days}天` },
  { label: '预算', value: getBudgetShort(form.value.budget) },
  { label: '偏好', value: getPreferenceText(form.value.preference) }
])
const scrollToRouteDay = (dayLabel: string) => {
  const root = routeResultReaderRef.value
  if (!root || !dayLabel) return

  const normalize = (value: string) => value.replace(/\s+/g, '').toLowerCase()
  const targetLabel = normalize(dayLabel)
  const headings = Array.from(root.querySelectorAll('h2, h3, h4'))
  const target = headings.find(heading => normalize(heading.textContent || '').includes(targetLabel))

  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const routeGenerationStage = computed(() => {
  if (!waitingForFirstToken.value || result.value) return 'streaming'
  if (generationElapsedSeconds.value < 4) return 'collecting'
  if (generationElapsedSeconds.value < 11) return 'thinking'
  return 'composing'
})
const routeGenerationProgressPercent = computed(() => {
  if (!loading.value) return 0
  if (!waitingForFirstToken.value || result.value) return 100
  return Math.min(88, 18 + generationElapsedSeconds.value * 5)
})
const routeGenerationStatusLabel = computed(() => {
  if (waitingForFirstToken.value && !result.value) {
    if (routeGenerationStage.value === 'collecting') return t('routePlanner.firstTokenCollecting')
    if (routeGenerationStage.value === 'thinking') return t('routePlanner.firstTokenThinking')
    return t('routePlanner.firstTokenComposing')
  }
  return streaming.value ? t('routePlanner.aiStreamingLabel') : t('routePlanner.preparingLabel')
})
const routeGenerationProgressSteps = computed(() => [
  {
    key: 'collecting',
    label: t('routePlanner.firstTokenStepCollecting'),
    description: t('routePlanner.firstTokenStepCollectingDesc'),
    active: routeGenerationStage.value === 'collecting'
  },
  {
    key: 'thinking',
    label: t('routePlanner.firstTokenStepThinking'),
    description: t('routePlanner.firstTokenStepThinkingDesc'),
    active: routeGenerationStage.value === 'thinking'
  },
  {
    key: 'composing',
    label: t('routePlanner.firstTokenStepComposing'),
    description: t('routePlanner.firstTokenStepComposingDesc'),
    active: routeGenerationStage.value === 'composing'
  }
])
const planningPulseItems = computed(() => {
  if (result.value) {
    const dayCount = routeDaySections.value.length
    const highlightItems = routeHighlightItems.value.slice(0, 3)

    return [
      dayCount > 0 ? `已整理 ${dayCount} 个每日安排节点。` : '路线正文已生成，可在右侧查看完整内容。',
      ...highlightItems
    ].slice(0, 4)
  }

  return [
    `${form.value.days}天行程，偏向${getPreferenceText(form.value.preference)}。`,
    `预算档位为${getBudgetShort(form.value.budget)}，会优先匹配相应住宿与体验强度。`,
    '生成后可继续查看可预订行程、报价与高原旅行包。'
  ]
})

const routeDraftStorageKey = computed(() => {
  const userId = auth.user?.id
  return userId ? `colorful-tibet:route-planner:draft:${userId}` : 'colorful-tibet:route-planner:draft:anonymous'
})

const { persistRouteDraft, restoreRouteDraft } = useRoutePlannerDraft(
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
    getRestoredStatusMessage: draft => t('routePlanner.routeGenComplete', { chars: draft.result.trim().length }),
    onRestoreResult: restoredResult => {
      charCount.value = restoredResult.length
      resultExpanded.value = false
      scheduleMarkdownRender(restoredResult, true)
    },
    onPersistError: error => console.warn('Failed to persist route planner draft:', error),
    onRestoreError: error => console.warn('Failed to restore route planner draft:', error)
  }
)

const renderMarkdownSync = (markdown: string) => {
  renderedResult.value = sanitizeHtml(markdown ? String(marked.parse(markdown)) : '')
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

const fetchItineraryQuote = async (itineraryId: number) => {
  const { data } = await api.get(endpoints.itineraries.quote(itineraryId))
  itineraryQuote.value = data
}

const fetchTibetTravelKit = async (itineraryId: number) => {
  tibetTravelKitLoading.value = true
  try {
    const { data } = await api.get(endpoints.tibetSpecialty.travelKit(itineraryId))
    tibetTravelKit.value = data
  } catch (error: any) {
    console.error('Failed to fetch Tibet travel kit:', error)
    tibetTravelKit.value = null
  } finally {
    tibetTravelKitLoading.value = false
  }
}

const refreshBookableItinerary = async () => {
  if (!bookableItinerary.value) return
  const { data } = await api.get(endpoints.itineraries.detail(bookableItinerary.value.id))
  bookableItinerary.value = data
}

const generateBookableItinerary = async (versionType = 'default') => {
  if (!(await auth.ensureSession())) {
    await requireAuth()
    return
  }

  const isVersion = versionType !== 'default' && !!bookableItinerary.value
  if (isVersion) {
    itineraryVersionLoading.value = versionType
  } else {
    itineraryLoading.value = true
  }

  try {
    const locale = localStorage.getItem('locale') || 'zh'
    const response = isVersion
      ? await api.post(endpoints.itineraries.createVersion(bookableItinerary.value!.id), { versionType })
      : await api.post(endpoints.itineraries.generate, {
          days: form.value.days,
          budget: form.value.budget,
          preference: form.value.preference,
          versionType,
          travelers: 2,
          locale
        })

    bookableItinerary.value = response.data
    await fetchItineraryQuote(response.data.id)
    await fetchTibetTravelKit(response.data.id)
    statusMessage.value = `已生成${response.data.versionLabel || '可预订'}行程，可查看报价并预订节点。`
    errorMessage.value = ''
  } catch (error: any) {
    console.error('Failed to generate bookable itinerary:', error)
    errorMessage.value = error.response?.data?.error || '可预订行程生成失败'
  } finally {
    itineraryLoading.value = false
    itineraryVersionLoading.value = null
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
  currentAiRouteRecordId.value = record.id
  currentAiRouteManuallySaved.value = Boolean(record.manuallySaved)
  form.value = {
    days: Number(record.days) || defaultForm.days,
    budget: record.budget || defaultForm.budget,
    preference: record.preference || defaultForm.preference
  }

  if (record.content?.trim()) {
    updateGeneratedRouteContent(record.content, true)
    resultExpanded.value = true
  }

  if (record.status === 'RUNNING' && record.jobId) {
    activeRouteJobId.value = record.jobId
    loading.value = true
    streaming.value = true
    errorMessage.value = ''
    statusMessage.value = t('routePlanner.aiGeneratingRoute', {
      days: record.days,
      pref: getPreferenceText(record.preference || form.value.preference)
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

  if (record.content?.trim()) {
    errorMessage.value = record.status === 'FAILED' ? record.errorMessage || t('routePlanner.generateFailed') : ''
    statusMessage.value = t('routePlanner.routeGenComplete', { chars: record.content.trim().length })
    persistRouteDraft({ jobId: '', completed: record.status === 'COMPLETED' })
    return true
  }

  if (record.status === 'FAILED') {
    errorMessage.value = record.errorMessage || t('routePlanner.generateFailed')
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
    console.warn('Failed to restore latest AI route:', error)
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
  statusMessage.value = t('routePlanner.routeGenComplete', { chars: finalContent.trim().length })
  generationStore.completeGeneration()
  persistRouteDraft({ jobId: '', completed: true })
  await generateBookableItinerary()
}

const applyRouteJobSnapshot = (snapshot: RouteGenerationJobSnapshot) => {
  if (!snapshot || (activeRouteJobId.value && snapshot.jobId !== activeRouteJobId.value)) return

  activeRouteJobId.value = snapshot.jobId
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
      days: snapshot.days,
      pref: getPreferenceText(form.value.preference)
    })
    persistRouteDraft({ jobId: snapshot.jobId, completed: false })
  } else if (snapshot.status === 'FAILED') {
    failRouteJob(snapshot.errorMessage || t('routePlanner.generateFailed'))
  } else if (snapshot.status === 'COMPLETED') {
    void finishRouteJob(snapshot.jobId, snapshot.content || result.value)
  }
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
      onError: message => failRouteJob(message)
    },
    controller.signal
  )

  if (streamAbortController.value === controller) {
    streamAbortController.value = null
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
  } catch (error: any) {
    console.error('Failed to resume route job:', error)
    if (result.value.trim()) {
      stopFirstTokenProgress()
      loading.value = false
      streaming.value = false
      activeRouteJobId.value = ''
      statusMessage.value = t('routePlanner.routeGenComplete', { chars: result.value.trim().length })
      errorMessage.value = ''
      generationStore.completeGeneration()
      persistRouteDraft({ jobId: '', completed: true })
      return
    }
    failRouteJob(error.message || t('routePlanner.generateFailed'))
  }
}

const generateRoute = async () => {
  if (!(await auth.ensureSession())) {
    await requireAuth()
    return
  }

  loading.value = true
  streaming.value = true
  streamAbortController.value?.abort()
  streamAbortController.value = null
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
    const snapshot = await startRouteGenerationJob(form.value)
    activeRouteJobId.value = snapshot.jobId
    applyRouteJobSnapshot(snapshot)
    if (snapshot.status === 'RUNNING') {
      await subscribeToRouteJob(snapshot.jobId)
    }
  } catch (error: any) {
    console.error('Failed to generate route:', error)
    stopFirstTokenProgress()
    let message = t('routePlanner.generateFailed')
    if (error.name === 'AbortError') {
      message = t('routePlanner.generationCancelled')
    } else if (error.message?.includes('timeout')) {
      message = t('routePlanner.timeoutError')
    }
    errorMessage.value = message
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
    console.error('Copy failed:', error)
    errorMessage.value = t('routePlanner.copyFailed')
    persistRouteDraft()
  } finally {
    copying.value = false
  }
}

const saveRoute = async () => {
  if (!result.value || saving.value) return

  if (!(await auth.ensureSession())) {
    await requireAuth()
    return
  }

  saving.value = true
  try {
    let recordId = currentAiRouteRecordId.value
    if (!recordId) {
      const latest = await fetchLatestAiRouteRecord()
      if (latest?.content?.trim()) {
        applyAiRouteRecord(latest)
        recordId = latest.id
      }
    }
    if (!recordId) {
      throw new Error('AI route record is missing')
    }

    const response = await api.post<AiRouteRecordResponse>(endpoints.routes.saveAiRoute(recordId))
    currentAiRouteRecordId.value = response.data.id
    currentAiRouteManuallySaved.value = true
    statusMessage.value = t('routePlanner.routeSavedPrivate')
    errorMessage.value = ''
    persistRouteDraft()
  } catch (error: any) {
    console.error('Failed to save AI route record:', error)
    errorMessage.value = error.response?.data?.error || t('routePlanner.saveFailed')
    persistRouteDraft()
  } finally {
    saving.value = false
  }
}

const downloadRoute = () => {
  if (!result.value) return
  const blob = new Blob([result.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = t('routePlanner.downloadFilename', { days: form.value.days })
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const getPreferenceText = (key: string) => {
  return t(`routePlanner.preferenceOptions.${key}`)
}

const communityBudgetValue = (key: string) => {
  const values: Record<string, string> = {
    economy: '经济型',
    comfort: '舒适型',
    luxury: '豪华型'
  }
  return values[key] || key
}

const communityPreferenceValue = (key: string) => {
  const values: Record<string, string> = {
    natural: '自然风光',
    cultural: '人文历史',
    photography: '深度摄影',
    relaxation: '休闲度假'
  }
  return values[key] || key
}

const shareRoute = async () => {
  if (!result.value) return

  if (!(await auth.ensureSession())) {
    await requireAuth()
    return
  }

  sharing.value = true
  try {
    await api.post('/routes/share', {
      title: t('routePlanner.routeTitle', { days: form.value.days, preference: getPreferenceText(form.value.preference) }),
      content: result.value,
      days: form.value.days,
      budget: communityBudgetValue(form.value.budget),
      preference: communityPreferenceValue(form.value.preference)
    })
    alert(t('routePlanner.shareSuccess'))
    router.push('/community')
  } catch (error: any) {
    console.error('Share failed:', error)
    if (error.response && error.response.status === 401) {
      await requireAuth()
    } else {
      alert(t('routePlanner.shareFailed'))
    }
  } finally {
    sharing.value = false
  }
}

const showPaymentModal = ref(false)
const pendingPaymentItem = ref<ItineraryItem | null>(null)
const pendingBookingPhone = ref('')

const resetPendingItineraryBooking = () => {
  showPaymentModal.value = false
  pendingPaymentItem.value = null
  pendingBookingPhone.value = ''
}

const bookItineraryItem = async (item: ItineraryItem) => {
  if (!bookableItinerary.value || !isBookableItem(item)) return

  if (!(await auth.ensureSession())) {
    await requireAuth()
    return
  }

  const phone = item.bookingAction === 'BOOK_HOTEL'
    ? (window.prompt(t('hotel.phonePlaceholder')) || '').trim()
    : ''

  if (item.bookingAction === 'BOOK_HOTEL' && !phone) {
    return
  }

  pendingBookingPhone.value = phone
  pendingPaymentItem.value = item
  showPaymentModal.value = true
}

const handleItineraryPaymentStatusCheck = async (recaptchaToken = '') => {
  const item = pendingPaymentItem.value
  if (!item || !bookableItinerary.value) return

  showPaymentModal.value = false
  const behaviorData = encodeBehaviorData()

  const user = auth.user || {}
  const guestName = String(user.nickname || user.username || '')
  const phone = pendingBookingPhone.value

  itineraryBookingItemId.value = item.id
  try {
    const { data } = await api.post(endpoints.itineraries.bookItem(bookableItinerary.value.id, item.id), {
      travelers: 2,
      guestName,
      phone
    }, {
      headers: {
        ...(recaptchaToken ? { 'X-Recaptcha-Token': recaptchaToken } : {}),
        ...(behaviorData ? { 'X-Behavior-Data': behaviorData } : {}),
      }
    })
    statusMessage.value = `${data.message || '预订成功'}，可在订单中心查看。`
    errorMessage.value = ''
    await refreshBookableItinerary()
    await fetchItineraryQuote(bookableItinerary.value.id)
    await fetchTibetTravelKit(bookableItinerary.value.id)
  } catch (error: any) {
    console.error('Failed to book itinerary item:', error)
    errorMessage.value = error.response?.data?.error || '预订失败'
  } finally {
    itineraryBookingItemId.value = null
    pendingPaymentItem.value = null
    pendingBookingPhone.value = ''
    resetBehavior()
  }
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

watch(form, () => persistRouteDraft(), { deep: true })

onMounted(async () => {
  window.addEventListener('auth-expired', onAuthExpired)
  generationStore.acknowledgeResult()
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
  window.removeEventListener('auth-expired', onAuthExpired)
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
