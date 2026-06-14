<template>
  <div class="min-h-screen tibet-page-shell py-12 sm:py-16 lg:py-24">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <motion.div
        class="text-center mb-8 sm:mb-10 lg:mb-12"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <h1 class="tibet-heading inline-flex justify-center text-3xl font-bold text-tibet-dark mb-3 sm:text-4xl sm:mb-4">{{ t('news.title') }}</h1>
        <p class="mx-auto max-w-2xl text-sm leading-relaxed text-tibet-brown/70 sm:text-lg">{{ t('news.description') }}</p>
      </motion.div>

      <motion.div
        class="mb-8 sm:mb-10"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <div class="tibet-panel rounded-3xl p-3 sm:p-4">
          <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <form class="min-w-0 flex-1" role="search" @submit.prevent>
              <label for="news-search" class="sr-only">{{ t('common.search') }}</label>
              <div class="relative">
                <svg xmlns="http://www.w3.org/2000/svg" class="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-tibet-brown/35" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  id="news-search"
                  v-model="searchKeyword"
                  type="search"
                  class="min-h-11 w-full rounded-2xl border border-tibet-gold/20 bg-white/80 py-3 pl-10 pr-12 text-sm text-tibet-dark outline-none transition placeholder:text-tibet-brown/35 focus:border-tibet-gold/60 focus:ring-2 focus:ring-tibet-gold/25"
                  :placeholder="t('news.searchPlaceholder')"
                  :aria-label="t('news.searchPlaceholder')"
                >
                <button
                  v-if="searchKeyword"
                  type="button"
                  class="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-tibet-brown/45 transition hover:bg-tibet-gold/10 hover:text-tibet-dark focus:outline-none focus:ring-2 focus:ring-tibet-gold/60"
                  :aria-label="t('common.clear')"
                  @click="searchKeyword = ''"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </form>

            <div class="-mx-3 overflow-x-auto px-3 pb-1 sm:mx-0 sm:px-0 sm:pb-0">
              <div class="flex w-max min-w-full gap-2 sm:min-w-0" role="group" :aria-label="t('news.categoryFilter')">
                <motion.button 
                  v-for="cat in categories" 
                  :key="cat.value"
                  type="button"
                  @click="selectedCategory = cat.value"
                  :aria-pressed="selectedCategory === cat.value"
                  layout
                  :whileHover="{ y: -2, scale: 1.04 }"
                  :whilePress="{ scale: 0.94 }"
                  :class="[
                      'shrink-0 rounded-full border px-4 py-2 text-sm font-medium transition-colors duration-200 mobile-touch-target focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2',
                      selectedCategory === cat.value
                        ? 'bg-tibet-red text-tibet-yellow border-tibet-red shadow-md shadow-tibet-red/15'
                        : 'bg-white/80 text-tibet-brown/80 hover:bg-tibet-gold/10 hover:text-tibet-dark border-tibet-gold/25'
                  ]"
                >
                  <span class="inline-flex items-center gap-2">
                    {{ cat.label }}
                    <span class="rounded-full px-2 py-0.5 text-[11px]" :class="selectedCategory === cat.value ? 'bg-white/15 text-tibet-yellow' : 'bg-tibet-gold/10 text-tibet-brown/55'">
                      {{ cat.count }}
                    </span>
                  </span>
                </motion.button>
              </div>
            </div>
          </div>
        </div>
      </motion.div>

      <div
        v-if="!loading && !errorMessage && newsItems.length"
        class="mb-6 inline-flex max-w-full rounded-full border border-tibet-gold/20 bg-white/65 px-4 py-2 text-sm font-medium text-tibet-brown/75 shadow-sm"
        role="status"
      >
        {{ resultSummaryText }}
      </div>

      <div
        v-if="loading"
        class="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-5 lg:grid-cols-3 lg:gap-6"
        role="status"
        aria-live="polite"
        aria-busy="true"
        :aria-label="t('news.loadingLabel')"
      >
        <span class="sr-only">{{ t('news.loadingLabel') }}</span>
        <div v-for="i in 6" :key="i" class="overflow-hidden rounded-2xl border border-tibet-gold/20 bg-white/75 shadow-sm" aria-hidden="true">
          <div class="h-44 animate-pulse bg-tibet-gold/10 sm:h-48"></div>
          <div class="p-4 sm:p-6">
            <div class="mb-3 h-5 w-3/4 animate-pulse rounded bg-tibet-gold/10"></div>
            <div class="mb-2 h-4 w-1/2 animate-pulse rounded bg-tibet-gold/10"></div>
            <div class="mb-2 h-4 animate-pulse rounded bg-tibet-gold/10"></div>
            <div class="mb-5 h-4 w-5/6 animate-pulse rounded bg-tibet-gold/10"></div>
            <div class="h-5 w-20 animate-pulse rounded bg-tibet-gold/10"></div>
          </div>
        </div>
      </div>

      <div v-else-if="errorMessage" class="py-12 sm:py-16">
        <div class="mx-auto max-w-2xl rounded-2xl border border-tibet-red/20 bg-white p-6 text-center shadow-xl shadow-tibet-dark/5 sm:p-8" role="alert" aria-live="assertive">
          <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-tibet-red/10 text-tibet-red">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 4h.01M4.93 19h14.14c1.54 0 2.5-1.67 1.73-3L13.73 4c-.77-1.33-2.69-1.33-3.46 0L3.2 16c-.77 1.33.19 3 1.73 3z" />
            </svg>
          </div>
          <h2 class="mb-2 text-xl font-bold text-tibet-dark">{{ t('news.loadErrorTitle') }}</h2>
          <p class="mx-auto mb-6 max-w-xl break-words text-sm leading-6 text-tibet-brown/70">{{ errorMessage }}</p>
          <button
            type="button"
            class="inline-flex min-h-11 items-center justify-center rounded-full bg-tibet-dark px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-tibet-dark/90 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 active:scale-95"
            @click="fetchNews()"
          >
            {{ t('news.retryLoad') }}
          </button>
        </div>
      </div>

      <div
        v-else-if="newsItems.length === 0 || filteredNews.length === 0"
        class="rounded-2xl border border-dashed border-tibet-gold/30 bg-white/65 px-6 py-12 text-center shadow-sm"
        role="status"
        aria-live="polite"
      >
        <h2 class="mb-2 text-xl font-bold text-tibet-dark">{{ t('news.emptyTitle') }}</h2>
        <p class="mx-auto mb-5 max-w-xl text-sm leading-6 text-tibet-brown/70">
          {{ emptyStateMessage }}
        </p>
        <button
          type="button"
          class="inline-flex min-h-11 items-center justify-center rounded-full bg-tibet-dark px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-tibet-dark/90 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 active:scale-95"
          @click="resetFilters"
        >
          {{ t('common.all') }}
        </button>
        <button
          v-if="hasMoreNews"
          type="button"
          class="mt-3 inline-flex min-h-11 items-center justify-center rounded-full border border-tibet-gold/25 bg-white px-6 py-2.5 text-sm font-semibold text-tibet-dark transition hover:bg-tibet-gold/10 disabled:cursor-wait disabled:opacity-60 sm:ml-3 sm:mt-0"
          :disabled="loadingMoreNews"
          :aria-busy="loadingMoreNews"
          @click="loadNextNewsPage"
        >
          {{ loadingMoreNews ? t('common.loading') : t('community.nextPage') }}
        </button>
        <p
          v-if="loadMoreErrorMessage"
          class="mx-auto mt-4 max-w-2xl rounded-2xl border border-tibet-red/20 bg-white px-4 py-3 text-center text-sm text-tibet-red shadow-sm"
          role="alert"
          aria-live="assertive"
        >
          {{ loadMoreErrorMessage }}
        </p>
      </div>

      <div v-else class="space-y-6">
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-5 lg:grid-cols-3 lg:gap-6">
        <AnimatePresence mode="popLayout">
        <motion.article
             v-for="(item, index) in filteredNews"
             :key="item.id"
             layout
             class="group tibet-card-elevated rounded-2xl overflow-hidden flex flex-col h-full"
             :initial="cardInitial"
             :whileInView="cardInView"
             :exit="cardExit"
             :inViewOptions="inViewOnce"
             :transition="cardTransition(index)"
             :whileHover="{ y: -5, scale: 1.012 }"
             :whilePress="{ scale: 0.996 }">
          <div class="w-full h-44 sm:h-48 relative flex-shrink-0">
            <img
              :src="resolveNewsImage(item)"
              :alt="item.title"
              class="w-full h-full object-cover tibet-image-hover will-change-transform"
              loading="lazy"
              @error="handleNewsImageError(item)"
            >
            <div class="absolute top-0 left-0 bg-tibet-red text-tibet-yellow px-3 py-1 m-3 rounded-full text-xs font-medium shadow-lg sm:m-4">
              {{ getCategoryLabel(item.category) }}
            </div>
          </div>
          <div class="p-4 sm:p-6 flex flex-col flex-grow">
            <h3 class="text-lg font-bold text-tibet-dark mb-2 line-clamp-2 sm:text-xl">{{ item.title }}</h3>
            <p class="text-tibet-brown/50 text-sm mb-3">{{ formatNewsMeta(item) }}</p>
            <p class="text-sm leading-relaxed text-tibet-brown/70 line-clamp-3 mb-4 flex-grow sm:text-base">{{ item.content || t('news.noSummary') }}</p>
            <div class="mt-auto">
              <button type="button" @click="openDetail(item)" class="tibet-link mobile-touch-target font-medium flex items-center focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2">
                {{ t('common.readMore') }}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </div>
        </motion.article>
        </AnimatePresence>
        </div>

        <div
          v-if="newsTotalPages > 1"
          class="flex flex-wrap items-center justify-center gap-3"
          role="navigation"
          :aria-label="t('news.paginationLabel', 'News pagination')"
        >
          <span class="text-sm text-tibet-brown/60" role="status" aria-live="polite">
            {{ newsPage + 1 }} / {{ newsTotalPages }}
          </span>
          <button
            type="button"
            class="inline-flex min-h-11 items-center justify-center rounded-full border border-tibet-gold/25 bg-white px-6 py-2.5 text-sm font-semibold text-tibet-dark transition hover:bg-tibet-gold/10 disabled:cursor-wait disabled:opacity-60"
            :disabled="loadingMoreNews || !hasMoreNews"
            :aria-busy="loadingMoreNews"
            @click="loadNextNewsPage"
          >
            {{ loadingMoreNews ? t('common.loading') : t('community.nextPage') }}
          </button>
        </div>

        <p
          v-if="loadMoreErrorMessage"
          class="mx-auto max-w-2xl rounded-2xl border border-tibet-red/20 bg-white px-4 py-3 text-center text-sm text-tibet-red shadow-sm"
          role="alert"
          aria-live="assertive"
        >
          {{ loadMoreErrorMessage }}
        </p>
      </div>

      <MotionModal
        :show="Boolean(selectedItem)"
        modal-key="news-detail-modal"
        labelled-by="news-detail-title"
        panel-class="tibet-card-elevated rounded-2xl max-w-4xl max-h-[90dvh] overflow-y-auto p-0"
        @close="closeDetail"
      >
        <template v-if="selectedItem">
              <div class="relative h-48 sm:h-64 md:h-96">
                <img
                  :src="resolveNewsImage(selectedItem)"
                  :alt="selectedItem.title"
                  class="w-full h-full object-cover"
                  @error="handleNewsImageError(selectedItem)"
                >
                <button type="button" @click="closeDetail" class="absolute top-3 right-3 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition-colors focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 sm:top-4 sm:right-4" :aria-label="t('common.closeMenu')">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 sm:h-6 sm:w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div class="p-4 sm:p-6 lg:p-8">
                <div class="flex flex-wrap items-center justify-between gap-2 mb-4">
                  <span class="bg-tibet-red/10 text-tibet-red border border-tibet-red/15 px-3 py-1 rounded-full text-sm font-medium">{{ getCategoryLabel(selectedItem.category) }}</span>
                  <span class="text-tibet-brown/50 text-sm">{{ formatNewsMeta(selectedItem) }}</span>
                </div>
                <h2 id="news-detail-title" class="text-2xl font-bold leading-tight text-tibet-dark mb-4 sm:text-3xl sm:mb-6">{{ selectedItem.title }}</h2>
                <div class="prose max-w-none text-sm leading-7 text-tibet-brown/75 whitespace-pre-line sm:text-base">
                  {{ selectedItem.content }}
                </div>
              </div>
        </template>
      </MotionModal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useI18n } from 'vue-i18n'
