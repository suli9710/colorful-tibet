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
          <div
            v-if="bookingDataLoading"
            class="rounded-2xl bg-tibet-gold/10 px-4 py-3 text-center text-sm font-medium text-tibet-brown/70"
            role="status"
            aria-live="polite"
            aria-busy="true"
          >
            {{ t('common.loading') }}
          </div>
          <div
            v-if="bookingDataLoadError"
            :id="bookingDataErrorId"
            class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800"
            role="alert"
            aria-live="assertive"
          >
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <span>{{ bookingDataLoadError }}</span>
              <button
                type="button"
                class="inline-flex min-h-10 items-center justify-center rounded-full bg-tibet-red px-4 py-2 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="bookingDataLoading"
                @click="loadBookingData"
              >
                {{ bookingDataLoading ? t('common.loading') : t('common.retry') }}
              </button>
            </div>
          </div>
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
              {{ t('hotel.bookingDateHelp', { max: MAX_BOOKING_NIGHTS }) }}
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
                {{ t('hotel.guestCapacityHelp', { capacity: selectedRoomCapacity }) }}
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
              {{ t('hotel.contactHelp') }}
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
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.perNight') }}</span><span class="font-medium">{{ t('common.priceCny', { price: selectedRoom.price }) }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.nights') }}</span><span class="font-medium">{{ nights }}{{ t('common.nightsUnit') }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.checkIn') }}</span><span class="font-medium">{{ form.checkInDate || '-' }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.checkOut') }}</span><span class="font-medium">{{ form.checkOutDate || '-' }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.roomSubtotal') }}</span><span class="font-medium">{{ t('common.priceCny', { price: roomSubtotal }) }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.platformServiceFee') }}</span><span class="font-medium">{{ t('common.priceCny', { price: 0 }) }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.dueToday') }}</span><span class="font-medium text-emerald-700">{{ t('common.priceCny', { price: 0 }) }}</span></div>
              <div class="flex justify-between gap-4"><span class="text-gray-500">{{ t('hotel.availabilityStatus') }}</span><span class="font-medium text-amber-700">{{ bookingAvailabilityLabel }}</span></div>
              <div class="flex justify-between border-t border-tibet-gold/20 pt-2 mt-2 gap-4">
                <span class="font-semibold">{{ t('hotel.referenceRoomFee') }}</span>
                <motion.span
                  :key="totalPrice"
                  class="text-xl font-bold text-blue-600"
                  :initial="{ opacity: 0, y: -6 }"
                  :animate="{ opacity: 1, y: 0 }"
                  :transition="{ duration: 0.2, ease: motionEase }"
                >
                  {{ totalPriceLabel }}
                </motion.span>
              </div>
              <div class="space-y-2 pt-1">
                <p v-for="item in bookingAssuranceItems" :key="item.id" class="rounded-xl px-3 py-2 text-xs leading-5" :class="item.className">
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
                {{ t('hotel.roomUnavailablePrompt') }}
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
    :title="totalPriceLabel"
    :meta="selectedRoom?.name || hotel?.name"
    :primary-label="submitting ? t('common.submitting') : t('hotel.submitInquiry')"
    :primary-disabled="submitting || bookingUnavailable"
    :primary-busy="submitting"
    :primary-described-by="bookingFormDescribedBy"
    @primary="submitBooking"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { isAxiosError } from 'axios'
import { getHotelById, hotels, type HotelItem, type HotelRoom } from '../data/hotels'
import { applyHotelImageFallback, resolveHotelCoverImage } from '../data/hotelImages'
import { getCanonicalRegion, localizeApiRoom, localizeHotel } from '../data/hotelTranslations'
import api, { endpoints } from '../api'
import { clearHotelOrderClientStorage } from '../api/cache'
import MobileStickyActionBar from '../components/MobileStickyActionBar.vue'
import { useBehaviorTracker } from '../composables/useBehaviorTracker'
import { toFiniteAmount } from '../i18n/formatting'
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

interface ApiHotelResponse {
  id: number
  name: string
  location?: string
  facilities?: string
  imageUrl?: string
  description?: string
  rating?: number | string | null
  priceMin?: number | string | null
  available?: boolean
}

interface BookingRoom extends HotelRoom {
  amenities?: string
  capacity?: number | string | null
  maxGuests?: number | string | null
}

interface BookingAssuranceItem {
  id: string
  title: string
  description: string
  className: string
}

const apiHotel = ref<ApiHotelResponse | null>(null)
const apiRoomTypes = ref<BookingRoom[]>([])
const bookingDataLoading = ref(false)
const bookingDataLoadError = ref('')
let bookingDataRequestId = 0

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const readText = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  if (typeof value === 'string') return value
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return undefined
}

