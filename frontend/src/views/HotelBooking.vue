<template>
  <div class="min-h-screen bg-tibet-white">
    <motion.section
      class="relative overflow-hidden bg-gradient-to-br from-tibet-dark via-tibet-brown to-tibet-red text-white"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :transition="{ duration: 0.45, ease: motionEase }"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 sm:py-20 relative z-10">
        <motion.div
          class="max-w-3xl"
          :initial="{ opacity: 0, y: 22 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, delay: 0.08, ease: motionEase }"
        >
          <span class="inline-flex items-center px-4 py-1.5 rounded-full bg-white/15 backdrop-blur-md border border-white/20 text-sm font-medium mb-6">{{ t('hotel.badge') }}</span>
          <h1 class="text-3xl font-bold mb-4 sm:text-4xl md:text-5xl">{{ t('hotel.bookingTitle') }}</h1>
          <p class="text-base text-white/80 sm:text-lg md:text-xl">{{ t('hotel.bookingSubtitle') }}</p>
        </motion.div>
      </div>
    </motion.section>

    <motion.section
      class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-10 -mt-10 relative z-10"
      :initial="cardInitial"
      :animate="cardInView"
      :transition="cardTransition(0, 0.08)"
    >
      <motion.div
        v-if="!hotel"
        class="bg-white/90 backdrop-blur-xl rounded-3xl shadow-xl p-10 text-center"
        :initial="{ opacity: 0, y: 18 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="revealTransition"
      >
        <p class="text-gray-500">{{ t('hotel.noResults') }}</p>
        <router-link to="/hotels" class="mt-4 inline-block text-blue-600 hover:underline">{{ t('hotel.viewDetails') }}</router-link>
      </motion.div>

      <motion.div
        v-else
        class="grid grid-cols-1 lg:grid-cols-3 gap-8"
        :initial="{ opacity: 0, y: 20 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.38, ease: motionEase }"
      >
        <form
          class="lg:col-span-2 space-y-6"
          novalidate
          :aria-describedby="bookingFormDescribedBy"
          @submit.prevent="submitBooking"
        >
          <motion.div
            class="bg-white rounded-3xl p-5 border border-tibet-gold/20 shadow-sm sm:p-7"
            :initial="cardInitial"
            :animate="cardInView"
            :transition="cardTransition(0, 0.16)"
          >
            <h2 class="text-xl font-bold text-gray-900 mb-6">{{ t('hotel.checkIn') }} / {{ t('hotel.checkOut') }}</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label for="hotel-booking-check-in" class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.checkIn') }}</label>
                <input
                  id="hotel-booking-check-in"
                  v-model="form.checkInDate"
                  type="date"
                  autocomplete="off"
                  class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                  :aria-describedby="dateHelpId"
                  :aria-invalid="dateInvalid"
                  required
                />
              </div>
              <div>
                <label for="hotel-booking-check-out" class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.checkOut') }}</label>
                <input
                  id="hotel-booking-check-out"
                  v-model="form.checkOutDate"
                  type="date"
                  autocomplete="off"
                  class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                  :aria-describedby="dateHelpId"
                  :aria-invalid="dateInvalid"
                  required
                />
              </div>
            </div>
            <p :id="dateHelpId" class="mt-3 text-xs leading-relaxed text-gray-500">
              离店日期需要晚于入住日期，系统仅提交咨询意向，不在站内收款。
            </p>
            <div class="mt-4">
              <label for="hotel-booking-guests" class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.guests') }}</label>
              <select
                id="hotel-booking-guests"
                v-model="form.guests"
                class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                :aria-describedby="dateHelpId"
              >
                <option v-for="n in 6" :key="n" :value="n">{{ n }}{{ t('hotel.guests') }}</option>
              </select>
            </div>
          </motion.div>

          <motion.div
            class="bg-white rounded-3xl p-5 border border-tibet-gold/20 shadow-sm sm:p-7"
            :initial="cardInitial"
            :animate="cardInView"
            :transition="cardTransition(1, 0.16)"
          >
            <h2 class="text-xl font-bold text-gray-900 mb-6">{{ t('hotel.booker') }}</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label for="hotel-booking-guest-name" class="block text-sm font-medium text-gray-700 mb-2">{{ t('contact.name') }}</label>
                <input
                  id="hotel-booking-guest-name"
                  v-model="form.guestName"
                  type="text"
                  autocomplete="name"
                  :placeholder="t('contact.namePlaceholder')"
                  class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                  :aria-describedby="contactHelpId"
                  :aria-invalid="guestNameInvalid"
                  required
                />
              </div>
              <div>
                <label for="hotel-booking-phone" class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.phone') }}</label>
                <input
                  id="hotel-booking-phone"
                  v-model="form.phone"
                  type="tel"
                  inputmode="tel"
                  autocomplete="tel"
                  :placeholder="t('hotel.phonePlaceholder')"
                  class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                  :aria-describedby="contactHelpId"
                  :aria-invalid="phoneInvalid"
                  required
                />
              </div>
            </div>
            <p :id="contactHelpId" class="mt-3 text-xs leading-relaxed text-gray-500">
              联系方式仅用于酒店咨询确认，请填写可联系到您的姓名和电话。
            </p>
            <div class="mt-4">
              <label for="hotel-booking-note" class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.noteLabel') }}</label>
              <textarea
                id="hotel-booking-note"
                v-model="form.note"
                rows="3"
                autocomplete="off"
                :placeholder="t('hotel.notePlaceholder')"
                class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all resize-none"
                :aria-describedby="contactHelpId"
              ></textarea>
            </div>
          </motion.div>

          <AnimatePresence>
            <motion.div
              v-if="submitError"
              key="hotel-booking-error"
              :id="bookingErrorId"
              class="bg-red-50 border border-red-200 rounded-2xl p-4 text-red-700 text-sm"
              role="alert"
              aria-live="assertive"
              aria-atomic="true"
              :initial="{ opacity: 0, y: -10, scale: 0.98 }"
              :animate="{ opacity: 1, y: 0, scale: 1 }"
              :exit="{ opacity: 0, y: -8, scale: 0.98 }"
              :transition="{ duration: 0.24, ease: motionEase }"
            >
              {{ submitError }}
            </motion.div>
          </AnimatePresence>

          <motion.button
            type="submit"
            :disabled="submitting"
            :aria-busy="submitting"
            :aria-describedby="bookingFormDescribedBy"
            class="hidden min-h-12 w-full py-4 rounded-full bg-tibet-red text-tibet-yellow font-bold text-lg hover:bg-tibet-red/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed md:block"
            :initial="cardInitial"
            :animate="cardInView"
            :transition="cardTransition(2, 0.16)"
            :whileHover="submitting ? {} : { y: -2, scale: 1.005 }"
            :whileTap="submitting ? {} : { scale: 0.985 }"
          >
            <AnimatePresence mode="wait">
              <motion.span
                v-if="submitting"
                key="booking-submitting"
                :initial="{ opacity: 0, y: 6 }"
                :animate="{ opacity: 1, y: 0 }"
                :exit="{ opacity: 0, y: -6 }"
                :transition="{ duration: 0.18, ease: motionEase }"
              >
                {{ t('common.submitting') }}
              </motion.span>
              <motion.span
                v-else
                key="booking-confirm"
                :initial="{ opacity: 0, y: 6 }"
                :animate="{ opacity: 1, y: 0 }"
                :exit="{ opacity: 0, y: -6 }"
                :transition="{ duration: 0.18, ease: motionEase }"
              >
                {{ t('hotel.submitInquiry') }}
              </motion.span>
            </AnimatePresence>
          </motion.button>
        </form>

        <div>
          <motion.div
            class="bg-white rounded-3xl p-5 border border-tibet-gold/20 shadow-lg lg:sticky lg:top-24 sm:p-7"
            :initial="cardInitial"
            :animate="cardInView"
            :transition="cardTransition(3, 0.12)"
          >
            <div class="h-40 rounded-2xl overflow-hidden mb-4 bg-gray-100">
              <motion.img
                :src="bookingCoverImage"
                :alt="hotel.name"
                class="w-full h-full object-cover"
                :initial="{ opacity: 0, scale: 1.04 }"
                :animate="{ opacity: 1, scale: 1 }"
                :transition="{ duration: 0.42, ease: motionEase }"
                @error="applyHotelImageFallback"
              />
            </div>
            <h3 class="text-xl font-bold text-gray-900">{{ hotel.name }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ hotel.city }} · {{ hotel.address }}</p>

            <AnimatePresence>
              <motion.div
                v-if="selectedRoom"
                key="booking-room-summary"
                class="mt-4 pt-4 border-t border-tibet-gold/20 space-y-2 text-sm"
                :initial="{ opacity: 0, y: 12 }"
                :animate="{ opacity: 1, y: 0 }"
                :exit="{ opacity: 0, y: 8 }"
                :transition="{ duration: 0.28, ease: motionEase }"
              >
              <div class="flex justify-between"><span class="text-gray-500">{{ t('hotel.roomType') }}</span><span class="font-medium">{{ selectedRoom.name }}</span></div>
              <div class="flex justify-between"><span class="text-gray-500">{{ t('hotel.perNight') }}</span><span class="font-medium">¥{{ selectedRoom.price }}</span></div>
              <div class="flex justify-between"><span class="text-gray-500">{{ t('hotel.nights') }}</span><span class="font-medium">{{ nights }}{{ t('common.nightsUnit') }}</span></div>
              <div class="flex justify-between"><span class="text-gray-500">{{ t('hotel.checkIn') }}</span><span class="font-medium">{{ form.checkInDate || '-' }}</span></div>
              <div class="flex justify-between"><span class="text-gray-500">{{ t('hotel.checkOut') }}</span><span class="font-medium">{{ form.checkOutDate || '-' }}</span></div>
              <div class="flex justify-between border-t border-tibet-gold/20 pt-2 mt-2">
                <span class="font-semibold">{{ t('hotel.referenceRoomFee') }}</span>
                <motion.span
                  :key="totalPrice"
                  class="text-xl font-bold text-blue-600"
                  :initial="{ opacity: 0, y: -6 }"
                  :animate="{ opacity: 1, y: 0 }"
                  :transition="{ duration: 0.2, ease: motionEase }"
                >
                  ¥{{ totalPrice }}
                </motion.span>
              </div>
              <p class="rounded-xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
                {{ t('hotel.noPlatformPaymentHint') }}
              </p>
              </motion.div>
            </AnimatePresence>
          </motion.div>
        </div>
      </motion.div>
    </motion.section>
  </div>

  <MobileStickyActionBar
    :show="Boolean(hotel)"
    :eyebrow="t('hotel.referenceRoomFee')"
    :title="`¥${totalPrice}`"
    :meta="selectedRoom?.name || hotel?.name"
    :primary-label="submitting ? t('common.submitting') : t('hotel.submitInquiry')"
    :primary-disabled="submitting"
    @primary="submitBooking"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getHotelById, getRoomById, hotels } from '../data/hotels'
