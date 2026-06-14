<template>
  <div v-if="hotel" class="min-h-screen tibet-bg-subtle" :aria-busy="loadingHotel || loadingRooms">
    <!-- Hero -->
    <motion.section
      class="relative h-[42vh] min-h-[360px] overflow-hidden sm:h-[50vh]"
      :initial="{ opacity: 0, scale: 1.02 }"
      :animate="{ opacity: 1, scale: 1 }"
      :transition="{ duration: 0.5, ease: motionEase }"
    >
      <img
        :src="hotelCoverImage"
        :alt="hotel.name"
        class="w-full h-full object-cover"
        @error="applyHotelImageFallback"
      />
      <div class="absolute inset-0 bg-gradient-to-b from-tibet-dark/30 via-tibet-dark/10 to-tibet-dark/80"></div>
      <!-- 底部经幡色带 -->
      <div class="absolute bottom-0 left-0 right-0 h-1 tibet-prayer-flag opacity-80"></div>
      <!-- Back -->
      <router-link
        to="/hotels"
        class="absolute top-6 left-6 p-2.5 rounded-full bg-tibet-white/15 backdrop-blur border border-tibet-gold/30 text-tibet-white hover:bg-tibet-white/25 transition-colors z-10"
        :aria-label="t('common.back')"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
      </router-link>
      <motion.div
        class="absolute bottom-0 left-0 right-0 max-w-7xl mx-auto p-5 sm:p-8"
        :initial="{ opacity: 0, y: 20 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.6, delay: 0.2, ease: motionEase }"
      >
        <div class="flex items-center gap-2 mb-3">
          <span class="flex items-center gap-0.5 text-tibet-yellow text-sm">
            <span v-for="n in hotel.stars" :key="n">★</span>
          </span>
          <span class="text-tibet-white/40 text-sm">·</span>
          <span class="text-tibet-white/70 text-sm">{{ t('hotel.detailBadge') }}</span>
        </div>
        <h1 class="text-2xl font-bold text-tibet-white mb-2 sm:text-3xl md:text-5xl">{{ hotel.name }}</h1>
        <div class="flex flex-wrap items-center gap-2 text-tibet-white/70 text-sm sm:gap-3">
          <span class="flex items-center gap-1"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg> {{ hotel.city }} · {{ hotel.address }}</span>
          <span>·</span>
          <span class="flex items-center gap-1">★ {{ hotel.rating }} ({{ hotel.reviewCount }}{{ t('hotel.reviewCountUnit') }})</span>
        </div>
      </motion.div>
    </motion.section>

    <!-- Content -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-10">
      <div
        v-if="hotelLoadError"
        class="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800"
        role="alert"
        aria-live="assertive"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <span>{{ hotelLoadError }}</span>
          <button
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-tibet-red px-4 py-2 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loadingHotel || loadingRooms"
            @click="loadHotelDetail"
          >
            {{ loadingHotel || loadingRooms ? t('common.loading') : t('common.retry') }}
          </button>
        </div>
      </div>
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Main -->
        <motion.div
          class="lg:col-span-2 space-y-8"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <!-- Description — 藏式卡片 -->
          <div class="tibet-card rounded-2xl p-5 sm:p-8">
            <h2 class="tibet-heading text-xl font-bold text-tibet-dark mb-5">{{ t('hotel.introduction') }}</h2>
            <p class="text-tibet-brown/80 leading-relaxed">{{ hotel.description }}</p>
            <div class="flex flex-wrap gap-2 mt-5">
              <span v-for="tag in hotel.tags" :key="tag" class="px-3 py-1.5 rounded-full bg-tibet-blue/10 text-tibet-blue text-sm font-medium">{{ tag }}</span>
            </div>
          </div>

          <!-- Amenities — 藏式方格 -->
          <div class="tibet-card rounded-2xl p-5 sm:p-8">
            <h2 class="tibet-heading text-xl font-bold text-tibet-dark mb-5">{{ t('hotel.amenities') }}</h2>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3">
              <div v-for="amenity in hotel.amenities" :key="amenity" class="flex items-center gap-2 px-4 py-3 rounded-xl bg-tibet-white text-tibet-brown/70 text-sm border border-tibet-gold/10 hover:border-tibet-gold/30 transition-colors">
                <svg class="w-4 h-4 text-tibet-turquoise shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                {{ amenity }}
              </div>
            </div>
          </div>

          <!-- Rooms — 藏式房型卡片 -->
          <div class="tibet-card rounded-2xl p-5 sm:p-8">
            <h2 class="tibet-heading text-xl font-bold text-tibet-dark mb-5">{{ t('hotel.roomSelection') }}</h2>
            <div
              v-if="loadingRooms"
              class="mb-4 rounded-xl bg-tibet-gold/10 px-4 py-3 text-center text-sm text-tibet-brown/70"
              role="status"
              aria-live="polite"
              aria-busy="true"
            >
              {{ t('common.loading') }}
            </div>
            <div
              v-if="roomTypesLoadError"
              class="mb-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
              role="alert"
              aria-live="assertive"
            >
              <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <span>{{ roomTypesLoadError }}</span>
                <button
                  type="button"
                  class="inline-flex min-h-10 items-center justify-center rounded-full bg-tibet-red px-4 py-2 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="loadingRooms"
                  @click="loadHotelDetail"
                >
                  {{ loadingRooms ? t('common.loading') : t('common.retry') }}
                </button>
              </div>
            </div>
            <div v-if="!loadingRooms && roomTypes.length === 0" class="text-center py-4 text-stone-500">{{ t('hotel.noRoomTypes') }}</div>
            <div v-else-if="roomTypes.length > 0" class="space-y-4" role="list">
              <div v-for="room in roomTypes" :key="room.id"
                   role="listitem"
                   class="group rounded-xl border border-tibet-gold/15 p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-tibet-gold/40 hover:shadow-lg hover:shadow-tibet-red/5 transition-all duration-300">
                <div class="flex-1">
                  <h3 class="text-lg font-semibold text-tibet-dark">{{ room.name }}</h3>
                  <p class="text-sm text-tibet-brown/50 mt-1">{{ room.amenities || (room.capacity ? room.capacity + t('common.peopleUnit') : '') }}</p>
                </div>
                <div class="flex w-full items-center justify-between gap-4 sm:w-auto sm:justify-end sm:gap-5">
                  <div class="text-right">
                    <div class="flex items-baseline gap-0.5 justify-end">
                      <span class="text-xs text-tibet-brown/40">¥</span>
                      <span class="text-2xl font-bold text-tibet-red">{{ room.price }}</span>
                    </div>
                    <span class="text-xs text-tibet-brown/40">{{ t('hotel.perNight') }}</span>
                  </div>
                  <button type="button" class="tibet-btn text-sm" :aria-label="`${t('hotel.externalBook')} ${room.name}`" @click="openHotelBooking(room)">{{ t('hotel.externalBook') }}</button>
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        <!-- Sidebar -->
        <motion.div
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="{ duration: 0.5, delay: 0.15, ease: motionEase }"
        >
          <div class="sticky top-24 space-y-6">
            <!-- Price Card — 藏式金边 -->
            <div class="tibet-card rounded-2xl p-5 sm:p-8">
              <p class="text-sm text-tibet-brown/50 mb-1">{{ t('hotel.startingPrice') }}</p>
              <div class="flex items-baseline gap-1 mb-6">
                <span class="text-4xl font-bold text-tibet-red">{{ displayPriceLabel }}</span>
                <span class="text-sm text-tibet-brown/40">{{ t('hotel.perNightCompact') }}</span>
              </div>
              <div class="space-y-3 mb-6">
                <div class="flex items-center gap-2 text-sm text-tibet-dark">
                  <span class="flex items-center gap-0.5 text-tibet-gold"><span v-for="n in hotel.stars" :key="n">★</span></span>
                  <span>{{ hotel.stars }}{{ t('hotel.starUnit') }} {{ t('hotel.hotelLevel') }}</span>
                </div>
                <div class="flex items-center gap-2 text-sm text-tibet-dark">
                  <span class="text-tibet-gold font-semibold">★ {{ hotel.rating }}</span>
                  <span class="text-tibet-brown/50">{{ hotel.reviewCount }}{{ t('hotel.reviewCountUnit') }}</span>
                </div>
              </div>
              <button type="button"
                      class="w-full flex items-center justify-center gap-2 px-5 py-3.5 rounded-full bg-tibet-red text-tibet-yellow font-semibold hover:bg-tibet-red/90 transition-colors shadow-lg shadow-tibet-red/20"
                      :aria-label="`${t('hotel.externalBook')} ${hotel.name}`"
                      @click="openHotelBooking()">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                {{ t('hotel.externalBook') }}
              </button>
              <a :href="`https://uri.amap.com/navigation?to=${hotel.lng},${hotel.lat},${encodeURIComponent(hotel.name)}&mode=car&utm_source=colorful-tibet`"
                 target="_blank" rel="noopener noreferrer"
                 :aria-label="`${t('hotel.navigate')} ${hotel.name}`"
                 class="mt-3 w-full flex items-center justify-center gap-2 px-5 py-3 rounded-full bg-tibet-turquoise text-white font-semibold hover:bg-tibet-turquoise/90 transition-colors">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                {{ t('hotel.navigate') }}
              </a>
            </div>

            <!-- Amenity Highlights -->
            <div class="tibet-card rounded-2xl p-6">
              <h3 class="text-sm font-semibold text-tibet-brown/50 uppercase tracking-wide mb-4">{{ t('hotel.amenities') }}</h3>
              <div class="space-y-2.5">
                <div v-for="amenity in hotel.amenities" :key="amenity" class="flex items-center gap-2 text-sm text-tibet-dark/80">
                  <svg class="w-4 h-4 text-tibet-turquoise shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                  {{ amenity }}
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>

    <!-- 底部经幡装饰 -->
    <div class="h-2 tibet-prayer-flag opacity-60"></div>
  </div>

  <div
    v-else
    class="min-h-screen tibet-bg-subtle px-4 py-24"
    :aria-busy="loadingHotel"
  >
    <div
      v-if="loadingHotel"
      class="mx-auto max-w-xl rounded-2xl bg-white p-8 text-center text-tibet-brown/70 shadow-sm"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      {{ t('common.loading') }}
    </div>
    <div
      v-else
      class="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-8 text-center text-amber-800"
      role="alert"
    >
      <p class="text-base font-semibold">{{ hotelLoadError || t('toast.pageLoadFailed') }}</p>
      <router-link to="/hotels" class="mt-4 inline-flex min-h-10 items-center justify-center rounded-full bg-tibet-red px-5 py-2 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90">
        {{ t('common.back') }}
      </router-link>
    </div>
  </div>

  <MobileStickyActionBar
    :show="Boolean(hotel?.id)"
    :eyebrow="t('hotel.startingPrice')"
    :title="displayPriceLabel"
    :meta="hotel?.name"
    :secondary-label="t('hotel.navigate')"
    :primary-label="t('hotel.externalBook')"
    @secondary="openHotelNavigation"
    @primary="openHotelBooking"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { motion } from 'motion-v'
