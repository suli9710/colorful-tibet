<template>
  <div class="min-h-screen bg-tibet-white">
    <section class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-16 space-y-5 sm:space-y-6">
      <motion.div
        class="bg-white rounded-2xl shadow-xl border border-tibet-gold/20 p-5 sm:rounded-3xl sm:p-8"
        :initial="revealInitial"
        :animate="revealInView"
        :transition="revealTransition"
      >
        <h1 class="text-2xl font-bold text-gray-900 mb-3 sm:text-3xl sm:mb-4">{{ t('hotel.ordersTitle') }}</h1>
        <p class="text-gray-600">{{ t('hotel.ordersSubtitle') }}</p>
      </motion.div>

      <motion.div
        v-if="showBookingConfirmation"
        class="flex flex-col gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-emerald-800 shadow-sm sm:flex-row sm:items-center sm:justify-between"
        role="status"
        aria-live="polite"
        aria-atomic="true"
        :initial="{ opacity: 0, y: -8 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.24 }"
      >
        <div class="min-w-0">
          <p class="text-sm font-bold">{{ t('hotel.ordersTitle') }} · {{ t('hotel.status.pending') }}</p>
          <p class="mt-1 text-sm leading-relaxed text-emerald-700">{{ t('hotel.ordersSubtitle') }}</p>
        </div>
        <button
          type="button"
          class="min-h-10 shrink-0 rounded-xl border border-emerald-300 bg-white px-4 py-2 text-sm font-semibold text-emerald-800 transition hover:bg-emerald-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2"
          :aria-label="t('common.close')"
          @click="dismissBookingConfirmation"
        >
          {{ t('common.close') }}
        </button>
      </motion.div>

      <motion.div
        class="bg-white rounded-2xl shadow-xl border border-tibet-gold/20 p-4 sm:rounded-3xl sm:p-8"
        :initial="cardInitial"
        :animate="cardInView"
        :transition="cardTransition(0, 0.1)"
      >
        <div
          v-if="loadError"
          class="mb-4 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
        >
          <p class="break-words">{{ loadError }}</p>
          <button
            type="button"
            class="mt-3 min-h-11 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:ring-offset-2"
            :aria-label="t('common.reload')"
            @click="reloadOrders"
          >
            {{ t('common.reload') }}
          </button>
        </div>
        <div
          v-if="loading && sortedOrders.length === 0"
          class="rounded-2xl border border-tibet-gold/15 bg-amber-50/50 px-4 py-8 text-center text-gray-600"
          role="status"
          aria-live="polite"
          aria-busy="true"
        >
          <div class="mx-auto mb-3 h-9 w-9 animate-spin rounded-full border-2 border-tibet-gold/20 border-b-tibet-gold"></div>
          <p class="text-sm font-medium">{{ t('hotel.ordersLoading') }}</p>
        </div>
        <div
          v-else-if="!loadError && sortedOrders.length === 0"
          class="rounded-2xl border border-dashed border-tibet-gold/25 bg-white/70 px-4 py-10 text-center text-gray-600"
          role="status"
          aria-live="polite"
        >
          <p class="text-base font-semibold text-gray-800">{{ t('hotel.noOrders') }}</p>
          <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-gray-500">{{ t('hotel.ordersEmptyHint') }}</p>
          <router-link
            to="/hotels"
            class="mt-4 inline-flex min-h-11 items-center justify-center rounded-xl bg-tibet-red px-4 py-2 text-sm font-semibold text-tibet-yellow transition hover:bg-tibet-red/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2"
          >
            {{ t('hotel.bookNow') }}
          </router-link>
        </div>
        <div v-else-if="sortedOrders.length > 0" class="space-y-4">
          <div class="space-y-4" role="list" :aria-label="t('hotel.ordersTitle')">
          <motion.div
            v-for="(order, index) in sortedOrders"
            :key="order.id"
            class="rounded-2xl border border-tibet-gold/20 p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-4 sm:p-5"
            role="listitem"
            :initial="cardInitial"
            :whileInView="cardInView"
            :inViewOptions="inViewOnce"
            :transition="cardTransition(index)"
          >
            <div class="flex w-full min-w-0 items-start gap-3 sm:items-center sm:gap-4">
              <div class="h-20 w-24 rounded-2xl overflow-hidden bg-gray-100 flex-shrink-0 sm:w-28">
                <img :src="resolveHotelBookingImage(order)" class="w-full h-full object-cover" :alt="order.hotelName" @error="applyHotelImageFallback">
              </div>
              <div class="min-w-0 flex-1">
                <h2 class="line-clamp-2 break-words text-lg font-semibold text-gray-900">{{ displayHotelName(order) }} · {{ order.roomName || '-' }}</h2>
                <p class="mt-1 break-words text-sm text-gray-500">{{ order.checkInDate }} {{ t('hotel.dateConnector') }} {{ order.checkOutDate }} · {{ order.guests }}{{ t('hotel.guests') }}</p>
                <p class="mt-1 break-words text-sm text-gray-500">{{ t('hotel.booker') }}：{{ order.guestName }} · {{ maskPhone(order.phone) }}</p>
              </div>
            </div>
            <div class="w-full min-w-0 text-left md:w-auto md:text-right">
              <p class="break-words text-xl font-bold text-blue-600">{{ formatCurrency(order.totalPrice) }}</p>
              <p class="mt-1 inline-flex rounded-full bg-gray-100 px-2.5 py-1 text-xs font-semibold text-gray-500">{{ orderStatusLabel(order.status) }}</p>
            </div>
          </motion.div>
          </div>

          <div
            v-if="hotelOrdersTotalPages > 1"
            class="flex flex-wrap items-center justify-center gap-3 pt-2"
            role="navigation"
            :aria-label="t('hotel.ordersTitle')"
          >
            <span class="text-sm text-gray-500" role="status" aria-live="polite">
              {{ hotelOrdersPage + 1 }} / {{ hotelOrdersTotalPages }}
            </span>
            <button
              type="button"
              class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-semibold text-gray-700 transition hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50"
              :disabled="loadingMoreOrders || !hasMoreHotelOrders"
              :aria-busy="loadingMoreOrders"
              @click="loadNextOrdersPage"
            >
              {{ loadingMoreOrders ? t('common.loading') : t('community.nextPage') }}
            </button>
          </div>
        </div>
      </motion.div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { motion } from 'motion-v'
