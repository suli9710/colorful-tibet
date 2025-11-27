<template>
  <div class="min-h-screen bg-apple-gray-50">
    <div v-if="loading" class="flex justify-center items-center h-screen">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-apple-blue"></div>
    </div>

    <div v-else-if="spot" class="relative">
      <!-- Immersive Header Image -->
      <div class="relative h-[60vh] w-full overflow-hidden bg-gray-200 animate-scale-in">
        <img v-if="spot.imageUrl" 
             :src="spot.imageUrl" 
             :alt="spot.name"
             loading="eager"
             class="w-full h-full object-cover img-fade-in">
        <div v-else class="w-full h-full flex items-center justify-center" :class="getGradientClass(spot)">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-40 w-40 text-white opacity-30 animate-pulse-slow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path v-if="spot.category === 'NATURAL'" stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        </div>
        <div class="absolute inset-0 bg-gradient-to-t from-apple-gray-900/80 via-transparent to-transparent"></div>
        
        <div class="absolute bottom-0 left-0 w-full p-8 md:p-16 text-white animate-slide-up">
          <div class="max-w-7xl mx-auto">
            <div class="flex items-center space-x-4 mb-4">
              <span class="px-4 py-1.5 bg-white/20 backdrop-blur-md rounded-full text-sm font-bold border border-white/30">
                {{ spot.category === 'NATURAL' ? '自然风光' : '人文景观' }}
              </span>
              <div class="flex space-x-2">
                <span v-for="tag in spot.tags" :key="tag.id" class="px-3 py-1 bg-black/30 backdrop-blur-sm rounded-full text-xs font-medium border border-white/10">
                  #{{ tag.tag }}
                </span>
              </div>
            </div>
            <h1 class="text-5xl md:text-6xl font-bold mb-4 tracking-tight">{{ spot.name }}</h1>
            <div class="flex items-center text-white/80 space-x-6">
              <span class="flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                西藏自治区
              </span>
              <span class="text-2xl font-bold text-apple-blue">¥{{ unitPrice }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Content Section -->
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 -mt-20 relative z-10">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-12">
          <!-- Left Column: Description -->
          <div class="lg:col-span-2 space-y-8 animate-slide-up" style="animation-delay: 0.2s">
            <div class="bg-white rounded-3xl p-8 shadow-xl border border-gray-100">
              <h2 class="text-2xl font-bold text-apple-gray-900 mb-6">景点介绍</h2>
              <p class="text-apple-gray-600 leading-loose text-lg whitespace-pre-line">
                {{ spot.description }}
              </p>
            </div>

            <!-- Comments Section -->
            <div class="bg-white rounded-3xl p-8 shadow-xl border border-gray-100">
              <h2 class="text-2xl font-bold text-apple-gray-900 mb-6">游客评论</h2>
              
              <!-- Comment Form -->
              <div v-if="user" class="mb-8 p-6 bg-gray-50 rounded-2xl">
                <h3 class="text-lg font-bold text-gray-800 mb-4">发表评论</h3>
                <div class="flex items-center mb-4">
                  <span class="mr-4 text-gray-600">评分:</span>
                  <div class="flex space-x-1">
                    <button v-for="star in 5" :key="star" @click="commentForm.rating = star" 
                            class="text-2xl focus:outline-none transition-transform hover:scale-110"
                            :class="star <= commentForm.rating ? 'text-yellow-400' : 'text-gray-300'">
                      ★
                    </button>
                  </div>
                </div>
                <textarea v-model="commentForm.content" rows="3" 
                          class="w-full p-4 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-100 focus:border-blue-400 outline-none transition-all duration-300 input-focus mb-3 resize-none"
                          placeholder="分享您的游玩体验..."></textarea>
                <div class="mb-4">
                  <label class="block text-sm font-medium text-gray-600 mb-2">添加照片（可选）</label>
                  <div class="flex items-center space-x-4">
                    <label for="comment-image-input"
                           class="inline-flex items-center px-4 py-2 rounded-full bg-white border border-gray-200 text-sm font-medium text-gray-600 cursor-pointer hover:bg-blue-50 hover:text-blue-600 transition-colors">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 15a4 4 0 004 4h10a4 4 0 004-4m-4-8h-4m0 0V3m0 4l3-3m-3 3L9 4" />
                      </svg>
                      选择图片
                    </label>
                    <span class="text-sm text-gray-500 truncate max-w-[200px]" v-if="commentImageFileName">{{ commentImageFileName }}</span>
                    <span class="text-sm text-gray-400" v-else>支持 JPG / PNG / HEIC</span>
                  </div>
                  <input id="comment-image-input" type="file" accept="image/*" class="hidden" @change="handleCommentImageChange">
                  <p class="text-xs text-gray-400 mt-1">提示：图片大小不超过 5MB</p>
                  <div v-if="commentImagePreview" class="mt-4 relative w-40 h-28">
                    <img :src="commentImagePreview" alt="评论预览" class="w-full h-full object-cover rounded-2xl border border-gray-100 shadow-sm">
                    <button type="button" @click="removeSelectedCommentImage"
                            class="absolute -top-2 -right-2 bg-white text-gray-500 hover:text-red-500 rounded-full p-1 shadow">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </div>
                <div class="mt-4 text-right">
                  <button @click="submitComment" :disabled="submittingComment"
                          class="bg-apple-blue text-white px-6 py-3 rounded-full font-medium hover:bg-blue-600 transition-all duration-300 transform hover:scale-105 hover:shadow-lg hover:shadow-blue-500/30 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none relative overflow-hidden group">
                    <span class="relative z-10">{{ submittingComment ? '提交中...' : '发布评论' }}</span>
                    <span class="absolute inset-0 bg-gradient-to-r from-blue-400 to-purple-400 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></span>
                  </button>
                </div>
              </div>
              <div v-else class="mb-8 p-6 bg-gray-50 rounded-2xl text-center">
                <p class="text-gray-500">登录后即可发表评论</p>
                <router-link to="/login" class="inline-block mt-2 text-blue-600 hover:underline">去登录</router-link>
              </div>

              <!-- Comment List -->
              <div class="space-y-6">
                <div v-for="comment in comments" :key="comment.id" class="border-b border-gray-100 pb-6 last:border-0">
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center space-x-3">
                      <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold">
                        {{ comment.user?.nickname?.charAt(0) || comment.user?.username?.charAt(0) }}
                      </div>
                      <div>
                        <p class="font-bold text-gray-900">{{ comment.user?.nickname || comment.user?.username }}</p>
                        <p class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</p>
                      </div>
                    </div>
                    <div class="flex text-yellow-400">
                      <span v-for="n in 5" :key="n">{{ n <= comment.rating ? '★' : '☆' }}</span>
                    </div>
                  </div>
                  <p class="text-gray-600 leading-relaxed pl-13 mb-3">{{ comment.content }}</p>
                  
                  <!-- 评论图片 -->
                  <div v-if="comment.imageUrl" class="pl-13 mb-3">
                    <img :src="comment.imageUrl" :alt="comment.user?.nickname + '的照片'"
                         loading="lazy"
                         class="rounded-2xl max-w-md w-full h-auto object-cover shadow-md hover:shadow-lg transition-shadow cursor-pointer bg-gray-100"
                         @click="openImageModal(comment.imageUrl)"
                         @error="handleCommentImageError($event)">
                  </div>
                  
                  <!-- 点赞按钮 -->
                  <div class="pl-13 flex items-center space-x-4">
                    <button v-if="user" @click="toggleLike(comment)" 
                            class="flex items-center space-x-2 text-gray-500 hover:text-red-500 transition-all duration-300 group">
                      <svg xmlns="http://www.w3.org/2000/svg" 
                           :class="comment.liked ? 'fill-red-500 text-red-500' : 'fill-none'"
                           class="w-5 h-5 transition-all duration-300 group-hover:scale-125 transform" 
                           viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" 
                              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      <span :class="comment.liked ? 'text-red-500 font-semibold transform scale-110' : ''" 
                            class="transition-all duration-300">
                        {{ comment.likeCount || 0 }}
                      </span>
                    </button>
                    <span v-else class="flex items-center space-x-2 text-gray-400">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" 
                              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      <span>{{ comment.likeCount || 0 }}</span>
                    </span>
                  </div>
                </div>
                <div v-if="comments.length === 0" class="text-center text-gray-400 py-8">
                  暂无评论，快来抢沙发吧！
                </div>
              </div>
            </div>

            <!-- Interactive Map -->
            <div class="bg-white rounded-3xl p-8 shadow-xl border border-gray-100 overflow-hidden">
              <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                <div>
                  <h2 class="text-2xl font-bold text-apple-gray-900">地理位置</h2>
                  <p class="text-sm text-apple-gray-500 mt-1">拖拽、缩放查看周边环境</p>
                </div>
                <button
                  @click="recenterMap"
                  :disabled="!mapReady || !hasValidLocation"
                  class="inline-flex items-center justify-center px-4 py-2 rounded-2xl text-sm font-semibold transition-all"
                  :class="!mapReady || !hasValidLocation
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    : 'bg-blue-600/10 text-blue-600 hover:bg-blue-600/20'">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none"
                       stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364l-1.414 1.414M7.05 16.95l-1.414 1.414m0-11.314L7.05 7.05m10.607 10.607l1.414 1.414" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  回到景点
                </button>
              </div>
              <div class="relative rounded-2xl overflow-hidden">
                <div v-if="!hasValidLocation" class="bg-apple-gray-100 h-72 flex items-center justify-center text-apple-gray-400 text-center px-6">
                  <div>
                    <p>该景点暂无地理坐标信息</p>
                    <p class="text-sm mt-2">请稍后再试</p>
                  </div>
                </div>
                <div v-else>
                  <div ref="mapContainer" class="h-72 w-full"></div>
                  <div
                    v-if="mapLoading"
                    class="absolute inset-0 bg-white/70 backdrop-blur-sm flex flex-col items-center justify-center text-apple-gray-500 text-sm">
                    <svg class="animate-spin h-6 w-6 text-blue-500 mb-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                    </svg>
                    地图加载中...
                  </div>
                </div>
              </div>
              <div class="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm text-apple-gray-600">
                <div class="bg-apple-gray-50 rounded-2xl px-4 py-3">
                  <p class="text-xs text-apple-gray-400">经度</p>
                  <p class="font-semibold mt-1">{{ spot.longitude || '—' }}</p>
                </div>
                <div class="bg-apple-gray-50 rounded-2xl px-4 py-3">
                  <p class="text-xs text-apple-gray-400">纬度</p>
                  <p class="font-semibold mt-1">{{ spot.latitude || '—' }}</p>
                </div>
                <div class="bg-apple-gray-50 rounded-2xl px-4 py-3">
                  <p class="text-xs text-apple-gray-400">海拔</p>
                  <p class="font-semibold mt-1">{{ spot.altitude ? spot.altitude + ' m' : '—' }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Right Column: Booking Form -->
          <div class="lg:col-span-1 animate-slide-up" style="animation-delay: 0.4s">
            <div class="sticky top-24">
              <div class="glass rounded-3xl p-8 shadow-2xl border border-white/50">
                <h2 class="text-2xl font-bold text-apple-gray-900 mb-6">立即预订</h2>
                
                <form @submit.prevent="handleBooking" class="space-y-6">
                  <div>
                    <label class="block text-sm font-medium text-apple-gray-700 mb-2">游玩日期</label>
                    <input type="date" v-model="bookingForm.visitDate" required
                           class="w-full px-4 py-3 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none transition-all">
                  </div>

                  <div>
                    <label class="block text-sm font-medium text-apple-gray-700 mb-2">购票数量</label>
                    <div class="flex items-center space-x-4">
                      <button type="button" @click="bookingForm.ticketCount > 1 && bookingForm.ticketCount--" 
                              class="w-10 h-10 rounded-full bg-apple-gray-100 hover:bg-apple-gray-200 flex items-center justify-center text-apple-gray-600 transition-all duration-300 transform hover:scale-110 active:scale-95">
                        -
                      </button>
                      <span class="text-xl font-bold text-apple-gray-900 w-8 text-center transition-all duration-300">{{ bookingForm.ticketCount }}</span>
                      <button type="button" @click="bookingForm.ticketCount++" 
                              class="w-10 h-10 rounded-full bg-apple-gray-100 hover:bg-apple-gray-200 flex items-center justify-center text-apple-gray-600 transition-all duration-300 transform hover:scale-110 active:scale-95">
                        +
                      </button>
                    </div>
                  </div>

                  <div class="pt-6 border-t border-gray-200">
                    <div class="flex justify-between items-center mb-6">
                      <span class="text-apple-gray-600">总计金额</span>
                      <span class="text-3xl font-bold text-apple-blue">¥{{ totalPrice }}</span>
                    </div>
                    
                    <button type="submit" :disabled="submitting"
                            class="w-full bg-apple-blue hover:bg-apple-blue-hover text-white font-bold py-4 px-6 rounded-2xl transition-all duration-300 transform hover:scale-[1.03] hover:-translate-y-1 active:scale-[0.98] shadow-lg hover:shadow-blue-500/50 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none relative overflow-hidden group">
                      <span class="relative z-10">{{ submitting ? '处理中...' : '确认支付' }}</span>
                      <span class="absolute inset-0 bg-gradient-to-r from-blue-400 to-purple-400 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { endpoints } from '../api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()
const spot = ref<any>(null)
const loading = ref(true)
const submitting = ref(false)
const mapContainer = ref<HTMLElement | null>(null)
const mapReady = ref(false)
const mapLoading = ref(true)
let map: L.Map | null = null
let marker: L.CircleMarker | null = null

const bookingForm = ref({
  visitDate: '',
  ticketCount: 1
})

// 计算当前日期下的单张门票价格（考虑淡季/旺季/冬季免票）
// 规则：
// - 旺季：5月1日-10月31日，优先使用 peakSeasonPrice，否则 ticketPrice
// - 淡季：11月1日-次年4月30日，优先使用 offSeasonPrice，否则 ticketPrice
// - 其中每年 1 月 1 日 - 3 月 31 日统一实施免票政策（价格为 0）
const unitPrice = computed(() => {
  if (!spot.value) return 0

  const base = Number(spot.value.ticketPrice || 0)
  const visitDateStr = bookingForm.value.visitDate
  if (!visitDateStr) return base

  const visit = new Date(`${visitDateStr}T00:00:00`)

  const month = visit.getMonth() + 1 // 1-12
  const day = visit.getDate()

  // 1-3 月统一免票
  if (month >= 1 && month <= 3) {
    return 0
  }

  const inRange = (start?: string, end?: string) => {
    if (!start || !end) return false
    const s = new Date(`${start}T00:00:00`)
    const e = new Date(`${end}T00:00:00`)
    return visit >= s && visit <= e
  }

  const isOnOrAfter = (m: number, d: number) =>
    month > m || (month === m && day >= d)
  const isOnOrBefore = (m: number, d: number) =>
    month < m || (month === m && day <= d)

  const isPeakSeason =
    isOnOrAfter(5, 1) && isOnOrBefore(10, 31) // 5月1日-10月31日

  // 旺季：优先使用 peakSeasonPrice
  if (isPeakSeason && spot.value.peakSeasonPrice != null) {
    return Number(spot.value.peakSeasonPrice)
  }

  // 淡季：11月1日-次年4月30日，优先使用 offSeasonPrice
  const isOffSeason = !isPeakSeason // 其余日期全部视为淡季
  if (isOffSeason && spot.value.offSeasonPrice != null) {
    return Number(spot.value.offSeasonPrice)
  }

  // 默认基础票价
  return base
})

const totalPrice = computed(() => {
  return unitPrice.value * bookingForm.value.ticketCount
})

const hasValidLocation = computed(() => {
  if (!spot.value) return false
  const lat = Number(spot.value.latitude)
  const lng = Number(spot.value.longitude)
  return !Number.isNaN(lat) && !Number.isNaN(lng)
})

const fetchSpotDetail = async () => {
  try {
    const response = await api.get(endpoints.spots.detail(Number(route.params.id)))
    spot.value = response.data
    loading.value = false
    await nextTick()
    initOrUpdateMap()
  } catch (error) {
    console.error('Failed to fetch spot detail:', error)
  } finally {
    loading.value = false
  }
}

watch(
  () => [spot.value?.longitude, spot.value?.latitude],
  () => {
    if (!loading.value) {
      initOrUpdateMap()
    }
  }
)

const initOrUpdateMap = async () => {
  if (!hasValidLocation.value || !mapContainer.value) {
    return
  }

  const lat = Number(spot.value.latitude)
  const lng = Number(spot.value.longitude)

  await nextTick()

  if (!map) {
    mapLoading.value = true
    map = L.map(mapContainer.value, {
      zoomControl: false,
      attributionControl: false
    })

    const tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      minZoom: 3
    })

    tileLayer.on('load', () => (mapLoading.value = false))
    tileLayer.on('loading', () => (mapLoading.value = true))
    tileLayer.addTo(map)
  }

  mapReady.value = true
  map.setView([lat, lng], map.getZoom() || 8, { animate: true })

  if (marker) {
    marker.remove()
  }

  marker = L.circleMarker([lat, lng], {
    radius: 12,
    color: '#2563eb',
    weight: 3,
    fillColor: '#60a5fa',
    fillOpacity: 0.7
  }).addTo(map)

  marker.bindPopup(`<strong>${spot.value.name || '目的地'}</strong><br/>经度 ${lng}, 纬度 ${lat}`)
}

