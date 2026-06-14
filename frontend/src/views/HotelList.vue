<template>
  <div class="min-h-screen tibet-page-shell" :aria-busy="loadingHotels">
    <!-- Hero — 藏式深色渐变 -->
    <section class="relative overflow-hidden bg-gradient-to-br from-tibet-dark via-tibet-red to-tibet-brown text-tibet-white">
      <!-- 装饰暗纹 -->
      <div class="absolute inset-0 opacity-35 tibet-hero-pattern"></div>
      <!-- 经幡色彩带 -->
      <div class="absolute bottom-0 left-0 right-0 h-1 tibet-prayer-flag opacity-80 z-20"></div>
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-24 relative z-10">
        <div class="max-w-2xl">
          <!-- 法轮装饰徽章 -->
          <span class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-tibet-white/10 backdrop-blur border border-tibet-gold/30 text-tibet-yellow text-sm mb-8">
            <svg class="w-4 h-4" viewBox="0 0 100 100"><circle cx="50" cy="50" r="40" stroke="currentColor" stroke-width="10" fill="none"/><circle cx="50" cy="50" r="12" fill="currentColor"/><line x1="50" y1="10" x2="50" y2="38" stroke="currentColor" stroke-width="6"/><line x1="50" y1="62" x2="50" y2="90" stroke="currentColor" stroke-width="6"/><line x1="10" y1="50" x2="38" y2="50" stroke="currentColor" stroke-width="6"/><line x1="62" y1="50" x2="90" y2="50" stroke="currentColor" stroke-width="6"/></svg>
            {{ t('hotel.badge') }}
          </span>
          <h1 class="mb-5 text-3xl font-bold leading-tight sm:mb-6 sm:text-4xl md:text-6xl tibetan-font">{{ t('hotel.listTitle') }}</h1>
          <p class="max-w-xl text-base leading-relaxed text-tibet-white/70 sm:text-lg">{{ t('hotel.listSubtitle') }}</p>
        </div>
      </div>
      <div class="absolute bottom-0 left-0 right-0 h-12 bg-gradient-to-t from-tibet-white/20 to-transparent"></div>
    </section>

    <!-- Filter Bar — 藏式金边 -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 -mt-8 relative z-20">
      <div class="tibet-panel flex flex-col gap-3 rounded-2xl p-4 sm:flex-row sm:p-5">
        <div class="flex-1 relative">
          <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-tibet-gold/60" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
          <label for="hotel-list-search" class="sr-only">{{ t('hotel.searchPlaceholder') }}</label>
          <input id="hotel-list-search" v-model.trim="keyword" type="text" :placeholder="t('hotel.searchPlaceholder')" class="w-full pl-12 pr-4 py-3 bg-tibet-white rounded-xl border-none outline-none focus:ring-2 focus:ring-tibet-red/20 transition-all text-tibet-dark placeholder-tibet-brown/30" />
        </div>
        <label for="hotel-list-city" class="sr-only">{{ t('hotel.allCities') }}</label>
        <select id="hotel-list-city" v-model="city" class="px-4 py-3 bg-tibet-white rounded-xl border-none outline-none focus:ring-2 focus:ring-tibet-red/20 transition-all text-tibet-dark cursor-pointer">
          <option value="">{{ t('hotel.allCities') }}</option>
          <option value="拉萨">{{ t('hotel.city.lhasa') }}</option>
          <option value="林芝">{{ t('hotel.city.nyingchi') }}</option>
          <option value="日喀则">{{ t('hotel.city.shigatse') }}</option>
          <option value="阿里">{{ t('hotel.city.ngari') }}</option>
          <option value="那曲">{{ t('hotel.city.naqu') }}</option>
        </select>
        <label for="hotel-list-stars" class="sr-only">{{ t('hotel.allStars') }}</label>
        <select id="hotel-list-stars" v-model="star" class="px-4 py-3 bg-tibet-white rounded-xl border-none outline-none focus:ring-2 focus:ring-tibet-red/20 transition-all text-tibet-dark cursor-pointer">
          <option value="">{{ t('hotel.allStars') }}</option>
          <option value="5">⭐ 5{{ t('hotel.starUnit') }}</option>
          <option value="4">⭐ 4{{ t('hotel.starUnit') }}</option>
          <option value="3">⭐ 3{{ t('hotel.starUnit') }}</option>
        </select>
      </div>
    </section>

    <!-- Hotel List -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-12">
      <div
        v-if="loadingHotels"
        class="mb-6 rounded-2xl bg-tibet-gold/10 px-4 py-3 text-center text-sm font-medium text-tibet-brown/70"
        role="status"
        aria-live="polite"
        aria-busy="true"
      >
        {{ t('common.loading') }}
      </div>
      <div
        v-if="hotelsLoadError"
        class="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800"
        role="alert"
        aria-live="assertive"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <span>{{ hotelsLoadError }}</span>
          <button
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-tibet-red px-4 py-2 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loadingHotels"
            @click="loadHotels"
          >
            {{ loadingHotels ? t('common.loading') : t('common.retry') }}
          </button>
        </div>
      </div>
      <div class="space-y-12 sm:space-y-16">
        <div v-for="group in hotelsByRegionVisible" :key="group.region">
          <!-- Region Header — 藏式标题 -->
          <motion.div
            class="mb-6 flex items-center gap-3 sm:mb-8 sm:gap-6"
            :initial="revealInitial"
            :whileInView="revealInView"
            :inViewOptions="inViewOnce"
            :transition="revealTransition"
          >
            <div class="flex-1 h-px bg-gradient-to-r from-transparent via-tibet-gold/50 to-transparent"></div>
            <div class="text-center shrink-0">
              <h2 class="tibet-heading text-xl font-bold text-tibet-dark sm:text-2xl md:text-3xl tibetan-font">
                {{ t('hotel.regionalHotels', { region: group.regionLabel }) }}
              </h2>
              <p class="text-sm text-tibet-brown/50 mt-1">{{ t('hotel.hotelsCount', { count: group.hotels.length }) }}</p>
            </div>
            <div class="flex-1 h-px bg-gradient-to-r from-transparent via-tibet-gold/50 to-transparent"></div>
          </motion.div>

          <!-- Hotel Cards -->
          <div class="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-4 xl:gap-6" role="list">
            <AnimatePresence mode="popLayout">
            <motion.article
                     v-for="(hotel, index) in group.hotels"
                     :key="hotel.id"
                     layout
                     class="tibet-card group rounded-2xl overflow-hidden"
                     :initial="cardInitial"
                     :whileInView="cardInView"
                     :exit="cardExit"
                     :inViewOptions="inViewOnce"
                     :transition="cardTransition(index)"
                     :whileHover="{ y: -5, scale: 1.012 }"
                     :whilePress="{ scale: 0.996 }"
                     role="listitem">
              <!-- Image -->
              <div class="relative h-48 overflow-hidden bg-tibet-brown/10 sm:h-52">
                <img
                  :src="resolveHotelCoverImage(hotel.coverImage)"
                  :alt="hotel.name"
                  class="w-full h-full object-cover tibet-image-hover will-change-transform"
                  loading="lazy"
                  @error="applyHotelImageFallback"
                />
                <div class="absolute inset-0 bg-gradient-to-t from-tibet-dark/50 via-transparent to-transparent"></div>
                <!-- Star Badge — 藏金星星 -->
                <div class="absolute top-3 left-3 flex items-center gap-0.5 px-2.5 py-1 rounded-full bg-tibet-white/90 backdrop-blur text-tibet-gold text-xs font-bold shadow">
                  <span v-for="n in hotel.stars" :key="n">★</span>
                </div>
                <!-- Availability -->
                <span class="absolute top-3 right-3 px-2.5 py-1 rounded-full text-xs font-medium"
                      :class="hotel.available ? 'bg-tibet-turquoise text-white' : 'bg-tibet-red/80 text-white'">
                  {{ hotel.available ? t('hotel.available') : t('hotel.full') }}
                </span>
              </div>

              <!-- Content -->
              <div class="space-y-3 bg-white p-4 sm:p-5">
                <div>
                  <h3 class="text-lg font-bold text-tibet-dark group-hover:text-tibet-red transition-colors line-clamp-1">{{ hotel.name }}</h3>
                  <p class="text-xs text-tibet-brown/50 mt-0.5 flex items-center gap-1">
                    <svg class="w-3.5 h-3.5 shrink-0 text-tibet-gold/70" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
                    {{ hotel.city }} · {{ hotel.address }}
                  </p>
                </div>

                <!-- Rating -->
                <div class="flex items-center gap-2 text-xs">
                  <span class="flex items-center gap-0.5 px-2 py-0.5 rounded-md bg-tibet-gold/10 text-tibet-gold font-semibold">★ {{ hotel.rating }}</span>
                  <span class="text-tibet-brown/40">{{ hotel.reviewCount }}{{ t('hotel.reviewCountUnit') }}</span>
                </div>

                <p class="text-sm text-tibet-brown/70 line-clamp-2 leading-relaxed">{{ hotel.description }}</p>

                <!-- Tags -->
                <div class="flex flex-wrap gap-1.5">
                  <span v-for="tag in hotel.tags" :key="tag" class="px-2 py-0.5 rounded-full bg-tibet-blue/10 text-tibet-blue text-xs font-medium">{{ tag }}</span>
                </div>

                <!-- Price & Actions -->
                <div class="flex flex-wrap items-end justify-between gap-3 border-t border-tibet-gold/20 pt-3">
                  <div>
                    <div class="flex items-baseline gap-0.5">
                      <span class="text-xs text-tibet-brown/40">¥</span>
                      <span class="text-xl font-bold text-tibet-red">{{ hotel.priceMin }}</span>
                    </div>
                    <span class="text-xs text-tibet-brown/40">{{ t('hotel.perNight') }}</span>
                  </div>
                  <div class="flex gap-1.5">
                    <a :href="`https://uri.amap.com/navigation?to=${hotel.lng},${hotel.lat},${encodeURIComponent(hotel.name)}&mode=car&utm_source=colorful-tibet`"
                       target="_blank" rel="noopener noreferrer"
                       class="p-2 rounded-lg bg-tibet-blue/10 text-tibet-blue hover:bg-tibet-blue hover:text-white transition-colors"
                       :title="t('hotel.navigate')"
                       :aria-label="`${t('hotel.navigate')} ${hotel.name}`">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                    </a>
                    <router-link :to="`/hotels/${hotel.id}`" class="tibet-btn text-sm" :aria-label="`${t('hotel.viewDetails')} ${hotel.name}`">{{ t('hotel.viewDetails') }}</router-link>
                  </div>
                </div>
              </div>
            </motion.article>
            </AnimatePresence>
          </div>
        </div>
      </div>

      <!-- Empty State — 藏式风格 -->
      <div v-if="hotelsByRegionVisible.length === 0" class="text-center py-24">
        <div class="inline-flex items-center justify-center w-24 h-24 rounded-full bg-tibet-white border border-tibet-gold/30 mb-6">
          <svg class="w-12 h-12 text-tibet-gold/30" viewBox="0 0 100 100"><circle cx="50" cy="50" r="40" stroke="currentColor" stroke-width="6" fill="none"/><circle cx="50" cy="50" r="8" fill="currentColor"/></svg>
        </div>
        <p class="text-tibet-brown/50 text-lg">{{ t('hotel.noResults') }}</p>
      </div>
    </section>

    <!-- 底部经幡装饰条 -->
    <div class="h-2 tibet-prayer-flag opacity-60"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useI18n } from 'vue-i18n'
