<template>
  <div class="min-h-screen bg-apple-gray-50 py-24">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="text-center mb-16 animate-fade-in">
        <h1 class="text-4xl font-bold text-apple-gray-900 mb-4 tibetan-font">{{ t('spots.title') }}</h1>
        <p class="text-lg text-apple-gray-500 max-w-2xl mx-auto tibetan-font">
          {{ t('spots.subtitle') }}
        </p>
      </div>

      <!-- Filters -->
      <div class="flex justify-center mb-12 animate-slide-up will-change-transform" style="animation-delay: 0.1s">
        <div class="bg-white p-1.5 rounded-full shadow-lg border border-gray-200 flex space-x-2">
          <button 
            v-for="cat in categories" 
            :key="cat.value"
            @click="selectedCategory = cat.value"
            :class="[
              'px-6 py-2.5 rounded-full text-sm font-medium transition-all duration-300 ease-out-expo relative overflow-hidden will-change-transform tibetan-font',
              selectedCategory === cat.value 
                ? 'bg-apple-gray-900 text-white shadow-md transform scale-105' 
                : 'text-apple-gray-600 hover:bg-apple-gray-100 hover:text-apple-gray-900 hover:scale-105'
            ]"
          >
            <span class="relative z-10">{{ cat.label }}</span>
            <span v-if="selectedCategory === cat.value" 
                  class="absolute inset-0 bg-gradient-to-r from-blue-500/20 to-purple-500/20 transition-opacity duration-300 ease-out-expo"></span>
          </button>
        </div>
      </div>

      <!-- Loading State with Skeleton -->
      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <div v-for="i in 6" :key="i" class="bg-white rounded-3xl shadow-sm overflow-hidden border border-gray-100 animate-pulse">
          <div class="h-72 bg-gray-200"></div>
          <div class="p-8">
            <div class="h-6 bg-gray-200 rounded mb-4 w-3/4"></div>
            <div class="h-4 bg-gray-200 rounded mb-2"></div>
            <div class="h-4 bg-gray-200 rounded mb-6 w-5/6"></div>
            <div class="flex space-x-2">
              <div class="h-6 bg-gray-200 rounded-full w-16"></div>
              <div class="h-6 bg-gray-200 rounded-full w-16"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Debug Info -->
      <div v-else-if="spots.length === 0" class="text-center py-20">
        <div class="bg-yellow-50 border-2 border-yellow-400 rounded-2xl p-8 max-w-2xl mx-auto">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-yellow-500 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <h3 class="text-xl font-bold text-gray-900 mb-2 tibetan-font">{{ t('spots.noDataTitle') }}</h3>
          <p class="text-gray-600 mb-4 tibetan-font">{{ t('spots.noDataMessage') }}</p>
          <div class="text-left bg-white p-4 rounded-lg text-sm">
            <p class="font-semibold mb-2 tibetan-font">{{ t('spots.checkList') }}</p>
            <ul class="list-disc list-inside space-y-1 text-gray-700">
              <li class="tibetan-font">{{ t('spots.checkItem1') }}</li>
              <li class="tibetan-font">{{ t('spots.checkItem2') }}</li>
              <li class="tibetan-font">{{ t('spots.checkItem3') }}</li>
            </ul>
          </div>
          <button @click="fetchSpots()" class="mt-4 bg-blue-500 text-white px-6 py-2 rounded-full hover:bg-blue-600 transition-colors tibetan-font">
            {{ t('spots.reload') }}
          </button>
        </div>
      </div>

      <!-- Spots Grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <div v-for="(spot, index) in filteredSpots" :key="spot.id" 
             class="group bg-white rounded-3xl shadow-sm hover:shadow-2xl card-hover overflow-hidden border border-gray-100 animate-slide-up hover:border-apple-blue/20 gpu-accelerated"
             :style="{ animationDelay: `${0.2 + index * 0.08}s` }">
          
          <!-- Image Container -->
          <div class="relative h-72 overflow-hidden cursor-pointer bg-gray-200" @click="router.push(`/spots/${spot.id}`)">
            <img v-if="spot.imageUrl" 
                 :src="spot.imageUrl" 
                 :alt="spot.name"
                 loading="lazy"
                 class="w-full h-full object-cover transition-transform duration-700 ease-out-expo group-hover:scale-110 img-fade-in will-change-transform"
                 @error="handleImageError($event)">
            <div v-else 
                 class="w-full h-full flex items-center justify-center"
                 :class="getGradientClass(spot)">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-24 w-24 text-white opacity-30 animate-pulse-slow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path v-if="spot.category === 'NATURAL'" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 ease-out-expo"></div>
            
            <!-- Category Badge -->
            <div class="absolute top-4 right-4 bg-white/95 backdrop-blur-md px-3 py-1.5 rounded-full text-xs font-bold text-apple-gray-900 shadow-xl transform group-hover:scale-110 transition-transform duration-300 ease-out-expo will-change-transform tibetan-font">
              {{ spot.category === 'NATURAL' ? t('spots.category.natural') : t('spots.category.cultural') }}
            </div>
          </div>
          
          <!-- Content -->
          <div class="p-8">
            <div class="flex justify-between items-start mb-4">
              <h3 class="text-xl font-bold text-apple-gray-900 group-hover:text-apple-blue transition-colors duration-300 ease-out-expo flex-1 tibetan-font">
                {{ spot.name }}
              </h3>
              <span class="text-lg font-semibold text-apple-blue shrink-0 ml-3 transform group-hover:scale-110 transition-transform duration-300 ease-out-expo will-change-transform">¥{{ spot.ticketPrice }}</span>
            </div>
            
            <p class="text-apple-gray-500 mb-6 line-clamp-3 leading-relaxed tibetan-font">
              {{ spot.description }}
            </p>
            
            <!-- Tags & Action -->
            <div class="flex items-center justify-between pt-6 border-t border-gray-100">
              <div class="flex space-x-2 overflow-hidden">
                <span v-for="tag in spot.tags?.slice(0, 2)" :key="tag.id" 
                      class="px-3 py-1 bg-apple-gray-100 text-apple-gray-600 rounded-full text-xs font-medium whitespace-nowrap transform group-hover:scale-105 transition-transform duration-300 ease-out-expo will-change-transform">
                  {{ tag.tag }}
                </span>
              </div>
              <button @click="router.push(`/spots/${spot.id}`)" 
                      class="text-apple-blue font-medium hover:text-apple-blue-hover transition-all duration-300 ease-out-expo flex items-center shrink-0 ml-4 group/btn tibetan-font">
                {{ t('spots.viewDetails') }}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1 transform group-hover/btn:translate-x-2 transition-transform duration-300 ease-out-expo will-change-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api, { endpoints } from '../api'

