<template>
  <div class="min-h-screen tibet-page-shell py-16 sm:py-24">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <motion.div
        class="mb-10 text-center sm:mb-12"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <h1 class="tibet-heading inline-flex justify-center text-3xl font-bold text-tibet-dark mb-4 sm:text-4xl">{{ t('community.title') }}</h1>
        <p class="text-base text-tibet-brown/70 sm:text-lg">{{ t('community.subtitle') }}</p>
      </motion.div>

      <!-- Tabs -->
      <div class="-mx-4 mb-8 overflow-x-auto px-4 pb-2 sm:mx-0 sm:flex sm:justify-center sm:overflow-visible sm:px-0 sm:pb-0">
        <LayoutGroup>
        <motion.div
          class="tibet-panel inline-flex min-w-max gap-1 rounded-2xl p-1.5"
          role="tablist"
          :aria-label="t('community.title')"
          @keydown="handleTabKeydown"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <motion.button
            type="button"
            role="tab"
            id="community-tab-routes"
            aria-controls="community-panel-routes"
            :aria-selected="activeTab === 'routes'"
            :tabindex="activeTab === 'routes' ? 0 : -1"
            :title="t('community.sharedRoutes')"
            @click="selectTab('routes')"
            class="min-h-11 px-6 py-2.5 rounded-xl font-semibold text-sm transition-colors duration-300 flex items-center gap-2 relative overflow-hidden whitespace-nowrap focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
            :class="activeTab === 'routes' ? 'text-tibet-yellow' : 'text-tibet-brown/70 hover:text-tibet-dark/80'"
            :whileHover="{ y: -2, scale: 1.03 }"
            :whilePress="{ scale: 0.95 }"
          >
            <motion.span
              v-if="activeTab === 'routes'"
              layoutId="community-tab-pill"
              class="absolute inset-0 rounded-xl bg-tibet-red shadow-md"
              :transition="softSpring"
            ></motion.span>
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l5.447 2.724A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
            </svg>
            <span class="relative z-10">{{ t('community.sharedRoutes') }}</span>
          </motion.button>
          <motion.button
            type="button"
            role="tab"
            id="community-tab-qa"
            aria-controls="community-panel-qa"
            :aria-selected="activeTab === 'qa'"
            :tabindex="activeTab === 'qa' ? 0 : -1"
            :title="t('community.travelQA')"
            @click="selectTab('qa')"
            class="min-h-11 px-6 py-2.5 rounded-xl font-semibold text-sm transition-colors duration-300 flex items-center gap-2 relative overflow-hidden whitespace-nowrap focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
            :class="activeTab === 'qa' ? 'text-tibet-yellow' : 'text-tibet-brown/70 hover:text-tibet-dark/80'"
            :whileHover="{ y: -2, scale: 1.03 }"
            :whilePress="{ scale: 0.95 }"
          >
            <motion.span
              v-if="activeTab === 'qa'"
              layoutId="community-tab-pill"
              class="absolute inset-0 rounded-xl bg-tibet-red shadow-md"
              :transition="softSpring"
            ></motion.span>
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span class="relative z-10">{{ t('community.travelQA') }}</span>
          </motion.button>
        </motion.div>
        </LayoutGroup>
      </div>

      <!-- ==================== ROUTE SHARING TAB ==================== -->
      <section
        id="community-panel-routes"
        role="tabpanel"
        aria-labelledby="community-tab-routes"
        :hidden="activeTab !== 'routes'"
      >
        <motion.div
          class="tibet-panel mb-8 flex flex-col gap-4 rounded-2xl p-4 sm:p-6 lg:flex-row lg:items-center lg:justify-between"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <div class="-mx-1 flex gap-3 overflow-x-auto px-1 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0" role="group" :aria-label="t('community.sharedRoutes')">
            <select v-model="routeFilters.days" @change="applyRouteFilters" :aria-label="t('community.allDays')" :title="t('community.allDays')" class="min-h-11 min-w-36 shrink-0 px-4 py-2 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-sm">
              <option value="">{{ t('community.allDays') }}</option>
              <option value="3">3{{ t('community.days') }}</option>
              <option value="5">5{{ t('community.days') }}</option>
              <option value="7">7{{ t('community.days') }}</option>
              <option value="10">10{{ t('community.days') }}+</option>
            </select>
            <select v-model="routeFilters.budget" @change="applyRouteFilters" :aria-label="t('community.allBudget')" :title="t('community.allBudget')" class="min-h-11 min-w-36 shrink-0 px-4 py-2 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-sm">
              <option value="">{{ t('community.allBudget') }}</option>
              <option v-for="opt in budgetOptions" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
            </select>
            <select v-model="routeFilters.preference" @change="applyRouteFilters" :aria-label="t('community.allPreference')" :title="t('community.allPreference')" class="min-h-11 min-w-36 shrink-0 px-4 py-2 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-sm">
              <option value="">{{ t('community.allPreference') }}</option>
              <option v-for="opt in preferenceOptions" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
            </select>
          </div>
          <button type="button" @click="router.push('/create-route')" :aria-label="t('community.createMyRoute')" :title="t('community.createMyRoute')" class="min-h-11 min-w-0 px-4 py-2.5 bg-tibet-red text-tibet-yellow rounded-xl hover:bg-tibet-red/90 transition-all duration-300 transform hover:scale-105 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-tibet-red/20 active:scale-95 font-medium text-sm whitespace-normal break-words">
            {{ t('community.createMyRoute') }}
          </button>
        </motion.div>

        <div v-if="routeLoading" class="text-center py-12" role="status" aria-live="polite" aria-busy="true">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold mx-auto" aria-hidden="true"></div>
          <span class="sr-only">{{ text('community.loadingRoutes', '路线列表加载中') }}</span>
        </div>

        <div v-else-if="routeError" class="rounded-2xl border border-red-100 bg-red-50 px-5 py-6 text-center text-red-700" role="alert">
          <p class="mb-4 font-medium">{{ routeError }}</p>
          <button
            type="button"
            @click="loadRoutes"
            class="min-h-11 rounded-xl bg-tibet-red px-5 py-2.5 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
          >
            {{ text('common.retry', '重试') }}
          </button>
        </div>

        <div v-else-if="routes.length === 0" class="text-center py-12 text-gray-500">
          {{ t('community.noRoutes') }}
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <AnimatePresence mode="popLayout">
          <motion.div
               v-for="(route, index) in routes"
               :key="route.id"
               layout
               role="button"
               tabindex="0"
               :aria-label="getRouteCardLabel(route)"
               :title="route.title"
               class="tibet-card-elevated rounded-2xl p-6 cursor-pointer group hover:border-tibet-gold/30 focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
               :initial="cardInitial"
               :whileInView="cardInView"
               :exit="cardExit"
               :inViewOptions="inViewOnce"
               :transition="cardTransition(index)"
               :whileHover="{ y: -5, scale: 1.012 }"
               :whilePress="{ scale: 0.996 }"
               @click="viewRoute(route.id)"
               @keydown.enter.prevent="viewRoute(route.id)"
               @keydown.space.prevent="viewRoute(route.id)">
            <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between mb-4">
              <h3 class="text-xl font-bold text-gray-900 group-hover:text-tibet-gold transition-colors duration-300 line-clamp-2 min-w-0 flex-1 break-words">
                {{ route.title }}
              </h3>
              <div class="flex flex-wrap gap-2 sm:justify-end">
                <span
                  v-if="route.sourceType === 'OFFICIAL'"
                  class="text-xs px-2.5 py-1 bg-emerald-50 text-emerald-700 rounded-full whitespace-nowrap font-semibold"
                >
                  {{ t('community.officialRoute') }}
                </span>
                <span class="text-xs px-2.5 py-1 bg-blue-50 text-blue-600 rounded-full whitespace-nowrap font-semibold transform group-hover:scale-110 transition-transform duration-300">
                  {{ route.days }}{{ t('community.days') }}
                </span>
              </div>
            </div>
            <div class="flex flex-wrap gap-2 mb-4 text-sm text-gray-600">
              <span class="max-w-full px-3 py-1.5 bg-gray-100 rounded-lg break-words">{{ getBudgetLabel(route.budget) }}</span>
              <span class="max-w-full px-3 py-1.5 bg-gray-100 rounded-lg break-words">{{ getPreferenceLabel(route.preference) }}</span>
            </div>
            <div class="flex flex-col gap-3 text-sm text-gray-500 pt-4 border-t border-tibet-gold/20 sm:flex-row sm:items-center sm:justify-between">
              <div class="flex min-w-0 flex-wrap items-center gap-3">
                <span class="flex items-center gap-1.5">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                  {{ route.viewCount }}
                </span>
                <span class="flex items-center gap-1.5">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                  {{ route.likeCount }}
                </span>
                <span class="flex items-center gap-1.5">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
                  {{ route.commentCount }}
                </span>
              </div>
              <span class="text-xs whitespace-nowrap">{{ formatDate(route.createdAt) }}</span>
            </div>
          </motion.div>
          </AnimatePresence>
        </div>

        <div v-if="routeTotalPages > 1" class="flex flex-wrap justify-center mt-8 gap-2" role="navigation" :aria-label="t('community.sharedRoutes')">
          <button type="button" @click="changeRoutePage(routePage - 1)" :disabled="routePage === 0"
                  :aria-label="routePreviousPageLabel" :title="routePreviousPageLabel"
                  class="min-h-11 min-w-0 px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm whitespace-normal break-words">
            {{ t('community.previousPage') }}
          </button>
          <span class="px-4 py-2 text-sm" role="status" aria-live="polite">{{ routePage + 1 }} / {{ routeTotalPages }}</span>
          <button type="button" @click="changeRoutePage(routePage + 1)" :disabled="routePage >= routeTotalPages - 1"
                  :aria-label="routeNextPageLabel" :title="routeNextPageLabel"
                  class="min-h-11 min-w-0 px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm whitespace-normal break-words">
            {{ t('community.nextPage') }}
          </button>
        </div>
      </section>

      <!-- ==================== Q&A TAB ==================== -->
      <section
        id="community-panel-qa"
        role="tabpanel"
        aria-labelledby="community-tab-qa"
        :hidden="activeTab !== 'qa'"
      >
        <motion.div
          class="tibet-panel mb-8 flex flex-col gap-4 rounded-2xl p-4 sm:p-6 lg:flex-row lg:items-center lg:justify-between"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <div class="min-w-0 space-y-3">
            <!-- Tag filter -->
            <div class="-mx-1 flex gap-1.5 overflow-x-auto px-1 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0" role="group" :aria-label="t('community.allTags')">
              <button
                type="button"
                @click="applyQuestionTagFilter('')"
                :aria-pressed="!qaFilters.tag"
                :aria-label="t('community.allTags')"
                :title="t('community.allTags')"
                class="min-h-9 shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium transition-all whitespace-nowrap"
                :class="!qaFilters.tag ? 'bg-tibet-gold text-white shadow-sm' : 'bg-white/60 text-gray-500 hover:bg-white hover:text-gray-700 border border-tibet-gold/20'"
              >{{ t('common.all') }}</button>
              <button
                v-for="tag in tagOptions" :key="tag.value"
                type="button"
                @click="applyQuestionTagFilter(tag.value)"
                :aria-pressed="qaFilters.tag === tag.value"
                :aria-label="tag.value"
                :title="tag.value"
                class="min-h-9 max-w-full shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium transition-all border whitespace-normal break-words"
                :class="qaFilters.tag === tag.value ? 'text-white shadow-sm border-transparent' : 'bg-white/60 text-gray-500 hover:bg-white hover:text-gray-700 border-tibet-gold/20'"
                :style="qaFilters.tag === tag.value ? { backgroundColor: tag.color, borderColor: tag.color } : {}"
              >{{ tag.value }}</button>
            </div>
            <!-- Sort -->
            <select v-model="qaFilters.sort" @change="applyQuestionSortFilter" :aria-label="qaSortControlLabel" :title="qaSortControlLabel" class="min-h-11 min-w-36 px-3 py-2 rounded-xl bg-white/60 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-xs">
              <option value="latest">{{ t('community.sortLatest') }}</option>
              <option value="hot">{{ t('community.sortHot') }}</option>
              <option value="unsolved">{{ t('community.sortUnanswered') }}</option>
            </select>
          </div>
          <button type="button" @click="showAskModal = true" :aria-label="t('community.askQuestion')" :title="t('community.askQuestion')" class="min-h-11 min-w-0 px-4 py-2.5 bg-tibet-red text-tibet-yellow rounded-xl hover:bg-tibet-red/90 transition-all duration-300 transform hover:scale-105 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-red-500/30 active:scale-95 font-medium text-sm whitespace-normal break-words">
            {{ t('community.askQuestion') }}
          </button>
        </motion.div>

        <div v-if="qaLoading" class="text-center py-12" role="status" aria-live="polite" aria-busy="true">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-red mx-auto" aria-hidden="true"></div>
          <span class="sr-only">{{ text('community.loadingQuestions', '问答列表加载中') }}</span>
        </div>

        <div v-else-if="qaError" class="rounded-2xl border border-red-100 bg-red-50 px-5 py-6 text-center text-red-700" role="alert">
          <p class="mb-4 font-medium">{{ qaError }}</p>
          <button
            type="button"
            @click="loadQuestions"
            class="min-h-11 rounded-xl bg-tibet-red px-5 py-2.5 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
          >
            {{ text('common.retry', '重试') }}
          </button>
        </div>

        <div v-else-if="questions.length === 0" class="text-center py-12 text-gray-500">
          {{ t('community.noQuestions') }}
        </div>

        <div v-else class="space-y-4">
          <AnimatePresence mode="popLayout">
          <motion.div
               v-for="(q, index) in questions"
               :key="q.id"
               layout
               role="button"
               tabindex="0"
               :aria-label="getQuestionCardLabel(q)"
               :title="q.title"
               class="tibet-card-elevated rounded-2xl p-6 cursor-pointer group focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
               :initial="cardInitial"
               :whileInView="cardInView"
               :exit="cardExit"
               :inViewOptions="inViewOnce"
               :transition="cardTransition(index)"
               :whileHover="{ y: -3, scale: 1.006 }"
               :whilePress="{ scale: 0.996 }"
               @click="viewQuestion(q.id)"
               @keydown.enter.prevent="viewQuestion(q.id)"
               @keydown.space.prevent="viewQuestion(q.id)">
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex flex-wrap items-center gap-2 mb-2">
                  <span v-if="q.isResolved" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-600 text-xs font-medium whitespace-nowrap">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    {{ t('community.solved') }}
                  </span>
                  <span v-else class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-amber-50 text-amber-600 text-xs font-medium whitespace-nowrap">
                    {{ t('community.unsolved') }}
                  </span>
                  <div v-if="q.tags" class="flex min-w-0 flex-wrap gap-1">
                    <span v-for="tag in parseTags(q.tags)" :key="tag" class="max-w-full px-2 py-0.5 rounded-md text-xs font-medium text-white break-words" :style="{ backgroundColor: getTagColor(tag) }">{{ tag }}</span>
                  </div>
                </div>
                <h3 class="text-lg font-bold text-gray-900 group-hover:text-tibet-red transition-colors duration-300 line-clamp-2 min-w-0 break-words mb-1">
                  {{ q.title }}
                </h3>
                <p v-if="questionPreview(q)" class="text-sm text-gray-500 line-clamp-2 break-words">
                  {{ questionPreview(q) }}
                </p>
              </div>
            </div>
            <div class="flex flex-wrap items-center gap-x-4 gap-y-2 mt-4 pt-4 border-t border-tibet-gold/20 text-xs text-gray-400">
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                {{ q.viewCount }} {{ t('community.viewCount') }}
              </span>
              <span class="flex items-center gap-1" :class="{ 'text-emerald-500 font-medium': q.isResolved }">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
                {{ q.answerCount }} {{ t('community.answers') }}
              </span>
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                {{ q.likeCount }}
              </span>
              <span class="whitespace-nowrap sm:ml-auto">{{ formatDate(q.createdAt) }}</span>
            </div>
          </motion.div>
          </AnimatePresence>
        </div>

        <div v-if="qaTotalPages > 1" class="flex flex-wrap justify-center mt-8 gap-2" role="navigation" :aria-label="t('community.travelQA')">
          <button type="button" @click="changeQaPage(qaPage - 1)" :disabled="qaPage === 0"
                  :aria-label="qaPreviousPageLabel" :title="qaPreviousPageLabel"
                  class="min-h-11 min-w-0 px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm whitespace-normal break-words">
            {{ t('community.previousPage') }}
          </button>
          <span class="px-4 py-2 text-sm" role="status" aria-live="polite">{{ qaPage + 1 }} / {{ qaTotalPages }}</span>
          <button type="button" @click="changeQaPage(qaPage + 1)" :disabled="qaPage >= qaTotalPages - 1"
                  :aria-label="qaNextPageLabel" :title="qaNextPageLabel"
                  class="min-h-11 min-w-0 px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm whitespace-normal break-words">
            {{ t('community.nextPage') }}
          </button>
        </div>
      </section>

      <!-- ==================== ASK QUESTION MODAL ==================== -->
      <MotionModal
        :show="showAskModal"
        modal-key="ask-question-modal"
        labelled-by="ask-question-modal-title"
        backdrop-class="bg-black/40 backdrop-blur-sm"
        panel-class="max-w-lg rounded-3xl bg-white p-8 overflow-auto max-h-[90vh]"
        @close="showAskModal = false"
      >
              <div class="flex items-center justify-between mb-6">
                <h2 id="ask-question-modal-title" class="text-xl font-bold text-gray-900">{{ t('community.askQuestion') }}</h2>
                <button type="button" @click="showAskModal = false" :aria-label="t('toast.close')" :title="t('toast.close')" class="p-2 rounded-xl hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                </button>
              </div>
              <form @submit.prevent="submitQuestion" class="space-y-4">
                <div>
                  <label for="ask-question-title" class="block text-sm font-semibold text-gray-700 mb-1.5">{{ t('community.questionTitle') }}</label>
                  <input id="ask-question-title" v-model="newQuestion.title" type="text" required
                         :placeholder="t('community.questionTitlePlaceholder')"
                         class="w-full px-4 py-3 rounded-xl border border-tibet-gold/25 bg-gray-50 focus:border-tibet-red focus:ring-4 focus:ring-red-50 outline-none transition-all text-sm" />
                </div>
                <div>
                  <label for="ask-question-content" class="block text-sm font-semibold text-gray-700 mb-1.5">{{ t('community.questionContent') }}</label>
                  <textarea id="ask-question-content" v-model="newQuestion.content" rows="4" required
                            :placeholder="t('community.questionContentPlaceholder')"
                            class="w-full px-4 py-3 rounded-xl border border-tibet-gold/25 bg-gray-50 focus:border-tibet-red focus:ring-4 focus:ring-red-50 outline-none transition-all text-sm resize-none"></textarea>
                </div>
                <div>
                  <span id="ask-question-tags-label" class="block text-sm font-semibold text-gray-700 mb-1.5">{{ t('community.questionTags') }}</span>
                  <p id="ask-question-tags-hint" class="text-xs text-gray-400 mb-2">{{ t('community.questionTagsHint') }}</p>
                  <div
                    class="flex flex-wrap gap-2"
                    role="group"
                    aria-labelledby="ask-question-tags-label"
                    aria-describedby="ask-question-tags-hint"
                  >
                    <button type="button" v-for="tag in tagOptions" :key="tag.value"
                            @click="toggleTag(tag.value)"
                            :aria-pressed="newQuestion.tags.includes(tag.value)"
                            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-all border"
                            :style="newQuestion.tags.includes(tag.value) ? { backgroundColor: tag.color + '18', color: tag.color, borderColor: tag.color + '40' } : {}"
                            :class="newQuestion.tags.includes(tag.value) ? '' : 'bg-gray-50 text-gray-500 border-tibet-gold/25 hover:border-gray-300'">
                      {{ tag.value }}
                    </button>
                  </div>
                </div>
                <button type="submit" :disabled="qaSubmitting"
                        class="w-full py-3 rounded-xl bg-gradient-to-r from-tibet-red to-rose-600 text-white font-semibold shadow-lg shadow-red-500/20 transition-all hover:shadow-xl hover:shadow-red-500/30 disabled:opacity-50 text-sm">
                  {{ qaSubmitting ? t('community.postingQuestion') : t('community.postQuestion') }}
                </button>
              </form>
      </MotionModal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, computed, nextTick } from 'vue'
