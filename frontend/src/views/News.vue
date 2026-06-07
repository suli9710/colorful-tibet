<template>
  <div class="min-h-screen tibet-page-shell py-12 sm:py-16 lg:py-24">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <motion.div
        class="text-center mb-8 sm:mb-10 lg:mb-12"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <h1 class="tibet-heading inline-flex justify-center text-3xl font-bold text-tibet-dark mb-3 sm:text-4xl sm:mb-4">{{ t('news.title') }}</h1>
        <p class="mx-auto max-w-2xl text-sm leading-relaxed text-tibet-brown/70 sm:text-lg">{{ t('news.description') }}</p>
      </motion.div>

      <motion.div
        class="-mx-4 mb-6 flex gap-2 overflow-x-auto px-4 pb-2 sm:mx-0 sm:mb-8 sm:flex-wrap sm:justify-center sm:gap-3 sm:overflow-visible sm:px-0 sm:pb-0"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <motion.button 
          v-for="cat in categories" 
          :key="cat.value"
          @click="selectedCategory = cat.value"
          layout
          :whileHover="{ y: -2, scale: 1.04 }"
          :whilePress="{ scale: 0.94 }"
          :class="[
              'shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-colors duration-200 border mobile-touch-target',
              selectedCategory === cat.value
                ? 'bg-tibet-red text-tibet-yellow border-tibet-red shadow-md shadow-tibet-red/15'
                : 'bg-white/80 text-tibet-brown/80 hover:bg-tibet-gold/10 hover:text-tibet-dark border-tibet-gold/25'
          ]"
        >
          {{ cat.label }}
        </motion.button>
      </motion.div>

      <div v-if="loading" class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>

      <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-5 lg:grid-cols-3 lg:gap-6">
        <AnimatePresence mode="popLayout">
        <motion.div
             v-for="(item, index) in filteredNews"
             :key="item.id"
             layout
             class="group tibet-card-elevated rounded-2xl overflow-hidden flex flex-col h-full"
             :initial="cardInitial"
             :whileInView="cardInView"
             :exit="cardExit"
             :inViewOptions="inViewOnce"
             :transition="cardTransition(index)"
             :whileHover="{ y: -5, scale: 1.012 }"
             :whilePress="{ scale: 0.996 }">
          <div class="w-full h-44 sm:h-48 relative flex-shrink-0">
            <img :src="item.imageUrl || '/images/news/default-news.jpg'" :alt="item.title" class="w-full h-full object-cover tibet-image-hover will-change-transform" onerror="this.src='/images/spots/布达拉宫.jpg'">
            <div class="absolute top-0 left-0 bg-tibet-red text-tibet-yellow px-3 py-1 m-3 rounded-full text-xs font-medium shadow-lg sm:m-4">
              {{ getCategoryLabel(item.category) }}
            </div>
          </div>
          <div class="p-4 sm:p-6 flex flex-col flex-grow">
            <h3 class="text-lg font-bold text-tibet-dark mb-2 line-clamp-2 sm:text-xl">{{ item.title }}</h3>
            <p class="text-tibet-brown/50 text-sm mb-3">{{ formatDate(item.createdAt) }} · {{ item.viewCount }} {{ t('news.viewCount') }}</p>
            <p class="text-sm leading-relaxed text-tibet-brown/70 line-clamp-3 mb-4 flex-grow sm:text-base">{{ item.content }}</p>
            <div class="mt-auto">
              <button @click="openDetail(item)" class="tibet-link mobile-touch-target font-medium flex items-center">
                {{ t('common.readMore') }}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </div>
        </motion.div>
        </AnimatePresence>
      </div>

      <MotionModal
        :show="Boolean(selectedItem)"
        modal-key="news-detail-modal"
        panel-class="tibet-card-elevated rounded-2xl max-w-4xl max-h-[90dvh] overflow-y-auto p-0"
        @close="closeDetail"
      >
        <template v-if="selectedItem">
              <div class="relative h-48 sm:h-64 md:h-96">
                <img :src="selectedItem.imageUrl || '/images/news/default-news.jpg'" :alt="selectedItem.title" class="w-full h-full object-cover" onerror="this.src='/images/spots/布达拉宫.jpg'">
                <button @click="closeDetail" class="absolute top-3 right-3 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition-colors sm:top-4 sm:right-4">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 sm:h-6 sm:w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div class="p-4 sm:p-6 lg:p-8">
                <div class="flex flex-wrap items-center justify-between gap-2 mb-4">
                  <span class="bg-tibet-red/10 text-tibet-red border border-tibet-red/15 px-3 py-1 rounded-full text-sm font-medium">{{ getCategoryLabel(selectedItem.category) }}</span>
                  <span class="text-tibet-brown/50 text-sm">{{ formatDate(selectedItem.createdAt) }}</span>
                </div>
                <h2 class="text-2xl font-bold leading-tight text-tibet-dark mb-4 sm:text-3xl sm:mb-6">{{ selectedItem.title }}</h2>
                <div class="prose max-w-none text-sm leading-7 text-tibet-brown/75 whitespace-pre-line sm:text-base">
                  {{ selectedItem.content }}
                </div>
              </div>
        </template>
      </MotionModal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useI18n } from 'vue-i18n'
import MotionModal from '../components/motion/MotionModal.vue'
import api, { endpoints } from '../api'
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

const categories = computed(() => [
  { label: t('common.all'), value: 'ALL' },
  { label: t('news.category.policy'), value: 'POLICY' },
  { label: t('news.category.event'), value: 'EVENT' },
  { label: t('news.category.notice'), value: 'NOTICE' }
])

const getCategoryLabel = (category: string) => {
  const map: Record<string, string> = {
    'POLICY': t('news.category.policy'),
    'EVENT': t('news.category.event'),
    'NOTICE': t('news.category.notice')
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
    // API拦截器会自动添加locale参数，根据localStorage中的locale设置
    const response = await api.get(endpoints.news.list)
    newsItems.value = response.data?.content || response.data || []
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

// 监听语言变化，重新获取数据
watch(locale, () => {
  fetchNews()
})

onMounted(() => {
  fetchNews()
})

onBeforeUnmount(() => {
  document.body.style.overflow = 'auto'
})
</script>
