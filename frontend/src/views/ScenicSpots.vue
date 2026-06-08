<template>
  <div class="min-h-screen tibet-page-shell py-16 sm:py-24" :aria-busy="loading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <motion.div
        class="mb-10 text-center sm:mb-16"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <h1 class="tibet-heading inline-flex justify-center text-3xl font-bold text-tibet-dark mb-4 sm:text-4xl tibetan-font">{{ t('spots.title') }}</h1>
        <p class="mx-auto max-w-2xl text-base text-tibet-brown/70 sm:text-lg tibetan-font">
          {{ t('spots.subtitle') }}
        </p>
        <div
          v-if="!loading && !errorMessage && spots.length"
          class="mt-5 inline-flex max-w-full items-center rounded-full border border-tibet-gold/20 bg-white/65 px-4 py-2 text-sm font-medium text-tibet-brown/75 shadow-sm backdrop-blur tibetan-font"
        >
          {{ t('spots.resultSummary', { count: filteredSpots.length, category: selectedCategoryLabel }) }}
        </div>
      </motion.div>

      <!-- Filters -->
      <motion.div
        class="-mx-4 mb-10 overflow-x-auto px-4 pb-2 sm:mx-0 sm:flex sm:justify-center sm:overflow-visible sm:px-0 sm:pb-0 sm:mb-12 will-change-transform"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <div class="tibet-panel flex w-max min-w-full space-x-2 rounded-full p-1.5 sm:min-w-0">
          <motion.button 
            v-for="cat in categories" 
            :key="cat.value"
            type="button"
            @click="selectedCategory = cat.value"
            :aria-pressed="selectedCategory === cat.value"
            :aria-label="t('spots.filterByCategory', { category: cat.label })"
            layout
            :whileHover="{ y: -2, scale: 1.04 }"
            :whilePress="{ scale: 0.94 }"
            :class="[
              'relative min-h-11 shrink-0 overflow-hidden rounded-full px-5 py-2.5 text-sm font-medium transition-all duration-300 ease-out-expo will-change-transform tibetan-font focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2',
              selectedCategory === cat.value 
                ? 'bg-tibet-dark text-white shadow-md transform scale-105' 
                : 'text-tibet-brown/80 hover:bg-tibet-gold/5 hover:text-tibet-dark hover:scale-105'
            ]"
          >
            <span class="relative z-10">{{ cat.label }}</span>
            <motion.span
              v-if="selectedCategory === cat.value"
              layoutId="spots-category-pill"
              class="absolute inset-0 bg-gradient-to-r from-tibet-blue/20 to-tibet-red/20"
              :transition="softSpring"
            ></motion.span>
          </motion.button>
        </div>
      </motion.div>

      <!-- Loading State with Skeleton -->
      <div
        v-if="loading"
        role="status"
        aria-live="polite"
        aria-busy="true"
        :aria-label="t('spots.loadingLabel')"
        class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3 lg:gap-8"
      >
        <span class="sr-only">{{ t('spots.loadingLabel') }}</span>
        <div v-for="i in 6" :key="i" class="overflow-hidden rounded-3xl border border-tibet-gold/20 bg-white shadow-sm" aria-hidden="true">
          <div class="h-56 animate-pulse bg-tibet-gold/10 sm:h-72"></div>
          <div class="p-8">
            <div class="mb-4 h-6 w-3/4 animate-pulse rounded bg-tibet-gold/10"></div>
            <div class="mb-2 h-4 animate-pulse rounded bg-tibet-gold/10"></div>
            <div class="mb-6 h-4 w-5/6 animate-pulse rounded bg-tibet-gold/10"></div>
            <div class="flex space-x-2">
              <div class="h-6 w-16 animate-pulse rounded-full bg-tibet-gold/10"></div>
              <div class="h-6 w-16 animate-pulse rounded-full bg-tibet-gold/10"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Request error -->
      <div v-else-if="errorMessage" class="py-12 sm:py-20">
        <div
          class="mx-auto max-w-2xl rounded-2xl border border-tibet-red/20 bg-white p-6 text-center shadow-xl shadow-tibet-dark/5 sm:p-8"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
        >
          <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-tibet-red/10 text-tibet-red">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 4h.01M4.93 19h14.14c1.54 0 2.5-1.67 1.73-3L13.73 4c-.77-1.33-2.69-1.33-3.46 0L3.2 16c-.77 1.33.19 3 1.73 3z" />
            </svg>
          </div>
          <h2 class="mb-2 text-xl font-bold text-tibet-dark tibetan-font">{{ t('spots.loadErrorTitle') }}</h2>
          <p class="mx-auto mb-6 max-w-xl break-words text-sm leading-6 text-tibet-brown/70 tibetan-font">{{ errorMessage }}</p>
          <button
            type="button"
            @click="fetchSpots()"
            class="inline-flex min-h-11 items-center justify-center rounded-full bg-tibet-dark px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-tibet-dark/90 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 active:scale-95 tibetan-font"
            :aria-label="t('spots.retryLoad')"
          >
            {{ t('spots.retryLoad') }}
          </button>
        </div>
      </div>

      <!-- All spots empty (API returned no data) -->
      <div
        v-else-if="spots.length === 0"
        class="text-center py-20"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        <div class="bg-yellow-50 border-2 border-yellow-400 rounded-2xl p-8 max-w-2xl mx-auto">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-yellow-500 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <h3 class="text-xl font-bold text-gray-900 mb-2 tibetan-font">{{ t('spots.noDataTitle') }}</h3>
          <p class="text-gray-600 mb-4 tibetan-font">{{ t('spots.noDataMessage') }}</p>
          <div class="text-left bg-white p-4 rounded-lg text-sm">
            <p class="font-semibold mb-2 tibetan-font">{{ t('spots.checkList') }}</p>
            <ul class="list-disc list-inside space-y-1 text-gray-700">
              <li class="tibetan-font">{{ t('spots.checkItem1') }}</li>
              <li class="tibetan-font">{{ t('spots.checkItem2') }}</li>
              <li class="tibetan-font">{{ t('spots.checkItem3') }}</li>
            </ul>
          </div>
          <button
            type="button"
            @click="fetchSpots()"
            class="mt-4 min-h-11 rounded-full bg-tibet-dark px-6 py-2 text-white transition-colors hover:bg-tibet-dark/90 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font"
            :aria-label="t('spots.reload')"
          >
            {{ t('spots.reload') }}
          </button>
        </div>
      </div>

      <!-- No spots match the current category filter -->
      <div
        v-else-if="filteredSpots.length === 0"
        class="text-center py-20"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        <div class="bg-gray-50 border-2 border-tibet-gold/25 rounded-2xl p-8 max-w-md mx-auto">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-gray-400 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <h3 class="text-lg font-bold text-gray-700 mb-2 tibetan-font">{{ t('spots.noCategoryTitle') }}</h3>
          <p class="text-gray-500 mb-4 tibetan-font">{{ t('spots.noCategoryMessage') }}</p>
          <button
            type="button"
            @click="selectedCategory = 'ALL'"
            class="min-h-11 rounded-full bg-tibet-dark px-6 py-2 text-white transition-colors hover:bg-black focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font"
            :aria-label="t('spots.showAll')"
          >
            {{ t('spots.showAll') }}
          </button>
        </div>
      </div>

      <!-- Spots Grid -->
      <div v-else class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3 lg:gap-8">
        <AnimatePresence mode="popLayout">
        <motion.div
             v-for="(spot, index) in filteredSpots"
             :key="spot.id"
             layout
             class="group tibet-card-elevated gpu-accelerated cursor-pointer overflow-hidden rounded-3xl border border-tibet-gold/20 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-4"
             role="link"
             tabindex="0"
             :aria-label="t('spots.cardAria', { name: spot.name })"
             @click="goToSpot(spot)"
             @keydown.enter.prevent="goToSpot(spot)"
             @keydown.space.prevent="goToSpot(spot)"
             :initial="cardInitial"
             :whileInView="cardInView"
             :exit="cardExit"
             :inViewOptions="inViewOnce"
             :transition="cardTransition(index)"
             :whileHover="{ y: -5, scale: 1.012 }"
             :whilePress="{ scale: 0.996 }">
          
          <!-- Image Container -->
          <div class="relative h-56 overflow-hidden bg-gray-200 sm:h-72">
            <img v-if="hasSpotImage(spot)"
                 :src="spot.imageUrl"
                 :alt="spot.name"
                 loading="lazy"
                 class="w-full h-full object-cover tibet-image-hover img-fade-in will-change-transform"
                 @error="handleImageError(spot)">
            <div v-else 
                 class="w-full h-full flex items-center justify-center"
                 :class="getGradientClass(spot)">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-24 w-24 text-white opacity-30 animate-pulse-slow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path v-if="spot.category === 'NATURAL'" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 ease-out-expo"></div>
            
            <!-- Category Badge -->
            <div class="absolute top-4 right-4 bg-white/95 backdrop-blur-md px-3 py-1.5 rounded-full text-xs font-bold text-tibet-dark shadow-xl transform group-hover:scale-110 transition-transform duration-300 ease-out-expo will-change-transform tibetan-font">
              {{ spot.category === 'NATURAL' ? t('spots.category.natural') : t('spots.category.cultural') }}
            </div>
          </div>
          
          <!-- Content -->
          <div class="p-5 sm:p-8">
            <div class="mb-4 flex items-start justify-between gap-3">
              <h3 class="min-w-0 flex-1 break-words text-xl font-bold leading-tight text-tibet-dark transition-colors duration-300 ease-out-expo line-clamp-2 group-hover:text-tibet-gold tibetan-font">
                {{ spot.name }}
              </h3>
              <div class="max-w-[45%] shrink-0 rounded-2xl bg-tibet-gold/10 px-3 py-1.5 text-right transition-transform duration-300 ease-out-expo group-hover:scale-105">
                <p class="text-[10px] font-semibold uppercase text-tibet-brown/45 tibetan-font">{{ t('spots.ticketFrom') }}</p>
                <p class="break-words text-base font-bold leading-tight text-tibet-gold">{{ formatPrice(spot.ticketPrice) }}</p>
              </div>
            </div>
            
            <p class="mb-6 break-words text-tibet-brown/70 line-clamp-3 leading-relaxed tibetan-font">
              {{ spot.description }}
            </p>

            <div v-if="spotHighlights(spot).length" class="mb-6 grid grid-cols-2 gap-2">
              <div
                v-for="highlight in spotHighlights(spot)"
                :key="highlight.label"
                class="min-w-0 rounded-2xl border border-tibet-gold/15 bg-tibet-white/70 px-3 py-2"
              >
                <p class="truncate text-[11px] font-medium text-tibet-brown/45 tibetan-font">{{ highlight.label }}</p>
                <p class="truncate text-sm font-semibold text-tibet-dark tibetan-font">{{ highlight.value }}</p>
              </div>
            </div>
            
            <!-- Tags & Action -->
            <div class="flex flex-wrap items-center justify-between gap-3 border-t border-tibet-gold/20 pt-5 sm:pt-6">
              <div class="flex min-w-0 flex-wrap gap-2">
                <span v-for="tag in spot.tags?.slice(0, 2)" :key="tag.id" 
                      class="max-w-full truncate rounded-full bg-tibet-gold/5 px-3 py-1 text-xs font-medium text-tibet-brown/80 transition-transform duration-300 ease-out-expo will-change-transform group-hover:scale-105">
                  {{ tag.tag }}
                </span>
              </div>
              <span
                      aria-hidden="true"
                      class="ml-0 inline-flex min-h-11 shrink-0 items-center text-tibet-gold font-medium transition-all duration-300 ease-out-expo group-hover:text-tibet-gold/80 sm:ml-4 group/btn tibetan-font">
                {{ t('spots.viewDetails') }}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1 transform group-hover/btn:translate-x-2 transition-transform duration-300 ease-out-expo will-change-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              </span>
            </div>
          </div>
        </motion.div>
        </AnimatePresence>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api, { endpoints } from '../api'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'
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