import { AnimatePresence, LayoutGroup, motion } from 'motion-v'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import MotionModal from '../components/motion/MotionModal.vue'
import api, { endpoints } from '../api'
import { useAuthGuard } from '../composables/useAuthGuard'
import { showToast } from '../composables/useToast'
import { summarizeClientError } from '../utils/errorMonitoring'
import { toIntlLocale } from '../i18n/formatting'
import {
  cardExit,
  cardInitial,
  cardInView,
  cardTransition,
  inViewOnce,
  revealInitial,
  revealInView,
  revealTransition,
  softSpring
} from '../motion/presets'

const { t, locale } = useI18n()
const router = useRouter()
const { requireAuth } = useAuthGuard()
const text = (key: string, fallback: string) => {
  const message = t(key)
  return message === key ? fallback : message
}

type CommunityTab = 'routes' | 'qa'

const activeTab = ref<CommunityTab>('routes')
const selectTab = (tab: CommunityTab) => { activeTab.value = tab }
const communityTabs: CommunityTab[] = ['routes', 'qa']
const communityTabIds: Record<CommunityTab, string> = {
  routes: 'community-tab-routes',
  qa: 'community-tab-qa'
}

const focusTab = (tab: CommunityTab) => {
  selectTab(tab)
  void nextTick(() => {
    document.getElementById(communityTabIds[tab])?.focus()
  })
}

