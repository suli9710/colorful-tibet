<template>
  <div class="min-h-screen bg-stone-50 py-12">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h1 class="text-4xl font-bold text-stone-800 mb-4">旅游资讯</h1>
        <p class="text-lg text-stone-600">获取最新的西藏旅游政策、活动和公告</p>
      </div>

      <div class="flex justify-center mb-8 space-x-4">
        <button 
          v-for="cat in categories" 
          :key="cat.value"
          @click="selectedCategory = cat.value"
          :class="[
            'px-4 py-2 rounded-full text-sm font-medium transition-colors duration-200',
            selectedCategory === cat.value 
              ? 'bg-red-600 text-white' 
              : 'bg-white text-stone-600 hover:bg-stone-100 border border-stone-200'
          ]"
        >
          {{ cat.label }}
        </button>
      </div>

      <div v-if="loading" class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>

      <div v-else class="space-y-6">
        <div v-for="item in filteredNews" :key="item.id" class="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 flex flex-col md:flex-row">
          <div class="md:w-1/3 h-48 md:h-auto relative">
            <img :src="item.imageUrl || '/images/news/default-news.jpg'" :alt="item.title" class="w-full h-full object-cover" onerror="this.src='/images/spots/布达拉宫.jpg'">
            <div class="absolute top-0 left-0 bg-red-600 text-white px-3 py-1 m-4 rounded-full text-xs font-medium">
              {{ getCategoryLabel(item.category) }}
            </div>
          </div>
          <div class="p-6 md:w-2/3 flex flex-col justify-between">
            <div>
              <h3 class="text-2xl font-bold text-stone-800 mb-2">{{ item.title }}</h3>
              <p class="text-stone-500 text-sm mb-4">{{ formatDate(item.createdAt) }} · {{ item.viewCount }} 次阅读</p>
              <p class="text-stone-600 line-clamp-3">{{ item.content }}</p>
            </div>
            <div class="mt-4 flex justify-end">
              <button @click="openDetail(item)" class="text-red-600 hover:text-red-700 font-medium flex items-center">
                阅读全文
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Modal for details -->
      <div v-if="selectedItem" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" @click="closeDetail">
        <div class="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto" @click.stop>
          <div class="relative h-64 md:h-96">
            <img :src="selectedItem.imageUrl || '/images/news/default-news.jpg'" :alt="selectedItem.title" class="w-full h-full object-cover" onerror="this.src='/images/spots/布达拉宫.jpg'">
            <button @click="closeDetail" class="absolute top-4 right-4 bg-black bg-opacity-50 text-white p-2 rounded-full hover:bg-opacity-70">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="p-8">
            <div class="flex items-center justify-between mb-4">
              <span class="bg-red-100 text-red-800 px-3 py-1 rounded-full text-sm font-medium">{{ getCategoryLabel(selectedItem.category) }}</span>
              <span class="text-stone-500 text-sm">{{ formatDate(selectedItem.createdAt) }}</span>
            </div>
            <h2 class="text-3xl font-bold text-stone-800 mb-6">{{ selectedItem.title }}</h2>
            <div class="prose max-w-none text-stone-600 whitespace-pre-line">
              {{ selectedItem.content }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api, { endpoints } from '../api'

interface NewsItem {
  id: number
  title: string
  content: string
  category: 'POLICY' | 'EVENT' | 'NOTICE'
  imageUrl: string
  viewCount: number
  createdAt: string
}

const newsItems = ref<NewsItem[]>([])
const loading = ref(true)
const selectedCategory = ref<string>('ALL')
const selectedItem = ref<NewsItem | null>(null)

const categories = [
  { label: '全部', value: 'ALL' },
  { label: '政策', value: 'POLICY' },
  { label: '活动', value: 'EVENT' },
  { label: '公告', value: 'NOTICE' }
]

const getCategoryLabel = (category: string) => {
  const map: Record<string, string> = {
    'POLICY': '政策',
    'EVENT': '活动',
    'NOTICE': '公告'
  }
  return map[category] || category
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const fetchNews = async () => {
  try {
    const response = await api.get(endpoints.news.list)
    newsItems.value = response.data
  } catch (error) {
    console.error('Failed to fetch news:', error)
  } finally {
    loading.value = false
  }
}

const filteredNews = computed(() => {
  if (selectedCategory.value === 'ALL') {
    return newsItems.value
  }
  return newsItems.value.filter(item => item.category === selectedCategory.value)
})

const openDetail = (item: NewsItem) => {
  selectedItem.value = item
  document.body.style.overflow = 'hidden'
}

const closeDetail = () => {
  selectedItem.value = null
  document.body.style.overflow = 'auto'
}

onMounted(() => {
  fetchNews()
})
</script>
