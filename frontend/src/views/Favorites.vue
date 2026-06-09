<template>
  <div class="min-h-screen bg-stone-50 py-8 pt-24 sm:py-12">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <motion.div
        class="text-center mb-8 sm:mb-12"
        :initial="revealInitial"
        :animate="revealInView"
        :transition="revealTransition"
      >
        <h1 class="text-3xl font-bold text-stone-800 mb-3 sm:text-4xl sm:mb-4">{{ favoriteLabel('title') }}</h1>
        <p class="text-base text-stone-600 sm:text-lg">{{ favoriteLabel('subtitle') }}</p>
      </motion.div>

      <div
        v-if="loading"
        role="status"
        aria-busy="true"
        aria-live="polite"
        :aria-label="favoriteLabel('loading')"
        class="flex h-64 items-center justify-center"
      >
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
        <span class="sr-only">{{ favoriteLabel('loading') }}</span>
      </div>

      <div v-else-if="errorMessage" role="alert" aria-live="assertive" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-10 text-center">
        <p class="text-base font-semibold text-rose-800">{{ favoriteLabel('loadErrorTitle') }}</p>
        <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-rose-700">{{ errorMessage }}</p>
        <button
          type="button"
          class="mt-5 rounded-lg bg-red-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-red-700 disabled:opacity-50"
          :disabled="loading"
          @click="fetchFavorites(currentPage)"
        >
          {{ favoriteLabel('retryLoad') }}
        </button>
      </div>

      <div v-else-if="favorites.length === 0" class="text-center py-16">
        <p class="text-stone-500 text-lg">{{ favoriteLabel('empty') }}</p>
        <router-link to="/route-planner" class="mt-4 inline-block px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
          {{ favoriteLabel('goExplore') }}
        </router-link>
      </div>

      <div v-else class="space-y-4">
        <motion.div
          v-for="(fav, index) in favorites"
          :key="fav.id"
          class="bg-white rounded-lg shadow p-4 hover:shadow-md transition-shadow sm:p-6"
          :initial="cardInitial"
          :whileInView="cardInView"
          :inViewOptions="inViewOnce"
          :transition="cardTransition(index)"
          :whileHover="{ y: -2, scale: 1.01 }"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 flex-1">
              <h3 class="text-lg font-bold text-stone-800">{{ fav.route?.name || favoriteLabel('unknownRoute') }}</h3>
              <p v-if="favoriteRouteDescription(fav.route)" class="break-words text-sm text-stone-500 mt-1">{{ favoriteRouteDescription(fav.route) }}</p>
              <div class="flex flex-wrap items-center gap-2 mt-3 text-sm text-stone-600 sm:gap-4">
                <span v-if="fav.route?.days">{{ t('admin.daysValue', { count: fav.route.days }) }}</span>
                <span v-if="fav.route && fav.route.price !== null" class="text-red-600 font-semibold">¥{{ fav.route.price }}</span>
                <span v-if="fav.route?.difficulty" :class="favoriteDifficultyClass(fav.route.difficulty)" class="px-2 py-0.5 rounded text-xs">{{ fav.route.difficulty }}</span>
                <span v-if="fav.route?.temperature" class="text-xs">{{ fav.route.temperature }}</span>
              </div>
            </div>
            <motion.button
              type="button"
              @click="removeFavorite(fav.route?.id)"
              class="shrink-0 text-red-500 hover:text-red-700"
              :title="favoriteLabel('remove')"
              :aria-label="favoriteLabel('remove')"
              :whileHover="{ scale: 1.2 }"
              :whilePress="{ scale: 0.9 }"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
            </motion.button>
          </div>
        </motion.div>
      </div>

      <div
        v-if="!loading && !errorMessage && totalPages > 1"
        role="navigation"
        :aria-label="favoriteLabel('paginationLabel')"
        class="flex justify-center mt-8 gap-2 overflow-x-auto pb-1"
      >
        <button v-for="page in totalPages" :key="page" type="button" @click="fetchFavorites(page - 1)"
          :aria-current="currentPage === page - 1 ? 'page' : undefined"
          :disabled="loading || currentPage === page - 1"
          :class="currentPage === page - 1 ? 'bg-red-600 text-white' : 'bg-white text-stone-600 hover:bg-stone-100'"
          class="px-4 py-2 rounded-lg border transition-colors disabled:cursor-not-allowed disabled:opacity-70">{{ page }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { motion } from 'motion-v'
import { revealInitial, revealInView, revealTransition, cardInitial, cardInView, cardTransition, inViewOnce } from '../motion/presets'
import api, { endpoints } from '../api'
import { showToast } from '../composables/useToast'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'

const { t } = useI18n()
const favoriteFallbacks = {
  title: '我的收藏',
  subtitle: '继续查看你收藏过的西藏路线',
  empty: '还没有收藏路线',
  goExplore: '去规划路线',
  unknownRoute: '未命名路线',
  remove: '取消收藏',
  operationFailed: '操作失败，请稍后重试',
  loading: '正在加载收藏路线',
  loadErrorTitle: '收藏列表加载失败',
  loadFailed: '收藏列表加载失败，请稍后重试',
  retryLoad: '重新加载',
  paginationLabel: '收藏路线分页'
} as const
const favoriteLabel = (key: keyof typeof favoriteFallbacks) => {
  const i18nKey = `favorites.${key}`
  const translated = t(i18nKey)
  return translated === i18nKey ? favoriteFallbacks[key] : translated
}

interface FavoriteRoute {
  id: number
  name: string
  description: string
  days: number | null
  price: number | null
  difficulty: string
  temperature: string
}

interface FavoriteItem {
  id: number | string
  route: FavoriteRoute | null
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const readString = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  if (typeof value === 'string') return value.trim()
  return ''
}

const readFiniteNumber = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  if (typeof value !== 'number' && typeof value !== 'string') return null

  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : null
}