const handleTabKeydown = (event: KeyboardEvent) => {
  let nextIndex = communityTabs.indexOf(activeTab.value)

  switch (event.key) {
    case 'ArrowRight':
      nextIndex = (nextIndex + 1) % communityTabs.length
      break
    case 'ArrowLeft':
      nextIndex = (nextIndex - 1 + communityTabs.length) % communityTabs.length
      break
    case 'Home':
      nextIndex = 0
      break
    case 'End':
      nextIndex = communityTabs.length - 1
      break
    default:
      return
  }

  event.preventDefault()
  focusTab(communityTabs[nextIndex])
}

// ========== Route sharing state ==========
const routeLoading = ref(true)
const routeError = ref('')
const routes = ref<any[]>([])
const routePage = ref(0)
const routeTotalPages = ref(0)
let latestRoutesRequest = 0

const BUDGET_KEY_TO_LABEL: Record<string, string> = {
  '经济型': 'routePlanner.budget.economy',
  '舒适型': 'routePlanner.budget.comfort',
  '豪华型': 'routePlanner.budget.luxury'
}
const PREFERENCE_KEY_TO_LABEL: Record<string, string> = {
  '自然风光': 'routePlanner.preferenceOptions.natural',
  '人文历史': 'routePlanner.preferenceOptions.cultural',
  '深度摄影': 'routePlanner.preferenceOptions.photography',
  '休闲度假': 'routePlanner.preferenceOptions.relaxation'
}