import { applyHotelImageFallback, resolveHotelCoverImage } from '../data/hotelImages'
import { getCanonicalRegion, localizeApiRoom, localizeHotel } from '../data/hotelTranslations'
import api, { endpoints } from '../api'
import { clearHotelOrderClientStorage } from '../api/cache'
import MobileStickyActionBar from '../components/MobileStickyActionBar.vue'
import { useBehaviorTracker } from '../composables/useBehaviorTracker'
import { safeClientErrorMessage } from '../utils/errorMonitoring'
import {
  cardInitial,
  cardInView,
  cardTransition,
  motionEase,
  revealTransition
} from '../motion/presets'

const { encodeBehaviorData, reset: resetBehavior } = useBehaviorTracker()

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const hotelId = computed(() => Number(route.params.id || 1))
const roomId = computed(() => Number(route.query.roomId || 1))
const apiHotel = ref<any>(null)
const apiRoomTypes = ref<any[]>([])

const loadBookingData = async () => {
  apiHotel.value = null
  apiRoomTypes.value = []
  try {
    const [hotelRes, roomRes] = await Promise.all([
      api.get(endpoints.hotels.detail(hotelId.value)),
      api.get(endpoints.hotels.roomTypes(hotelId.value))
    ])
    apiHotel.value = hotelRes.data
    apiRoomTypes.value = Array.isArray(roomRes.data) ? roomRes.data : []
  } catch (e) { /* fallback */ }
}

