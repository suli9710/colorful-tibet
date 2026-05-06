<template>
  <div class="min-h-screen tibet-bg-subtle">
    <!-- Hero -->
    <section class="relative h-[50vh] overflow-hidden">
      <img :src="hotel.coverImage" :alt="hotel.name" class="w-full h-full object-cover" />
      <div class="absolute inset-0 bg-gradient-to-b from-tibet-dark/30 via-tibet-dark/10 to-tibet-dark/80"></div>
      <!-- 底部经幡色带 -->
      <div class="absolute bottom-0 left-0 right-0 h-1 tibet-prayer-flag opacity-80"></div>
      <!-- Back -->
      <router-link to="/hotels" class="absolute top-6 left-6 p-2.5 rounded-full bg-tibet-white/15 backdrop-blur border border-tibet-gold/30 text-tibet-white hover:bg-tibet-white/25 transition-colors z-10">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
      </router-link>
      <div class="absolute bottom-0 left-0 right-0 p-8 max-w-7xl mx-auto">
        <div class="flex items-center gap-2 mb-3">
          <span class="flex items-center gap-0.5 text-tibet-yellow text-sm">
            <span v-for="n in hotel.stars" :key="n">★</span>
          </span>
          <span class="text-tibet-white/40 text-sm">·</span>
          <span class="text-tibet-white/70 text-sm">{{ t('hotel.detailBadge') }}</span>
        </div>
        <h1 class="text-3xl md:text-5xl font-bold text-tibet-white mb-2">{{ hotel.name }}</h1>
        <div class="flex items-center gap-3 text-tibet-white/60 text-sm">
          <span class="flex items-center gap-1"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg> {{ hotel.city }} · {{ hotel.address }}</span>
          <span>·</span>
          <span class="flex items-center gap-1">★ {{ hotel.rating }} ({{ hotel.reviewCount }}{{ t('hotel.reviewCountUnit') }})</span>
        </div>
      </div>
    </section>

    <!-- Content -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Main -->
        <div class="lg:col-span-2 space-y-8">
          <!-- Description — 藏式卡片 -->
          <div class="tibet-card rounded-2xl p-8">
            <h2 class="tibet-heading text-xl font-bold text-tibet-dark mb-5">{{ t('hotel.introduction') }}</h2>
            <p class="text-tibet-brown/80 leading-relaxed">{{ hotel.description }}</p>
            <div class="flex flex-wrap gap-2 mt-5">
              <span v-for="tag in hotel.tags" :key="tag" class="px-3 py-1.5 rounded-full bg-tibet-blue/10 text-tibet-blue text-sm font-medium">{{ tag }}</span>
            </div>
          </div>

          <!-- Amenities — 藏式方格 -->
          <div class="tibet-card rounded-2xl p-8">
            <h2 class="tibet-heading text-xl font-bold text-tibet-dark mb-5">{{ t('hotel.amenities') }}</h2>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
              <div v-for="amenity in hotel.amenities" :key="amenity" class="flex items-center gap-2 px-4 py-3 rounded-xl bg-tibet-white text-tibet-brown/70 text-sm border border-tibet-gold/10 hover:border-tibet-gold/30 transition-colors">
                <svg class="w-4 h-4 text-tibet-turquoise shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                {{ amenity }}
              </div>
            </div>
          </div>

          <!-- Rooms — 藏式房型卡片 -->
          <div class="tibet-card rounded-2xl p-8">
            <h2 class="tibet-heading text-xl font-bold text-tibet-dark mb-5">{{ t('hotel.roomSelection') }}</h2>
            <div class="space-y-4">
              <div v-for="room in hotel.rooms" :key="room.id"
                   class="group rounded-xl border border-tibet-gold/15 p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-tibet-gold/40 hover:shadow-lg hover:shadow-tibet-red/5 transition-all duration-300">
                <div class="flex-1">
                  <h3 class="text-lg font-semibold text-tibet-dark">{{ room.name }}</h3>
                  <p class="text-sm text-tibet-brown/50 mt-1">{{ room.desc }}</p>
                </div>
                <div class="flex items-center gap-5">
                  <div class="text-right">
                    <div class="flex items-baseline gap-0.5 justify-end">
                      <span class="text-xs text-tibet-brown/40">¥</span>
                      <span class="text-2xl font-bold text-tibet-red">{{ room.price }}</span>
                    </div>
                    <span class="text-xs text-tibet-brown/40">{{ t('hotel.perNight') }}</span>
                  </div>
                  <router-link :to="`/hotel-booking/${hotel.id}?roomId=${room.id}`" class="tibet-btn text-sm">{{ t('hotel.bookThisRoom') }}</router-link>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div>
          <div class="sticky top-28 space-y-6">
            <!-- Price Card — 藏式金边 -->
            <div class="tibet-card rounded-2xl p-8">
              <p class="text-sm text-tibet-brown/50 mb-1">{{ t('hotel.startingPrice') }}</p>
              <div class="flex items-baseline gap-1 mb-6">
                <span class="text-xs text-tibet-brown/40">¥</span>
                <span class="text-4xl font-bold text-tibet-red">{{ hotel.priceMin }}</span>
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
              <router-link :to="`/hotel-booking/${hotel.id}?roomId=${hotel.rooms[0]?.id || 1}`"
                           class="w-full flex items-center justify-center gap-2 px-5 py-3.5 rounded-full bg-tibet-red text-tibet-yellow font-semibold hover:bg-tibet-red/90 transition-colors shadow-lg shadow-tibet-red/20">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                {{ t('hotel.bookNow') }}
              </router-link>
              <a :href="`https://uri.amap.com/navigation?to=${hotel.lng},${hotel.lat},${encodeURIComponent(hotel.name)}&mode=car&utm_source=colorful-tibet`"
                 target="_blank" rel="noopener"
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
        </div>
      </div>
    </section>

    <!-- 底部经幡装饰 -->
    <div class="h-2 tibet-prayer-flag opacity-60"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getHotelById, hotels } from '../data/hotels'

const { t } = useI18n()
const route = useRoute()
const hotelId = Number(route.params.id || 1)
const hotel = computed(() => getHotelById(hotelId) || hotels[0])
</script>