import { motionEase, revealInitial, revealInView, revealTransition, inViewOnce } from '../motion/presets'
import { getHotelById, hotels, type HotelItem, type HotelRoom } from '../data/hotels'
import { applyHotelImageFallback, resolveHotelCoverImage } from '../data/hotelImages'
import { getCanonicalRegion, localizeApiRoom, localizeHotel } from '../data/hotelTranslations'
import api, { endpoints } from '../api'
import MobileStickyActionBar from '../components/MobileStickyActionBar.vue'
import { openExternalBooking } from '../utils/externalBooking'
import { useToast } from '../composables/useToast'
import { toFiniteAmount } from '../i18n/formatting'

const { t, locale } = useI18n()
const { showToast } = useToast()
const route = useRoute()
const hotelId = Number(route.params.id || 1)
const rawHotel = ref<HotelDisplayItem | null>(getHotelById(hotelId) || null)
const rawRoomTypes = ref<HotelDisplayRoom[]>([])
const loadingHotel = ref(true)
const loadingRooms = ref(true)
const hotelLoadError = ref('')
const roomTypesLoadError = ref('')
let hotelDetailRequestId = 0

interface ApiHotelResponse {
  id: number
  name: string
  location?: string
  priceRange?: string
  rating?: number | string | null
  imageUrl?: string
  facilities?: string
}

