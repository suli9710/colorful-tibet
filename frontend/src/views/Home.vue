<template>
  <div class="min-h-screen bg-tibet-white">
    <!-- Hero Section: Multi-layer Parallax -->
    <div class="relative h-[calc(100vh-5rem)] md:h-[calc(100vh-6rem)] flex items-center justify-center overflow-hidden -mt-20 md:-mt-24">
      <!-- Layer 0: Sky gradient base -->
      <div class="absolute inset-0 z-0 bg-gradient-to-b from-tibet-dark via-tibet-brown/60 to-tibet-dark/40"></div>

      <!-- Layer 1: Background image -->
      <img :src="heroSlides[currentSlide].image" :alt="heroSlides[currentSlide].title"
           class="absolute inset-0 w-full h-full object-cover z-1 opacity-60 hero-parallax-bg will-change-transform"
           style="transform-origin: center center;">

      <!-- Layer 2: Distant mountains (slowest parallax) -->
      <div class="hero-mountains-far z-2 parallax-layer" data-speed="0.1" style="bottom: 20%; height: 40%;"></div>

      <!-- Layer 3: Mid-ground mountains with snow -->
      <div class="hero-mountains-mid z-3 parallax-layer" data-speed="0.25" style="bottom: 10%; height: 50%;"></div>

      <!-- Layer 4: Foreground terrain -->
      <div class="hero-foreground z-4 parallax-layer" data-speed="0.45" style="bottom: 0; height: 30%;"></div>

      <!-- Layer 5: Golden light -->
      <div class="hero-golden-light z-5"></div>

      <!-- Layer 6: Floating mist -->
      <div class="hero-mist z-6"></div>

      <!-- Layer 7: Prayer flags -->
      <div class="hero-prayer-flags z-7"></div>

      <!-- Layer 8: Content overlay -->
      <div class="relative z-10 text-center px-4 max-w-5xl mx-auto">
        <div class="mb-6 inline-flex items-center gap-2 rounded-full bg-tibet-red/25 px-4 py-2 text-sm text-tibet-yellow backdrop-blur-md border border-tibet-gold/30 animate-slide-up will-change-transform" style="animation-delay: 0.1s">
          <span class="h-2 w-2 rounded-full bg-tibet-yellow animate-pulse-slow"></span>
          {{ heroSlides[currentSlide].tag }}
        </div>
        <h1 class="text-5xl md:text-7xl font-bold text-white mb-6 tracking-tight animate-slide-up will-change-transform font-display" style="animation-delay: 0.2s; text-shadow: 0 2px 24px rgba(0,0,0,0.3);">
          {{ heroSlides[currentSlide].title }}
        </h1>
        <p class="text-xl md:text-2xl text-white/85 mb-10 font-light max-w-2xl mx-auto animate-slide-up will-change-transform" style="animation-delay: 0.4s; text-shadow: 0 1px 12px rgba(0,0,0,0.2);">
          {{ heroSlides[currentSlide].subtitle }}
        </p>
        <div class="flex flex-col sm:flex-row justify-center gap-4 animate-slide-up will-change-transform" style="animation-delay: 0.6s">
          <router-link to="/spots" class="tibet-btn text-lg px-8 py-4 shadow-xl will-change-transform">
            {{ t('home.startExploring') }}
          </router-link>
          <button @click="scrollToHeatmap"
                  class="tibet-btn-ghost text-white border-white/30 hover:bg-white/10 hover:border-white/50 hover:text-white text-lg px-8 py-4 will-change-transform">
            {{ t('home.viewHeatmap') }}
          </button>
        </div>

        <!-- Bead-style carousel dots -->
        <div class="mt-10 flex items-center justify-center gap-2.5">
          <button v-for="(slide, index) in heroSlides" :key="slide.title" @click="goToSlide(index)"
                  class="tibet-carousel-dot"
                  :class="{ active: currentSlide === index }"
                  :aria-label="`切换到第 ${index + 1} 张轮播图`"></button>
        </div>
      </div>
    </div>

    <!-- Mountain divider -->
    <div class="tibet-mountain-divider -mt-8 relative z-10"></div>

    <!-- Heatmap Section -->
    <div id="heatmap" class="py-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <div class="text-center mb-16 animate-on-scroll">
        <h2 class="tibet-heading text-4xl font-bold text-tibet-dark mb-4 tibetan-font">{{ t('home.hotSpotsDistribution') }}</h2>
        <p class="text-lg text-tibet-brown/70 tibetan-font">{{ t('home.hotSpotsDescription') }}</p>
      </div>
      
      <div class="bg-white rounded-3xl p-6 shadow-2xl animate-on-scroll">
        <HeatMap />
      </div>
    </div>

    <!-- Recommendations Section -->
    <div class="py-24 bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-end mb-12 animate-on-scroll">
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
        </div>

        <div v-if="loading" class="flex justify-center py-20">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold"></div>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div v-for="(spot, index) in recommendedSpots" :key="spot.id"
               class="group tibet-card-elevated overflow-hidden animate-on-scroll gpu-accelerated"
               :style="{ animationDelay: `${index * 100}ms` }">
            <div class="relative h-72 overflow-hidden">
              <img :src="spot.imageUrl" :alt="spot.name"
                   class="w-full h-full object-cover tibet-image-hover img-fade-in will-change-transform"
                   loading="lazy">
              <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/15 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 ease-out-expo"></div>
              <div class="absolute top-4 right-4 bg-tibet-white/95 backdrop-blur-md px-3 py-1.5 rounded-full text-xs font-bold text-tibet-red shadow-lg transform group-hover:scale-105 transition-transform duration-300 ease-out-expo will-change-transform">
                {{ spot.category === 'NATURAL' ? t('home.natural') : t('home.cultural') }}
              </div>
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
                  <span v-for="tag in spot.tags?.slice(0, 2)" :key="tag.id"
                        class="tibet-tag tibetan-font">
                    {{ tag.tag }}
                  </span>
                </div>
                <button @click="router.push(`/spots/${spot.id}`)"
                        class="tibet-link text-sm flex items-center group/btn tibetan-font">
                  {{ t('common.book') }}
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import HeatMap from '../components/HeatMap.vue'
import api, { endpoints } from '../api'