const getBudgetLabel = (key: string) => {
  const labelKey = BUDGET_KEY_TO_LABEL[key]
  return labelKey ? t(labelKey) : key
}
const getPreferenceLabel = (key: string) => {
  const labelKey = PREFERENCE_KEY_TO_LABEL[key]
  return labelKey ? t(labelKey) : key
}

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

const routeFilters = reactive({ days: '', budget: '', preference: '' })
const routePreviousPageLabel = computed(() => `${t('community.sharedRoutes')} ${t('community.previousPage')}`)
const routeNextPageLabel = computed(() => `${t('community.sharedRoutes')} ${t('community.nextPage')}`)

const loadRoutes = async () => {
  const requestId = ++latestRoutesRequest
  routeLoading.value = true
  routeError.value = ''
  try {
    const params: any = { page: routePage.value, size: 9, sortField: 'createdAt' }
    if (routeFilters.days) { const d = parseInt(routeFilters.days); if (!isNaN(d)) params.days = d }
    if (routeFilters.budget) params.budget = routeFilters.budget
    if (routeFilters.preference) params.preference = routeFilters.preference
    const response = await api.get(endpoints.routes.shared, { params })
    if (requestId !== latestRoutesRequest) return
    routes.value = response.data.content || []
    routeTotalPages.value = response.data.totalPages || 0
  } catch (error) {
    if (requestId !== latestRoutesRequest) return
    console.error('Failed to load routes:', summarizeClientError(error))
    routeError.value = text('community.routesLoadFailed', '路线列表加载失败，请稍后重试。')
    routeTotalPages.value = 0
  } finally {
    if (requestId === latestRoutesRequest) {
      routeLoading.value = false
    }
  }
}