interface ApiRoomTypeResponse {
  id: number
  name: string
  price: number
  capacity?: number | null
  imageUrl?: string
  amenities?: string
}

interface HotelDisplayItem extends HotelItem {
  priceRange?: string
  phone?: string
}

interface HotelDisplayRoom extends HotelRoom {
  capacity?: number | null
  imageUrl?: string
  amenities?: string
}

const fallbackHotel = () => getHotelById(hotelId) || hotels[0] || null
const hotel = computed(() => rawHotel.value ? localizeHotel(rawHotel.value, locale.value) : null)
const hotelCoverImage = computed(() => resolveHotelCoverImage(hotel.value?.coverImage || ''))
const roomTypes = computed(() =>
  rawRoomTypes.value.map(room => localizeApiRoom(room, locale.value) as HotelDisplayRoom)
)

const displayPrice = computed(() => {
  if (hotel.value?.priceRange) return hotel.value.priceRange
  if (hotel.value?.priceMin) return String(hotel.value.priceMin)
  return t('common.consult')
})
const displayPriceLabel = computed(() =>
  displayPrice.value === t('common.consult') || displayPrice.value.includes('¥')
    ? displayPrice.value
    : `¥${displayPrice.value}`
)

const openHotelBooking = (room?: { name?: string }) => {
  if (!hotel.value) return
  const opened = openExternalBooking({
    kind: 'hotel',
    name: [hotel.value.name, room?.name].filter(Boolean).join(' '),
    location: hotel.value.city || hotel.value.address
  })
  if (!opened) {
    showToast(t('spotDetail.externalBookBlocked'), 'warning')
  }
}

