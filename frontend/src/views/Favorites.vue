<template>
  <div class="min-h-screen bg-stone-50 py-12 pt-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h1 class="text-4xl font-bold text-stone-800 mb-4">{{ t('favorites.title') }}</h1>
        <p class="text-lg text-stone-600">{{ t('favorites.subtitle') }}</p>
      </div>

      <div v-if="loading" class="flex justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>

      <div v-else-if="favorites.length === 0" class="text-center py-16">
        <p class="text-stone-500 text-lg">{{ t('favorites.empty') }}</p>
        <router-link to="/route-planner" class="mt-4 inline-block px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
          {{ t('favorites.goExplore') }}
        </router-link>
      </div>

      <div v-else class="space-y-4">
        <div v-for="fav in favorites" :key="fav.id" class="bg-white rounded-lg shadow p-6 hover:shadow-md transition-shadow">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <h3 class="text-lg font-bold text-stone-800">{{ fav.route?.name || t('favorites.unknownRoute') }}</h3>
              <p class="text-sm text-stone-500 mt-1">{{ fav.route?.description?.substring(0, 100) }}...</p>
              <div class="flex items-center gap-4 mt-3 text-sm text-stone-600">
                <span>{{ t('admin.daysValue', { count: fav.route?.days || 0 }) }}</span>
                <span class="text-red-600 font-semibold">¥{{ fav.route?.price }}</span>
                <span :class="{
                  'bg-green-100 text-green-800': fav.route?.difficulty === 'EASY',
                  'bg-yellow-100 text-yellow-800': fav.route?.difficulty === 'MEDIUM',
                  'bg-red-100 text-red-800': fav.route?.difficulty === 'HARD'
                }" class="px-2 py-0.5 rounded text-xs">{{ fav.route?.difficulty }}</span>
                <span v-if="fav.route?.temperature" class="text-xs">{{ fav.route.temperature }}</span>
              </div>
            </div>
            <button @click="removeFavorite(fav.route?.id)" class="text-red-500 hover:text-red-700" :title="t('favorites.remove')">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div v-if="totalPages > 1" class="flex justify-center mt-8 gap-2">
        <button v-for="page in totalPages" :key="page" @click="fetchFavorites(page - 1)"
          :class="currentPage === page - 1 ? 'bg-red-600 text-white' : 'bg-white text-stone-600 hover:bg-stone-100'"
          class="px-4 py-2 rounded-lg border transition-colors">{{ page }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import api, { endpoints } from '../api'

const { t } = useI18n()
const favorites = ref<any[]>([])
const loading = ref(true)
const currentPage = ref(0)
const totalPages = ref(1)

const fetchFavorites = async (page = 0) => {
  loading.value = true
  try {
    const response = await api.get(endpoints.favorites.list, { params: { page, size: 20 } })
    favorites.value = response.data?.content || (Array.isArray(response.data) ? response.data : [])
    totalPages.value = response.data?.totalPages || 1
    currentPage.value = page
  } catch (e) {
    favorites.value = []
  } finally {
    loading.value = false
  }
}

const removeFavorite = async (routeId: number) => {
  try {
    await api.delete(endpoints.favorites.remove(routeId))
    favorites.value = favorites.value.filter(f => f.route?.id !== routeId)
  } catch (e) {
    alert(t('favorites.operationFailed'))
  }
}

onMounted(() => fetchFavorites())
</script>