const router = useRouter()
const { t, locale } = useI18n()

type SpotCategory = 'NATURAL' | 'CULTURAL'

interface SpotTag {
  id: string | number
  tag: string
}

interface ScenicSpot {
  id: number | string
  name: string
  description?: string
  imageUrl?: string
  category?: SpotCategory | string
  ticketPrice?: number | string | null
  altitude?: number | string | null
  location?: string | null
  rating?: number | string | null
  visitCount?: number | string | null
  tags?: SpotTag[]
}

type CategoryFilter = 'ALL' | SpotCategory

const spots = ref<ScenicSpot[]>([])
const loading = ref(true)
const errorMessage = ref('')
const selectedCategory = ref<CategoryFilter>('ALL')
const failedSpotImages = ref<Record<string, boolean>>({})

const categories = computed(() => [
  { label: t('spots.category.all'), value: 'ALL' },
  { label: t('spots.category.natural'), value: 'NATURAL' },
  { label: t('spots.category.cultural'), value: 'CULTURAL' }
] satisfies Array<{ label: string; value: CategoryFilter }>)

const selectedCategoryLabel = computed(() => (
  categories.value.find(category => category.value === selectedCategory.value)?.label || t('spots.category.all')
))

const fetchSpots = async () => {
  try {
    loading.value = true
    errorMessage.value = ''
    const response = await api.get(endpoints.spots.list)
    const payload = response.data?.content || response.data || []
    spots.value = Array.isArray(payload) ? payload : []

    if (!spots.value || spots.value.length === 0) {
      console.warn('后端返回了空数据')
    }
  } catch (error) {
    console.error('Failed to fetch scenic spots:', summarizeClientError(error))
    spots.value = []
    errorMessage.value = safeClientErrorMessage(error, t('toast.pageLoadFailed'))
  } finally {
    loading.value = false
  }
}