const openHotelNavigation = () => {
  if (!hotel.value) return
  window.open(
    `https://uri.amap.com/navigation?to=${hotel.value.lng},${hotel.value.lat},${encodeURIComponent(hotel.value.name)}&mode=car&utm_source=colorful-tibet`,
    '_blank',
    'noopener,noreferrer'
  )
}

const resolveRegion = (location: string): string => {
  return getCanonicalRegion(location)
}

const splitFacilities = (facilities?: string): string[] =>
  facilities
    ? facilities.split(',').map((item: string) => item.trim()).filter(Boolean)
    : []

const parsePriceMin = (priceRange?: string, fallback = 300): number => {
  const match = (priceRange || '').match(/(\d+)/)
  return match ? parseInt(match[1], 10) || fallback : fallback
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const readText = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  if (typeof value === 'string') return value.trim()
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return undefined
}

const readAmount = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  return typeof value === 'number' || typeof value === 'string' ? value : undefined
}

const readPositiveId = (record: Record<string, unknown>, key: string) => {
  const id = Math.trunc(toFiniteAmount(readAmount(record, key)))
  return id > 0 ? id : undefined
}

const toList = <T>(value: unknown, normalize: (item: unknown) => T | null) =>
  Array.isArray(value)
    ? value.map(normalize).filter((item): item is T => item !== null)
    : []

const normalizeApiHotel = (value: unknown): ApiHotelResponse | null => {
  if (!isRecord(value)) return null

  const id = readPositiveId(value, 'id')
  const name = readText(value, 'name')
  if (!id || !name) return null

  return {
    id,
    name,
    location: readText(value, 'location'),
    priceRange: readText(value, 'priceRange'),
    rating: readAmount(value, 'rating') ?? null,
    imageUrl: readText(value, 'imageUrl'),
    facilities: readText(value, 'facilities')
  }
}

const normalizeApiRoom = (value: unknown): ApiRoomTypeResponse | null => {
  if (!isRecord(value)) return null

  const id = readPositiveId(value, 'id')
  const name = readText(value, 'name')
  if (!id || !name) return null

  return {
    id,
    name,
    price: toFiniteAmount(readAmount(value, 'price')),
    capacity: readPositiveId(value, 'capacity') ?? null,
    imageUrl: readText(value, 'imageUrl'),
    amenities: readText(value, 'amenities')
  }
}

const toHotelDisplayRoom = (room: HotelRoom | ApiRoomTypeResponse): HotelDisplayRoom => {
  const desc = 'desc' in room ? room.desc : (room.amenities || '')
  const amenities = 'amenities' in room ? (room.amenities || desc) : desc

  return {
    ...room,
    desc,
    amenities
  }
}

