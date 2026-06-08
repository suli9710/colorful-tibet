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
        class="bg-white rounded-2xl shadow-xl border border-tibet-gold/20 p-4 sm:rounded-3xl sm:p-8"
        :initial="cardInitial"
        :animate="cardInView"
        :transition="cardTransition(0, 0.1)"
      >
        <div
          v-if="loadError"
          class="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
        >
          <p class="break-words">{{ loadError }}</p>
          <button
            type="button"
            class="mt-3 min-h-11 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:ring-offset-2"
            :aria-label="t('common.reload')"
            @click="loadOrders"
          >
            {{ t('common.reload') }}
          </button>
        </div>
        <div
          v-else-if="loading"
          class="rounded-2xl border border-tibet-gold/15 bg-amber-50/50 px-4 py-8 text-center text-gray-600"
          role="status"
          aria-live="polite"
          aria-busy="true"
        >
          <div class="mx-auto mb-3 h-9 w-9 animate-spin rounded-full border-2 border-tibet-gold/20 border-b-tibet-gold"></div>
          <p class="text-sm font-medium">{{ t('hotel.ordersLoading') }}</p>
        </div>
        <div
          v-else-if="sortedOrders.length === 0"
          class="rounded-2xl border border-dashed border-tibet-gold/25 bg-white/70 px-4 py-10 text-center text-gray-600"
          role="status"
          aria-live="polite"
        >
          <p class="text-base font-semibold text-gray-800">{{ t('hotel.noOrders') }}</p>
          <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-gray-500">选择酒店和房型后提交咨询意向，记录会出现在这里。</p>
          <router-link
            to="/hotels"
            class="mt-4 inline-flex min-h-11 items-center justify-center rounded-xl bg-tibet-red px-4 py-2 text-sm font-semibold text-tibet-yellow transition hover:bg-tibet-red/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2"
          >
            {{ t('hotel.bookNow') }}
          </router-link>
        </div>
        <div v-else class="space-y-4" role="list" :aria-label="t('hotel.ordersTitle')">
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
      </motion.div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { motion } from 'motion-v'
import { revealInitial, revealInView, revealTransition, cardInitial, cardInView, cardTransition, inViewOnce } from '../motion/presets'
import api, { endpoints } from '../api'
import { clearHotelOrderClientStorage } from '../api/cache'
import { applyHotelImageFallback, resolveHotelBookingImage } from '../data/hotelImages'
import { safeClientErrorMessage } from '../utils/errorMonitoring'

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

const { t } = useI18n()
const orders = ref<HotelOrder[]>([])
const loading = ref(false)
const loadError = ref('')

const clearHotelOrderCache = clearHotelOrderClientStorage

const loadOrders = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const response = await api.get(endpoints.hotelBookings.my)
    clearHotelOrderCache()
    orders.value = Array.isArray(response.data?.content) ? response.data.content : (Array.isArray(response.data) ? response.data : [])
  } catch (error) {
    clearHotelOrderCache()
    orders.value = []
    loadError.value = safeClientErrorMessage(error, t('hotel.bookingFailed'))
  } finally {
    loading.value = false
  }
}

const displayHotelName = (order: HotelOrder) => order.hotel?.name || order.hotelName || '-'

const formatCurrency = (value?: number | string | null) => {
  const amount = Number(value || 0)
  return amount.toLocaleString('zh-CN', { style: 'currency', currency: 'CNY', maximumFractionDigits: 0 })
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
