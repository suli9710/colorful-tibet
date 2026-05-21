<template>
  <div class="min-h-screen bg-tibet-white">
    <div v-if="loading" class="flex justify-center items-center h-screen">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold"></div>
    </div>

    <div v-else-if="spot" class="relative">
      <!-- Immersive Header Image -->
      <motion.div
        class="relative h-[60vh] w-full overflow-hidden bg-gray-200"
        :initial="{ opacity: 0, scale: 1.02 }"
        :animate="{ opacity: 1, scale: 1 }"
        :transition="{ duration: 0.5, ease: motionEase }"
      >
        <img v-if="hasSpotImage"
             :src="spot.imageUrl"
             :alt="spot.name"
             loading="eager"
             class="w-full h-full object-cover img-fade-in"
             @error="handleSpotImageError">
        <div v-else class="w-full h-full flex items-center justify-center" :class="getGradientClass(spot)">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-40 w-40 text-white opacity-30 animate-pulse-slow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path v-if="spot.category === 'NATURAL'" stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        </div>
        <div class="absolute inset-0 bg-gradient-to-t from-tibet-dark/80 via-transparent to-transparent"></div>
        
        <motion.div
          class="absolute bottom-0 left-0 w-full p-8 md:p-16 text-white"
          :initial="{ opacity: 0, y: 24 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.6, delay: 0.2, ease: motionEase }"
        >
          <div class="max-w-7xl mx-auto">
            <div class="flex items-center space-x-4 mb-4">
              <span class="px-4 py-1.5 bg-white/20 backdrop-blur-md rounded-full text-sm font-bold border border-white/30 tibetan-font">
                {{ spot.category === 'NATURAL' ? t('spotDetail.natural') : t('spotDetail.cultural') }}
              </span>
              <div class="flex space-x-2">
                <span v-for="tag in spot.tags" :key="tag.id" class="px-3 py-1 bg-black/30 backdrop-blur-sm rounded-full text-xs font-medium border border-white/10">
                  #{{ tag.tag }}
                </span>
              </div>
            </div>
            <h1 class="text-5xl md:text-6xl font-bold mb-4 tibetan-font">{{ spot.name }}</h1>
            <div class="flex items-center text-white/80 space-x-6">
              <span class="flex items-center tibetan-font">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                {{ t('spotDetail.tibetAutonomousRegion') }}
              </span>
              <span class="text-2xl font-bold text-tibet-gold">¥{{ unitPrice }}</span>
            </div>
          </div>
        </motion.div>
      </motion.div>

      <!-- Content Section -->
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 -mt-20 relative z-10">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-12">
          <!-- Left Column: Description -->
          <motion.div
            class="lg:col-span-2 space-y-8"
            :initial="revealInitial"
            :whileInView="revealInView"
            :inViewOptions="inViewOnce"
            :transition="{ duration: 0.5, delay: 0.1, ease: motionEase }"
          >
            <div class="bg-white rounded-3xl p-8 shadow-xl border border-tibet-gold/20">
              <h2 class="text-2xl font-bold text-tibet-dark mb-6 tibetan-font">{{ t('spotDetail.introduction') }}</h2>
              <p class="text-tibet-brown/80 leading-loose text-lg whitespace-pre-line tibetan-font">
                {{ spot.description }}
              </p>
            </div>

            <!-- Comments Section -->
            <div class="bg-white rounded-3xl p-8 shadow-xl border border-tibet-gold/20">
              <h2 class="text-2xl font-bold text-tibet-dark mb-6 tibetan-font">{{ t('spotDetail.comments') }}</h2>
              
              <!-- Comment Form -->
              <div v-if="user" class="mb-8 p-6 bg-gray-50 rounded-2xl">
                <h3 class="text-lg font-bold text-gray-800 mb-4 tibetan-font">{{ t('spotDetail.postComment') }}</h3>
                <div class="flex items-center mb-4">
                  <span class="mr-4 text-gray-600 tibetan-font">{{ t('spotDetail.rating') }}:</span>
                  <div class="flex space-x-1">
                    <button v-for="star in 5" :key="star" @click="commentForm.rating = star" 
                            class="text-2xl focus:outline-none transition-transform hover:scale-110"
                            :class="star <= commentForm.rating ? 'text-yellow-400' : 'text-gray-300'">
                      ★
                    </button>
                  </div>
                </div>
                <textarea v-model="commentForm.content" rows="3" 
                          class="w-full p-4 rounded-xl border border-tibet-gold/25 focus:ring-2 focus:ring-blue-100 focus:border-blue-400 outline-none transition-all duration-300 input-focus mb-3 resize-none tibetan-font"
                          :placeholder="t('spotDetail.shareExperience')"></textarea>
                <div class="mb-4">
                  <label class="block text-sm font-medium text-gray-600 mb-2 tibetan-font">{{ t('spotDetail.addPhoto') }}</label>
                  <div class="flex items-center space-x-4">
                    <label v-if="!commentImageFile" for="comment-image-input"
                           class="inline-flex items-center px-4 py-2 rounded-full bg-white border border-tibet-gold/25 text-sm font-medium text-gray-600 cursor-pointer hover:bg-blue-50 hover:text-blue-600 transition-colors tibetan-font">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 15a4 4 0 004 4h10a4 4 0 004-4m-4-8h-4m0 0V3m0 4l3-3m-3 3L9 4" />
                      </svg>
                      {{ t('spotDetail.selectImage') }}
                    </label>
                    <span v-if="uploadingCommentImage" class="text-sm text-blue-500 tibetan-font flex items-center">
                      <svg class="animate-spin w-4 h-4 mr-1" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                      </svg>
                      {{ t('spotDetail.uploading') }}
                    </span>
                    <span v-else-if="uploadedCommentImageUrl" class="text-sm text-green-600 tibetan-font flex items-center">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                      {{ t('spotDetail.imageUploaded') }}
                    </span>
                    <span v-else-if="commentImageFileName" class="text-sm text-gray-500 truncate max-w-[200px]">{{ commentImageFileName }}</span>
                    <span v-else class="text-sm text-gray-400 tibetan-font">{{ t('spotDetail.imageFormats') }}</span>
                  </div>
                  <input id="comment-image-input" type="file" accept="image/*" class="hidden" @change="handleCommentImageChange" :disabled="uploadingCommentImage">
                  <p class="text-xs text-gray-400 mt-1 tibetan-font">{{ t('spotDetail.imageSizeHint') }}</p>
                  <div v-if="commentImagePreview" class="mt-4 relative w-40 h-28">
                    <img :src="commentImagePreview" :alt="t('spotDetail.comments')" class="w-full h-full object-cover rounded-2xl border border-tibet-gold/20 shadow-sm">
                    <div v-if="uploadingCommentImage" class="absolute inset-0 bg-black/40 rounded-2xl flex items-center justify-center">
                      <svg class="animate-spin w-6 h-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                      </svg>
                    </div>
                    <button v-if="!uploadingCommentImage" type="button" @click="removeSelectedCommentImage"
                            class="absolute -top-2 -right-2 bg-white text-gray-500 hover:text-red-500 rounded-full p-1 shadow">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </div>
                <div class="mt-4 text-right">
                  <button @click="submitComment" :disabled="submittingComment || uploadingCommentImage"
                          class="bg-tibet-red text-tibet-yellow px-6 py-3 rounded-full font-semibold hover:bg-tibet-red/85 transition-all duration-300 transform hover:scale-105 hover:shadow-lg hover:shadow-tibet-red/25 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none tibetan-font">
                    {{ submittingComment ? t('spotDetail.submitting') : t('spotDetail.publishComment') }}
                  </button>
                </div>
              </div>
              <div v-else class="mb-8 p-6 bg-gray-50 rounded-2xl text-center">
                <p class="text-gray-500 tibetan-font">{{ t('spotDetail.loginToComment') }}</p>
                <router-link to="/login" class="inline-block mt-2 text-blue-600 hover:underline tibetan-font">{{ t('spotDetail.goToLogin') }}</router-link>
              </div>

              <!-- Comment List -->
              <div class="space-y-6">
                <div v-for="comment in comments" :key="comment.id" class="border-b border-tibet-gold/20 pb-6 last:border-0">
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center space-x-3">
                      <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold">
                        {{ getCommentAuthor(comment).charAt(0) }}
                      </div>
                      <div>
                        <p class="font-bold text-gray-900">{{ getCommentAuthor(comment) }}</p>
                        <p class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</p>
                      </div>
                    </div>
                    <div class="flex items-center gap-3">
                      <button
                        v-if="isOwnComment(comment)"
                        @click="deleteComment(comment)"
                        class="text-xs font-medium text-red-500 hover:text-red-700 transition-colors tibetan-font"
                      >
                        {{ t('common.delete') }}
                      </button>
                      <div class="flex text-yellow-400">
                        <span v-for="n in 5" :key="n">{{ n <= comment.rating ? '★' : '☆' }}</span>
                      </div>
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
                <div v-if="comments.length === 0" class="text-center text-gray-400 py-8 tibetan-font">
                  {{ t('spotDetail.noComments') }}
                </div>
              </div>
            </div>

            <!-- Interactive Map -->
            <div class="bg-white rounded-3xl p-8 shadow-xl border border-tibet-gold/20 overflow-hidden">
              <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                <div>
                  <h2 class="text-2xl font-bold text-tibet-dark tibetan-font">{{ t('spotDetail.location') }}</h2>
                  <p class="text-sm text-tibet-brown/70 mt-1 tibetan-font">{{ t('spotDetail.mapHint') }}</p>
                </div>
                <button
                  @click="recenterMap"
                  :disabled="!mapReady || !hasValidLocation"
                  class="inline-flex items-center justify-center px-4 py-2 rounded-2xl text-sm font-semibold transition-all tibetan-font"
                  :class="!mapReady || !hasValidLocation
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    : 'bg-blue-600/10 text-blue-600 hover:bg-blue-600/20'">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none"
                       stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364l-1.414 1.414M7.05 16.95l-1.414 1.414m0-11.314L7.05 7.05m10.607 10.607l1.414 1.414" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  {{ t('spotDetail.backToSpot') }}
                </button>
              </div>
              <div class="relative rounded-2xl overflow-hidden">
                <div v-if="!hasValidLocation" class="bg-tibet-gold/5 h-72 flex items-center justify-center text-tibet-brown/50 text-center px-6">
                  <div class="tibetan-font">
                    <p>{{ t('spotDetail.noLocationInfo') }}</p>
                    <p class="text-sm mt-2">{{ t('spotDetail.tryLater') }}</p>
                  </div>
                </div>
                <div v-else>
                  <div ref="mapContainer" class="h-72 w-full"></div>
                  <div
                    v-if="mapLoading"
                    class="absolute inset-0 bg-white/70 backdrop-blur-sm flex flex-col items-center justify-center text-tibet-brown/70 text-sm tibetan-font">
                    <svg class="animate-spin h-6 w-6 text-blue-500 mb-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                    </svg>
                    {{ t('spotDetail.mapLoading') }}
                  </div>
                </div>
              </div>
              <div class="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm text-tibet-brown/80">
                <div class="bg-tibet-white rounded-2xl px-4 py-3">
                  <p class="text-xs text-tibet-brown/50 tibetan-font">{{ t('spotDetail.longitude') }}</p>
                  <p class="font-semibold mt-1">{{ spot.longitude || '—' }}</p>
                </div>
                <div class="bg-tibet-white rounded-2xl px-4 py-3">
                  <p class="text-xs text-tibet-brown/50 tibetan-font">{{ t('spotDetail.latitude') }}</p>
                  <p class="font-semibold mt-1">{{ spot.latitude || '—' }}</p>
                </div>
                <div class="bg-tibet-white rounded-2xl px-4 py-3">
                  <p class="text-xs text-tibet-brown/50 tibetan-font">{{ t('spotDetail.altitude') }}</p>
                  <p class="font-semibold mt-1">{{ spot.altitude ? spot.altitude + ' m' : '—' }}</p>
                </div>
              </div>
            </div>
          </motion.div>

          <!-- Right Column: Booking Form -->
          <motion.div
            class="lg:col-span-1"
            :initial="revealInitial"
            :whileInView="revealInView"
            :inViewOptions="inViewOnce"
            :transition="{ duration: 0.5, delay: 0.25, ease: motionEase }"
          >
              <div class="sticky top-20">
              <div class="glass-card rounded-3xl p-8 border border-white/50">
                <h2 class="text-2xl font-bold text-tibet-dark mb-6 tibetan-font">{{ t('spotDetail.bookNow') }}</h2>
                
                <form @submit.prevent="handleBooking" class="space-y-6">
                  <div>
                    <label class="block text-sm font-medium text-tibet-dark/80 mb-2 tibetan-font">{{ t('spotDetail.visitDate') }}</label>
                    <input type="date" v-model="bookingForm.visitDate" required
                           class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 outline-none transition-all">
                  </div>

                  <div>
                    <label class="block text-sm font-medium text-tibet-dark/80 mb-2 tibetan-font">{{ t('spotDetail.ticketCount') }}</label>
                    <div class="flex items-center space-x-4">
                      <button type="button" @click="bookingForm.ticketCount > 1 && bookingForm.ticketCount--" 
                              class="w-10 h-10 rounded-full bg-tibet-gold/5 hover:bg-tibet-gold/15 flex items-center justify-center text-tibet-brown/80 transition-all duration-300 transform hover:scale-110 active:scale-95">
                        -
                      </button>
                      <span class="text-xl font-bold text-tibet-dark w-8 text-center transition-all duration-300">{{ bookingForm.ticketCount }}</span>
                      <button type="button" @click="bookingForm.ticketCount++" 
                              class="w-10 h-10 rounded-full bg-tibet-gold/5 hover:bg-tibet-gold/15 flex items-center justify-center text-tibet-brown/80 transition-all duration-300 transform hover:scale-110 active:scale-95">
                        +
                      </button>
                    </div>
                  </div>

                  <div class="pt-6 border-t border-tibet-gold/25">
                    <div class="flex justify-between items-center mb-6">
                      <span class="text-tibet-brown/80 tibetan-font">{{ t('spotDetail.totalAmount') }}</span>
                      <span class="text-3xl font-bold text-tibet-gold">¥{{ totalPrice }}</span>
                    </div>
                    
                    <button type="submit" :disabled="submitting"
                            class="w-full bg-tibet-red text-tibet-yellow font-bold py-4 px-6 rounded-2xl transition-all duration-300 transform hover:scale-[1.02] hover:-translate-y-0.5 active:scale-[0.98] shadow-lg hover:shadow-tibet-red/30 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none tibetan-font">
                      {{ submitting ? t('spotDetail.processing') : t('spotDetail.confirmPayment') }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  </div>

  <PaymentModal
    :show="showPaymentModal"
    :amount="totalPrice"
    recaptcha-action="booking"
    @close="showPaymentModal = false"
    @paid="handlePaymentConfirmed"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { motion } from 'motion-v'
import { motionEase, revealInitial, revealInView, inViewOnce } from '../motion/presets'
import api, { endpoints } from '../api'
import PaymentModal from '../components/PaymentModal.vue'
import { useBehaviorTracker } from '../composables/useBehaviorTracker'
import { useAuthStore } from '../stores/auth'
import type * as Leaflet from 'leaflet'

const { t, locale } = useI18n()

const { encodeBehaviorData, reset: resetBehavior } = useBehaviorTracker()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const spot = ref<any>(null)
const loading = ref(true)
const submitting = ref(false)
const spotImageFailed = ref(false)
const mapContainer = ref<HTMLElement | null>(null)
const mapReady = ref(false)
const mapLoading = ref(true)
let map: Leaflet.Map | null = null
let marker: Leaflet.CircleMarker | null = null
let leafletLoader: Promise<typeof Leaflet> | null = null

const loadLeaflet = async () => {
  if (!leafletLoader) {
    leafletLoader = Promise.all([
      import('leaflet'),
      import('leaflet/dist/leaflet.css')
    ]).then(([leaflet]) => leaflet)
  }

  return leafletLoader
}

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

const hasSpotImage = computed(() => Boolean(spot.value?.imageUrl) && !spotImageFailed.value)

const handleSpotImageError = () => {
  spotImageFailed.value = true
}

const hasValidLocation = computed(() => {
  if (!spot.value) return false
  const lat = Number(spot.value.latitude)
  const lng = Number(spot.value.longitude)
  return !Number.isNaN(lat) && !Number.isNaN(lng)
})

const fetchSpotDetail = async () => {
  try {
    const response = await api.get(endpoints.spots.detail(Number(route.params.id)))
    spotImageFailed.value = false
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

// WGS-84 → GCJ-02 坐标转换（国内地图需要）
const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

const outOfChina = (lng: number, lat: number): boolean =>
  lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271

const transformLat = (x: number, y: number): number => {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
  return ret
}

const transformLon = (x: number, y: number): number => {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

const wgs84ToGcj02 = (lng: number, lat: number): [number, number] => {
  if (outOfChina(lng, lat)) return [lng, lat]
  const dlat = transformLat(lng - 105.0, lat - 35.0)
  const dlng = transformLon(lng - 105.0, lat - 35.0)
  const radlat = lat / 180.0 * PI
  let magic = Math.sin(radlat)
  magic = 1 - EE * magic * magic
  const sqrtmagic = Math.sqrt(magic)
  const mglat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
  const mglng = (dlng * 180.0) / (A / sqrtmagic * Math.cos(radlat) * PI)
  return [lng + mglng, lat + mglat]
}

const initOrUpdateMap = async () => {
  if (!hasValidLocation.value || !mapContainer.value) {
    return
  }

  const wgsLat = Number(spot.value.latitude)
  const wgsLng = Number(spot.value.longitude)
  // 转为 GCJ-02 以匹配高德地图瓦片
  const [lng, lat] = wgs84ToGcj02(wgsLng, wgsLat)

  await nextTick()
  const L = await loadLeaflet()

  if (!map) {
    mapLoading.value = true
    map = L.map(mapContainer.value, {
      zoomControl: false,
      attributionControl: false
    })

    // 高德地图瓦片（国内加载快，无需 API Key）
    const tileLayer = L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      maxZoom: 18,
      minZoom: 3,
      subdomains: '1234'
    })

    let tilesLoaded = false
    tileLayer.on('load', () => {
      tilesLoaded = true
      mapLoading.value = false
    })
    tileLayer.on('loading', () => {
      mapLoading.value = true
    })
    tileLayer.on('tileerror', () => {
      mapLoading.value = false
    })
    tileLayer.addTo(map)

    // 8 秒超时兜底
    setTimeout(() => {
      if (!tilesLoaded) {
        mapLoading.value = false
      }
    }, 8000)
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

  marker.bindPopup(`<strong>${spot.value.name || t('spotDetail.location')}</strong><br/>${t('spotDetail.longitude')} ${wgsLng}, ${t('spotDetail.latitude')} ${wgsLat}`)
}

const recenterMap = () => {
  if (!map || !hasValidLocation.value) return
  const wgsLat = Number(spot.value.latitude)
  const wgsLng = Number(spot.value.longitude)
  const [lng, lat] = wgs84ToGcj02(wgsLng, wgsLat)
  map.flyTo([lat, lng], 9, { duration: 0.6 })
  marker?.openPopup()
}

onBeforeUnmount(() => {
  map?.remove()
  map = null
  marker = null
  clearCommentImagePreview()
})

const showPaymentModal = ref(false)

const handleBooking = async () => {
  if (!(await auth.ensureSession())) {
    router.push('/login')
    return
  }
  if (!bookingForm.value.visitDate) {
    alert(t('spotDetail.pleaseSelectDate'))
    return
  }
  showPaymentModal.value = true
}

const handlePaymentConfirmed = async (recaptchaToken = '') => {
  showPaymentModal.value = false
  submitting.value = true
  const behaviorData = encodeBehaviorData()

  try {
    await api.post('/bookings', {
      spotId: spot.value.id,
      visitDate: bookingForm.value.visitDate,
      ticketCount: bookingForm.value.ticketCount
    }, {
      headers: {
        ...(recaptchaToken ? { 'X-Recaptcha-Token': recaptchaToken } : {}),
        ...(behaviorData ? { 'X-Behavior-Data': behaviorData } : {}),
      }
    })
    alert(t('spotDetail.bookingSuccess'))
    router.push('/profile')
  } catch (error) {
    console.error('Booking failed:', error)
    alert(t('spotDetail.bookingFailed'))
  } finally {
    submitting.value = false
    resetBehavior()
  }
}

const user = computed(() => auth.user)
const comments = ref<any[]>([])
const submittingComment = ref(false)
const commentForm = ref({
  content: '',
  rating: 5
})
const commentImageFile = ref<File | null>(null)
const commentImagePreview = ref('')
const uploadedCommentImageUrl = ref<string | null>(null)
const uploadingCommentImage = ref(false)
const commentImageFileName = computed(() => commentImageFile.value?.name || '')

const fetchComments = async () => {
  try {
    const response = await api.get(endpoints.comments.list(Number(route.params.id)))
    comments.value = response.data?.content || response.data || []
    
    // 如果用户已登录，检查每条评论的点赞状态
    if (user.value) {
      for (const comment of comments.value) {
        try {
          const likedResponse = await api.get(`/comments/${comment.id}/liked`)
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
  uploadedCommentImageUrl.value = null
  const input = document.getElementById('comment-image-input') as HTMLInputElement | null
  if (input) {
    input.value = ''
  }
  clearCommentImagePreview()
}

async function handleCommentImageChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {
    removeSelectedCommentImage()
    return
  }

  if (!file.type.startsWith('image/')) {
    alert(t('spotDetail.selectImageFile'))
    target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    alert(t('spotDetail.imageTooLarge'))
    target.value = ''
    return
  }

  commentImageFile.value = file
  uploadedCommentImageUrl.value = null
  clearCommentImagePreview()
  commentImagePreview.value = URL.createObjectURL(file)

  // Upload immediately after selection
  uploadingCommentImage.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadResponse = await api.post(endpoints.comments.uploadImage, formData)
    uploadedCommentImageUrl.value = uploadResponse.data.imageUrl
  } catch (error) {
    console.error('Failed to upload image:', error)
    alert(t('spotDetail.commentFailed'))
    removeSelectedCommentImage()
  } finally {
    uploadingCommentImage.value = false
  }
}

const submitComment = async () => {
  if (!commentForm.value.content.trim()) return

  if (!(await auth.ensureSession())) {
    router.push('/login')
    return
  }

  submittingComment.value = true
  try {
    await api.post(endpoints.comments.create, {
      spotId: spot.value.id,
      content: commentForm.value.content,
      rating: commentForm.value.rating,
      imageUrl: uploadedCommentImageUrl.value
    })
    
    // Reset form and refresh list
    commentForm.value.content = ''
    commentForm.value.rating = 5
    removeSelectedCommentImage()
    await fetchComments()
    alert(t('spotDetail.commentSuccess'))
  } catch (error) {
    console.error('Failed to submit comment:', error)
    alert(t('spotDetail.commentFailed'))
  } finally {
    submittingComment.value = false
  }
}

const getCommentAuthor = (comment: any) => {
  return comment.user?.nickname || comment.user?.username || comment.nickname || comment.username || t('routeDetail.anonymous')
}

const isOwnComment = (comment: any) => {
  if (!user.value) return false
  return Number(comment.user?.id || comment.userId) === Number(user.value.id) || comment.username === user.value.username
}

const deleteComment = async (comment: any) => {
  if (!confirm(t('spotDetail.confirmDeleteComment'))) return

  try {
    await api.delete(endpoints.comments.delete(comment.id))
    comments.value = comments.value.filter(item => item.id !== comment.id)
  } catch (error) {
    console.error('Failed to delete comment:', error)
    alert(t('spotDetail.deleteCommentFailed'))
  }
}

const toggleLike = async (comment: any) => {
  if (!(await auth.ensureSession())) {
    router.push('/login')
    return
  }
  
  try {
    const response = await api.post(`/comments/${comment.id}/like`)
    
    comment.liked = response.data.liked
    comment.likeCount = response.data.likeCount
  } catch (error) {
    console.error('Failed to toggle like:', error)
    alert(t('spotDetail.operationFailed'))
  }
}

const openImageModal = (imageUrl: string) => {
  window.open(imageUrl, '_blank')
}

const handleCommentImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  const errorText = encodeURIComponent(t('spotDetail.imageLoadFailed'))
  img.src = `data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%23e5e7eb" width="400" height="300"/%3E%3Ctext x="50%25" y="50%25" font-family="Arial" font-size="16" fill="%239ca3af" text-anchor="middle" dy=".3em"%3E${errorText}%3C/text%3E%3C/svg%3E`
}

const getGradientClass = (spot: any) => {
  if (!spot) return 'bg-gradient-to-br from-blue-500 via-blue-600 to-indigo-700'
  
  const gradients = [
    'bg-gradient-to-br from-tibet-blue via-tibet-dark to-tibet-red',
    'bg-gradient-to-br from-tibet-red via-tibet-gold to-tibet-yellow',
    'bg-gradient-to-br from-tibet-turquoise via-tibet-blue to-tibet-dark',
    'bg-gradient-to-br from-tibet-brown via-tibet-red to-tibet-gold',
    'bg-gradient-to-br from-tibet-dark via-tibet-blue to-tibet-turquoise',
    'bg-gradient-to-br from-tibet-gold via-tibet-yellow to-tibet-white'
  ]
  
  const index = spot.id % gradients.length
  return gradients[index]
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  fetchSpotDetail()
})

onMounted(async () => {
  fetchSpotDetail()
  await auth.refreshSession()
  fetchComments()
})
</script>