import { hotels, hotelsByRegion, hotelRegions, type HotelItem } from '../data/hotels'
import { applyHotelImageFallback, resolveHotelCoverImage } from '../data/hotelImages'
import { getCanonicalRegion, localizeHotel, localizeRegion } from '../data/hotelTranslations'
import api, { endpoints } from '../api'
import { toFiniteAmount } from '../i18n/formatting'
import {
  cardExit,
  cardInitial,
  cardInView,
  cardTransition,
  inViewOnce,
  revealInitial,
  revealInView,
  revealTransition
} from '../motion/presets'

const { t, locale } = useI18n()
const keyword = ref('')
const city = ref('')
const star = ref('')
const loadingHotels = ref(false)
const hotelsLoadError = ref('')
let hotelListRequestId = 0

// --- Merge API hotels into static data ---

interface ApiHotelResponse {
  id: number
  name: string
  location?: string
  priceRange?: string
  rating?: number | string | null
  imageUrl?: string
  facilities?: string
  phone?: string
}

interface HotelDisplayItem extends HotelItem {
  isApiHotel?: boolean
  phone?: string
  priceRange?: string
}

const resolveRegion = (location: string): string => {
  return getCanonicalRegion(location)
}

const splitFacilities = (facilities?: string): string[] =>
  facilities
    ? facilities.split(',').map((f: string) => f.trim()).filter(Boolean)
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