const filteredSpots = computed(() => {
  if (selectedCategory.value === 'ALL') {
    return spots.value
  }
  return spots.value.filter(spot => spot.category === selectedCategory.value)
})

const goToSpot = (spot: ScenicSpot) => {
  if (spot?.id == null) {
    console.warn('景点数据缺少 id，无法进入详情页:', spot)
    return
  }
  router.push(`/spots/${encodeURIComponent(String(spot.id))}`)
}

const getSpotImageKey = (spot: ScenicSpot) => String(spot?.id ?? spot?.imageUrl ?? spot?.name ?? '')

const hasSpotImage = (spot: ScenicSpot) => Boolean(spot?.imageUrl) && !failedSpotImages.value[getSpotImageKey(spot)]

const handleImageError = (spot: ScenicSpot) => {
  failedSpotImages.value[getSpotImageKey(spot)] = true
}

const formatNumber = (value: number | string | null | undefined) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const formatPrice = (price: ScenicSpot['ticketPrice']) => {
  const value = formatNumber(price)
  if (value == null) return t('common.pendingConfirm')
  if (value <= 0) return t('common.freeTicket')
  return t('common.priceCny', { price: value })
}

const formatAltitude = (altitude: ScenicSpot['altitude']) => {
  const value = formatNumber(altitude)
  if (value == null) return ''
  return `${Math.round(value)} m`
}

