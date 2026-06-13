<template>
  <div class="min-h-screen tibet-page-shell">
    <!-- Hero Section: Multi-layer Parallax -->
    <div class="relative flex h-[calc(100svh-5rem)] min-h-[520px] items-center justify-center overflow-hidden -mt-20 sm:min-h-[680px] md:h-[calc(100vh-6rem)] md:-mt-24">
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
          :transition="{ duration: 1.05, ease: motionEase }"
          class="absolute inset-0 w-full h-full object-cover z-1 scale-[1.05]"
        />
      </AnimatePresence>

      <!-- Layer 2: Distant mountains (slowest parallax) -->
      <motion.div class="hero-mountains-far z-2" style="bottom: 20%; height: 40%;"></motion.div>

      <!-- Layer 3: Mid-ground mountains with snow -->
      <motion.div class="hero-mountains-mid z-3" style="bottom: 10%; height: 50%;"></motion.div>

      <!-- Layer 4: Foreground terrain -->
      <motion.div class="hero-foreground z-4" style="bottom: 0; height: 30%;"></motion.div>

      <!-- Layer 5: Golden light -->
      <motion.div
        class="hero-golden-light z-5"
        :animate="prefersReducedMotion ? { opacity: 0.68, scale: 1 } : { opacity: [0.55, 0.95, 0.65], scale: [1, 1.05, 1] }"
        :transition="prefersReducedMotion ? { duration: 0 } : { duration: 9, repeat: Infinity, ease: 'easeInOut' }"
      ></motion.div>

      <!-- Layer 6: Floating mist -->
      <div class="hero-mist z-6"></div>

      <!-- Layer 7: Prayer-flag bunting strung across the mountain pass -->
      <div class="hero-prayer-flags pointer-events-none absolute inset-x-0 top-0 z-[7] px-1 sm:px-6">
        <PrayerFlags :count="17" />
      </div>

      <!-- Layer 8: Content overlay -->
      <div class="relative z-10 text-center px-4 max-w-5xl mx-auto">
        <div :key="`hero-content-${currentSlide}`" class="hero-content-fade">
            <div
              class="mb-5 inline-flex items-center gap-2 rounded-full bg-tibet-red/25 px-3.5 py-2 text-xs text-tibet-yellow backdrop-blur-md border border-tibet-gold/30 sm:mb-6 sm:px-4 sm:text-sm"
            >
              <span class="h-2 w-2 rounded-full bg-tibet-yellow animate-pulse-slow"></span>
              {{ heroSlides[currentSlide].tag }}
            </div>
            <h1
              class="mb-5 text-3xl font-bold leading-tight text-white sm:text-5xl md:mb-6 md:text-7xl font-display"
              style="text-shadow: 0 2px 24px rgba(0,0,0,0.3);"
            >
              {{ heroSlides[currentSlide].title }}
            </h1>
            <p
              class="mx-auto mb-8 max-w-2xl text-sm font-light leading-relaxed text-white/85 sm:text-xl md:mb-10 md:text-2xl"
              style="text-shadow: 0 1px 12px rgba(0,0,0,0.2);"
            >
              {{ heroSlides[currentSlide].subtitle }}
            </p>
            <div
              class="flex flex-col justify-center gap-3 sm:flex-row sm:gap-4"
            >
              <router-link to="/spots" class="tibet-btn w-full px-6 py-3.5 text-base shadow-xl will-change-transform hover:-translate-y-1 hover:scale-[1.03] active:scale-95 sm:w-auto sm:px-8 sm:py-4 sm:text-lg">
                {{ t('home.startExploring') }}
              </router-link>
              <button
                      @click="scrollToHeatmap"
                      class="tibet-btn-ghost w-full border-white/30 px-6 py-3.5 text-base text-white will-change-transform hover:-translate-y-1 hover:scale-[1.03] hover:border-white/50 hover:bg-white/10 hover:text-white active:scale-95 sm:w-auto sm:px-8 sm:py-4 sm:text-lg">
                {{ t('home.viewHeatmap') }}
              </button>
            </div>
        </div>

        <!-- Bead-style carousel dots -->
        <div class="mt-8 flex items-center justify-center gap-2.5 sm:mt-10">
          <button v-for="(slide, index) in heroSlides" :key="`${slide.image}-${index}`" type="button" @click="goToSlide(index)"
                  class="tibet-carousel-dot"
                  :style="{ width: currentSlide === index ? '28px' : '8px', opacity: currentSlide === index ? 1 : 0.68 }"
                  :class="{ active: currentSlide === index }"
                  :aria-current="currentSlide === index ? 'true' : undefined"
                  :aria-label="t('home.carouselDot', { index: index + 1 })"></button>
        </div>
      </div>

      <div
        v-if="!prefersReducedMotion"
        class="absolute bottom-12 left-1/2 z-10 hidden -translate-x-1/2 flex-col items-center gap-2 text-xs text-white/70 md:flex"
      >
        <span class="h-1.5 w-1.5 rounded-full bg-tibet-yellow shadow-[0_0_10px_rgba(242,201,76,0.7)] animate-bounce-subtle"></span>
        <span class="h-10 w-px bg-gradient-to-b from-white/70 to-transparent"></span>
      </div>
    </div>

    <!-- Mountain divider -->
    <div class="tibet-mountain-divider -mt-8 relative z-10"></div>

    <!-- Heatmap Section -->
    <div id="heatmap" class="mx-auto max-w-7xl px-4 py-14 sm:px-6 sm:py-20 lg:px-8 lg:py-24">
      <motion.div
        class="mb-10 text-center sm:mb-16"
        :initial="revealInitial"
        :whileInView="revealInView"
        :inViewOptions="inViewOnce"
        :transition="revealTransition"
      >
        <h2 class="tibet-heading mb-3 text-3xl font-bold text-tibet-dark sm:mb-4 sm:text-4xl tibetan-font">{{ t('home.hotSpotsDistribution') }}</h2>
        <p class="text-base text-tibet-brown/70 sm:text-lg tibetan-font">{{ t('home.hotSpotsDescription') }}</p>
      </motion.div>
      
      <motion.div
        class="rounded-2xl p-3 shadow-2xl sm:rounded-3xl sm:p-6 tibet-panel"
        :initial="cardInitial"
        :whileInView="cardInView"
        :whileHover="{ y: -4, boxShadow: '0 22px 60px rgba(92, 61, 46, 0.14)' }"
        :inViewOptions="inViewOnce"
        :transition="cardTransition(0, 0.08)"
        @viewportEnter="heatmapMounted = true"
      >
        <HeatMap v-if="heatmapMounted" />
        <div v-else class="flex h-[420px] items-center justify-center rounded-2xl bg-white/70 sm:h-[600px]" role="status" :aria-label="t('common.loading')">
          <div class="tibet-spinner"></div>
          <span class="sr-only">{{ t('common.loading') }}</span>
        </div>
      </motion.div>
    </div>

    <!-- Recommendations Section -->
    <div class="py-14 sm:py-20 lg:py-24 tibet-section-band">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <motion.div
          class="mb-10 flex flex-col gap-4 sm:mb-12 sm:flex-row sm:items-end sm:justify-between"
          :initial="revealInitial"
          :whileInView="revealInView"
          :inViewOptions="inViewOnce"
          :transition="revealTransition"
        >
          <div>
            <h2 class="tibet-heading mb-2 text-3xl font-bold text-tibet-dark sm:text-4xl tibetan-font">{{ t('home.recommendations') }}</h2>
            <p class="text-base text-tibet-brown/70 sm:text-lg tibetan-font">{{ t('home.recommendationsDescription') }}</p>
          </div>
          <router-link to="/spots" class="hidden md:flex items-center text-tibet-red hover:text-tibet-red/80 font-medium transition-colors tibetan-font">
            {{ t('common.viewAll') }}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 ml-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
            </svg>
          </router-link>
        </motion.div>

        <div
          v-if="recommendationNotice"
          class="mb-6 rounded-2xl border border-tibet-gold/20 bg-white/70 px-4 py-3 text-sm leading-6 text-tibet-brown/75 shadow-sm backdrop-blur tibetan-font"
          role="status"
        >
          {{ recommendationNotice }}
        </div>

        <div v-if="loading" role="status" :aria-label="t('home.recommendationsLoading')" class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3 lg:gap-8">
          <span class="sr-only">{{ t('home.recommendationsLoading') }}</span>
          <div v-for="i in 3" :key="i" class="overflow-hidden rounded-3xl border border-tibet-gold/20 bg-white/75 shadow-sm" aria-hidden="true">
            <div class="h-56 animate-pulse bg-tibet-gold/10 sm:h-72"></div>
            <div class="p-5 sm:p-8">
              <div class="mb-4 h-6 w-3/4 animate-pulse rounded bg-tibet-gold/10"></div>
              <div class="mb-2 h-4 animate-pulse rounded bg-tibet-gold/10"></div>
              <div class="mb-6 h-4 w-5/6 animate-pulse rounded bg-tibet-gold/10"></div>
              <div class="flex gap-2">
                <div class="h-6 w-16 animate-pulse rounded-full bg-tibet-gold/10"></div>
                <div class="h-6 w-16 animate-pulse rounded-full bg-tibet-gold/10"></div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="recommendedSpots.length === 0" class="rounded-3xl border border-tibet-gold/20 bg-white/75 p-8 text-center shadow-sm">
          <h3 class="mb-2 text-xl font-bold text-tibet-dark tibetan-font">{{ t('home.noRecommendationsTitle') }}</h3>
          <p class="mx-auto mb-5 max-w-xl text-sm leading-6 text-tibet-brown/70 tibetan-font">{{ t('home.noRecommendationsMessage') }}</p>
          <router-link to="/spots" class="inline-flex min-h-11 items-center justify-center rounded-full bg-tibet-dark px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-tibet-dark/90 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font">
            {{ t('common.viewAll') }}
          </router-link>
        </div>

        <div v-else class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3 lg:gap-8">
          <AnimatePresence mode="popLayout">
          <motion.div v-for="(spot, index) in recommendedSpots" :key="spot.id"
               layout
               class="group tibet-card-elevated overflow-hidden gpu-accelerated cursor-pointer focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-4"
               role="link"
               tabindex="0"
               :aria-label="t('spots.cardAria', { name: spot.name })"
               @click="goToSpot(spot)"
               @keydown.enter.prevent="goToSpot(spot)"
               @keydown.space.prevent="goToSpot(spot)"
               :initial="cardInitial"
               :whileInView="cardInView"
               :exit="cardExit"
               :inViewOptions="inViewOnce"
               :transition="cardTransition(index)"
               :whileHover="{ y: -4, scale: 1.01 }"
               :whilePress="{ scale: 0.998 }">
            <div class="relative h-56 overflow-hidden sm:h-72">
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

              <div class="p-5 sm:p-8">
              <div class="mb-4 flex items-start justify-between gap-3">
                <h3 class="min-w-0 break-words text-xl font-bold leading-tight text-tibet-dark transition-colors duration-300 ease-out-expo line-clamp-2 group-hover:text-tibet-red sm:text-2xl font-display">{{ spot.name }}</h3>
                <span class="max-w-[42%] shrink-0 break-words text-right text-base font-semibold leading-tight text-tibet-red transform transition-transform duration-300 ease-out-expo group-hover:scale-105 sm:text-lg will-change-transform">{{ formatPrice(spot.ticketPrice) }}</span>
              </div>
              <p v-if="getRecommendationReason(spot.id)" class="mb-3 break-words text-xs font-medium text-tibet-gold tibetan-font">
                💡 {{ getRecommendationReason(spot.id) }}
              </p>
              <p class="mb-6 break-words text-tibet-brown/70 line-clamp-2 leading-relaxed tibetan-font">{{ spot.description }}</p>

              <div class="flex flex-wrap items-center justify-between gap-3 border-t border-tibet-gold/20 pt-5 sm:pt-6">
                <div class="flex min-w-0 flex-wrap gap-2">
                  <motion.span v-for="(tag, tagIndex) in spot.tags?.slice(0, 2)" :key="tag.id"
                        class="tibet-tag max-w-full truncate tibetan-font">
                    {{ tag.tag }}
                  </motion.span>
                </div>
                <motion.button type="button" @click.stop="goToSpot(spot)"
                        :aria-label="t('spots.cardAria', { name: spot.name })"
                        :whileHover="{ x: 3 }"
                        :whilePress="{ scale: 0.96 }"
                        class="tibet-link inline-flex min-h-11 items-center text-sm group/btn tibetan-font focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2">
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
import { AnimatePresence, motion, useReducedMotion } from 'motion-v'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api, { endpoints } from '../api'
import PrayerFlags from '../components/PrayerFlags.vue'
import { useAuthStore } from '../stores/auth'
import { summarizeClientError } from '../utils/errorMonitoring'
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