const recenterMap = () => {
  if (!map || !hasValidLocation.value) return
  const lat = Number(spot.value.latitude)
  const lng = Number(spot.value.longitude)
  map.flyTo([lat, lng], 9, { duration: 0.6 })
  marker?.openPopup()
}

onBeforeUnmount(() => {
  map?.remove()
  map = null
  marker = null
  clearCommentImagePreview()
})

const handleBooking = async () => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    router.push('/login')
    return
  }

  const user = JSON.parse(userStr)
  submitting.value = true

  try {
    await api.post('/bookings', {
      userId: user.id,
      spotId: spot.value.id,
      visitDate: bookingForm.value.visitDate,
      ticketCount: bookingForm.value.ticketCount
    })
    alert('预订成功！')
    router.push('/profile')
  } catch (error) {
    console.error('Booking failed:', error)
    alert('预订失败，请重试')
  } finally {
    submitting.value = false
  }
}

const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const comments = ref<any[]>([])
const submittingComment = ref(false)
const commentForm = ref({
  content: '',
  rating: 5
})
const commentImageFile = ref<File | null>(null)
const commentImagePreview = ref('')
const commentImageFileName = computed(() => commentImageFile.value?.name || '')

const fetchComments = async () => {
  try {
    const response = await api.get(endpoints.comments.list(Number(route.params.id)))
    comments.value = response.data
    
    // 如果用户已登录，检查每条评论的点赞状态
    if (user.value) {
      for (const comment of comments.value) {
        try {
          const likedResponse = await api.get(`/comments/${comment.id}/liked?userId=${user.value.id}`)
          comment.liked = likedResponse.data.liked
        } catch (error) {
          console.error('Failed to check liked status:', error)
          comment.liked = false
        }
      }
    }
  } catch (error) {
    console.error('Failed to fetch comments:', error)
  }
}