import { revealInitial, revealInView, revealTransition, cardInitial, cardInView, cardTransition, inViewOnce } from '../motion/presets'
import api, { endpoints } from '../api'
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata, type PaginatedHttpResponse } from '../api/endpoints'
import { clearHotelOrderClientStorage } from '../api/cache'
import { applyHotelImageFallback, resolveHotelBookingImage } from '../data/hotelImages'
import { safeClientErrorMessage } from '../utils/errorMonitoring'
import { toFiniteAmount, toIntlLocale } from '../i18n/formatting'

interface HotelOrder {
  id: string
  hotelId: number
  hotel?: {
    id?: number
    name?: string
    coverImage?: string
    imageUrl?: string
  }
  hotelName: string
  roomId: number
  roomName: string
  checkInDate: string
  checkOutDate: string
  guests: number
  guestName: string
  phone: string
  note: string
  nights: number
  subtotal: number
  discount: number
  totalPrice: number
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  createdAt: string
}

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const hotelOrdersPageSize = 20
const orders = ref<HotelOrder[]>([])
const hotelOrdersPageInfo = ref<PageMetadata>({
  page: 0,
  size: hotelOrdersPageSize,
  totalElements: 0,
  totalPages: 0
})
const loading = ref(false)
const loadingMoreOrders = ref(false)
const loadError = ref('')
let hotelOrdersRequestSequence = 0