const readBoolean = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  return typeof value === 'boolean' ? value : undefined
}

const readAmount = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  return typeof value === 'number' || typeof value === 'string' ? value : undefined
}

const readPositiveId = (record: Record<string, unknown>, key: string) => {
  const id = Math.trunc(toFiniteAmount(readAmount(record, key)))
  return id > 0 ? id : undefined
}

const normalizeApiHotel = (value: unknown): ApiHotelResponse | null => {
  if (!isRecord(value)) return null

  const id = readPositiveId(value, 'id') ?? hotelId.value
  const name = readText(value, 'name') || getHotelById(id)?.name || ''
  if (!name) return null

  return {
    id,
    name,
    location: readText(value, 'location'),
    facilities: readText(value, 'facilities'),
    imageUrl: readText(value, 'imageUrl'),
    description: readText(value, 'description'),
    rating: readAmount(value, 'rating') ?? null,
    priceMin: readAmount(value, 'priceMin') ?? null,
    available: readBoolean(value, 'available')
  }
}

const normalizeApiRoom = (value: unknown): BookingRoom | null => {
  if (!isRecord(value)) return null

  const id = readPositiveId(value, 'id')
  const name = readText(value, 'name')
  if (!id || !name) return null

  const desc = readText(value, 'desc') || readText(value, 'amenities') || ''
  return {
    id,
    name,
    price: toFiniteAmount(readAmount(value, 'price')),
    desc,
    amenities: readText(value, 'amenities') || desc,
    capacity: readAmount(value, 'capacity') ?? null,
    maxGuests: readAmount(value, 'maxGuests') ?? null
  }
}

const normalizeApiRooms = (value: unknown) =>
  Array.isArray(value)
    ? value.map(normalizeApiRoom).filter((room): room is BookingRoom => room !== null)
    : []

const loadBookingData = async () => {
  const requestId = ++bookingDataRequestId
  const requestedHotelId = hotelId.value
  bookingDataLoading.value = true
  bookingDataLoadError.value = ''
  apiHotel.value = null
  apiRoomTypes.value = []
  try {
    const [hotelRes, roomRes] = await Promise.all([
      api.get<unknown>(endpoints.hotels.detail(requestedHotelId)),
      api.get<unknown>(endpoints.hotels.roomTypes(requestedHotelId))
    ])
    if (requestId !== bookingDataRequestId || requestedHotelId !== hotelId.value) return

    const nextHotel = normalizeApiHotel(hotelRes.data)
    const nextRooms = normalizeApiRooms(roomRes.data)
    if (!nextHotel || nextRooms.length === 0) {
      throw new Error('Authoritative hotel booking data unavailable')
    }

    apiHotel.value = nextHotel
    apiRoomTypes.value = nextRooms
  } catch {
    if (requestId !== bookingDataRequestId || requestedHotelId !== hotelId.value) return
    apiHotel.value = null
    apiRoomTypes.value = []
    bookingDataLoadError.value = t('toast.pageLoadFailed')
  } finally {
    if (requestId === bookingDataRequestId && requestedHotelId === hotelId.value) {
      bookingDataLoading.value = false
    }
  }
}

watch(
  () => [route.params.id, route.query.roomId],
  () => {
    void loadBookingData()
  },
  { immediate: true }
)

