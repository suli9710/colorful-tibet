<template>
  <div class="min-h-screen tibet-page-shell">
    <!-- Hero Section: Multi-layer Parallax -->
    <div ref="heroRef" class="relative h-[calc(100vh-5rem)] md:h-[calc(100vh-6rem)] flex items-center justify-center overflow-hidden -mt-20 md:-mt-24">
      <!-- Layer 0: Sky gradient base -->
      <div class="absolute inset-0 z-0 bg-gradient-to-b from-tibet-dark via-tibet-brown/60 to-tibet-dark/40"></div>

      <!-- Layer 1: Background image -->
      <AnimatePresence :initial="false">
        <motion.img
          :key="heroSlides[currentSlide].image"
          :src="heroSlides[currentSlide].image"
          :alt="heroSlides[currentSlide].title"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: 0.6 }"
          :exit="{ opacity: 0 }"
          :transition="{ duration: 1.05, ease: [0.16, 1, 0.3, 1] }"
          class="absolute inset-0 w-full h-full object-cover z-1 will-change-transform"
          :style="{ y: heroBgY, scale: heroBgScale, transformOrigin: 'center center' }"
        />
      </AnimatePresence>

      <!-- Layer 2: Distant mountains (slowest parallax) -->
      <motion.div class="hero-mountains-far z-2" :style="{ y: heroFarY }" style="bottom: 20%; height: 40%;"></motion.div>

      <!-- Layer 3: Mid-ground mountains with snow -->
      <motion.div class="hero-mountains-mid z-3" :style="{ y: heroMidY }" style="bottom: 10%; height: 50%;"></motion.div>

      <!-- Layer 4: Foreground terrain -->
      <motion.div class="hero-foreground z-4" :style="{ y: heroFrontY }" style="bottom: 0; height: 30%;"></motion.div>

      <!-- Layer 5: Golden light -->
      <motion.div
        class="hero-golden-light z-5"
        :animate="prefersReducedMotion ? { opacity: 0.68, scale: 1 } : { opacity: [0.55, 0.95, 0.65], scale: [1, 1.05, 1] }"
        :transition="prefersReducedMotion ? { duration: 0 } : { duration: 9, repeat: Infinity, ease: 'easeInOut' }"
      ></motion.div>

      <!-- Layer 6: Floating mist -->
      <div class="hero-mist z-6"></div>

      <!-- Layer 7: Prayer flags -->
      <div class="hero-prayer-flags z-7"></div>

      <!-- Layer 8: Content overlay -->
      <motion.div class="relative z-10 text-center px-4 max-w-5xl mx-auto" :style="{ y: heroContentY, opacity: heroContentOpacity }">
        <AnimatePresence mode="wait">
          <motion.div
            :key="`hero-content-${currentSlide}`"
            :initial="{ opacity: 0, y: 28, filter: 'blur(8px)' }"
            :animate="{ opacity: 1, y: 0, filter: 'blur(0px)' }"
            :exit="{ opacity: 0, y: -18, filter: 'blur(8px)' }"
            :transition="{ duration: 0.62, ease: [0.16, 1, 0.3, 1] }"
          >
            <motion.div
              class="mb-6 inline-flex items-center gap-2 rounded-full bg-tibet-red/25 px-4 py-2 text-sm text-tibet-yellow backdrop-blur-md border border-tibet-gold/30 will-change-transform"
              :initial="heroItemInitial"
              :animate="heroItemAnimate"
              :transition="heroTransition(0.08)"
            >
              <span class="h-2 w-2 rounded-full bg-tibet-yellow animate-pulse-slow"></span>
              {{ heroSlides[currentSlide].tag }}
            </motion.div>
            <motion.h1
              class="text-5xl md:text-7xl font-bold text-white mb-6 tracking-tight will-change-transform font-display"
              style="text-shadow: 0 2px 24px rgba(0,0,0,0.3);"
              :initial="heroItemInitial"
              :animate="heroItemAnimate"
              :transition="heroTransition(0.18)"
            >
              {{ heroSlides[currentSlide].title }}
            </motion.h1>
            <motion.p
              class="text-xl md:text-2xl text-white/85 mb-10 font-light max-w-2xl mx-auto will-change-transform"
              style="text-shadow: 0 1px 12px rgba(0,0,0,0.2);"
              :initial="heroItemInitial"
              :animate="heroItemAnimate"
              :transition="heroTransition(0.32)"
            >
              {{ heroSlides[currentSlide].subtitle }}
            </motion.p>
            <motion.div
              class="flex flex-col sm:flex-row justify-center gap-4 will-change-transform"
              :initial="heroItemInitial"
              :animate="heroItemAnimate"
              :transition="heroTransition(0.48)"
            >
              <motion.div :whileHover="{ y: -3, scale: 1.03 }" :whilePress="{ scale: 0.98 }">
                <router-link to="/spots" class="tibet-btn text-lg px-8 py-4 shadow-xl will-change-transform">
                  {{ t('home.startExploring') }}
                </router-link>
              </motion.div>
              <motion.button
                      @click="scrollToHeatmap"
                      :whileHover="{ y: -3, scale: 1.03 }"
                      :whilePress="{ scale: 0.98 }"
                      class="tibet-btn-ghost text-white border-white/30 hover:bg-white/10 hover:border-white/50 hover:text-white text-lg px-8 py-4 will-change-transform">
                {{ t('home.viewHeatmap') }}
              </motion.button>
            </motion.div>
          </motion.div>
        </AnimatePresence>

        <!-- Bead-style carousel dots -->
        <div class="mt-10 flex items-center justify-center gap-2.5">
          <motion.button v-for="(slide, index) in heroSlides" :key="`${slide.image}-${index}`" @click="goToSlide(index)"
                  class="tibet-carousel-dot"
                  layout
                  :animate="{ width: currentSlide === index ? 28 : 8, opacity: currentSlide === index ? 1 : 0.68 }"
                  :whileHover="{ scale: 1.18 }"
                  :whilePress="{ scale: 0.85 }"
                  :transition="{ type: 'spring', stiffness: 420, damping: 28 }"
                  :class="{ active: currentSlide === index }"
                  :aria-label="t('home.carouselDot', { index: index + 1 })"></motion.button>
        </div>
      </motion.div>

      <motion.div
        v-if="!prefersReducedMotion"
        class="absolute bottom-12 left-1/2 z-10 hidden -translate-x-1/2 flex-col items-center gap-2 text-xs text-white/70 md:flex"
        :animate="{ y: [0, 8, 0], opacity: [0.55, 1, 0.55] }"
        :transition="{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }"
      >
        <span class="h-10 w-px bg-gradient-to-b from-white/70 to-transparent"></span>
      </motion.div>
    </div>

    <!-- Mountain divider -->
    <div class="tibet-mountain-divider -mt-8 relative z-10"></div>

    <!-- Heatmap Section -->
    <div id="heatmap" class="py-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <motion.div
        class="text-center mb-16"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <h2 class="tibet-heading text-4xl font-bold text-tibet-dark mb-4 tibetan-font">{{ t('home.hotSpotsDistribution') }}</h2>
        <p class="text-lg text-tibet-brown/70 tibetan-font">{{ t('home.hotSpotsDescription') }}</p>
      </motion.div>
      
      <motion.div
        class="rounded-3xl p-6 shadow-2xl tibet-panel"
        :initial="cardInitial"
        :whileInView="cardInView"
        :whileHover="{ y: -4, boxShadow: '0 22px 60px rgba(92, 61, 46, 0.14)' }"
        :inViewOptions="inViewOnce"
        :transition="cardTransition(0, 0.08)"
        @viewportEnter="heatmapMounted = true"
      >
        <HeatMap v-if="heatmapMounted" />
        <div v-else class="flex h-[600px] items-center justify-center rounded-2xl bg-white/70">
          <div class="tibet-spinner"></div>
        </div>
      </motion.div>
    </div>

    <!-- Recommendations Section -->
    <div class="py-24 tibet-section-band">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <motion.div
          class="flex justify-between items-end mb-12"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <div>
            <h2 class="tibet-heading text-4xl font-bold text-tibet-dark mb-2 tibetan-font">{{ t('home.recommendations') }}</h2>
            <p class="text-lg text-tibet-brown/70 tibetan-font">{{ t('home.recommendationsDescription') }}</p>
          </div>
          <router-link to="/spots" class="hidden md:flex items-center text-tibet-red hover:text-tibet-red/80 font-medium transition-colors tibetan-font">
            {{ t('common.viewAll') }}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 ml-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
            </svg>
          </router-link>
        </motion.div>

        <div v-if="loading" class="flex justify-center py-20">
          <motion.div
            class="rounded-full h-12 w-12 border-b-2 border-tibet-gold"
            :animate="{ rotate: 360 }"
            :transition="{ duration: 0.9, ease: 'linear', repeat: Infinity }"
          />
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <AnimatePresence mode="popLayout">
          <motion.div v-for="(spot, index) in recommendedSpots" :key="spot.id"
               layout
               class="group tibet-card-elevated overflow-hidden gpu-accelerated"
               :initial="cardInitial"
               :whileInView="cardInView"
               :exit="cardExit"
               :inViewOptions="inViewOnce"
               :transition="cardTransition(index)"
               :whileHover="{ y: -4, scale: 1.01 }"
               :whilePress="{ scale: 0.998 }">
            <div class="relative h-72 overflow-hidden">
              <motion.img :src="spot.imageUrl" :alt="spot.name"
                   class="w-full h-full object-cover tibet-image-hover img-fade-in will-change-transform"
                   loading="lazy"
                   :whileHover="{ scale: 1.08 }"
                   :transition="{ duration: 0.65, ease: [0.16, 1, 0.3, 1] }">
              </motion.img>
              <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/15 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 ease-out-expo"></div>
              <motion.div
                class="absolute left-0 top-0 h-full w-1/2 bg-gradient-to-r from-transparent via-white/20 to-transparent opacity-0 mix-blend-screen group-hover:opacity-100"
                :initial="{ x: '-120%' }"
                :whileHover="{ x: '220%' }"
                :transition="{ duration: 0.85, ease: [0.16, 1, 0.3, 1] }"
              ></motion.div>
              <motion.div
                class="absolute top-4 right-4 bg-tibet-white/95 backdrop-blur-md px-3 py-1.5 rounded-full text-xs font-bold text-tibet-red shadow-lg will-change-transform"
                :whileHover="{ scale: 1.08, rotate: 1 }"
              >
                {{ spot.category === 'NATURAL' ? t('home.natural') : t('home.cultural') }}
              </motion.div>
            </div>

              <div class="p-8">
              <div class="flex justify-between items-start mb-4">
                <h3 class="text-2xl font-bold text-tibet-dark group-hover:text-tibet-red transition-colors duration-300 ease-out-expo font-display">{{ spot.name }}</h3>
                <span class="text-lg font-semibold text-tibet-red transform group-hover:scale-105 transition-transform duration-300 ease-out-expo will-change-transform">¥{{ spot.ticketPrice }}</span>
              </div>
              <p v-if="getRecommendationReason(spot.id)" class="text-xs text-tibet-gold mb-3 font-medium tibetan-font">
                💡 {{ getRecommendationReason(spot.id) }}
              </p>
              <p class="text-tibet-brown/70 mb-6 line-clamp-2 leading-relaxed tibetan-font">{{ spot.description }}</p>

              <div class="flex items-center justify-between pt-6 border-t border-tibet-gold/20">
                <div class="flex space-x-2">
                  <motion.span v-for="(tag, tagIndex) in spot.tags?.slice(0, 2)" :key="tag.id"
                        class="tibet-tag tibetan-font">
                    {{ tag.tag }}
                  </motion.span>
                </div>
                <motion.button @click="router.push(`/spots/${spot.id}`)"
                        :whileHover="{ x: 3 }"
                        :whilePress="{ scale: 0.96 }"
                        class="tibet-link text-sm flex items-center group/btn tibetan-font">
                  {{ t('common.book') }}
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1 transform group-hover/btn:translate-x-2 transition-transform duration-300 ease-out-expo will-change-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                  </svg>
                </motion.button>
              </div>
            </div>
          </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, ref, onMounted, onUnmounted, watch } from 'vue'
