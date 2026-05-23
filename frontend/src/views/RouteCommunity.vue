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
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <motion.button
            @click="activeTab = 'routes'"
            class="px-6 py-2.5 rounded-xl font-semibold text-sm transition-colors duration-300 flex items-center gap-2 relative overflow-hidden"
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
            @click="activeTab = 'qa'"
            class="px-6 py-2.5 rounded-xl font-semibold text-sm transition-colors duration-300 flex items-center gap-2 relative overflow-hidden"
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
      <template v-if="activeTab === 'routes'">
        <motion.div
          class="tibet-panel mb-8 flex flex-col gap-4 rounded-2xl p-4 sm:p-6 lg:flex-row lg:items-center lg:justify-between"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <div class="-mx-1 flex gap-3 overflow-x-auto px-1 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0">
            <select v-model="routeFilters.days" @change="loadRoutes" class="shrink-0 px-4 py-2 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-sm">
              <option value="">{{ t('community.allDays') }}</option>
              <option value="3">3{{ t('community.days') }}</option>
              <option value="5">5{{ t('community.days') }}</option>
              <option value="7">7{{ t('community.days') }}</option>
              <option value="10">10{{ t('community.days') }}+</option>
            </select>
            <select v-model="routeFilters.budget" @change="loadRoutes" class="shrink-0 px-4 py-2 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-sm">
              <option value="">{{ t('community.allBudget') }}</option>
              <option v-for="opt in budgetOptions" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
            </select>
            <select v-model="routeFilters.preference" @change="loadRoutes" class="shrink-0 px-4 py-2 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-sm">
              <option value="">{{ t('community.allPreference') }}</option>
              <option v-for="opt in preferenceOptions" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
            </select>
          </div>
          <button @click="router.push('/create-route')" class="px-4 py-2.5 bg-tibet-red text-tibet-yellow rounded-xl hover:bg-tibet-red/90 transition-all duration-300 transform hover:scale-105 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-tibet-red/20 active:scale-95 font-medium text-sm">
            {{ t('community.createMyRoute') }}
          </button>
        </motion.div>

        <div v-if="routeLoading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold mx-auto"></div>
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
               class="tibet-card-elevated rounded-2xl p-6 cursor-pointer group hover:border-tibet-gold/30"
               :initial="cardInitial"
               :whileInView="cardInView"
               :exit="cardExit"
               :inViewOptions="inViewOnce"
               :transition="cardTransition(index)"
               :whileHover="{ y: -5, scale: 1.012 }"
               :whilePress="{ scale: 0.996 }"
               @click="viewRoute(route.id)">
            <div class="flex justify-between items-start mb-4">
              <h3 class="text-xl font-bold text-gray-900 group-hover:text-tibet-gold transition-colors duration-300 line-clamp-2 flex-1">
                {{ route.title }}
              </h3>
              <span
                v-if="route.sourceType === 'OFFICIAL'"
                class="text-xs px-2.5 py-1 bg-emerald-50 text-emerald-700 rounded-full whitespace-nowrap font-semibold ml-2"
              >
                {{ t('community.officialRoute') }}
              </span>
              <span class="text-xs px-2.5 py-1 bg-blue-50 text-blue-600 rounded-full whitespace-nowrap font-semibold transform group-hover:scale-110 transition-transform duration-300 ml-2">
                {{ route.days }}{{ t('community.days') }}
              </span>
            </div>
            <div class="flex gap-2 mb-4 text-sm text-gray-600">
              <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ getBudgetLabel(route.budget) }}</span>
              <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ getPreferenceLabel(route.preference) }}</span>
            </div>
            <div class="flex justify-between items-center text-sm text-gray-500 pt-4 border-t border-tibet-gold/20">
              <div class="flex items-center gap-4">
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
              <span class="text-xs">{{ formatDate(route.createdAt) }}</span>
            </div>
          </motion.div>
          </AnimatePresence>
        </div>

        <div v-if="routeTotalPages > 1" class="flex justify-center mt-8 gap-2">
          <button @click="changeRoutePage(routePage - 1)" :disabled="routePage === 0"
                  class="px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm">
            {{ t('community.previousPage') }}
          </button>
          <span class="px-4 py-2 text-sm">{{ routePage + 1 }} / {{ routeTotalPages }}</span>
          <button @click="changeRoutePage(routePage + 1)" :disabled="routePage >= routeTotalPages - 1"
                  class="px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm">
            {{ t('community.nextPage') }}
          </button>
        </div>
      </template>

      <!-- ==================== Q&A TAB ==================== -->
      <template v-if="activeTab === 'qa'">
        <motion.div
          class="tibet-panel mb-8 flex flex-col gap-4 rounded-2xl p-4 sm:p-6 lg:flex-row lg:items-center lg:justify-between"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <div class="min-w-0 space-y-3">
            <!-- Tag filter -->
            <div class="-mx-1 flex gap-1.5 overflow-x-auto px-1 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0">
              <button
                @click="qaFilters.tag = ''; loadQuestions()"
                class="shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium transition-all"
                :class="!qaFilters.tag ? 'bg-tibet-gold text-white shadow-sm' : 'bg-white/60 text-gray-500 hover:bg-white hover:text-gray-700 border border-tibet-gold/20'"
              >{{ t('common.all') }}</button>
              <button
                v-for="tag in tagOptions" :key="tag.value"
                @click="qaFilters.tag = qaFilters.tag === tag.value ? '' : tag.value; loadQuestions()"
                class="shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium transition-all border"
                :class="qaFilters.tag === tag.value ? 'text-white shadow-sm border-transparent' : 'bg-white/60 text-gray-500 hover:bg-white hover:text-gray-700 border-tibet-gold/20'"
                :style="qaFilters.tag === tag.value ? { backgroundColor: tag.color, borderColor: tag.color } : {}"
              >{{ tag.value }}</button>
            </div>
            <!-- Sort -->
            <select v-model="qaFilters.sort" @change="loadQuestions" class="px-3 py-2 rounded-xl bg-white/60 border border-tibet-gold/25 focus:border-tibet-gold outline-none text-xs">
              <option value="latest">{{ t('community.sortLatest') }}</option>
              <option value="hot">{{ t('community.sortHot') }}</option>
              <option value="unsolved">{{ t('community.sortUnanswered') }}</option>
            </select>
          </div>
          <button @click="showAskModal = true" class="px-4 py-2.5 bg-tibet-red text-tibet-yellow rounded-xl hover:bg-tibet-red/90 transition-all duration-300 transform hover:scale-105 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-red-500/30 active:scale-95 font-medium text-sm">
            {{ t('community.askQuestion') }}
          </button>
        </motion.div>

        <div v-if="qaLoading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-red mx-auto"></div>
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
               class="tibet-card-elevated rounded-2xl p-6 cursor-pointer group"
               :initial="cardInitial"
               :whileInView="cardInView"
               :exit="cardExit"
               :inViewOptions="inViewOnce"
               :transition="cardTransition(index)"
               :whileHover="{ y: -3, scale: 1.006 }"
               :whilePress="{ scale: 0.996 }"
               @click="viewQuestion(q.id)">
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-2">
                  <span v-if="q.isResolved" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-600 text-xs font-medium">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    {{ t('community.solved') }}
                  </span>
                  <span v-else class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-amber-50 text-amber-600 text-xs font-medium">
                    {{ t('community.unsolved') }}
                  </span>
                  <div v-if="q.tags" class="flex gap-1">
                    <span v-for="tag in parseTags(q.tags)" :key="tag" class="px-2 py-0.5 rounded-md text-xs font-medium text-white" :style="{ backgroundColor: getTagColor(tag) }">{{ tag }}</span>
                  </div>
                </div>
                <h3 class="text-lg font-bold text-gray-900 group-hover:text-tibet-red transition-colors duration-300 line-clamp-1 mb-1">
                  {{ q.title }}
                </h3>
                <p class="text-sm text-gray-500 line-clamp-2">{{ q.content }}</p>
              </div>
            </div>
            <div class="flex items-center gap-4 mt-4 pt-4 border-t border-tibet-gold/20 text-xs text-gray-400">
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
              <span class="ml-auto">{{ formatDate(q.createdAt) }}</span>
            </div>
          </motion.div>
          </AnimatePresence>
        </div>

        <div v-if="qaTotalPages > 1" class="flex justify-center mt-8 gap-2">
          <button @click="changeQaPage(qaPage - 1)" :disabled="qaPage === 0"
                  class="px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm">
            {{ t('community.previousPage') }}
          </button>
          <span class="px-4 py-2 text-sm">{{ qaPage + 1 }} / {{ qaTotalPages }}</span>
          <button @click="changeQaPage(qaPage + 1)" :disabled="qaPage >= qaTotalPages - 1"
                  class="px-4 py-2 rounded-lg bg-white border border-tibet-gold/25 disabled:opacity-50 hover:bg-gray-50 text-sm">
            {{ t('community.nextPage') }}
          </button>
        </div>
      </template>

      <!-- ==================== ASK QUESTION MODAL ==================== -->
      <MotionModal
        :show="showAskModal"
        modal-key="ask-question-modal"
        backdrop-class="bg-black/40 backdrop-blur-sm"
        panel-class="max-w-lg rounded-3xl bg-white p-8 overflow-auto max-h-[90vh]"
        @close="showAskModal = false"
      >
              <div class="flex items-center justify-between mb-6">
                <h2 class="text-xl font-bold text-gray-900">{{ t('community.askQuestion') }}</h2>
                <button @click="showAskModal = false" class="p-2 rounded-xl hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                </button>
              </div>
              <form @submit.prevent="submitQuestion" class="space-y-4">
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-1.5">{{ t('community.questionTitle') }}</label>
                  <input v-model="newQuestion.title" type="text" required
                         :placeholder="t('community.questionTitlePlaceholder')"
                         class="w-full px-4 py-3 rounded-xl border border-tibet-gold/25 bg-gray-50 focus:border-tibet-red focus:ring-4 focus:ring-red-50 outline-none transition-all text-sm" />
                </div>
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-1.5">{{ t('community.questionContent') }}</label>
                  <textarea v-model="newQuestion.content" rows="4" required
                            :placeholder="t('community.questionContentPlaceholder')"
                            class="w-full px-4 py-3 rounded-xl border border-tibet-gold/25 bg-gray-50 focus:border-tibet-red focus:ring-4 focus:ring-red-50 outline-none transition-all text-sm resize-none"></textarea>
                </div>
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-1.5">{{ t('community.questionTags') }}</label>
                  <p class="text-xs text-gray-400 mb-2">{{ t('community.questionTagsHint') }}</p>
                  <div class="flex flex-wrap gap-2">
                    <button type="button" v-for="tag in tagOptions" :key="tag.value"
                            @click="toggleTag(tag.value)"
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
import { ref, reactive, onMounted, onActivated, computed } from 'vue'
import { AnimatePresence, LayoutGroup, motion } from 'motion-v'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import MotionModal from '../components/motion/MotionModal.vue'
import api from '../api'
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