const router = useRouter()
const { t, locale } = useI18n()
const spots = ref<any[]>([])
const loading = ref(true)
const selectedCategory = ref<string>('ALL')

const categories = computed(() => [
  { label: t('spots.category.all'), value: 'ALL' },
  { label: t('spots.category.natural'), value: 'NATURAL' },
  { label: t('spots.category.cultural'), value: 'CULTURAL' }
])

const fetchSpots = async () => {
  try {
    loading.value = true
    const response = await api.get(endpoints.spots.list)
    spots.value = response.data
    
    if (!response.data || response.data.length === 0) {
      console.warn('后端返回了空数据')
    }
  } catch (error: any) {
    console.error('获取景点失败:', error)
    const apiBaseURL = (api.defaults.baseURL || '/api')
    
    let errorMessage = `${t('spots.errorTitle')}\n\n`
    errorMessage += `${t('common.error')}: ${apiBaseURL}\n\n`
    
    if (error.code === 'ECONNREFUSED' || error.message?.includes('Network Error')) {
      if (apiBaseURL === '/api') {
        errorMessage += `${t('spots.errorLocal')}\n\n`
        errorMessage += `${t('spots.errorSolution')}\n`
        errorMessage += `${t('spots.errorSolutionLocal')}`
      } else {
        errorMessage += `${t('spots.errorRemote')}: ${apiBaseURL}\n\n`
        errorMessage += `${t('spots.errorSolution')}\n`
        errorMessage += `${t('spots.errorSolutionRemote')}`
      }
    } else if (error.response) {
      errorMessage += `${t('common.error')}: ${error.response.status}\n`
      errorMessage += `${error.response.data?.message || error.response.statusText}`
    } else {
      errorMessage += `${error.message || error}`
    }
    
    alert(errorMessage)
  } finally {
    loading.value = false
  }
}

const filteredSpots = computed(() => {
  if (selectedCategory.value === 'ALL') {
    return spots.value
  }
  return spots.value.filter(spot => spot.category === selectedCategory.value)
})

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
  const parent = img.parentElement
  if (parent) {
    parent.classList.add('bg-gradient-to-br', 'from-blue-500', 'to-purple-600')
  }
}

const getGradientClass = (spot: any) => {
  const gradients = [
    'bg-gradient-to-br from-blue-500 via-blue-600 to-indigo-700',
    'bg-gradient-to-br from-purple-500 via-purple-600 to-pink-600',
    'bg-gradient-to-br from-green-500 via-teal-600 to-cyan-700',
    'bg-gradient-to-br from-orange-500 via-red-500 to-pink-600',
    'bg-gradient-to-br from-indigo-500 via-purple-600 to-blue-700',
    'bg-gradient-to-br from-teal-500 via-green-600 to-emerald-700',
    'bg-gradient-to-br from-rose-500 via-pink-600 to-purple-700',
    'bg-gradient-to-br from-amber-500 via-orange-600 to-red-700',
    'bg-gradient-to-br from-cyan-500 via-blue-600 to-indigo-700',
    'bg-gradient-to-br from-emerald-500 via-teal-600 to-cyan-700'
  ]
  const index = spot.id % gradients.length
  return gradients[index]
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  fetchSpots()
})

onMounted(() => {
  fetchSpots()
})
</script>