const readPositiveInteger = (record: Record<string, unknown>, key: string) => {
  const value = readFiniteNumber(record, key)
  if (value === null) return null

  const integerValue = Math.trunc(value)
  return integerValue > 0 ? integerValue : null
}

const normalizeFavoriteRoute = (value: unknown): FavoriteRoute | null => {
  if (!isRecord(value)) return null

  const id = readPositiveInteger(value, 'id')
  if (!id) return null

  const days = readPositiveInteger(value, 'days')
  const priceValue = readFiniteNumber(value, 'price')
  const price = priceValue !== null && priceValue >= 0 ? priceValue : null

  return {
    id,
    name: readString(value, 'name'),
    description: readString(value, 'description'),
    days,
    price,
    difficulty: readString(value, 'difficulty'),
    temperature: readString(value, 'temperature')
  }
}

const normalizeFavoriteItem = (value: unknown, index: number): FavoriteItem | null => {
  if (!isRecord(value)) return null

  const route = normalizeFavoriteRoute(value.route)
  const id = readPositiveInteger(value, 'id') ?? route?.id ?? `favorite-${index}`

  return { id, route }
}

const normalizeFavorites = (value: unknown): FavoriteItem[] => {
  const items = isRecord(value) && Array.isArray(value.content)
    ? value.content
    : Array.isArray(value)
      ? value
      : []

  return items
    .map(normalizeFavoriteItem)
    .filter((favorite): favorite is FavoriteItem => favorite !== null)
}

const normalizeTotalPages = (value: unknown) => {
  if (!isRecord(value)) return 1

  const totalPageCount = readPositiveInteger(value, 'totalPages')
  return totalPageCount ?? 1
}

const favoriteRouteDescription = (route: FavoriteRoute | null) => {
  const description = route?.description.trim()
  if (!description) return ''

  return description.length > 100 ? `${description.substring(0, 100)}...` : description
}

const favoriteDifficultyClass = (difficulty: string) => ({
  'bg-green-100 text-green-800': difficulty === 'EASY',
  'bg-yellow-100 text-yellow-800': difficulty === 'MEDIUM',
  'bg-red-100 text-red-800': difficulty === 'HARD'
})

const favorites = ref<FavoriteItem[]>([])
const loading = ref(true)
const currentPage = ref(0)
const totalPages = ref(1)
const errorMessage = ref('')
let latestFavoritesRequestId = 0

const fetchFavorites = async (page = 0) => {
  const requestId = latestFavoritesRequestId + 1
  latestFavoritesRequestId = requestId
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await api.get<unknown>(endpoints.favorites.list, { params: { page, size: 20 } })
    if (requestId !== latestFavoritesRequestId) return

    favorites.value = normalizeFavorites(response.data)
    totalPages.value = normalizeTotalPages(response.data)
    currentPage.value = page
  } catch (e) {
    if (requestId !== latestFavoritesRequestId) return

    console.error('Failed to fetch favorites:', summarizeClientError(e))
    errorMessage.value = safeClientErrorMessage(e, favoriteLabel('loadFailed'))
    favorites.value = []
  } finally {
    if (requestId === latestFavoritesRequestId) {
      loading.value = false
    }
  }
}

const removeFavorite = async (routeId?: number) => {
  if (!routeId) {
    showToast(favoriteLabel('operationFailed'), 'warning')
    return
  }

  try {
    await api.delete(endpoints.favorites.remove(routeId))
    favorites.value = favorites.value.filter(f => f.route?.id !== routeId)
  } catch (e) {
    console.error('Failed to remove favorite:', summarizeClientError(e))
    showToast(safeClientErrorMessage(e, favoriteLabel('operationFailed')), 'error')
  }
}

onMounted(() => fetchFavorites())
</script>