const router = useRouter()
const { t, locale } = useI18n()
const recommendedSpots = ref<any[]>([])
const recommendationReasons = ref<Map<number, string>>(new Map())
const loading = ref(true)
const currentSlide = ref(0)
const heroSlides = ref<Array<{ image: string; title: string; subtitle: string; tag: string; linkUrl?: string }>>([
  { image: '/heritage/布达拉宫3.jpg', title: '', subtitle: '', tag: '' }
])
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
  document.getElementById('heatmap')?.scrollIntoView({ behavior: 'smooth' })
}

const goToSlide = (index: number) => {
  currentSlide.value = index
}

const startCarousel = () => {
  stopCarousel()
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

// 获取推荐原因（处理类型转换）
const getRecommendationReason = (spotId: number) => {
  if (!spotId) return null
  // 尝试多种可能的 key 类型
  const reason = recommendationReasons.value.get(spotId) || 
                 recommendationReasons.value.get(Number(spotId))
  // 如果还是没有，返回默认原因
  return reason || t('home.recommendationReason')
}

const logDebugToConsole = (debug: any) => {
  console.group('%c🔬 协同过滤推荐算法 - 中间过程', 'font-size:16px;font-weight:bold;color:#e74c3c')

  // 1. 基本信息
  console.group('%c📋 基本信息', 'font-weight:bold;color:#3498db')
  console.log('用户ID:', debug.userId)
  console.log('是否有历史记录:', debug.hasHistory)
  console.log('是否使用兜底策略(冷启动):', debug.fallbackUsed)
  console.log('计算耗时:', debug.computationTimeMs + 'ms')
  console.groupEnd()

  // 2. 算法配置
  if (debug.algorithmConfig) {
    console.group('%c⚙️ 算法配置', 'font-weight:bold;color:#9b59b6')
    console.table(debug.algorithmConfig)
    console.groupEnd()
  }

  // 3. 用户历史记录
  if (debug.history && debug.history.length > 0) {
    console.group('%c📜 用户访问历史 (' + debug.history.length + '条)', 'font-weight:bold;color:#e67e22')
    console.table(debug.history.map((h: any) => ({
      '景点ID': h.spotId,
      '景点名称': h.spotName,
      '评分': h.rating,
      '访问时间': h.visitDate
    })))
    console.groupEnd()
  } else {
    console.log('%c📜 用户访问历史: 无 (冷启动用户)', 'color:#e67e22')
  }

  // 4. 用户标签画像
  if (debug.tagProfile && Object.keys(debug.tagProfile).length > 0) {
    console.group('%c🏷️ 用户标签画像', 'font-weight:bold;color:#2ecc71')
    const tagEntries = Object.entries(debug.tagProfile) as [string, number][]
    tagEntries.sort((a, b) => b[1] - a[1])
    console.table(tagEntries.map(([tag, weight]) => ({
      '标签': tag,
      '权重': Number(weight).toFixed(4)
    })))
    console.groupEnd()
  }

  // 5. 相似用户 (User-Based CF 核心)
  if (debug.similarUsers && debug.similarUsers.length > 0) {
    console.group('%c👥 相似用户 (User-Based CF)', 'font-weight:bold;color:#e91e63')
    console.log('找到 ' + debug.similarUsers.length + ' 个相似用户')
    console.table(debug.similarUsers.map((u: any, i: number) => ({
      '排名': i + 1,
      '用户ID': u.userId,
      '综合相似度': Number(u.similarity).toFixed(4),
      '余弦相似度(Adjusted)': u.adjustedCosine != null ? Number(u.adjustedCosine).toFixed(4) : '-',
      'Jaccard相似度': u.jaccard != null ? Number(u.jaccard).toFixed(4) : '-',
      '时间加权相似度': u.timeWeighted != null ? Number(u.timeWeighted).toFixed(4) : '-',
      '共同访问景点数': u.commonSpotsCount ?? '-'
    })))
    console.groupEnd()
  } else if (!debug.fallbackUsed) {
    console.log('%c👥 相似用户: 未找到相似度达标的用户', 'color:#e91e63')
  }

  // 6. 候选景点得分 (最终排序依据)
  if (debug.candidateScores && debug.candidateScores.length > 0) {
    console.group('%c🎯 候选景点得分 (混合协同过滤 + 标签匹配)', 'font-weight:bold;color:#f39c12')
    console.log('候选景点总数: ' + debug.candidateScores.length)
    console.table(debug.candidateScores.map((c: any, i: number) => ({
      '排名': i + 1,
      '景点ID': c.spotId,
      '景点名称': c.spotName,
      '最终得分': Number(c.finalScore).toFixed(4),
      '混合协同(UB+IB)': Number(c.collaborativeScore || 0).toFixed(4),
      'User-Based CF': Number(c.userBasedScore || 0).toFixed(4),
      'Item-Based CF': Number(c.itemBasedScore || 0).toFixed(4),
      '标签匹配得分': Number(c.tagScore || 0).toFixed(4)
    })))
    console.groupEnd()
  }

  // 7. 最终推荐结果
  if (debug.recommendations && debug.recommendations.length > 0) {
    console.group('%c✨ 最终推荐结果 (' + debug.recommendations.length + '个)', 'font-weight:bold;color:#27ae60')
    console.table(debug.recommendations.map((spot: any, i: number) => ({
      '排名': i + 1,
      'ID': spot.id,
      '名称': spot.name,
      '类别': spot.category,
      '评分': spot.rating,
      '访问量': spot.visitCount,
      '推荐原因': (debug.recommendationReasons && debug.recommendationReasons[spot.id]) || '-'
    })))
    console.groupEnd()
  }

  console.groupEnd()
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

      recommendedSpots.value = recommendationRes.data

      // 将协同过滤中间过程输出到浏览器控制台
      if (debugRes && debugRes.data) {
        logDebugToConsole(debugRes.data)
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
            reasonsMap.set(spot.id, '高评分景点')
          } else if (spot.visitCount && spot.visitCount > 15000) {
            reasonsMap.set(spot.id, '热门景点')
          } else {
            reasonsMap.set(spot.id, '为您精选')
          }
        })
        recommendationReasons.value = reasonsMap
      }
    } else {
      const response = await api.get(endpoints.spots.list)
      const spots = response.data?.content || response.data || []
      recommendedSpots.value = spots.slice(0, 3)
      const defaultReasons = new Map<number, string>()
      recommendedSpots.value.forEach((spot: any) => {
        defaultReasons.set(spot.id, '热门景点')
      })
      recommendationReasons.value = defaultReasons
    }
  } catch (error) {
    console.error('Failed to fetch recommendations:', error)
  } finally {
    loading.value = false
  }
}

