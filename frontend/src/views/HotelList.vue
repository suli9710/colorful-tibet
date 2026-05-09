<template>
  <div class="min-h-screen tibet-bg-subtle">
    <!-- Hero — 藏式深色渐变 -->
    <section class="relative overflow-hidden bg-gradient-to-br from-tibet-dark via-tibet-red to-tibet-brown text-tibet-white">
      <!-- 装饰暗纹 -->
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-10 left-10 w-80 h-80 bg-tibet-gold rounded-full blur-3xl"></div>
        <div class="absolute bottom-10 right-10 w-96 h-96 bg-tibet-turquoise rounded-full blur-3xl"></div>
        <div class="absolute top-1/2 left-1/3 w-64 h-64 bg-tibet-yellow rounded-full blur-3xl"></div>
      </div>
      <!-- 经幡色彩带 -->
      <div class="absolute bottom-0 left-0 right-0 h-1 tibet-prayer-flag opacity-80 z-20"></div>
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 relative z-10">
        <div class="max-w-2xl">
          <!-- 法轮装饰徽章 -->
          <span class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-tibet-white/10 backdrop-blur border border-tibet-gold/30 text-tibet-yellow text-sm mb-8">
            <svg class="w-4 h-4" viewBox="0 0 100 100"><circle cx="50" cy="50" r="40" stroke="currentColor" stroke-width="10" fill="none"/><circle cx="50" cy="50" r="12" fill="currentColor"/><line x1="50" y1="10" x2="50" y2="38" stroke="currentColor" stroke-width="6"/><line x1="50" y1="62" x2="50" y2="90" stroke="currentColor" stroke-width="6"/><line x1="10" y1="50" x2="38" y2="50" stroke="currentColor" stroke-width="6"/><line x1="62" y1="50" x2="90" y2="50" stroke="currentColor" stroke-width="6"/></svg>
            {{ t('hotel.badge') }}
          </span>
          <h1 class="text-4xl md:text-6xl font-bold tracking-tight mb-6 leading-tight tibetan-font">{{ t('hotel.listTitle') }}</h1>
          <p class="text-lg text-tibet-white/70 leading-relaxed max-w-xl">{{ t('hotel.listSubtitle') }}</p>
        </div>
      </div>
      <div class="absolute bottom-0 left-0 right-0 h-12 bg-gradient-to-t from-tibet-white/20 to-transparent"></div>
    </section>

    <!-- Filter Bar — 藏式金边 -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 -mt-8 relative z-20">
      <div class="bg-white rounded-2xl shadow-xl shadow-tibet-red/5 border border-tibet-gold/30 p-5 flex flex-col sm:flex-row gap-3">
        <div class="flex-1 relative">
          <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-tibet-gold/60" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
          <input v-model="keyword" type="text" :placeholder="t('hotel.searchPlaceholder')" class="w-full pl-12 pr-4 py-3 bg-tibet-white rounded-xl border-none outline-none focus:ring-2 focus:ring-tibet-red/20 transition-all text-tibet-dark placeholder-tibet-brown/30" />
        </div>
        <select v-model="city" class="px-4 py-3 bg-tibet-white rounded-xl border-none outline-none focus:ring-2 focus:ring-tibet-red/20 transition-all text-tibet-dark cursor-pointer">
          <option value="">{{ t('hotel.allCities') }}</option>
          <option value="拉萨">{{ t('hotel.city.lhasa') }}</option>
          <option value="林芝">{{ t('hotel.city.nyingchi') }}</option>
          <option value="日喀则">{{ t('hotel.city.shigatse') }}</option>
          <option value="阿里">{{ t('hotel.city.ngari') }}</option>
          <option value="那曲">{{ t('hotel.city.naqu') }}</option>
        </select>
        <select v-model="star" class="px-4 py-3 bg-tibet-white rounded-xl border-none outline-none focus:ring-2 focus:ring-tibet-red/20 transition-all text-tibet-dark cursor-pointer">
          <option value="">{{ t('hotel.allStars') }}</option>
          <option value="5">⭐ 5{{ t('hotel.starUnit') }}</option>
          <option value="4">⭐ 4{{ t('hotel.starUnit') }}</option>
          <option value="3">⭐ 3{{ t('hotel.starUnit') }}</option>
        </select>
      </div>
    </section>

    <!-- Hotel List -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div class="space-y-16">
        <div v-for="group in hotelsByRegionVisible" :key="group.region">
          <!-- Region Header — 藏式标题 -->
          <div class="flex items-center gap-6 mb-8 animate-on-scroll">
            <div class="flex-1 h-px bg-gradient-to-r from-transparent via-tibet-gold/50 to-transparent"></div>
            <div class="text-center shrink-0">
              <h2 class="tibet-heading text-2xl md:text-3xl font-bold text-tibet-dark tibetan-font">
                {{ t('hotel.regionalHotels', { region: group.region }) }}
              </h2>
              <p class="text-sm text-tibet-brown/50 mt-1">{{ t('hotel.hotelsCount', { count: group.hotels.length }) }}</p>
            </div>
            <div class="flex-1 h-px bg-gradient-to-r from-transparent via-tibet-gold/50 to-transparent"></div>
          </div>

          <!-- Hotel Cards -->
          <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
            <article v-for="(hotel, index) in group.hotels" :key="hotel.id"
                     class="tibet-card group rounded-2xl overflow-hidden hover:shadow-2xl hover:shadow-tibet-red/8 hover:-translate-y-1.5 transition-all duration-500 ease-out-expo animate-on-scroll"
                     :style="{ animationDelay: `${index * 80}ms` }">
              <!-- Image -->
              <div class="relative h-52 overflow-hidden bg-tibet-brown/10">
                <img :src="hotel.coverImage" :alt="hotel.name" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out-expo" loading="lazy" />
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
              <div class="p-5 space-y-3 bg-white">
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
                <div class="flex items-end justify-between pt-3 border-t border-tibet-gold/20">
                  <div>
                    <div class="flex items-baseline gap-0.5">
                      <span class="text-xs text-tibet-brown/40">¥</span>
                      <span class="text-xl font-bold text-tibet-red">{{ hotel.priceMin }}</span>
                    </div>
                    <span class="text-xs text-tibet-brown/40">{{ t('hotel.perNight') }}</span>
                  </div>
                  <div class="flex gap-1.5">
                    <a :href="`https://uri.amap.com/navigation?to=${hotel.lng},${hotel.lat},${encodeURIComponent(hotel.name)}&mode=car&utm_source=colorful-tibet`"
                       target="_blank" rel="noopener"
                       class="p-2 rounded-lg bg-tibet-blue/10 text-tibet-blue hover:bg-tibet-blue hover:text-white transition-colors"
                       :title="t('hotel.navigate')">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                    </a>
                    <router-link :to="`/hotels/${hotel.id}`" class="tibet-btn text-sm">查看详情</router-link>
                  </div>
                </div>
              </div>
            </article>
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
import { useI18n } from 'vue-i18n'
import { hotelsByRegion, hotelRegions, type HotelItem } from '../data/hotels'
import api, { endpoints } from '../api'