const { t } = useI18n()
const router = useRouter()

const activeTab = ref<'routes' | 'qa'>('routes')

// ========== Route sharing state ==========
const routeLoading = ref(true)
const routes = ref<any[]>([])
const routePage = ref(0)
const routeTotalPages = ref(0)

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

const loadRoutes = async () => {
  routeLoading.value = true
  try {
    const params: any = { page: routePage.value, size: 9, sortField: 'createdAt' }
    if (routeFilters.days) { const d = parseInt(routeFilters.days); if (!isNaN(d)) params.days = d }
    if (routeFilters.budget) params.budget = routeFilters.budget
    if (routeFilters.preference) params.preference = routeFilters.preference
    const response = await api.get('/routes/shared', { params })
    routes.value = response.data.content || []
    routeTotalPages.value = response.data.totalPages || 0
  } catch (error) {
    console.error('Failed to load routes:', error)
    routes.value = []; routeTotalPages.value = 0
  } finally {
    routeLoading.value = false
  }
}

const changeRoutePage = (p: number) => { routePage.value = p; loadRoutes(); window.scrollTo({ top: 0, behavior: 'smooth' }) }
const viewRoute = (id: number) => router.push(`/community/${id}`)

// ========== Q&A state ==========
const qaLoading = ref(true)
const questions = ref<any[]>([])
const qaPage = ref(0)
const qaTotalPages = ref(0)
const qaFilters = reactive({ tag: '', sort: 'latest' })
const showAskModal = ref(false)
const qaSubmitting = ref(false)
const newQuestion = reactive({ title: '', content: '', tags: [] as string[] })

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
  qaLoading.value = true
  try {
    const params: any = { page: qaPage.value, size: 10, sort: qaFilters.sort }
    if (qaFilters.tag) params.tag = qaFilters.tag
    const response = await api.get('/community/questions', { params })
    questions.value = response.data.content || []
    qaTotalPages.value = response.data.totalPages || 0
  } catch (error) {
    console.error('Failed to load questions:', error)
    questions.value = []; qaTotalPages.value = 0
  } finally {
    qaLoading.value = false
  }
}

