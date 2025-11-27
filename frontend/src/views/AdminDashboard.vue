<template>
  <div class="min-h-screen bg-stone-50 py-12 pt-24">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h1 class="text-4xl font-bold text-stone-800 mb-4">后台管理看板</h1>
        <p class="text-lg text-stone-600">数据概览与运营统计</p>
      </div>

      <div v-if="loading" class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>

      <div v-else class="space-y-8">
        <!-- Stats Cards -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div class="bg-white rounded-lg shadow p-6 border-l-4 border-blue-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">总用户数</p>
                <p class="text-3xl font-bold text-stone-800">{{ stats.userCount }}</p>
              </div>
              <div class="bg-blue-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-lg shadow p-6 border-l-4 border-green-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">总订单数</p>
                <p class="text-3xl font-bold text-stone-800">{{ stats.bookingCount }}</p>
              </div>
              <div class="bg-green-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-lg shadow p-6 border-l-4 border-yellow-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">总营收</p>
                <p class="text-3xl font-bold text-stone-800">¥{{ stats.totalRevenue?.toLocaleString() || 0 }}</p>
              </div>
              <div class="bg-yellow-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <!-- Recent Bookings -->
          <div class="bg-white rounded-lg shadow overflow-hidden">
            <div class="px-6 py-4 border-b border-stone-200">
              <h3 class="text-lg font-bold text-stone-800">最新订单</h3>
            </div>
            <div class="divide-y divide-stone-200">
              <div v-for="booking in stats.recentBookings" :key="booking.id" class="px-6 py-4 flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-stone-800">{{ booking.spot?.name || '未知景点' }}</p>
                  <p class="text-xs text-stone-500">{{ formatDate(booking.createdAt) }}</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-bold text-stone-800">¥{{ booking.totalPrice }}</p>
                  <span :class="getStatusClass(booking.status)" class="text-xs px-2 py-1 rounded-full">{{ booking.status }}</span>
                </div>
              </div>
              <div v-if="!stats.recentBookings?.length" class="px-6 py-4 text-center text-stone-500">
                暂无订单
              </div>
            </div>
          </div>

          <!-- Popular Spots -->
          <div class="bg-white rounded-lg shadow overflow-hidden">
            <div class="px-6 py-4 border-b border-stone-200">
              <h3 class="text-lg font-bold text-stone-800">热门景点</h3>
            </div>
            <div class="divide-y divide-stone-200">
              <div v-for="(spot, index) in sortedPopularSpots" :key="spot.id" class="px-6 py-4 flex items-center">
                <span class="text-lg font-bold text-stone-400 w-8">{{ index + 1 }}</span>
                <img :src="spot.imageUrl" class="w-10 h-10 rounded object-cover mr-4" alt="">
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-stone-800">{{ spot.name }}</p>
                  <p class="text-xs text-stone-500 mb-2">{{ spot.location }}</p>
                  <!-- 点击量条 -->
                  <div class="flex items-center gap-2 mt-1">
                    <div class="flex-1 bg-stone-100 rounded-full h-2 overflow-hidden">
                      <div 
                        class="h-full bg-gradient-to-r from-blue-400 to-blue-600 rounded-full transition-all duration-500"
                        :style="{ width: getClickCountPercentage(spot) + '%' }"
                      ></div>
                    </div>
                    <span class="text-xs text-stone-600 font-medium whitespace-nowrap">
                      {{ spot.visitCount || 0 }}次
                    </span>
                  </div>
                </div>
                <div class="text-right ml-4">
                  <p class="text-sm font-bold text-red-600">¥{{ spot.ticketPrice }}</p>
                </div>
              </div>
              <div v-if="!sortedPopularSpots?.length" class="px-6 py-4 text-center text-stone-500">
                暂无数据
              </div>
            </div>
          </div>
        </div>
        
        <!-- Spots Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-200 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-800">景点管理 <span class="text-sm font-normal text-stone-500">(共{{ spots.length }}个)</span></h3>
            <div class="flex space-x-3">
              <button @click="fetchSpots" class="text-sm text-blue-600 hover:text-blue-800" :disabled="loadingSpots">
                {{ loadingSpots ? '加载中...' : '刷新列表' }}
              </button>
              <button v-if="spots.length > 6 && !showAllSpots" @click="showAllSpots = true" class="text-sm text-blue-600 hover:text-blue-800">
                查看全部
              </button>
              <button v-if="showAllSpots" @click="showAllSpots = false" class="text-sm text-stone-600 hover:text-stone-800">
                收起
              </button>
            </div>
          </div>
          
          <!-- Loading State -->
          <div v-if="loadingSpots" class="p-8 text-center text-stone-500">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
            <p>正在加载景点数据...</p>
          </div>
          
          <!-- Error State -->
          <div v-else-if="spotsError" class="p-8 text-center text-red-500">
            <p class="mb-2">❌ 加载失败</p>
            <p class="text-sm">{{ spotsError }}</p>
            <button @click="fetchSpots" class="mt-4 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
              重试
            </button>
          </div>
          
          <!-- Empty State -->
          <div v-else-if="spots.length === 0" class="p-8 text-center text-stone-500">
            <p>暂无景点数据</p>
            <p class="text-sm mt-2">请确保后端服务已启动并且数据库中有景点数据</p>
          </div>
          
          <!-- Spots Grid (Compact View) -->
          <div v-else class="p-6">
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              <div v-for="spot in displayedSpots" :key="spot.id" 
                   class="group cursor-pointer border border-stone-200 rounded-lg overflow-hidden hover:shadow-lg hover:border-blue-400 transition-all"
                   @click="openEditModal(spot)">
                <div class="relative h-24 bg-gray-200">
                  <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300">
                  <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600 text-white text-2xl font-bold">
                    {{ spot.name.charAt(0) }}
                  </div>
                  <!-- Overlay on hover -->
                  <div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-30 transition-all flex items-center justify-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-white opacity-0 group-hover:opacity-100 transition-opacity" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </div>
                </div>
                <div class="p-2">
                  <h4 class="font-bold text-stone-800 text-sm truncate">{{ spot.name }}</h4>
                  <p class="text-xs text-red-600 font-semibold">¥{{ spot.ticketPrice }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- User Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-200 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-800">用户管理</h3>
            <button @click="fetchUsers" class="text-sm text-blue-600 hover:text-blue-800">刷新列表</button>
          </div>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-stone-200">
              <thead class="bg-stone-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">ID</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">用户名</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">昵称</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">角色</th>
                  <th class="px-6 py-3 text-right text-xs font-medium text-stone-500 uppercase tracking-wider">操作</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-stone-200">
                <tr v-for="u in users" :key="u.id">
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ u.id }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-stone-900">{{ u.username }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ u.nickname || '-' }}</td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span :class="u.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-800'" 
                          class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full">
                      {{ u.role }}
                    </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button v-if="u.role !== 'ADMIN'" @click="updateRole(u.id, 'ADMIN')" class="text-blue-600 hover:text-blue-900 mr-4">设为管理员</button>
                    <button v-else-if="u.username !== 'lzh'" @click="updateRole(u.id, 'USER')" class="text-red-600 hover:text-red-900">取消管理员</button>
                    <span v-else class="text-gray-400 cursor-not-allowed">不可操作</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Spot Modal -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto"
      @click.self="closeEditModal"
    >
      <div class="bg-white rounded-2xl max-w-2xl w-full p-8 animate-scale-in max-h-[85vh] overflow-y-auto">
        <h2 class="text-2xl font-bold mb-6 text-stone-800">编辑景点信息</h2>
        
        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">景点名称</label>
          <p class="text-lg font-bold text-stone-900">{{ editingSpot.name }}</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">当前封面</label>
          <div class="relative h-64 bg-gray-200 rounded-lg overflow-hidden mb-4">
            <img v-if="editingSpot.imageUrl" :src="editingSpot.imageUrl" :alt="editingSpot.name" class="w-full h-full object-cover">
            <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600 text-white text-6xl font-bold">
              {{ editingSpot.name?.charAt(0) }}
            </div>
          </div>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">新图片URL</label>
          <input v-model="newImageUrl" type="url" 
                 class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                 placeholder="https://example.com/image.jpg">
          <p class="text-xs text-stone-500 mt-2">提示：输入有效的图片URL地址，建议使用600px宽度的图片</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">门票价格（元）</label>
          <div class="flex items-center space-x-2">
            <span class="text-stone-500 text-sm">当前：</span>
            <span class="font-semibold text-red-600 mr-4">¥{{ editingSpot.ticketPrice }}</span>
          </div>
          <input
            v-model.number="newTicketPrice"
            type="number"
            min="0"
            step="0.01"
            class="mt-2 w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            placeholder="不修改则留空，例：199.00"
          >
          <p class="text-xs text-stone-500 mt-2">提示：输入新的门票价格（保留两位小数），不修改价格时可留空。</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">景点介绍</label>
          <textarea v-model="newDescription" rows="6"
                    class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                    placeholder="输入景点介绍..."></textarea>
          <p class="text-xs text-stone-500 mt-2">当前字数：{{ newDescription.length }}</p>
        </div>

        <div v-if="newImageUrl" class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">预览</label>
          <div class="relative h-64 bg-gray-200 rounded-lg overflow-hidden">
            <img :src="newImageUrl" alt="预览" class="w-full h-full object-cover" @error="imageError = true">
            <div v-if="imageError" class="absolute inset-0 flex items-center justify-center bg-red-100 text-red-600">
              <p>图片加载失败，请检查URL是否正确</p>
            </div>
          </div>
        </div>

        <div class="flex space-x-4">
          <button @click="updateSpotImage" :disabled="imageError || updating"
                  class="flex-1 bg-blue-500 text-white py-3 rounded-lg hover:bg-blue-600 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed">
            {{ updating ? '保存中...' : '保存修改' }}
          </button>
          <button @click="closeEditModal" :disabled="updating"
                  class="flex-1 bg-stone-200 text-stone-700 py-3 rounded-lg hover:bg-stone-300 transition-colors disabled:cursor-not-allowed">
            取消
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api, { endpoints } from '../api'