import MotionModal from '../components/motion/MotionModal.vue'
import api, { endpoints } from '../api'
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata } from '../api/endpoints'
import {
  cardExit,
  cardInitial,
  cardInView,
  cardTransition,
  inViewOnce,
  revealInitial,
  revealInView,
  revealTransition
} from '../motion/presets'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'
import { markNewsImageFailed, resolveNewsImage as resolveNewsImageSrc } from '../utils/newsImages'
import { toIntlLocale } from '../i18n/formatting'

const { t, locale } = useI18n()

interface NewsItem {
  id: number
  title: string
  content: string
  category: 'POLICY' | 'EVENT' | 'NOTICE'
  imageUrl?: string | null
  viewCount?: number | null
  createdAt: string
}

const newsPageSize = 20
const newsItems = ref<NewsItem[]>([])
const newsPageInfo = ref<PageMetadata>({
  page: 0,
  size: newsPageSize,
  totalElements: 0,
  totalPages: 0
})
const loading = ref(true)
const loadingMoreNews = ref(false)
const errorMessage = ref('')
const loadMoreErrorMessage = ref('')
const selectedCategory = ref<string>('ALL')
const searchKeyword = ref('')
const selectedItem = ref<NewsItem | null>(null)
const failedNewsImages = ref<Record<string, boolean>>({})
let newsRequestSequence = 0
let activeNewsLoadMoreRequest = 0