watch(
  () => [route.params.id, route.query.roomId],
  () => {
    void loadBookingData()
  },
  { immediate: true }
)

const matchingStaticHotel = computed(() => {
  if (!apiHotel.value) return getHotelById(hotelId.value)
  return hotels.find(item => item.name === apiHotel.value.name) || getHotelById(hotelId.value)
})

const mappedApiHotel = computed(() => {
  if (!apiHotel.value) return null
  const staticHotel = matchingStaticHotel.value
  const region = getCanonicalRegion(apiHotel.value.location || '')
  const amenities = apiHotel.value.facilities
    ? apiHotel.value.facilities.split(',').map((f: string) => f.trim()).filter(Boolean)
    : []
  return {
    ...(staticHotel || {}),
    ...apiHotel.value,
    id: apiHotel.value.id,
    coverImage: resolveHotelCoverImage(staticHotel?.coverImage || apiHotel.value.imageUrl),
    city: region,
    address: apiHotel.value.location || staticHotel?.address || '',
    tags: amenities.length ? amenities.slice(0, 4) : (staticHotel?.tags || []),
    amenities: amenities.length ? amenities : (staticHotel?.amenities || []),
    reviewCount: staticHotel?.reviewCount || 0,
    stars: Math.min(5, Math.max(3, Math.round(Number(apiHotel.value.rating) || staticHotel?.rating || 4))),
    lng: staticHotel?.lng || 91.0,
    lat: staticHotel?.lat || 29.6
  }
})