import { AnimatePresence, motion, useReducedMotion, useScroll, useSpring, useTransform } from 'motion-v'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api, { endpoints } from '../api'
import {
  cardExit,
  cardInitial,
  cardInView,
  cardTransition,
  inViewOnce,
  motionEase,
  revealInitial,
  revealInView,
  revealTransition
} from '../motion/presets'

const HeatMap = defineAsyncComponent(() => import('../components/HeatMap.vue'))
const heroItemInitial = { opacity: 0, y: 24, scale: 0.98 }
const heroItemAnimate = { opacity: 1, y: 0, scale: 1 }

const heroTransition = (delay = 0) => ({
  duration: 0.7,
  delay,
  ease: motionEase
})

const router = useRouter()
const { t, locale } = useI18n()
const prefersReducedMotion = useReducedMotion()
const heroRef = ref<HTMLElement | null>(null)
const { scrollYProgress: heroScrollProgress } = useScroll({
  target: heroRef,
  offset: ['start start', 'end start']
})
const heroProgress = useSpring(heroScrollProgress, {
  stiffness: 140,
  damping: 32,
  mass: 0.2
})
const heroBgY = useTransform(heroProgress, [0, 1], ['0px', '180px'])
const heroBgScale = useTransform(heroProgress, [0, 1], [1.05, 1.16])
const heroFarY = useTransform(heroProgress, [0, 1], ['0px', '42px'])
const heroMidY = useTransform(heroProgress, [0, 1], ['0px', '96px'])
const heroFrontY = useTransform(heroProgress, [0, 1], ['0px', '150px'])
const heroContentY = useTransform(heroProgress, [0, 1], ['0px', '-78px'])
const heroContentOpacity = useTransform(heroProgress, [0, 0.72], [1, 0])
const recommendedSpots = ref<any[]>([])
const recommendationReasons = ref<Map<number, string>>(new Map())
const loading = ref(true)
const heatmapMounted = ref(false)
const currentSlide = ref(0)
const heroSlides = ref<Array<{ image: string; title: string; subtitle: string; tag: string; linkUrl?: string }>>([
  { image: '/heritage/布达拉宫3.jpg', title: '', subtitle: '', tag: '' }
])
const fallbackRecommendedSpots = [
  {
    id: 1,
    name: '布达拉宫',
    imageUrl: '/heritage/布达拉宫3.jpg',
    category: 'CULTURAL',
    ticketPrice: 200,
    description: '拉萨城市天际线中最具代表性的宫堡建筑，也是藏地历史、宗教与建筑艺术的集中呈现。',
    tags: [
      { id: 'fallback-potala-1', tag: '世界遗产' },
      { id: 'fallback-potala-2', tag: '文化地标' }
    ]
  },
  {
    id: 2,
    name: '纳木错',
    imageUrl: '/heritage/纳木错.jpg',
    category: 'NATURAL',
    ticketPrice: 120,
    description: '高原湖泊、雪山和辽阔草甸交织的经典路线，适合把西藏的空间感慢慢看进去。',
    tags: [
      { id: 'fallback-namco-1', tag: '圣湖' },
      { id: 'fallback-namco-2', tag: '自然风光' }
    ]
  },
  {
    id: 3,
    name: '雅鲁藏布大峡谷',
    imageUrl: '/heritage/雅鲁藏布大峡谷.jpg',
    category: 'NATURAL',
    ticketPrice: 150,
    description: '峡谷、江流与南迦巴瓦峰同框出现，层次强烈，适合深度旅行和摄影。',
    tags: [
      { id: 'fallback-canyon-1', tag: '峡谷' },
      { id: 'fallback-canyon-2', tag: '摄影' }
    ]
  }
]
let slideTimer: number | null = null

