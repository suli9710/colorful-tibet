<template>
  <div class="min-h-screen bg-apple-gray-50">
    <section class="relative overflow-hidden bg-gradient-to-br from-blue-600 via-indigo-600 to-purple-700 text-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 relative z-10">
        <div class="max-w-3xl">
          <span class="inline-flex items-center px-4 py-1.5 rounded-full bg-white/15 backdrop-blur-md border border-white/20 text-sm font-medium mb-6">{{ t('hotel.badge') }}</span>
          <h1 class="text-4xl md:text-6xl font-bold tracking-tight mb-4">{{ t('hotel.listTitle') }}</h1>
          <p class="text-lg md:text-xl text-white/80 leading-relaxed">{{ t('hotel.listSubtitle') }}</p>
        </div>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 -mt-10 relative z-10">
      <div class="bg-white/90 backdrop-blur-xl border border-white/50 rounded-3xl shadow-xl p-6 mb-8">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <input v-model="keyword" type="text" :placeholder="t('hotel.searchPlaceholder')" class="md:col-span-2 px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all" />
          <select v-model="city" class="px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all">
            <option value="">{{ t('hotel.allCities') }}</option><option value="拉萨">{{ t('hotel.city.lhasa') }}</option><option value="林芝">{{ t('hotel.city.nyingchi') }}</option><option value="日喀则">{{ t('hotel.city.shigatse') }}</option><option value="阿里">{{ t('hotel.city.ngari') }}</option>
          </select>
          <select v-model="star" class="px-4 py-3 rounded-2xl border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all">
            <option value="">{{ t('hotel.allStars') }}</option><option value="5">5{{ t('hotel.starUnit') }}</option><option value="4">4{{ t('hotel.starUnit') }}</option><option value="3">3{{ t('hotel.starUnit') }}</option>
          </select>
        </div>
      </div>

      <div class="space-y-12">
        <section v-for="group in hotelsByRegionVisible" :key="group.region" class="space-y-5">
          <div class="flex items-end justify-between gap-4">
            <div>
              <h2 class="text-2xl md:text-3xl font-bold text-gray-900">{{ t('hotel.regionalHotels', { region: group.region }) }}</h2>
              <p class="text-sm text-gray-500 mt-1">{{ t('hotel.hotelsCount', { count: group.hotels.length }) }}</p>
            </div>
            <span class="text-sm text-blue-600 font-medium">{{ t('hotel.minHotelsHint') }}</span>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
            <article v-for="(hotel, index) in group.hotels" :key="hotel.id" class="group bg-white rounded-3xl overflow-hidden shadow-lg border border-gray-100 hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 animate-on-scroll"
                     :style="{ animationDelay: `${index * 80}ms` }">
              <div class="h-56 overflow-hidden bg-gray-100"><img :src="hotel.coverImage" :alt="hotel.name" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" /></div>
              <div class="p-6 space-y-4">
                <div class="flex items-start justify-between gap-4">
                  <div><h3 class="text-2xl font-bold text-gray-900">{{ hotel.name }}</h3><p class="text-gray-500 mt-1">{{ hotel.city }} · {{ hotel.address }}</p></div>
                  <div class="text-right shrink-0"><div class="text-2xl font-bold text-blue-600">¥{{ hotel.priceMin }}</div><div class="text-xs text-gray-400">{{ t('hotel.perNight') }}</div></div>
                </div>
                <div class="flex flex-wrap items-center gap-2 text-sm text-gray-600">
                  <span class="px-3 py-1 rounded-full bg-amber-50 text-amber-700">{{ hotel.stars }}{{ t('hotel.starUnit') }}</span>
                  <span class="px-3 py-1 rounded-full bg-blue-50 text-blue-700">⭐ {{ hotel.rating }}</span>
                  <span class="px-3 py-1 rounded-full bg-gray-100">{{ hotel.reviewCount }}{{ t('hotel.reviewCountUnit') }}</span>
                  <span class="px-3 py-1 rounded-full" :class="hotel.available ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'">{{ hotel.available ? t('hotel.available') : t('hotel.full') }}</span>
                </div>
                <p class="text-gray-600 line-clamp-2">{{ hotel.description }}</p>
                <div class="flex flex-wrap gap-2"><span v-for="tag in hotel.tags" :key="tag" class="px-3 py-1 rounded-full bg-gray-50 text-gray-600 text-sm">#{{ tag }}</span></div>
                <div class="flex items-center justify-between pt-2">
                  <router-link :to="`/hotels/${hotel.id}`" class="inline-flex items-center px-5 py-2.5 rounded-full bg-blue-600 text-white font-medium hover:bg-blue-700 transition-colors">{{ t('hotel.viewDetails') }}</router-link>
                  <span class="text-xs text-gray-400">{{ hotel.amenities.join(' · ') }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>
      <div v-if="hotelsByRegionVisible.length === 0" class="text-center py-16 text-gray-500">{{ t('hotel.noResults') }}</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { hotelsByRegion, hotelRegions, type HotelItem } from '../data/hotels'

const { t } = useI18n()
const keyword = ref('')
const city = ref('')
const star = ref('')
const matchHotel = (hotel: HotelItem) => (!keyword.value || hotel.name.includes(keyword.value) || hotel.city.includes(keyword.value) || hotel.tags.some((tag) => tag.includes(keyword.value))) && (!city.value || hotel.city === city.value) && (!star.value || hotel.stars === Number(star.value))
const hotelsByRegionVisible = computed(() => hotelRegions.map((region) => ({ region, hotels: hotelsByRegion[region].filter(matchHotel) })).filter((group) => group.hotels.length > 0))

onMounted(() => {
  setTimeout(initScrollAnimations, 100)
})

const initScrollAnimations = () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' })

  document.querySelectorAll('.animate-on-scroll:not(.revealed)').forEach(el => {
    observer.observe(el)
  })
}
</script>