const matchingStaticHotel = computed<HotelItem | undefined>(() => {
  const loadedHotel = apiHotel.value
  if (!loadedHotel) return getHotelById(hotelId.value)
  return hotels.find(item => item.name === loadedHotel.name) || getHotelById(hotelId.value)
})

const mappedApiHotel = computed<HotelItem | null>(() => {
  const loadedHotel = apiHotel.value
  if (!loadedHotel) return null

  const staticHotel = matchingStaticHotel.value
  const region = getCanonicalRegion(loadedHotel.location || '')
  const amenities = loadedHotel.facilities
    ? loadedHotel.facilities.split(',').map(f => f.trim()).filter(Boolean)
    : []
  const rating = toFiniteAmount(loadedHotel.rating ?? staticHotel?.rating ?? 4) || 4

  return {
    ...(staticHotel || {}),
    id: loadedHotel.id,
    region: staticHotel?.region || region,
    name: loadedHotel.name || staticHotel?.name || '',
    coverImage: resolveHotelCoverImage(staticHotel?.coverImage || loadedHotel.imageUrl || ''),
    city: region,
    address: loadedHotel.location || staticHotel?.address || '',
    description: loadedHotel.description || staticHotel?.description || '',
    tags: amenities.length ? amenities.slice(0, 4) : (staticHotel?.tags || []),
    amenities: amenities.length ? amenities : (staticHotel?.amenities || []),
    reviewCount: staticHotel?.reviewCount || 0,
    priceMin: toFiniteAmount(loadedHotel.priceMin ?? staticHotel?.priceMin ?? 0),
    available: loadedHotel.available ?? staticHotel?.available ?? true,
    rating,
    stars: Math.min(5, Math.max(3, Math.round(rating))),
    lng: staticHotel?.lng || 91.0,
    lat: staticHotel?.lat || 29.6,
    rooms: staticHotel?.rooms || []
  }
})

const hotel = computed<HotelItem | null>(() => {
  const rawHotel = mappedApiHotel.value || getHotelById(hotelId.value)
  return rawHotel ? localizeHotel(rawHotel, locale.value) : null
})

const bookingCoverImage = computed(() => {
  const coverImage = hotel.value?.coverImage || ''
  return resolveHotelCoverImage(coverImage)
})

const selectedRoomSource = computed<BookingRoom | null>(() => {
  if (!apiHotel.value || bookingDataLoadError.value) return null

  const apiRoom = apiRoomTypes.value.find(room => room.id === roomId.value)
  return apiRoom || null
})