const fetchCarousels = async () => {
  try {
    const response = await api.get(endpoints.carousels.list)
    if (response.data && response.data.length > 0) {
      heroSlides.value = response.data.map((c: any) => ({
        image: c.imageUrl,
        title: c.title,
        subtitle: c.subtitle || '',
        tag: c.tag || '',
        linkUrl: c.linkUrl || ''
      }))
    }
  } catch (e) {
    // Fall back to defaults
  }
}

const scrollToHeatmap = () => {
  document.getElementById('heatmap')?.scrollIntoView({
    behavior: prefersReducedMotion.value ? 'auto' : 'smooth'
  })
}

const goToSlide = (index: number) => {
  currentSlide.value = index
}

const startCarousel = () => {
  stopCarousel()
  if (prefersReducedMotion.value || heroSlides.value.length < 2) return

  slideTimer = window.setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % heroSlides.value.length
  }, 4000)
}

const stopCarousel = () => {
  if (slideTimer !== null) {
    window.clearInterval(slideTimer)
    slideTimer = null
  }
}

const applyFallbackRecommendations = () => {
  recommendedSpots.value = fallbackRecommendedSpots
  recommendationReasons.value = new Map(
    fallbackRecommendedSpots.map((spot) => [spot.id, t('home.selectedForYou')])
  )
}