const applyRouteFilters = () => {
  routePage.value = 0
  loadRoutes()
}

const changeRoutePage = (p: number) => { routePage.value = p; loadRoutes(); window.scrollTo({ top: 0, behavior: 'smooth' }) }
const viewRoute = (id: number) => router.push(`/community/${id}`)
const getRouteCardLabel = (route: any) => `${t('community.sharedRoutes')} ${route?.title || ''}`.trim()

// ========== Q&A state ==========
const qaLoading = ref(true)
const qaError = ref('')
const questions = ref<any[]>([])
const qaPage = ref(0)
const qaTotalPages = ref(0)
let latestQuestionsRequest = 0
const qaFilters = reactive({ tag: '', sort: 'latest' })
const showAskModal = ref(false)
const qaSubmitting = ref(false)
const newQuestion = reactive({ title: '', content: '', tags: [] as string[] })
const qaSortOptionLabels = computed<Record<string, string>>(() => ({
  latest: t('community.sortLatest'),
  hot: t('community.sortHot'),
  unsolved: t('community.sortUnanswered')
}))
const qaSortControlLabel = computed(() => `${t('community.travelQA')} ${qaSortOptionLabels.value[qaFilters.sort] || t('community.sortLatest')}`)
const qaPreviousPageLabel = computed(() => `${t('community.travelQA')} ${t('community.previousPage')}`)
const qaNextPageLabel = computed(() => `${t('community.travelQA')} ${t('community.nextPage')}`)