const categoryCounts = computed<Record<string, number>>(() => ({
  ALL: newsItems.value.length,
  POLICY: newsItems.value.filter(item => item.category === 'POLICY').length,
  EVENT: newsItems.value.filter(item => item.category === 'EVENT').length,
  NOTICE: newsItems.value.filter(item => item.category === 'NOTICE').length
}))

const categories = computed(() => [
  { label: t('common.all'), value: 'ALL', count: categoryCounts.value.ALL },
  { label: t('news.category.policy'), value: 'POLICY', count: categoryCounts.value.POLICY },
  { label: t('news.category.event'), value: 'EVENT', count: categoryCounts.value.EVENT },
  { label: t('news.category.notice'), value: 'NOTICE', count: categoryCounts.value.NOTICE }
])

const selectedCategoryLabel = computed(() => (
  categories.value.find(category => category.value === selectedCategory.value)?.label || t('common.all')
))

const normalizedSearchKeyword = computed(() => searchKeyword.value.trim().toLocaleLowerCase())
const newsPage = computed(() => newsPageInfo.value.page)
const newsTotalPages = computed(() => newsPageInfo.value.totalPages)
const hasMoreNews = computed(() => hasNextPage(newsPageInfo.value))

const getCategoryLabel = (category: string) => {
  const map: Record<string, string> = {
    'POLICY': t('news.category.policy'),
    'EVENT': t('news.category.event'),
    'NOTICE': t('news.category.notice')
  }
  return map[category] || category
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return t('common.pendingConfirm')
  return date.toLocaleDateString(toIntlLocale(locale.value), {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const formatViewCount = (count?: number | null) => {
  const value = Number(count || 0)
  const formattedCount = (Number.isFinite(value) ? value : 0).toLocaleString(toIntlLocale(locale.value))
  return t('news.viewCountWithCount', { count: formattedCount })
}

const formatNewsMeta = (item: NewsItem) => t('news.metaLine', {
  date: formatDate(item.createdAt),
  views: formatViewCount(item.viewCount)
})

const resolveNewsImage = (item: NewsItem) => {
  return resolveNewsImageSrc(item, failedNewsImages.value)
}

const handleNewsImageError = (item: NewsItem) => {
  markNewsImageFailed(item, failedNewsImages.value)
}

const resetNewsPageInfo = () => {
  newsPageInfo.value = {
    page: 0,
    size: newsPageSize,
    totalElements: 0,
    totalPages: 0
  }
}

const applyNewsPage = (response: any, append = false) => {
  const page = readPaginatedResponse<NewsItem>(response, {
    page: append ? newsPageInfo.value.page + 1 : 0,
    size: newsPageSize
  })

  newsItems.value = append ? mergeUniqueById(newsItems.value, page.content) : page.content
  newsPageInfo.value = {
    page: page.page,
    size: page.size || newsPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const fetchNews = async (page = 0, append = false) => {
  const requestSequence = ++newsRequestSequence
  if (append) {
    loadingMoreNews.value = true
    activeNewsLoadMoreRequest = requestSequence
  } else {
    loading.value = true
    loadingMoreNews.value = false
    activeNewsLoadMoreRequest = 0
  }
  errorMessage.value = ''
  loadMoreErrorMessage.value = ''

  try {
    // API interceptor appends the active locale parameter.
    const params: Record<string, string | number> = { page, size: newsPageSize }
    if (selectedCategory.value !== 'ALL') {
      params.category = selectedCategory.value
    }
    if (normalizedSearchKeyword.value) {
      params.keyword = normalizedSearchKeyword.value
    }
    const response = await api.get(endpoints.news.list, {
      params
    })
    if (requestSequence !== newsRequestSequence) return
    applyNewsPage(response, append)
  } catch (error) {
    if (requestSequence !== newsRequestSequence) return
    console.error('Failed to fetch news:', summarizeClientError(error))
    if (append) {
      loadMoreErrorMessage.value = safeClientErrorMessage(error, t('toast.pageLoadFailed'))
    } else {
      newsItems.value = []
      resetNewsPageInfo()
      errorMessage.value = safeClientErrorMessage(error, t('toast.pageLoadFailed'))
    }
  } finally {
    if (append) {
      if (activeNewsLoadMoreRequest === requestSequence) {
        loadingMoreNews.value = false
        activeNewsLoadMoreRequest = 0
      }
    } else {
      if (requestSequence === newsRequestSequence) {
        loading.value = false
      }
    }
  }
}

const loadNextNewsPage = async () => {
  if (loading.value || loadingMoreNews.value || !hasMoreNews.value) return
  await fetchNews(newsPageInfo.value.page + 1, true)
}

const filteredNews = computed(() => {
  const keyword = normalizedSearchKeyword.value
  return newsItems.value.filter((item) => {
    if (selectedCategory.value !== 'ALL' && item.category !== selectedCategory.value) {
      return false
    }
    if (!keyword) return true

    return [
      item.title,
      item.content,
      getCategoryLabel(item.category)
    ]
      .filter(Boolean)
      .join(' ')
      .toLocaleLowerCase()
      .includes(keyword)
  })
})

const resultSummaryText = computed(() => {
  const base = t('news.resultSummary', {
    count: filteredNews.value.length,
    category: selectedCategoryLabel.value
  })
  return normalizedSearchKeyword.value
    ? t('news.resultSummaryWithKeyword', { base, keyword: searchKeyword.value.trim() })
    : base
})

const emptyStateMessage = computed(() => (
  newsItems.value.length === 0
    ? t('news.emptyMessage')
    : t('news.noSearchMessage')
))

const resetFilters = () => {
  selectedCategory.value = 'ALL'
  searchKeyword.value = ''
}

const openDetail = (item: NewsItem) => {
  selectedItem.value = item
  document.body.style.overflow = 'hidden'
}

const closeDetail = () => {
  selectedItem.value = null
  document.body.style.overflow = 'auto'
}

// Refetch localized server content when language or filters change.
watch([locale, selectedCategory, normalizedSearchKeyword], () => {
  fetchNews()
})

onMounted(() => {
  fetchNews()
})

onBeforeUnmount(() => {
  document.body.style.overflow = 'auto'
})
</script>