const clearHotelOrderCache = clearHotelOrderClientStorage
const isCurrentHotelOrdersRequest = (requestSequence: number) => requestSequence === hotelOrdersRequestSequence

const hotelOrdersPage = computed(() => hotelOrdersPageInfo.value.page)
const hotelOrdersTotalPages = computed(() => hotelOrdersPageInfo.value.totalPages)
const hasMoreHotelOrders = computed(() => hasNextPage(hotelOrdersPageInfo.value))
const showBookingConfirmation = computed(() => route.query.created === '1')

const dismissBookingConfirmation = () => {
  const { created: _created, ...query } = route.query
  router.replace({ path: route.path, query })
}

const applyHotelOrdersPage = (response: PaginatedHttpResponse, append = false) => {
  const page = readPaginatedResponse<HotelOrder>(response, {
    page: append ? hotelOrdersPageInfo.value.page + 1 : 0,
    size: hotelOrdersPageSize
  })

  orders.value = append ? mergeUniqueById(orders.value, page.content) : page.content
  hotelOrdersPageInfo.value = {
    page: page.page,
    size: page.size || hotelOrdersPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const loadOrders = async (page = 0, append = false) => {
  const requestSequence = ++hotelOrdersRequestSequence
  if (append) {
    loading.value = false
    loadingMoreOrders.value = true
  } else {
    loading.value = true
    loadingMoreOrders.value = false
  }
  loadError.value = ''
  try {
    const response = await api.get(endpoints.hotelBookings.my, {
      params: { page, size: hotelOrdersPageSize }
    })
    clearHotelOrderCache()
    if (!isCurrentHotelOrdersRequest(requestSequence)) return
    applyHotelOrdersPage(response, append)
  } catch (error) {
    clearHotelOrderCache()
    if (!isCurrentHotelOrdersRequest(requestSequence)) return
    if (!append && orders.value.length === 0) {
      orders.value = []
      hotelOrdersPageInfo.value = {
        page: 0,
        size: hotelOrdersPageSize,
        totalElements: 0,
        totalPages: 0
      }
    }
    loadError.value = safeClientErrorMessage(error, t('hotel.bookingFailed'))
  } finally {
    if (!isCurrentHotelOrdersRequest(requestSequence)) return
    if (append) {
      loadingMoreOrders.value = false
    } else {
      loading.value = false
    }
  }
}

const loadNextOrdersPage = async () => {
  if (loading.value || loadingMoreOrders.value || !hasMoreHotelOrders.value) return
  await loadOrders(hotelOrdersPageInfo.value.page + 1, true)
}

const reloadOrders = () => {
  void loadOrders()
}

const displayHotelName = (order: HotelOrder) => order.hotel?.name || order.hotelName || '-'

const formatCurrency = (value?: number | string | null) => {
  const amount = toFiniteAmount(value)
  return amount.toLocaleString(toIntlLocale(locale.value), { style: 'currency', currency: 'CNY', maximumFractionDigits: 0 })
}

const maskPhone = (phone: string) => {
  const normalized = String(phone || '').replace(/\s+/g, '')
  if (normalized.length <= 4) return normalized || '-'
  return `${'*'.repeat(Math.max(3, normalized.length - 4))}${normalized.slice(-4)}`
}

const orderStatusLabel = (status: HotelOrder['status']) => {
  const key = status.toLowerCase() as 'pending' | 'confirmed' | 'cancelled'
  return t(`hotel.status.${key}`)
}

const handleOrdersUpdate = () => {
  loadOrders()
}

onMounted(() => {
  loadOrders()
  window.addEventListener('hotel-orders-updated', handleOrdersUpdate)
  window.addEventListener('bookings-updated', handleOrdersUpdate)
})

onUnmounted(() => {
  window.removeEventListener('hotel-orders-updated', handleOrdersUpdate)
  window.removeEventListener('bookings-updated', handleOrdersUpdate)
})

const sortedOrders = computed(() => [...orders.value].sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt)))
</script>
