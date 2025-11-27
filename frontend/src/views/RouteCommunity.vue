<template>
  <div class="min-h-screen bg-apple-gray-50 py-24">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="text-center mb-12 animate-fade-in">
        <h1 class="text-4xl font-bold text-apple-gray-900 mb-4">路线分享社区</h1>
        <p class="text-lg text-apple-gray-500">
          探索其他旅行者的精彩行程，发现不一样的西藏。
        </p>
      </div>

      <!-- Filters -->
      <div class="glass rounded-2xl p-6 mb-8 flex flex-wrap gap-4 items-center justify-between animate-slide-up">
        <div class="flex flex-wrap gap-4">
          <select v-model="filters.days" @change="loadRoutes" class="px-4 py-2 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue outline-none">
            <option value="">全部天数</option>
            <option value="3">3天</option>
            <option value="5">5天</option>
            <option value="7">7天</option>
            <option value="10">10天+</option>
          </select>
          <select v-model="filters.budget" @change="loadRoutes" class="px-4 py-2 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue outline-none">
            <option value="">全部预算</option>
            <option value="经济型">经济型</option>
            <option value="舒适型">舒适型</option>
            <option value="豪华型">豪华型</option>
          </select>
          <select v-model="filters.preference" @change="loadRoutes" class="px-4 py-2 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue outline-none">
            <option value="">全部偏好</option>
            <option value="自然风光">自然风光</option>
            <option value="人文历史">人文历史</option>
            <option value="深度摄影">深度摄影</option>
            <option value="休闲度假">休闲度假</option>
          </select>
        </div>
        
        <div class="flex gap-2">
           <button @click="router.push('/route-planner')" class="px-4 py-2.5 bg-apple-blue text-white rounded-xl hover:bg-apple-blue-hover transition-all duration-300 transform hover:scale-105 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/30 active:scale-95 font-medium">
             + 创建我的行程
           </button>
        </div>
      </div>

      <!-- Route List -->
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-apple-blue mx-auto"></div>
      </div>
      
      <div v-else-if="routes.length === 0" class="text-center py-12 text-gray-500">
        暂无分享的路线，快去创建一个吧！
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="(route, index) in routes" :key="route.id" 
             class="glass rounded-2xl p-6 hover:shadow-2xl transition-all duration-500 ease-out cursor-pointer group transform hover:-translate-y-2 hover:border-apple-blue/30 border border-white/20 animate-slide-up"
             :style="{ animationDelay: `${index * 0.1}s` }"
             @click="viewRoute(route.id)">
          <div class="flex justify-between items-start mb-4">
            <h3 class="text-xl font-bold text-gray-900 group-hover:text-apple-blue transition-colors duration-300 line-clamp-2 flex-1">
              {{ route.title }}
            </h3>
            <span class="text-xs px-2.5 py-1 bg-blue-50 text-blue-600 rounded-full whitespace-nowrap font-semibold transform group-hover:scale-110 transition-transform duration-300 ml-2">
              {{ route.days }}天
            </span>
          </div>
          
          <div class="flex gap-2 mb-4 text-sm text-gray-600">
            <span class="px-3 py-1.5 bg-gray-100 rounded-lg transform group-hover:scale-105 transition-transform duration-300">{{ route.budget }}</span>
            <span class="px-3 py-1.5 bg-gray-100 rounded-lg transform group-hover:scale-105 transition-transform duration-300">{{ route.preference }}</span>
          </div>
          
          <div class="flex justify-between items-center text-sm text-gray-500 pt-4 border-t border-gray-100">
            <div class="flex items-center gap-4">
              <span class="flex items-center gap-1.5 transform group-hover:scale-110 transition-transform duration-300">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                {{ route.viewCount }}
              </span>
              <span class="flex items-center gap-1.5 transform group-hover:scale-110 transition-transform duration-300">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
                {{ route.likeCount }}
              </span>
              <span class="flex items-center gap-1.5 transform group-hover:scale-110 transition-transform duration-300">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
                {{ route.commentCount }}
              </span>
            </div>
            <span class="text-xs">{{ formatDate(route.createdAt) }}</span>
          </div>
        </div>
      </div>
      
      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex justify-center mt-8 gap-2">
        <button 
          @click="changePage(currentPage - 1)" 
          :disabled="currentPage === 0"
          class="px-4 py-2 rounded-lg bg-white border border-gray-200 disabled:opacity-50 hover:bg-gray-50"
        >
          上一页
        </button>
        <span class="px-4 py-2">
          {{ currentPage + 1 }} / {{ totalPages }}
        </span>
        <button 
          @click="changePage(currentPage + 1)" 
          :disabled="currentPage >= totalPages - 1"
          class="px-4 py-2 rounded-lg bg-white border border-gray-200 disabled:opacity-50 hover:bg-gray-50"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'

const router = useRouter()
const route = useRoute()
const loading = ref(true)
const routes = ref<any[]>([])
const currentPage = ref(0)
const totalPages = ref(0)

const filters = reactive({
  days: '',
  budget: '',
  preference: ''
})

const loadRoutes = async () => {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: 9,
      sortField: 'createdAt'
    }
    
    if (filters.days) {
      // 将字符串转换为整数
      const daysNum = parseInt(filters.days)
      if (!isNaN(daysNum)) {
        params.days = daysNum
      }
    }
    if (filters.budget) params.budget = filters.budget
    if (filters.preference) params.preference = filters.preference
    
    const response = await api.get('/routes/shared', { params })
    routes.value = response.data.content || []
    totalPages.value = response.data.totalPages || 0
  } catch (error) {
    console.error('Failed to load routes:', error)
    routes.value = []
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadRoutes()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const viewRoute = (id: number) => {
  router.push(`/community/${id}`)
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadRoutes()
})

// 当页面被激活时（从其他页面返回时）重新加载数据
onActivated(() => {
  loadRoutes()
})
</script>