const { t } = useI18n()
const keyword = ref('')
const city = ref('')
const star = ref('')

// --- Merge API hotels into static data ---

const resolveRegion = (location: string): string => {
  for (const region of hotelRegions) {
    if (location.includes(region)) return region
  }
  if (location.includes('那曲')) return '那曲'
  return '拉萨'
}

const mapApiHotel = (apiHotel: any, idOffset: number): HotelItem => {
  const region = resolveRegion(apiHotel.location || '')
  const amenities = apiHotel.facilities
    ? apiHotel.facilities.split(',').map((f: string) => f.trim()).filter(Boolean)
    : []
  let priceMin = 300
  if (apiHotel.priceRange) {
    const m = (apiHotel.priceRange as string).match(/(\d+)/)
    if (m) priceMin = parseInt(m[1]) || 300
  }

  return {
    id: apiHotel.id + idOffset,
    region,
    name: apiHotel.name,
    city: region,
    address: apiHotel.location || '西藏',
    lng: 91.0,
    lat: 29.6,
    stars: Math.min(5, Math.max(3, Math.round(Number(apiHotel.rating) || 4))),
    rating: Number(apiHotel.rating) || 4.0,
    reviewCount: 0,
    priceMin,
    available: true,
    tags: ['可预订'],
    coverImage: apiHotel.imageUrl || '',
    description: `${apiHotel.name}位于${apiHotel.location || '西藏'}，电话：${apiHotel.phone || '暂无'}`,
    amenities,
    rooms: [],
  }
}

// Reactive deep copy of static data that we merge API hotels into
const mergedByRegion = ref<Record<string, HotelItem[]>>(
  JSON.parse(JSON.stringify(hotelsByRegion))
)

const staticNames = new Set(
  Object.values(hotelsByRegion).flat().map(h => h.name)
)

onMounted(async () => {
  try {
    const res = await api.get(endpoints.hotels.list)
    if (res.data && res.data.length > 0) {
      const offset = 10000 // push API ids far above static range
      for (const apiHotel of res.data) {
        if (staticNames.has(apiHotel.name)) continue
        const mapped = mapApiHotel(apiHotel, offset)
        const region = mapped.region
        if (!mergedByRegion.value[region]) {
          mergedByRegion.value[region] = []
        }
        mergedByRegion.value[region].push(mapped)
      }
    }
  } catch (_) { /* fallback to static data */ }
  setTimeout(initScrollAnimations, 100)
})

// --- Filtering ---

const hotelsByRegionVisible = computed(() =>
  hotelRegions
    .map(region => ({
      region,
      hotels: (mergedByRegion.value[region] || []).filter((h: HotelItem) =>
        (!keyword.value || h.name.includes(keyword.value) || h.city.includes(keyword.value) || h.tags.some(tag => tag.includes(keyword.value))) &&
        (!city.value || h.city === city.value) &&
        (!star.value || h.stars === Number(star.value))
      )
    }))
    .filter(group => group.hotels.length > 0)
)

// --- Scroll animations ---

const initScrollAnimations = () => {
  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' })

  document.querySelectorAll('.animate-on-scroll:not(.revealed)').forEach(el => observer.observe(el))
}
</script>