const submitQuestion = async () => {
  if (!newQuestion.title.trim() || !newQuestion.content.trim()) return
  qaSubmitting.value = true
  try {
    await api.post('/community/questions', {
      title: newQuestion.title,
      content: newQuestion.content,
      tags: newQuestion.tags.join(',')
    })
    showAskModal.value = false
    newQuestion.title = ''; newQuestion.content = ''; newQuestion.tags = []
    qaPage.value = 0
    loadQuestions()
  } catch (error: any) {
    if (error.response?.status === 401 && confirm(t('routePlanner.loginRequired'))) {
      router.push('/login')
    } else {
      alert(t('community.publishFailedRetry'))
    }
  } finally {
    qaSubmitting.value = false
  }
}

const changeQaPage = (p: number) => { qaPage.value = p; loadQuestions(); window.scrollTo({ top: 0, behavior: 'smooth' }) }
const viewQuestion = (id: number) => router.push(`/community/question/${id}`)

const formatDate = (dateStr: string) => {
  const locale = localStorage.getItem('locale') || 'zh'
  return new Date(dateStr).toLocaleDateString(locale === 'bo' ? 'bo-CN' : 'zh-CN')
}

onMounted(() => { loadRoutes(); loadQuestions() })
onActivated(() => { loadRoutes(); loadQuestions() })
</script>
