<template>
  <div class="min-h-screen bg-apple-gray-50">
    <section class="relative overflow-hidden bg-gradient-to-br from-blue-600 via-indigo-600 to-purple-700 text-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 relative z-10">
        <div class="max-w-3xl">
          <span class="inline-flex items-center px-4 py-1.5 rounded-full bg-white/15 backdrop-blur-md border border-white/20 text-sm font-medium mb-6">{{ t('hotel.badge') }}</span>
          <h1 class="text-4xl md:text-5xl font-bold tracking-tight mb-4">{{ t('hotel.bookingTitle') }}</h1>
          <p class="text-lg md:text-xl text-white/80">{{ t('hotel.bookingSubtitle') }}</p>
        </div>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 -mt-10 relative z-10">
      <div v-if="!hotel" class="bg-white/90 backdrop-blur-xl rounded-3xl shadow-xl p-10 text-center">
        <p class="text-gray-500">{{ t('hotel.noResults') }}</p>
        <router-link to="/hotels" class="mt-4 inline-block text-blue-600 hover:underline">{{ t('hotel.viewDetails') }}</router-link>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div class="lg:col-span-2 space-y-6">
          <div class="bg-white rounded-3xl p-7 border border-gray-100 shadow-sm">
            <h2 class="text-xl font-bold text-gray-900 mb-6">{{ t('hotel.checkIn') }} / {{ t('hotel.checkOut') }}</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.checkIn') }}</label>
                <input v-model="form.checkInDate" type="date" class="w-full px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.checkOut') }}</label>
                <input v-model="form.checkOutDate" type="date" class="w-full px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all" />
              </div>
            </div>
            <div class="mt-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.guests') }}</label>
              <select v-model="form.guests" class="w-full px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all">
                <option v-for="n in 6" :key="n" :value="n">{{ n }}{{ t('hotel.guests') }}</option>
              </select>
            </div>
          </div>

          <div class="bg-white rounded-3xl p-7 border border-gray-100 shadow-sm">
            <h2 class="text-xl font-bold text-gray-900 mb-6">{{ t('hotel.booker') }}</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">{{ t('contact.name') }}</label>
                <input v-model="form.guestName" type="text" :placeholder="t('contact.namePlaceholder')" class="w-full px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">电话</label>
                <input v-model="form.phone" type="tel" placeholder="请输入联系电话" class="w-full px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all" />
              </div>
            </div>
            <div class="mt-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">备注（选填）</label>
              <textarea v-model="form.note" rows="3" placeholder="如有特殊需求请在此说明..." class="w-full px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all resize-none"></textarea>
            </div>
          </div>

          <div v-if="submitError" class="bg-red-50 border border-red-200 rounded-2xl p-4 text-red-700 text-sm">{{ submitError }}</div>

          <button @click="submitBooking" :disabled="submitting" class="w-full py-4 rounded-full bg-blue-600 text-white font-bold text-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            {{ submitting ? '提交中...' : '确认预订' }}
          </button>
        </div>

        <div>
          <div class="sticky top-28 bg-white rounded-3xl p-7 border border-gray-100 shadow-lg">
            <div class="h-40 rounded-2xl overflow-hidden mb-4 bg-gray-100">
              <img :src="hotel.coverImage" :alt="hotel.name" class="w-full h-full object-cover" />
            </div>
            <h3 class="text-xl font-bold text-gray-900">{{ hotel.name }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ hotel.city }} · {{ hotel.address }}</p>

            <div v-if="selectedRoom" class="mt-4 pt-4 border-t border-gray-100 space-y-2 text-sm">
              <div class="flex justify-between"><span class="text-gray-500">房型</span><span class="font-medium">{{ selectedRoom.name }}</span></div>
              <div class="flex justify-between"><span class="text-gray-500">{{ t('hotel.perNight') }}</span><span class="font-medium">¥{{ selectedRoom.price }}</span></div>
              <div class="flex justify-between"><span class="text-gray-500">晚数</span><span class="font-medium">{{ nights }} 晚</span></div>
              <div class="flex justify-between"><span class="text-gray-500">入住</span><span class="font-medium">{{ form.checkInDate || '-' }}</span></div>
              <div class="flex justify-between"><span class="text-gray-500">离店</span><span class="font-medium">{{ form.checkOutDate || '-' }}</span></div>
              <div class="flex justify-between border-t border-gray-100 pt-2 mt-2"><span class="font-semibold">合计</span><span class="text-xl font-bold text-blue-600">¥{{ totalPrice }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getHotelById, getRoomById } from '../data/hotels'