const formatRating = (rating: ScenicSpot['rating']) => {
  const value = formatNumber(rating)
  if (value == null) return ''
  return t('spots.ratingValue', { rating: value.toFixed(1) })
}

const formatVisitCount = (visitCount: ScenicSpot['visitCount']) => {
  const value = formatNumber(visitCount)
  if (value == null) return ''
  return t('spots.visitCountValue', { count: value.toLocaleString() })
}

const spotHighlights = (spot: ScenicSpot) => [
  spot.altitude ? { label: t('spots.altitude'), value: formatAltitude(spot.altitude) } : null,
  spot.location ? { label: t('spots.location'), value: spot.location } : null,
  spot.rating ? { label: t('spots.rating'), value: formatRating(spot.rating) } : null,
  spot.visitCount ? { label: t('spots.visitCount'), value: formatVisitCount(spot.visitCount) } : null
].filter((highlight): highlight is { label: string; value: string } => Boolean(highlight?.value)).slice(0, 2)

const getGradientClass = (spot: ScenicSpot) => {
  const gradients = [
    'bg-gradient-to-br from-tibet-blue via-tibet-dark to-tibet-red',
    'bg-gradient-to-br from-tibet-red via-tibet-gold to-tibet-yellow',
    'bg-gradient-to-br from-tibet-turquoise via-tibet-blue to-tibet-dark',
    'bg-gradient-to-br from-tibet-brown via-tibet-red to-tibet-gold',
    'bg-gradient-to-br from-tibet-dark via-tibet-blue to-tibet-turquoise',
    'bg-gradient-to-br from-tibet-gold via-tibet-yellow to-tibet-white'
  ]
  const numericId = Number(spot.id)
  const index = Number.isFinite(numericId) ? Math.abs(numericId) % gradients.length : 0
  return gradients[index]
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  fetchSpots()
})

onMounted(() => {
  fetchSpots()
})
</script>