const initScrollAnimations = () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' })

  document.querySelectorAll('.animate-on-scroll:not(.revealed)').forEach(el => {
    observer.observe(el)
  })
}

// 多层视差滚动效果
const initParallax = () => {
  const layers = document.querySelectorAll('.parallax-layer') as NodeListOf<HTMLElement>
  if (!layers.length) return

  let ticking = false
  const onScroll = () => {
    if (!ticking) {
      requestAnimationFrame(() => {
        const scrollY = window.scrollY
        layers.forEach(layer => {
          const speed = parseFloat(layer.dataset.speed || '0.3')
          layer.style.transform = `translateY(${scrollY * speed}px)`
        })
        // Also move the background image for depth
        const heroBg = document.querySelector('.hero-parallax-bg') as HTMLElement | null
        if (heroBg) {
          heroBg.style.transform = `translateY(${scrollY * 0.15}px) scale(1.05)`
        }
        ticking = false
      })
      ticking = true
    }
  }

  window.addEventListener('scroll', onScroll, { passive: true })
  return () => window.removeEventListener('scroll', onScroll)
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  fetchRecommendations()
  setTimeout(initScrollAnimations, 300)
})

onMounted(async () => {
  await fetchCarousels()
  await fetchRecommendations()
  setTimeout(initScrollAnimations, 100)
  const cleanupParallax = initParallax()
  startCarousel()
  if (cleanupParallax) {
    onUnmounted(cleanupParallax)
  }
})

onUnmounted(() => {
  stopCarousel()
})
</script>