const readList = (value: unknown): unknown[] => {
  if (Array.isArray(value)) return value
  if (isRecord(value) && Array.isArray(value.content)) return value.content
  return []
}

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
    facilities: readText(value, 'facilities'),
    phone: readText(value, 'phone')
  }
}

const normalizeApiHotels = (value: unknown) =>
  readList(value).map(normalizeApiHotel).filter((hotel): hotel is ApiHotelResponse => hotel !== null)

const mapApiHotel = (apiHotel: ApiHotelResponse, staticHotel?: HotelItem): HotelDisplayItem => {
  const region = resolveRegion(apiHotel.location || '')
  const amenities = splitFacilities(apiHotel.facilities)
  const rating = toFiniteAmount(apiHotel.rating ?? staticHotel?.rating ?? 4) || 4

  return {
    ...(staticHotel || {}),
    id: apiHotel.id,
    region,
    name: apiHotel.name,
    city: region,
    address: apiHotel.location || staticHotel?.address || '西藏',
    lng: staticHotel?.lng || 91.0,
    lat: staticHotel?.lat || 29.6,
    stars: Math.min(5, Math.max(3, Math.round(rating))),
    rating,
    reviewCount: staticHotel?.reviewCount || 0,
    priceMin: apiHotel.priceRange ? parsePriceMin(apiHotel.priceRange, staticHotel?.priceMin || 300) : (staticHotel?.priceMin || 300),
    available: staticHotel?.available ?? true,
    tags: amenities.length ? amenities.slice(0, 4) : (staticHotel?.tags || ['可预订']),
    coverImage: resolveHotelCoverImage(staticHotel?.coverImage || apiHotel.imageUrl),
    description: staticHotel?.description || '',
    amenities: amenities.length ? amenities : (staticHotel?.amenities || []),
    rooms: staticHotel?.rooms || [],
    isApiHotel: true,
    phone: apiHotel.phone || '',
    priceRange: apiHotel.priceRange
  }
}

