<template>
  <div class="min-h-screen bg-tibet-white">
    <section class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-6">
      <motion.div
        class="bg-white rounded-3xl shadow-xl border border-tibet-gold/20 p-8"
        :initial="revealInitial"
        :animate="revealInView"
        :transition="revealTransition"
      >
        <h1 class="text-3xl font-bold text-gray-900 mb-4">{{ t('hotel.ordersTitle') }}</h1>
        <p class="text-gray-600">{{ t('hotel.ordersSubtitle') }}</p>
      </motion.div>

      <motion.div
        class="bg-white rounded-3xl shadow-xl border border-tibet-gold/20 p-8"
        :initial="cardInitial"
        :animate="cardInView"
        :transition="cardTransition(0, 0.1)"
      >
        <div v-if="loading" class="text-center text-gray-500 py-8">{{ t('hotel.ordersLoading') }}</div>
        <div v-else-if="sortedOrders.length === 0" class="text-center text-gray-500 py-8">{{ t('hotel.noOrders') }}</div>
        <div v-else class="space-y-4">
          <motion.div
            v-for="(order, index) in sortedOrders"
            :key="order.id"
            class="rounded-2xl border border-tibet-gold/20 p-5 flex flex-col md:flex-row md:items-center md:justify-between gap-4"
            :initial="cardInitial"
            :whileInView="cardInView"
            :inViewOptions="inViewOnce"
            :transition="cardTransition(index)"
          >
            <div class="flex items-center gap-4 min-w-0">
              <div class="h-20 w-28 rounded-2xl overflow-hidden bg-gray-100 flex-shrink-0">
                <img :src="resolveHotelBookingImage(order)" class="w-full h-full object-cover" :alt="order.hotelName" @error="applyHotelImageFallback">
              </div>
              <div class="min-w-0">
                <h2 class="text-lg font-semibold text-gray-900 truncate">{{ displayHotelName(order) }} · {{ order.roomName || '-' }}</h2>
                <p class="text-sm text-gray-500 mt-1">{{ order.checkInDate }} {{ t('hotel.dateConnector') }} {{ order.checkOutDate }} · {{ order.guests }}{{ t('hotel.guests') }}</p>
                <p class="text-sm text-gray-500 mt-1">{{ t('hotel.booker') }}：{{ order.guestName }} · {{ order.phone }}</p>
              </div>
            </div>
            <div class="text-right">
              <p class="text-xl font-bold text-blue-600">¥{{ order.totalPrice }}</p>
              <p class="text-xs text-gray-400 mt-1">{{ order.status }}</p>
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
import { applyHotelImageFallback, resolveHotelBookingImage } from '../data/hotelImages'

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
  serviceFee: number
  discount: number
  totalPrice: number
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  createdAt: string
}

const { t } = useI18n()
const orders = ref<HotelOrder[]>([])
const loading = ref(false)

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await api.get(endpoints.hotelBookings.my)
    localStorage.removeItem('hotel-orders')
    orders.value = Array.isArray(response.data?.content) ? response.data.content : (Array.isArray(response.data) ? response.data : [])
  } catch {
    localStorage.removeItem('hotel-orders')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const displayHotelName = (order: HotelOrder) => order.hotel?.name || order.hotelName || '-'

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
