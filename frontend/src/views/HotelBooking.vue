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
                  :min="minCheckInDate"
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
                  :min="minCheckOutDate"
                  class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                  :aria-describedby="dateHelpId"
                  :aria-invalid="dateInvalid"
                  required
                />
              </div>
            </div>
            <p :id="dateHelpId" class="mt-3 text-xs leading-relaxed text-gray-500">
              离店日期需要晚于入住日期，最长可提交 30 晚咨询；系统仅提交咨询意向，不在站内收款。
            </p>
            <div class="mt-4">
              <label for="hotel-booking-guests" class="block text-sm font-medium text-gray-700 mb-2">{{ t('hotel.guests') }}</label>
              <select
                id="hotel-booking-guests"
                v-model="form.guests"
                class="w-full px-4 py-3 rounded-2xl border border-tibet-gold/25 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                :aria-describedby="guestHelpId"
                :aria-invalid="guestsInvalid"
              >
                <option v-for="n in guestOptions" :key="n" :value="n">{{ n }}{{ t('hotel.guests') }}</option>
              </select>
              <p :id="guestHelpId" class="mt-2 text-xs leading-relaxed text-gray-500">
                当前房型建议 {{ selectedRoomCapacity }} 人内入住，超出人数请备注说明或更换房型。
              </p>
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
            :disabled="submitting || bookingUnavailable"
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
                id="hotel-booking-summary"
                role="status"
                aria-live="polite"
                class="mt-4 pt-4 border-t border-tibet-gold/20 space-y-2 text-sm"
                :initial="{ opacity: 0, y: 12 }"
                :animate="{ opacity: 1, y: 0 }"
                :exit="{ opacity: 0, y: 8 }"
                :transition="{ duration: 0.28, ease: motionEase }"
              >
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.roomType') }}</span><span class="text-right font-medium">{{ selectedRoom.name }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.perNight') }}</span><span class="font-medium">¥{{ selectedRoom.price }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.nights') }}</span><span class="font-medium">{{ nights }}{{ t('common.nightsUnit') }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.checkIn') }}</span><span class="font-medium">{{ form.checkInDate || '-' }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.checkOut') }}</span><span class="font-medium">{{ form.checkOutDate || '-' }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">房费小计</span><span class="font-medium">¥{{ roomSubtotal }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">平台服务费</span><span class="font-medium">¥0</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">今日应付</span><span class="font-medium text-emerald-700">¥0</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">房态</span><span class="font-medium text-amber-700">{{ bookingAvailabilityLabel }}</span></div>
              <div class="flex justify-between border-t border-tibet-gold/20 pt-2 mt-2 gap-4">
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
              <div class="space-y-2 pt-1">
                <p v-for="item in bookingAssuranceItems" :key="item.title" class="rounded-xl px-3 py-2 text-xs leading-5" :class="item.className">
                  <span class="font-semibold">{{ item.title }}</span> {{ item.description }}
                </p>
              </div>
              </motion.div>
              <motion.div
                v-else
                key="booking-room-unavailable"
                class="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-relaxed text-amber-800"
                role="alert"
                :initial="{ opacity: 0, y: 12 }"
                :animate="{ opacity: 1, y: 0 }"
                :exit="{ opacity: 0, y: 8 }"
                :transition="{ duration: 0.28, ease: motionEase }"
              >
                当前房型暂不可咨询，请返回酒店详情重新选择。
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
    :primary-disabled="submitting || bookingUnavailable"
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

const MAX_BOOKING_NIGHTS = 30
const DEFAULT_ROOM_CAPACITY = 2

const padDatePart = (value: number) => String(value).padStart(2, '0')
const formatDate = (d: Date) =>
  `${d.getFullYear()}-${padDatePart(d.getMonth() + 1)}-${padDatePart(d.getDate())}`

const parseDateValue = (value: string) => {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return null
  const [, year, month, day] = match
  const parsed = new Date(Number(year), Number(month) - 1, Number(day))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

const addDays = (date: Date, days: number) => {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

const today = new Date()
const defaultCheckInDate = addDays(today, 1)
const defaultCheckOutDate = addDays(defaultCheckInDate, 1)
const minCheckInDate = formatDate(today)

form.value.checkInDate = formatDate(defaultCheckInDate)
form.value.checkOutDate = formatDate(defaultCheckOutDate)

const minCheckOutDate = computed(() => {
  const checkIn = parseDateValue(form.value.checkInDate)
  return formatDate(addDays(checkIn || today, 1))
})

const nights = computed(() => {
  if (!form.value.checkInDate || !form.value.checkOutDate) return 0
  const checkIn = parseDateValue(form.value.checkInDate)
  const checkOut = parseDateValue(form.value.checkOutDate)
  if (!checkIn || !checkOut) return 0
  const diff = checkOut.getTime() - checkIn.getTime()
  return Math.max(0, Math.floor(diff / (1000 * 60 * 60 * 24)))
})

const totalPrice = computed(() => {
  const roomTotal = (Number(selectedRoom.value?.price) || 0) * nights.value
  return roomTotal
})
const roomSubtotal = computed(() => totalPrice.value)
const bookingAvailabilityLabel = computed(() => hotel.value?.available === false ? '暂满' : '需二次确认')

const parseRoomCapacity = (room: any) => {
  const explicitCapacity = Number(room?.capacity || room?.maxGuests)
  if (Number.isFinite(explicitCapacity) && explicitCapacity > 0) return explicitCapacity

  const text = [room?.desc, room?.amenities].filter(Boolean).join(' ')
  const match = text.match(/(?:可住|适合)\s*(\d+)\s*人|(\d+)\s*人/)
  const parsed = Number(match?.[1] || match?.[2])
  return Number.isFinite(parsed) && parsed > 0 ? parsed : DEFAULT_ROOM_CAPACITY
}

const selectedRoomCapacity = computed(() =>
  Math.min(6, Math.max(1, Math.round(parseRoomCapacity(selectedRoom.value))))
)
const guestOptions = computed(() =>
  Array.from({ length: selectedRoomCapacity.value }, (_, index) => index + 1)
)
const bookingUnavailable = computed(() =>
  !hotel.value || !selectedRoom.value || hotel.value.available === false
)
const bookingAssuranceItems = computed(() => [
  {
    title: '房态说明',
    description: '提交后生成咨询单，客服或供应商确认房态后再推进预订。',
    className: 'bg-blue-50 text-blue-800'
  },
  {
    title: '费用说明',
    description: '今日无需支付，参考房费按所选晚数估算，实际价格以供应商确认为准。',
    className: 'bg-emerald-50 text-emerald-800'
  },
  {
    title: '退改说明',
    description: '确认前可在订单中心取消咨询；确认后的退改政策以酒店或第三方平台为准。',
    className: 'bg-amber-50 text-amber-800'
  },
  {
    title: '平台提示',
    description: t('hotel.noPlatformPaymentHint'),
    className: 'bg-gray-50 text-gray-700'
  }
])

const submitting = ref(false)
const submitError = ref('')
const submitAttempted = ref(false)
const bookingErrorId = 'hotel-booking-error-message'
const dateHelpId = 'hotel-booking-date-help'
const guestHelpId = 'hotel-booking-guest-help'
const contactHelpId = 'hotel-booking-contact-help'
const bookingSummaryId = 'hotel-booking-summary'
const bookingValidationMessages = {
  roomUnavailable: '当前房型暂不可咨询，请返回酒店详情重新选择',
  dateRequired: '请选择入住和离店日期',
  checkInPast: '入住日期不能早于今天',
  dateRangeInvalid: '离店日期需要晚于入住日期',
  nightsTooLong: '单次酒店咨询最多支持 30 晚',
  guestsTooHigh: '入住人数超过当前房型建议容量，请调整人数或在备注中说明',
  guestNameRequired: '请填写预订人姓名',
  phoneRequired: '请填写可联系到的电话号码',
  securityVerificationFailed: '安全校验未通过，请稍后重试'
}
const phoneDigits = computed(() => form.value.phone.replace(/\D/g, ''))
const checkInBeforeToday = computed(() =>
  Boolean(form.value.checkInDate && form.value.checkInDate < minCheckInDate)
)
const dateInvalid = computed(() =>
  submitAttempted.value && (
    !form.value.checkInDate ||
    !form.value.checkOutDate ||
    checkInBeforeToday.value ||
    nights.value <= 0 ||
    nights.value > MAX_BOOKING_NIGHTS
  )
)
const guestsInvalid = computed(() => submitAttempted.value && Number(form.value.guests) > selectedRoomCapacity.value)
const guestNameInvalid = computed(() => submitAttempted.value && !form.value.guestName.trim())
const phoneInvalid = computed(() => submitAttempted.value && phoneDigits.value.length < 6)
const bookingFormDescribedBy = computed(() =>
  [
    dateHelpId,
    guestHelpId,
    contactHelpId,
    selectedRoom.value ? bookingSummaryId : '',
    submitError.value ? bookingErrorId : ''
  ].filter(Boolean).join(' ')
)

const clearHotelOrderCache = clearHotelOrderClientStorage

watch(selectedRoomCapacity, capacity => {
  if (Number(form.value.guests) > capacity) {
    form.value.guests = capacity
  }
})

watch(
  () => form.value.checkInDate,
  () => {
    if (!form.value.checkOutDate || form.value.checkOutDate <= form.value.checkInDate) {
      form.value.checkOutDate = minCheckOutDate.value
    }
  }
)

const submitBooking = async () => {
  if (submitting.value) return
  submitAttempted.value = true
  submitError.value = ''
  if (bookingUnavailable.value) {
    submitError.value = bookingValidationMessages.roomUnavailable
    return
  }
  if (!form.value.checkInDate || !form.value.checkOutDate) {
    submitError.value = bookingValidationMessages.dateRequired
    return
  }
  if (checkInBeforeToday.value) {
    submitError.value = bookingValidationMessages.checkInPast
    return
  }
  if (nights.value <= 0) {
    submitError.value = bookingValidationMessages.dateRangeInvalid
    return
  }
  if (nights.value > MAX_BOOKING_NIGHTS) {
    submitError.value = bookingValidationMessages.nightsTooLong
    return
  }
  if (Number(form.value.guests) > selectedRoomCapacity.value) {
    submitError.value = bookingValidationMessages.guestsTooHigh
    return
  }
  if (!form.value.guestName.trim()) {
    submitError.value = bookingValidationMessages.guestNameRequired
    return
  }
  if (phoneDigits.value.length < 6) {
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