interface TagOption { value: string; color: string }

const tagOptions = computed<TagOption[]>(() => {
  const tags = t('community.tagLabels')
  return Array.isArray(tags) ? tags as TagOption[] : []
})

const getTagColor = (tagValue: string): string => {
  const tag = (tagOptions.value as TagOption[]).find(t => t.value === tagValue)
  return tag?.color || '#6b7280'
}

const parseTags = (tagsStr: string): string[] => {
  if (!tagsStr) return []
  return tagsStr.split(',').map(s => s.trim()).filter(Boolean)
}

const toggleTag = (tag: string) => {
  const idx = newQuestion.tags.indexOf(tag)
  if (idx >= 0) { newQuestion.tags.splice(idx, 1) }
  else if (newQuestion.tags.length < 3) { newQuestion.tags.push(tag) }
}

const loadQuestions = async () => {
  const requestId = ++latestQuestionsRequest
  qaLoading.value = true
  qaError.value = ''
  try {
    const params: any = { page: qaPage.value, size: 10, sort: qaFilters.sort }
    if (qaFilters.tag) params.tag = qaFilters.tag
    const response = await api.get(endpoints.community.questions, { params })
    if (requestId !== latestQuestionsRequest) return
    questions.value = response.data.content || []
    qaTotalPages.value = response.data.totalPages || 0
  } catch (error) {
    if (requestId !== latestQuestionsRequest) return
    console.error('Failed to load questions:', summarizeClientError(error))
    qaError.value = text('community.questionsLoadFailed', '问答列表加载失败，请稍后重试。')
    qaTotalPages.value = 0
  } finally {
    if (requestId === latestQuestionsRequest) {
      qaLoading.value = false
    }
  }
}