function clearCommentImagePreview() {
  if (commentImagePreview.value) {
    URL.revokeObjectURL(commentImagePreview.value)
    commentImagePreview.value = ''
  }
}

function removeSelectedCommentImage() {
  commentImageFile.value = null
  const input = document.getElementById('comment-image-input') as HTMLInputElement | null
  if (input) {
    input.value = ''
  }
  clearCommentImagePreview()
}

function handleCommentImageChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {
    removeSelectedCommentImage()
    return
  }

  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB')
    target.value = ''
    return
  }

  commentImageFile.value = file
  clearCommentImagePreview()
  commentImagePreview.value = URL.createObjectURL(file)
}

const submitComment = async () => {
  if (!commentForm.value.content.trim()) return
  
  submittingComment.value = true
  try {
    let uploadedImageUrl: string | null = null
    if (commentImageFile.value) {
      const formData = new FormData()
      formData.append('file', commentImageFile.value)
      const uploadResponse = await api.post(endpoints.comments.uploadImage, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      uploadedImageUrl = uploadResponse.data.imageUrl
    }

    await api.post(endpoints.comments.create, {
      userId: user.value.id,
      spotId: spot.value.id,
      content: commentForm.value.content,
      rating: commentForm.value.rating,
      imageUrl: uploadedImageUrl
    })
    
    // Reset form and refresh list
    commentForm.value.content = ''
    commentForm.value.rating = 5
    removeSelectedCommentImage()
    await fetchComments()
    alert('评论发布成功！')
  } catch (error) {
    console.error('Failed to submit comment:', error)
    alert('评论失败，请重试')
  } finally {
    submittingComment.value = false
  }
}

const toggleLike = async (comment: any) => {
  if (!user.value) {
    router.push('/login')
    return
  }
  
  try {
    const response = await api.post(`/comments/${comment.id}/like`, {
      userId: user.value.id
    })
    
    comment.liked = response.data.liked
    comment.likeCount = response.data.likeCount
  } catch (error) {
    console.error('Failed to toggle like:', error)
    alert('操作失败，请重试')
  }
}

const openImageModal = (imageUrl: string) => {
  window.open(imageUrl, '_blank')
}

const handleCommentImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%23e5e7eb" width="400" height="300"/%3E%3Ctext x="50%25" y="50%25" font-family="Arial" font-size="16" fill="%239ca3af" text-anchor="middle" dy=".3em"%3E图片加载失败%3C/text%3E%3C/svg%3E'
}

const getGradientClass = (spot: any) => {
  if (!spot) return 'bg-gradient-to-br from-blue-500 via-blue-600 to-indigo-700'
  
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

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchSpotDetail()
  fetchComments()
})
</script>