// Reactive deep copy of static data that we merge API hotels into
const cloneHotel = (hotel: HotelItem): HotelDisplayItem => ({
  ...hotel,
  tags: [...hotel.tags],
  amenities: [...hotel.amenities],
  rooms: hotel.rooms.map(room => ({ ...room }))
})

const cloneHotelsByRegion = () =>
  hotelRegions.reduce<Record<string, HotelDisplayItem[]>>((acc, region) => {
    acc[region] = (hotelsByRegion[region] || []).map(cloneHotel)
    return acc
  }, {})

const mergedByRegion = ref<Record<string, HotelDisplayItem[]>>(cloneHotelsByRegion())

const staticHotelsByName = new Map(hotels.map(hotel => [hotel.name, hotel]))

const mergeApiHotels = (apiHotels: ApiHotelResponse[]) => {
  const nextByRegion = cloneHotelsByRegion()

  for (const apiHotel of apiHotels) {
    const staticHotel = staticHotelsByName.get(apiHotel.name)
    const mapped = mapApiHotel(apiHotel, staticHotel)
    const region = mapped.region

    if (staticHotel) {
      Object.keys(nextByRegion).forEach(key => {
        nextByRegion[key] = nextByRegion[key].filter(hotel => hotel.name !== apiHotel.name)
      })
    }

    const regionHotels = nextByRegion[region] || []
    const existingIndex = regionHotels.findIndex(hotel => hotel.id === mapped.id || hotel.name === mapped.name)
    if (existingIndex >= 0) {
      regionHotels[existingIndex] = mapped
    } else {
      regionHotels.push(mapped)
    }
    nextByRegion[region] = regionHotels
  }

  return nextByRegion
}

const loadHotels = async () => {
  const requestId = ++hotelListRequestId
  loadingHotels.value = true
  hotelsLoadError.value = ''

  try {
    const res = await api.get<unknown>(endpoints.hotels.list)
    if (requestId !== hotelListRequestId) return

    const apiHotels = normalizeApiHotels(res.data)
    if (apiHotels.length > 0) {
      mergedByRegion.value = mergeApiHotels(apiHotels)
    }
  } catch {
    if (requestId !== hotelListRequestId) return
    hotelsLoadError.value = t('toast.pageLoadFailed')
  } finally {
    if (requestId === hotelListRequestId) {
      loadingHotels.value = false
    }
  }
}

onMounted(() => {
  void loadHotels()
})

// --- Filtering ---

const toDisplayHotel = (hotel: HotelDisplayItem): HotelDisplayItem => {
  const localized = localizeHotel(hotel, locale.value)
  if (hotel.isApiHotel) {
    return {
      ...localized,
      region: localizeRegion(hotel.region, locale.value),
      city: localizeRegion(hotel.city, locale.value),
      tags: localized.tags.length ? localized.tags : [t('hotel.bookable')],
      description: localized.description || t('hotel.apiHotelDescription', {
        name: hotel.name,
        location: hotel.address || t('common.unknownLocation'),
        phone: hotel.phone || t('hotel.noPhone')
      })
    }
  }
  return localized
}

const hotelsByRegionVisible = computed(() =>
  hotelRegions
    .map(region => {
      const hotels = (mergedByRegion.value[region] || [])
        .filter((h: HotelDisplayItem) => {
          const displayHotel = toDisplayHotel(h)
          const searchable = [
            displayHotel.name,
            displayHotel.city,
            displayHotel.address,
            displayHotel.description,
            ...(displayHotel.tags || [])
          ].join(' ')
          return (!keyword.value || searchable.includes(keyword.value)) &&
            (!city.value || h.city === city.value) &&
            (!star.value || h.stars === Number(star.value))
        })
        .map(toDisplayHotel)

      return {
        region,
        regionLabel: localizeRegion(region, locale.value),
        hotels
      }
    })
    .filter(group => group.hotels.length > 0)
)

</script>