interface Stats {
  userCount: number
  bookingCount: number
  totalRevenue: number
  recentBookings: any[]
  popularSpots: any[]
}

const stats = ref<Stats>({
  userCount: 0,
  bookingCount: 0,
  totalRevenue: 0,
  recentBookings: [],
  popularSpots: []
})
const loading = ref(true)

const fetchStats = async () => {
  try {
    const response = await api.get(endpoints.admin.stats)
    stats.value = response.data
  } catch (error) {
    console.error('Failed to fetch admin stats:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const getStatusClass = (status: string) => {
  switch (status) {
    case 'CONFIRMED': return 'bg-green-100 text-green-800'
    case 'PENDING': return 'bg-yellow-100 text-yellow-800'
    case 'CANCELLED': return 'bg-red-100 text-red-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

// 确保热门景点按点击量降序排序
const sortedPopularSpots = computed(() => {
  if (!stats.value.popularSpots || stats.value.popularSpots.length === 0) {
    return []
  }
  return [...stats.value.popularSpots].sort((a: any, b: any) => {
    const countA = a.visitCount || 0
    const countB = b.visitCount || 0
    return countB - countA // 降序排序
  })
})

// 计算点击量百分比（相对于最高点击量）
const getClickCountPercentage = (spot: any) => {
  if (!sortedPopularSpots.value || sortedPopularSpots.value.length === 0) {
    return 0
  }
  const maxCount = Math.max(...sortedPopularSpots.value.map((s: any) => s.visitCount || 0))
  if (maxCount === 0) return 0
  const currentCount = spot.visitCount || 0
  return Math.round((currentCount / maxCount) * 100)
}

const users = ref<any[]>([])
const spots = ref<any[]>([])
const loadingSpots = ref(false)
const spotsError = ref('')
const showAllSpots = ref(false)
const showEditModal = ref(false)
const editingSpot = ref<any>({})
const newImageUrl = ref('')
const newDescription = ref('')
const newTicketPrice = ref<number | null>(null)
const imageError = ref(false)
const updating = ref(false)
const originalBodyOverflow = ref<string | null>(null)

// Computed property to control displayed spots
const displayedSpots = computed(() => {
  return showAllSpots.value ? spots.value : spots.value.slice(0, 6)
})

const fetchUsers = async () => {
  try {
    console.log('正在获取用户数据...')
    const response = await api.get(endpoints.admin.users)
    console.log('用户数据响应:', response)
    if (Array.isArray(response.data)) {
      users.value = response.data
      console.log(`成功加载 ${users.value.length} 个用户`)
    } else {
      console.error('响应数据格式错误:', response.data)
      users.value = []
    }
  } catch (error: any) {
    console.error('获取用户数据失败:', error)
    if (error.response) {
      console.error('响应状态:', error.response.status)
      console.error('响应数据:', error.response.data)
    } else if (error.request) {
      console.error('请求已发送但无响应:', error.request)
    }
    users.value = []
  }
}

const fetchSpots = async () => {
  loadingSpots.value = true
  spotsError.value = ''
  try {
    console.log('=== 开始获取景点数据 ===')
    console.log('请求 URL:', endpoints.admin.spots)
    console.log('完整路径:', '/api' + endpoints.admin.spots)
    
    // 检查认证 token
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      console.log('用户 token 存在:', user.token ? '是' : '否')
    } else {
      console.warn('未找到用户信息，可能未登录')
    }
    
    const response = await api.get(endpoints.admin.spots)
    console.log('景点数据响应状态:', response.status)
    console.log('景点数据响应头:', response.headers)
    console.log('景点数据响应数据:', response.data)
    console.log('数据类型:', Array.isArray(response.data) ? '数组' : typeof response.data)
    
    if (Array.isArray(response.data)) {
      spots.value = response.data
      console.log(`✅ 成功加载 ${spots.value.length} 个景点`)
      if (spots.value.length > 0) {
        console.log('第一个景点示例:', spots.value[0])
      }
    } else {
      console.error('❌ 响应数据格式错误:', response.data)
      spotsError.value = '响应数据格式错误: 期望数组，但收到 ' + typeof response.data
      spots.value = []
    }
  } catch (error: any) {
    console.error('❌ 获取景点数据失败:', error)
    console.error('错误类型:', error.constructor.name)
    console.error('错误消息:', error.message)
    
    if (error.response) {
      console.error('响应状态:', error.response.status)
      console.error('响应头:', error.response.headers)
      console.error('响应数据:', error.response.data)
      
      if (error.response.status === 401) {
        spotsError.value = '未授权：请先登录管理员账户'
      } else if (error.response.status === 403) {
        spotsError.value = '禁止访问：您没有管理员权限'
      } else {
        spotsError.value = error.response.data?.message || `服务器错误 (${error.response.status})`
      }
    } else if (error.request) {
      console.error('请求已发送但无响应')
      console.error('请求配置:', error.config)
      spotsError.value = '无法连接到后端服务，请检查：1) 后端是否运行在 http://localhost:8080 2) 网络连接是否正常'
    } else {
      console.error('请求配置错误:', error.config)
      spotsError.value = error.message || '未知错误'
    }
    spots.value = []
  } finally {
    loadingSpots.value = false
    console.log('=== 获取景点数据完成 ===')
  }
}

const openEditModal = (spot: any) => {
  editingSpot.value = { ...spot }
  newImageUrl.value = spot.imageUrl || ''
  newDescription.value = spot.description || ''
  newTicketPrice.value = spot.ticketPrice ?? null
  imageError.value = false
  showEditModal.value = true

  // 锁定主页面滚动
  if (typeof document !== 'undefined') {
    if (originalBodyOverflow.value === null) {
      originalBodyOverflow.value = document.body.style.overflow || ''
    }
    document.body.style.overflow = 'hidden'
  }
}

const closeEditModal = () => {
  showEditModal.value = false
  editingSpot.value = {}
  newImageUrl.value = ''
  newDescription.value = ''
  newTicketPrice.value = null
  imageError.value = false

  // 恢复主页面滚动
  if (typeof document !== 'undefined' && originalBodyOverflow.value !== null) {
    document.body.style.overflow = originalBodyOverflow.value
    originalBodyOverflow.value = null
  }
}

const updateSpotImage = async () => {
  if (imageError.value) {
    alert('图片URL无效，请检查后重试')
    return
  }
  
  updating.value = true
  try {
    const payload: any = {}
    if (newImageUrl.value) payload.imageUrl = newImageUrl.value
    if (newDescription.value) payload.description = newDescription.value
    if (newTicketPrice.value !== null && !Number.isNaN(newTicketPrice.value)) {
      payload.ticketPrice = newTicketPrice.value
    }

    if (Object.keys(payload).length === 0) {
      alert('请至少修改一项内容')
      updating.value = false
      return
    }
    
    await api.put(endpoints.admin.updateSpot(editingSpot.value.id), payload)
    alert('景点信息更新成功！')
    await fetchSpots()
    closeEditModal()
  } catch (error) {
    console.error('Failed to update spot:', error)
    alert('更新失败，请重试')
  } finally {
    updating.value = false
  }
}

const updateRole = async (userId: number, newRole: string) => {
  if (!confirm(`确定要将该用户设置为 ${newRole} 吗？`)) return
  
  try {
    await api.post(endpoints.admin.updateRole(userId), { role: newRole })
    await fetchUsers() // Refresh list
    alert('操作成功')
  } catch (error) {
    console.error('Failed to update role:', error)
    alert('操作失败')
  }
}

onMounted(() => {
  fetchStats()
  fetchUsers()
  fetchSpots()
})

onUnmounted(() => {
  // 组件卸载时确保恢复主页面滚动
  if (typeof document !== 'undefined' && originalBodyOverflow.value !== null) {
    document.body.style.overflow = originalBodyOverflow.value
    originalBodyOverflow.value = null
  }
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-scale-in {
  animation: scale-in 0.2s ease-out;
}
</style>