const hotel = computed(() => {
  const rawHotel = mappedApiHotel.value || getHotelById(hotelId.value)
  return rawHotel ? localizeHotel(rawHotel, locale.value) : null
})

const bookingCoverImage = computed(() => {
  const coverImage = hotel.value?.coverImage || ''
  return resolveHotelCoverImage(coverImage)
})

const selectedRoom = computed(() => {
  const apiRoom = apiRoomTypes.value.find((r: any) => r.id === roomId.value)
  if (apiRoom) return localizeApiRoom({ ...apiRoom, price: apiRoom.price, desc: apiRoom.amenities }, locale.value)
  const staticRoom = matchingStaticHotel.value?.rooms?.find(room => room.id === roomId.value) || getRoomById(hotelId.value, roomId.value)
  return staticRoom ? localizeApiRoom(staticRoom, locale.value) : null
})

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

const totalPrice = computed(() => {
  const roomTotal = (selectedRoom.value?.price || 0) * nights.value
  return roomTotal
})

const submitting = ref(false)
const submitError = ref('')
const submitAttempted = ref(false)
const bookingErrorId = 'hotel-booking-error-message'
const dateHelpId = 'hotel-booking-date-help'
const contactHelpId = 'hotel-booking-contact-help'
const bookingValidationMessages = {
  dateRequired: '请选择入住和离店日期',
  dateRangeInvalid: '离店日期需要晚于入住日期',
  guestNameRequired: '请填写预订人姓名',
  phoneRequired: '请填写联系电话',
  securityVerificationFailed: '安全校验未通过，请稍后重试'
}
const dateInvalid = computed(() => submitAttempted.value && (!form.value.checkInDate || !form.value.checkOutDate || nights.value <= 0))
const guestNameInvalid = computed(() => submitAttempted.value && !form.value.guestName.trim())
const phoneInvalid = computed(() => submitAttempted.value && !form.value.phone.trim())
const bookingFormDescribedBy = computed(() =>
  [dateHelpId, contactHelpId, submitError.value ? bookingErrorId : ''].filter(Boolean).join(' ')
)

const clearHotelOrderCache = clearHotelOrderClientStorage

const submitBooking = async () => {
  submitAttempted.value = true
  submitError.value = ''
  if (!form.value.checkInDate || !form.value.checkOutDate) {
    submitError.value = bookingValidationMessages.dateRequired
    return
  }
  if (nights.value <= 0) {
    submitError.value = bookingValidationMessages.dateRangeInvalid
    return
  }
  if (!form.value.guestName.trim()) {
    submitError.value = bookingValidationMessages.guestNameRequired
    return
  }
  if (!form.value.phone.trim()) {
    submitError.value = bookingValidationMessages.phoneRequired
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    const behaviorData = encodeBehaviorData()
    await api.post(endpoints.hotelBookings.create, {
      hotelId: hotelId.value,
      roomId: roomId.value,
      roomName: selectedRoom.value?.name || '',
      roomPrice: selectedRoom.value?.price || 0,
      nights: nights.value,
      checkInDate: form.value.checkInDate,
      checkOutDate: form.value.checkOutDate,
      guests: form.value.guests,
      guestName: form.value.guestName,
      phone: form.value.phone,
      note: form.value.note,
    }, {
      headers: {
        ...(behaviorData ? { 'X-Behavior-Data': behaviorData } : {}),
      }
    })
  } catch (error: any) {
    clearHotelOrderCache()
    if (error.response) {
      if (String(error.response?.data?.code || '').startsWith('ANTIBOT_')) {
        submitError.value = bookingValidationMessages.securityVerificationFailed
        return
      }
      submitError.value = safeClientErrorMessage(error, t('hotel.bookingFailed'))
      return
    }

    submitError.value = safeClientErrorMessage(error, t('hotel.bookingFailed'))
    return
  } finally {
    submitting.value = false
    resetBehavior()
  }

  clearHotelOrderCache()
  submitAttempted.value = false
  window.dispatchEvent(new CustomEvent('hotel-orders-updated'))
  window.dispatchEvent(new CustomEvent('bookings-updated'))
  router.push('/orders')
}
</script>