import api, { endpoints } from '../api'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const hotelId = Number(route.params.id || 1)
const roomId = Number(route.query.roomId || 1)

const hotel = computed(() => getHotelById(hotelId))
const selectedRoom = computed(() => getRoomById(hotelId, roomId))

const form = ref({
  checkInDate: '',
  checkOutDate: '',
  guests: 2,
  guestName: '',
  phone: '',
  note: '',
})

const tonight = new Date()
tonight.setDate(tonight.getDate() + 1)
const tomorrow = new Date(tonight)
tomorrow.setDate(tomorrow.getDate() + 1)

const formatDate = (d: Date) => d.toISOString().split('T')[0]

form.value.checkInDate = formatDate(tonight)
form.value.checkOutDate = formatDate(tomorrow)

const nights = computed(() => {
  if (!form.value.checkInDate || !form.value.checkOutDate) return 0
  const diff = new Date(form.value.checkOutDate).getTime() - new Date(form.value.checkInDate).getTime()
  return Math.max(0, Math.floor(diff / (1000 * 60 * 60 * 24)))
})

const serviceFee = computed(() => Math.round((selectedRoom.value?.price || 0) * nights.value * 0.05))

const totalPrice = computed(() => {
  const roomTotal = (selectedRoom.value?.price || 0) * nights.value
  return roomTotal + serviceFee.value
})

const submitting = ref(false)
const submitError = ref('')

const submitBooking = async () => {
  submitError.value = ''
  if (!form.value.checkInDate || !form.value.checkOutDate) {
    submitError.value = '请选择入住和离店日期'
    return
  }
  if (!form.value.guestName.trim()) {
    submitError.value = '请填写预订人姓名'
    return
  }
  if (!form.value.phone.trim()) {
    submitError.value = '请填写联系电话'
    return
  }

  submitting.value = true
  try {
    await api.post(endpoints.hotelBookings.create, {
      hotelId,
      roomId,
      roomName: selectedRoom.value?.name || '',
      roomPrice: selectedRoom.value?.price || 0,
      nights: nights.value,
      checkInDate: form.value.checkInDate,
      checkOutDate: form.value.checkOutDate,
      guests: form.value.guests,
      guestName: form.value.guestName,
      phone: form.value.phone,
      note: form.value.note,
    })
  } catch {
    const order = {
      id: Date.now().toString(),
      hotelId,
      roomId,
      hotelName: hotel.value?.name || '',
      roomName: selectedRoom.value?.name || '',
      checkInDate: form.value.checkInDate,
      checkOutDate: form.value.checkOutDate,
      guests: form.value.guests,
      guestName: form.value.guestName,
      phone: form.value.phone,
      note: form.value.note,
      nights: nights.value,
      subtotal: (selectedRoom.value?.price || 0) * nights.value,
      serviceFee: serviceFee.value,
      discount: 0,
      totalPrice: totalPrice.value,
      status: 'PENDING',
      createdAt: new Date().toISOString(),
    }
    const existing = JSON.parse(localStorage.getItem('hotel-orders') || '[]')
    localStorage.setItem('hotel-orders', JSON.stringify([order, ...existing]))
  } finally {
    submitting.value = false
  }

  window.dispatchEvent(new CustomEvent('hotel-orders-updated'))
  window.dispatchEvent(new CustomEvent('bookings-updated'))
  router.push('/hotel-orders')
}
</script>