const mapApiHotel = (apiHotel: ApiHotelResponse, staticHotel?: HotelItem): HotelDisplayItem => {
  const region = resolveRegion(apiHotel.location || '')
  const facilities = splitFacilities(apiHotel.facilities)
  const rating = toFiniteAmount(apiHotel.rating ?? staticHotel?.rating ?? 4) || 4

  return {
    ...(staticHotel || {}),
    id: apiHotel.id,
    name: apiHotel.name,
    region,
    coverImage: resolveHotelCoverImage(staticHotel?.coverImage || apiHotel.imageUrl),
    stars: Math.min(5, Math.max(3, Math.round(rating))),
    rating,
    city: region,
    address: apiHotel.location || staticHotel?.address || '',
    priceRange: apiHotel.priceRange,
    description: staticHotel?.description || t('hotel.apiHotelDescription', {
      name: apiHotel.name,
      location: apiHotel.location || t('common.unknownLocation'),
      phone: t('hotel.noPhone')
    }),
    tags: facilities.length ? facilities.slice(0, 4) : (staticHotel?.tags || []),
    amenities: facilities.length ? facilities : (staticHotel?.amenities || []),
    reviewCount: staticHotel?.reviewCount || 0,
    priceMin: apiHotel.priceRange ? parsePriceMin(apiHotel.priceRange, staticHotel?.priceMin || 300) : (staticHotel?.priceMin || 300),
    available: staticHotel?.available ?? true,
    lng: staticHotel?.lng || 91.0,
    lat: staticHotel?.lat || 29.6,
    rooms: staticHotel?.rooms || []
  }
}

const loadHotelDetail = async () => {
  const requestId = ++hotelDetailRequestId
  const staticHotel = getHotelById(hotelId)
  let matchingStaticHotel = staticHotel
  loadingHotel.value = true
  loadingRooms.value = true
  hotelLoadError.value = ''
  roomTypesLoadError.value = ''

  if (staticHotel) {
    rawHotel.value = staticHotel
  }

  try {
    const hotelRes = await api.get<unknown>(endpoints.hotels.detail(hotelId))
    if (requestId !== hotelDetailRequestId) return
    const apiHotel = normalizeApiHotel(hotelRes.data)
    if (apiHotel) {
      matchingStaticHotel = hotels.find(item => item.name === apiHotel.name) || staticHotel
      rawHotel.value = mapApiHotel(apiHotel, matchingStaticHotel)
    } else if (!staticHotel) {
      rawHotel.value = fallbackHotel()
      matchingStaticHotel = rawHotel.value || undefined
      hotelLoadError.value = t('toast.pageLoadFailed')
    }
  } catch {
    if (requestId !== hotelDetailRequestId) return
    hotelLoadError.value = t('toast.pageLoadFailed')
    if (!staticHotel) {
      rawHotel.value = fallbackHotel()
      matchingStaticHotel = rawHotel.value || undefined
    }
  } finally {
    if (requestId === hotelDetailRequestId) {
      loadingHotel.value = false
    }
  }

  try {
    const roomRes = await api.get<unknown>(endpoints.hotels.roomTypes(hotelId))
    if (requestId !== hotelDetailRequestId) return
    const apiRooms = toList(roomRes.data, normalizeApiRoom).map(toHotelDisplayRoom)
    if (apiRooms.length > 0) {
      rawRoomTypes.value = apiRooms
    } else if (matchingStaticHotel?.rooms) {
      rawRoomTypes.value = matchingStaticHotel.rooms.map(toHotelDisplayRoom)
    } else {
      rawRoomTypes.value = []
    }
  } catch {
    if (requestId !== hotelDetailRequestId) return
    roomTypesLoadError.value = t('toast.pageLoadFailed')
    if (matchingStaticHotel?.rooms) {
      rawRoomTypes.value = matchingStaticHotel.rooms.map(toHotelDisplayRoom)
    } else {
      rawRoomTypes.value = []
    }
  } finally {
    if (requestId === hotelDetailRequestId) {
      loadingRooms.value = false
    }
  }
}

onMounted(() => {
  void loadHotelDetail()
})
</script>