// 获取推荐原因（处理类型转换）
const getRecommendationReason = (spotId: number) => {
  if (!spotId) return null
  // 尝试多种可能的 key 类型
  const reason = recommendationReasons.value.get(spotId) || 
                 recommendationReasons.value.get(Number(spotId))
  // 如果还是没有，返回默认原因
  return reason || t('home.recommendationReason')
}

const fetchRecommendations = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)

      // 并行请求：常规推荐 + debug 详情
      const [recommendationRes, debugRes] = await Promise.all([
        api.get(`${endpoints.spots.recommendations}?userId=${user.id}`),
        api.get(`${endpoints.spots.recommendationsDebug}?userId=${user.id}`).catch(() => null)
      ])

      recommendedSpots.value = Array.isArray(recommendationRes.data) ? recommendationRes.data : []

      if (!recommendedSpots.value.length) {
        applyFallbackRecommendations()
        return
      }

      // 使用后端返回的推荐原因
      if (debugRes && debugRes.data && debugRes.data.recommendationReasons) {
        const reasonsMap = new Map<number, string>()
        Object.entries(debugRes.data.recommendationReasons).forEach(([spotId, reason]) => {
          reasonsMap.set(Number(spotId), reason as string)
        })
        recommendationReasons.value = reasonsMap
      } else {
        const reasonsMap = new Map<number, string>()
        recommendedSpots.value.forEach((spot: any) => {
          if (spot.rating && spot.rating >= 4.0) {
            reasonsMap.set(spot.id, t('home.highRatingSpot'))
          } else if (spot.visitCount && spot.visitCount > 15000) {
            reasonsMap.set(spot.id, t('home.popularSpot'))
          } else {
            reasonsMap.set(spot.id, t('home.selectedForYou'))
          }
        })
        recommendationReasons.value = reasonsMap
      }
    } else {
      const response = await api.get(endpoints.spots.list)
      const spots = response.data?.content || response.data || []
      recommendedSpots.value = spots.slice(0, 3)
      if (!recommendedSpots.value.length) {
        applyFallbackRecommendations()
        return
      }
      const defaultReasons = new Map<number, string>()
      recommendedSpots.value.forEach((spot: any) => {
        defaultReasons.set(spot.id, t('home.popularSpot'))
      })
      recommendationReasons.value = defaultReasons
    }
  } catch (error) {
    console.warn('Using fallback recommendations after request failed:', error)
    applyFallbackRecommendations()
  } finally {
    loading.value = false
  }
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  fetchRecommendations()
})

watch(prefersReducedMotion, () => {
  startCarousel()
})

onMounted(async () => {
  await fetchCarousels()
  await fetchRecommendations()
  startCarousel()
})

onUnmounted(() => {
  stopCarousel()
})
</script>