const applyQuestionTagFilter = (tag: string) => {
  qaFilters.tag = tag && qaFilters.tag === tag ? '' : tag
  qaPage.value = 0
  loadQuestions()
}

const applyQuestionSortFilter = () => {
  qaPage.value = 0
  loadQuestions()
}

const submitQuestion = async () => {
  if (!newQuestion.title.trim() || !newQuestion.content.trim()) return
  qaSubmitting.value = true
  try {
    await api.post(endpoints.community.questions, {
      title: newQuestion.title,
      content: newQuestion.content,
      tags: newQuestion.tags.join(',')
    })
    showAskModal.value = false
    newQuestion.title = ''; newQuestion.content = ''; newQuestion.tags = []
    qaPage.value = 0
    loadQuestions()
  } catch (error: any) {
    if (error.response?.status === 401 && !(await requireAuth())) {
      return
    } else {
      showToast(t('community.publishFailedRetry'), 'error')
    }
  } finally {
    qaSubmitting.value = false
  }
}

const changeQaPage = (p: number) => { qaPage.value = p; loadQuestions(); window.scrollTo({ top: 0, behavior: 'smooth' }) }
const viewQuestion = (id: number) => router.push(`/community/question/${id}`)
const getQuestionCardLabel = (question: any) => `${t('community.travelQA')} ${question?.title || ''}`.trim()
const questionPreview = (question: any) =>
  typeof question?.excerpt === 'string' && question.excerpt.trim() ? question.excerpt.trim() : ''

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString(toIntlLocale(locale.value))
}

onMounted(() => { loadRoutes(); loadQuestions() })
onActivated(() => { loadRoutes(); loadQuestions() })
</script>