const selectedRoom = computed<BookingRoom | null>(() => {
  if (!selectedRoomSource.value) return null
  return localizeApiRoom(selectedRoomSource.value, locale.value) as BookingRoom
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
const totalPriceLabel = computed(() => t('common.priceCny', { price: totalPrice.value }))
const bookingAvailabilityLabel = computed(() =>
  hotel.value?.available === false
    ? t('hotel.availability.full')
    : t('hotel.availability.needsConfirm')
)

const parseRoomCapacity = (room: BookingRoom | null) => {
  const explicitCapacity = Number(room?.capacity || room?.maxGuests)
  if (Number.isFinite(explicitCapacity) && explicitCapacity > 0) return explicitCapacity

  const text = [room?.desc, room?.amenities].filter(Boolean).join(' ')
  const match = text.match(/(?:可住|适合)\s*(\d+)\s*人|(\d+)\s*人/)
  const parsed = Number(match?.[1] || match?.[2])
  return Number.isFinite(parsed) && parsed > 0 ? parsed : DEFAULT_ROOM_CAPACITY
}

const selectedRoomCapacity = computed(() =>
  Math.min(6, Math.max(1, Math.round(parseRoomCapacity(selectedRoomSource.value))))
)
const guestOptions = computed(() =>
  Array.from({ length: selectedRoomCapacity.value }, (_, index) => index + 1)
)
const bookingUnavailable = computed(() =>
  bookingDataLoading.value ||
  Boolean(bookingDataLoadError.value) ||
  !apiHotel.value ||
  !hotel.value ||
  !selectedRoom.value ||
  hotel.value.available === false
)
const bookingAssuranceItems = computed<BookingAssuranceItem[]>(() => [
  {
    id: 'availability',
    title: t('hotel.assurance.availability.title'),
    description: t('hotel.assurance.availability.description'),
    className: 'bg-blue-50 text-blue-800'
  },
  {
    id: 'fee',
    title: t('hotel.assurance.fee.title'),
    description: t('hotel.assurance.fee.description'),
    className: 'bg-emerald-50 text-emerald-800'
  },
  {
    id: 'cancellation',
    title: t('hotel.assurance.cancellation.title'),
    description: t('hotel.assurance.cancellation.description'),
    className: 'bg-amber-50 text-amber-800'
  },
  {
    id: 'platform',
    title: t('hotel.assurance.platform.title'),
    description: t('hotel.noPlatformPaymentHint'),
    className: 'bg-gray-50 text-gray-700'
  }
])

const submitting = ref(false)
const submitError = ref('')
const submitAttempted = ref(false)
const bookingErrorId = 'hotel-booking-error-message'
const bookingDataErrorId = 'hotel-booking-data-error-message'
const dateHelpId = 'hotel-booking-date-help'
const guestHelpId = 'hotel-booking-guest-help'
const contactHelpId = 'hotel-booking-contact-help'
const bookingSummaryId = 'hotel-booking-summary'
const bookingValidationMessages = computed(() => ({
  roomUnavailable: t('hotel.validation.roomUnavailable'),
  dateRequired: t('hotel.validation.dateRequired'),
  checkInPast: t('hotel.validation.checkInPast'),
  dateRangeInvalid: t('hotel.validation.dateRangeInvalid'),
  nightsTooLong: t('hotel.validation.nightsTooLong', { max: MAX_BOOKING_NIGHTS }),
  guestsTooHigh: t('hotel.validation.guestsTooHigh'),
  guestNameRequired: t('hotel.validation.guestNameRequired'),
  phoneRequired: t('hotel.validation.phoneRequired'),
  securityVerificationFailed: t('hotel.validation.securityVerificationFailed')
}))
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
    bookingDataLoadError.value ? bookingDataErrorId : '',
    submitError.value ? bookingErrorId : ''
  ].filter(Boolean).join(' ')
)

const clearHotelOrderCache = clearHotelOrderClientStorage

const hasAntibotCode = (value: unknown) =>
  isRecord(value) &&
  typeof value.code === 'string' &&
  value.code.startsWith('ANTIBOT_')

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
    submitError.value = bookingValidationMessages.value.roomUnavailable
    return
  }
  if (!form.value.checkInDate || !form.value.checkOutDate) {
    submitError.value = bookingValidationMessages.value.dateRequired
    return
  }
  if (checkInBeforeToday.value) {
    submitError.value = bookingValidationMessages.value.checkInPast
    return
  }
  if (nights.value <= 0) {
    submitError.value = bookingValidationMessages.value.dateRangeInvalid
    return
  }
  if (nights.value > MAX_BOOKING_NIGHTS) {
    submitError.value = bookingValidationMessages.value.nightsTooLong
    return
  }
  if (Number(form.value.guests) > selectedRoomCapacity.value) {
    submitError.value = bookingValidationMessages.value.guestsTooHigh
    return
  }
  if (!form.value.guestName.trim()) {
    submitError.value = bookingValidationMessages.value.guestNameRequired
    return
  }
  if (phoneDigits.value.length < 6) {
    submitError.value = bookingValidationMessages.value.phoneRequired
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
  } catch (error: unknown) {
    clearHotelOrderCache()
    if (isAxiosError(error) && hasAntibotCode(error.response?.data)) {
      submitError.value = bookingValidationMessages.value.securityVerificationFailed
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
  router.push({ path: '/hotel-orders', query: { created: '1' } })
}
</script>
