<template>
  <div class="min-h-screen bg-apple-gray-50">
    <section class="relative h-[42vh] overflow-hidden bg-gray-200">
      <img :src="hotel.coverImage" :alt="hotel.name" class="w-full h-full object-cover" />
      <div class="absolute inset-0 bg-gradient-to-t from-black/65 via-black/10 to-transparent"></div>
      <div class="absolute bottom-0 left-0 right-0 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-10 text-white">
        <p class="text-sm mb-2 opacity-90">{{ t('hotel.detailBadge') }}</p>
        <h1 class="text-4xl md:text-5xl font-bold mb-3">{{ hotel.name }}</h1>
        <p class="text-white/85">{{ hotel.city }} · {{ hotel.address }} · {{ hotel.stars }}{{ t('hotel.starUnit') }}</p>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div class="lg:col-span-2 space-y-6">
          <div class="bg-white rounded-3xl p-7 border border-gray-100 shadow-sm">
            <h2 class="text-2xl font-bold text-gray-900 mb-4">{{ t('hotel.introduction') }}</h2>
            <p class="text-gray-600 leading-relaxed">{{ hotel.description }}</p>
          </div>

          <div class="bg-white rounded-3xl p-7 border border-gray-100 shadow-sm">
            <h2 class="text-2xl font-bold text-gray-900 mb-4">{{ t('hotel.roomSelection') }}</h2>
            <div class="space-y-4">
              <div v-for="room in hotel.rooms" :key="room.id" class="rounded-2xl border border-gray-100 p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-blue-200 transition-colors">
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">{{ room.name }}</h3>
                  <p class="text-sm text-gray-500 mt-1">{{ room.desc }}</p>
                </div>
                <div class="text-right">
                  <p class="text-xl font-bold text-blue-600">¥{{ room.price }} <span class="text-xs text-gray-400">{{ t('hotel.perNightCompact') }}</span></p>
                  <router-link :to="`/hotel-booking/${hotel.id}?roomId=${room.id}`" class="inline-block mt-2 px-4 py-2 rounded-full bg-blue-600 text-white text-sm hover:bg-blue-700 transition-colors">{{ t('hotel.bookThisRoom') }}</router-link>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div>
          <div class="sticky top-28 bg-white rounded-3xl p-7 border border-gray-100 shadow-lg">
            <p class="text-sm text-gray-500 mb-2">{{ t('hotel.startingPrice') }}</p>
            <p class="text-3xl font-bold text-blue-600 mb-4">¥{{ hotel.priceMin }} <span class="text-sm text-gray-400">{{ t('hotel.perNightCompact') }}</span></p>
            <div class="space-y-2 text-sm text-gray-600 mb-6">
              <p>⭐ {{ hotel.rating }}（{{ t('hotel.reviewCount', { count: hotel.reviewCount }) }}）</p>
              <p v-for="amenity in hotel.amenities" :key="amenity">✓ {{ amenity }}</p>
            </div>
            <router-link :to="`/hotel-booking/${hotel.id}?roomId=${hotel.rooms[0]?.id || 1}`" class="w-full inline-flex justify-center items-center px-5 py-3 rounded-full bg-blue-600 text-white font-medium hover:bg-blue-700 transition-colors">{{ t('hotel.bookNow') }}</router-link>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getHotelById, hotels } from '../api'

const { t } = useI18n()
const route = useRoute()
const hotelId = Number(route.params.id || 1)
const hotel = computed(() => getHotelById(hotelId) || hotels[0])
</script>