const router = useRouter()
const { t, locale } = useI18n()
const auth = useAuthStore()
const prefersReducedMotion = useReducedMotion()

type SpotCategory = 'NATURAL' | 'CULTURAL'

interface SpotTag {
  id: string | number
  tag: string
}

interface ScenicSpot {
  id: number | string
  name: string
  imageUrl?: string
  category?: SpotCategory | string
  ticketPrice?: number | string | null
  description?: string
  rating?: number | string | null
  visitCount?: number | string | null
  tags?: SpotTag[]
}

interface HeroSlide {
  image: string
  title: string
  subtitle: string
  tag: string
  linkUrl?: string
}

interface CarouselResponse {
  imageUrl: string
  title: string
  subtitle?: string
  tag?: string
  linkUrl?: string
}

const recommendedSpots = ref<ScenicSpot[]>([])
const recommendationReasons = ref<Map<number, string>>(new Map())
const recommendationNotice = ref('')
const loading = ref(true)
const heatmapMounted = ref(false)
const currentSlide = ref(0)
const createDefaultHeroSlides = () => [
  {
    image: '/heritage/布达拉宫3.jpg',
    title: t('home.carousel1Title'),
    subtitle: t('home.carousel1Sub'),
    tag: t('home.carousel1Tag'),
    linkUrl: '/spots'
  },
  {
    image: '/heritage/纳木错.jpg',
    title: t('home.carousel2Title'),
    subtitle: t('home.carousel2Sub'),
    tag: t('home.carousel2Tag'),
    linkUrl: '/spots'
  },
  {
    image: '/heritage/藏戏.jpg',
    title: t('home.carousel3Title'),
    subtitle: t('home.carousel3Sub'),
    tag: t('home.carousel3Tag'),
    linkUrl: '/heritage'
  }
]
const heroSlides = ref<HeroSlide[]>(createDefaultHeroSlides())
const fallbackRecommendedSpots: ScenicSpot[] = [
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
      heroSlides.value = response.data.map((c: CarouselResponse) => ({
        image: c.imageUrl,
        title: c.title,
        subtitle: c.subtitle || '',
        tag: c.tag || '',
        linkUrl: c.linkUrl || ''
      }))
    } else {
      heroSlides.value = createDefaultHeroSlides()
    }
  } catch (e) {
    heroSlides.value = createDefaultHeroSlides()
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

const applyFallbackRecommendations = (notice = '') => {
  recommendedSpots.value = fallbackRecommendedSpots
  recommendationReasons.value = new Map(
    fallbackRecommendedSpots.map((spot) => [Number(spot.id), t('home.selectedForYou')])
  )
  recommendationNotice.value = notice
}

// 获取推荐原因（处理类型转换）
const getRecommendationReason = (spotId: ScenicSpot['id']) => {
  if (!spotId) return null
  // 尝试多种可能的 key 类型
  const reason = recommendationReasons.value.get(Number(spotId))
  // 如果还是没有，返回默认原因
  return reason || t('home.recommendationReason')
}

const formatNumber = (value: number | string | null | undefined) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const formatPrice = (price: ScenicSpot['ticketPrice']) => {
  const value = formatNumber(price)
  if (value == null) return t('common.pendingConfirm')
  if (value <= 0) return t('common.freeTicket')
  return t('common.priceCny', { price: value })
}

const goToSpot = (spot: ScenicSpot) => {
  if (spot?.id == null) {
    console.warn('推荐景点数据缺少 id，无法进入详情页:', spot)
    return
  }
  router.push(`/spots/${encodeURIComponent(String(spot.id))}`)
}

const fetchRecommendations = async () => {
  try {
    loading.value = true
    recommendationNotice.value = ''
    if (auth.hasValidSession()) {
      const recommendationRes = await api.get(endpoints.spots.recommendationsMe)

      recommendedSpots.value = Array.isArray(recommendationRes.data) ? recommendationRes.data : []

      if (!recommendedSpots.value.length) {
        applyFallbackRecommendations(t('home.noRecommendationsMessage'))
        return
      }

      const reasonsMap = new Map<number, string>()
      recommendedSpots.value.forEach((spot) => {
        const rating = formatNumber(spot.rating)
        const visitCount = formatNumber(spot.visitCount)
        if (rating != null && rating >= 4.0) {
          reasonsMap.set(Number(spot.id), t('home.highRatingSpot'))
        } else if (visitCount != null && visitCount > 15000) {
          reasonsMap.set(Number(spot.id), t('home.popularSpot'))
        } else {
          reasonsMap.set(Number(spot.id), t('home.selectedForYou'))
        }
      })
      recommendationReasons.value = reasonsMap
    } else {
      const response = await api.get(endpoints.spots.list)
      const spots = response.data?.content || response.data || []
      recommendedSpots.value = Array.isArray(spots) ? spots.slice(0, 3) : []
      if (!recommendedSpots.value.length) {
        applyFallbackRecommendations(t('home.noRecommendationsMessage'))
        return
      }
      const defaultReasons = new Map<number, string>()
      recommendedSpots.value.forEach((spot) => {
        defaultReasons.set(Number(spot.id), t('home.popularSpot'))
      })
      recommendationReasons.value = defaultReasons
    }
  } catch (error) {
    console.warn('Using fallback recommendations after request failed:', summarizeClientError(error))
    applyFallbackRecommendations(t('home.recommendationsFallbackMessage'))
  } finally {
    loading.value = false
  }
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  heroSlides.value = createDefaultHeroSlides()
  fetchRecommendations()
  fetchCarousels()
})

watch(prefersReducedMotion, () => {
  startCarousel()
})

onMounted(async () => {
  await auth.refreshSession()
  await Promise.allSettled([
    fetchCarousels(),
    fetchRecommendations()
  ])
  startCarousel()
})

onUnmounted(() => {
  stopCarousel()
})
</script>

<style scoped>
/* Drape the prayer-flag bunting like a string tied between two poles. */
.hero-prayer-flags {
  transform: rotate(-1.5deg);
  transform-origin: top center;
  filter: drop-shadow(0 6px 10px rgba(0, 0, 0, 0.25));
}

.hero-content-fade {
  animation: heroContentFade 320ms ease-out both;
}

@keyframes heroContentFade {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

.tibet-carousel-dot {
  transition: width 220ms ease, opacity 220ms ease, transform 160ms ease;
}

.tibet-carousel-dot:hover {
  transform: scale(1.18);
}

.tibet-carousel-dot:active {
  transform: scale(0.85);
}
</style>
