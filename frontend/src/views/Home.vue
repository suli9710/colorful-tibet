<template>
  <div class="min-h-screen bg-apple-gray-50">
    <!-- Hero Section -->
    <div class="relative h-[calc(100vh-6rem)] md:h-[calc(100vh-7rem)] flex items-center justify-center overflow-hidden -mt-24 md:-mt-28">
      <!-- Background Video/Image -->
      <div class="absolute inset-0 z-0">
        <img src="/heritage/布达拉宫3.jpg" alt="Potala Palace" class="w-full h-full object-cover scale-105 animate-float-slow will-change-transform hero-parallax-bg" style="transform-origin: center center;">
        <div class="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-apple-gray-50"></div>
      </div>

      <!-- Hero Content -->
      <div class="relative z-10 text-center px-4 max-w-5xl mx-auto">
        <h1 class="text-5xl md:text-7xl font-bold text-white mb-6 tracking-tight animate-slide-up will-change-transform tibetan-font" style="animation-delay: 0.1s">
          {{ t('home.title') }}
        </h1>
        <p class="text-xl md:text-2xl text-white/90 mb-10 font-light max-w-2xl mx-auto animate-slide-up will-change-transform tibetan-font" style="animation-delay: 0.3s">
          {{ t('home.subtitle') }}
        </p>
        <div class="flex flex-col sm:flex-row justify-center gap-4 animate-slide-up will-change-transform" style="animation-delay: 0.5s">
          <router-link to="/spots" class="group px-8 py-4 bg-white text-apple-gray-900 rounded-full font-semibold text-lg hover:bg-gray-100 transition-all duration-300 ease-out-expo transform hover:scale-105 hover:shadow-2xl shadow-lg relative overflow-hidden will-change-transform tibetan-font">
            <span class="relative z-10">{{ t('home.startExploring') }}</span>
            <span class="absolute inset-0 bg-gradient-to-r from-blue-500 to-purple-500 opacity-0 group-hover:opacity-10 transition-opacity duration-300 ease-out-expo"></span>
          </router-link>
          <button @click="scrollToHeatmap" class="px-8 py-4 bg-white/20 backdrop-blur-md border border-white/30 text-white rounded-full font-semibold text-lg hover:bg-white/30 transition-all duration-300 ease-out-expo transform hover:scale-105 hover:shadow-xl hover:border-white/50 will-change-transform tibetan-font">
            {{ t('home.viewHeatmap') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Heatmap Section -->
    <div id="heatmap" class="py-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <div class="text-center mb-16 animate-on-scroll">
        <h2 class="text-4xl font-bold text-apple-gray-900 mb-4 tibetan-font">{{ t('home.hotSpotsDistribution') }}</h2>
        <p class="text-lg text-apple-gray-500 tibetan-font">{{ t('home.hotSpotsDescription') }}</p>
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
            <h2 class="text-4xl font-bold text-apple-gray-900 mb-2 tibetan-font">{{ t('home.recommendations') }}</h2>
            <p class="text-lg text-apple-gray-500 tibetan-font">{{ t('home.recommendationsDescription') }}</p>
          </div>
          <router-link to="/spots" class="hidden md:flex items-center text-apple-blue hover:text-apple-blue-hover font-medium transition-colors tibetan-font">
            {{ t('common.viewAll') }}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 ml-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
            </svg>
          </router-link>
        </div>

        <div v-if="loading" class="flex justify-center py-20">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-apple-blue"></div>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div v-for="(spot, index) in recommendedSpots" :key="spot.id" 
               class="group bg-white rounded-3xl shadow-sm hover:shadow-2xl card-hover overflow-hidden border border-gray-100 animate-on-scroll hover:border-apple-blue/20 gpu-accelerated"
               :style="{ animationDelay: `${index * 100}ms` }">
            <div class="relative h-72 overflow-hidden">
              <img :src="spot.imageUrl" :alt="spot.name" 
                   class="w-full h-full object-cover transition-transform duration-700 ease-out-expo group-hover:scale-110 img-fade-in will-change-transform"
                   loading="lazy">
              <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 ease-out-expo"></div>
              <div class="absolute top-4 right-4 bg-white/95 backdrop-blur-md px-3 py-1.5 rounded-full text-xs font-bold text-apple-gray-900 shadow-xl transform group-hover:scale-110 transition-transform duration-300 ease-out-expo will-change-transform tibetan-font">
                {{ spot.category === 'NATURAL' ? t('home.natural') : t('home.cultural') }}
              </div>
            </div>
            
              <div class="p-8">
              <div class="flex justify-between items-start mb-4">
                <h3 class="text-2xl font-bold text-apple-gray-900 group-hover:text-apple-blue transition-colors duration-300 ease-out-expo">{{ spot.name }}</h3>
                <span class="text-lg font-semibold text-apple-blue transform group-hover:scale-110 transition-transform duration-300 ease-out-expo will-change-transform">¥{{ spot.ticketPrice }}</span>
              </div>
              <!-- 推荐原因 -->
              <p v-if="getRecommendationReason(spot.id)" class="text-xs text-apple-blue mb-3 font-medium tibetan-font">
                💡 {{ getRecommendationReason(spot.id) }}
              </p>
              <p class="text-apple-gray-500 mb-6 line-clamp-2 leading-relaxed tibetan-font">{{ spot.description }}</p>
              
              <div class="flex items-center justify-between pt-6 border-t border-gray-100">
                <div class="flex space-x-2">
                  <span v-for="tag in spot.tags?.slice(0, 2)" :key="tag.id" 
                        class="px-3 py-1 bg-apple-gray-100 text-apple-gray-600 rounded-full text-xs font-medium transform group-hover:scale-105 transition-transform duration-300 ease-out-expo will-change-transform tibetan-font">
                    {{ tag.tag }}
                  </span>
                </div>
                <button @click="router.push(`/spots/${spot.id}`)" 
                        class="text-apple-blue font-medium hover:text-apple-blue-hover transition-all duration-300 ease-out-expo flex items-center group/btn tibetan-font">
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
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import HeatMap from '../components/HeatMap.vue'
import api, { endpoints } from '../api'

const router = useRouter()
const { t, locale } = useI18n()
const recommendedSpots = ref<any[]>([])
const recommendationReasons = ref<Map<number, string>>(new Map())
const loading = ref(true)

const scrollToHeatmap = () => {
  document.getElementById('heatmap')?.scrollIntoView({ behavior: 'smooth' })
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

const logRecommendationDebugInfo = (debugData: any) => {
  if (!debugData) return
  
  // 主标题
  console.group('%c🎯 协同过滤算法训练详情', 'color:#2563eb;font-weight:bold;font-size:16px;padding:8px;background:#e3f2fd;border-radius:4px;')
  
  // 基础信息
  console.group('%c📊 基础信息', 'color:#1976d2;font-weight:bold;')
  console.log('%c用户ID:', 'color:#666;font-weight:bold', debugData.userId)
  console.log('%c使用历史记录:', 'color:#666;font-weight:bold', debugData.hasHistory ? '✅ 是' : '❌ 否')
  console.log('%c是否回退热门:', 'color:#666;font-weight:bold', debugData.fallbackUsed ? '✅ 是' : '❌ 否')
  if (debugData.computationTimeMs) {
    console.log('%c计算耗时:', 'color:#666;font-weight:bold', `${debugData.computationTimeMs}ms`)
  }
  console.groupEnd()
  
  // 算法配置
  if (debugData.algorithmConfig) {
    console.group('%c⚙️ 算法配置', 'color:#1976d2;font-weight:bold;')
    const config = debugData.algorithmConfig
    console.table({
      '最大相似用户数': config.maxSimilarUsers,
      '推荐结果数量': config.maxResults,
      '最小相似度阈值': config.minSimilarity,
      '协同过滤权重': `${(config.collaborativeWeight * 100).toFixed(0)}%`,
      '内容过滤权重': `${(config.contentWeight * 100).toFixed(0)}%`,
      '多样性惩罚系数': config.diversityPenalty,
      '探索率': `${(config.explorationRate * 100).toFixed(0)}%`
    })
    console.groupEnd()
  }
  
  // 用户访问记录
  if (debugData.history?.length) {
    console.group('%c📝 用户访问记录', 'color:#1976d2;font-weight:bold;')
    console.table(debugData.history.map((item: any) => ({
      景点: item.spotName,
      评分: item.rating ? '⭐'.repeat(item.rating) + ` (${item.rating})` : '未评分',
      访问时间: item.visitDate ? new Date(item.visitDate).toLocaleString('zh-CN') : '未知'
    })))
    console.groupEnd()
  }
  
  // 标签画像
  if (debugData.tagProfile && Object.keys(debugData.tagProfile).length > 0) {
    console.group('%c🏷️ 用户标签画像', 'color:#1976d2;font-weight:bold;')
    const tagEntries = Object.entries(debugData.tagProfile)
      .sort((a: any, b: any) => b[1] - a[1])
      .map(([tag, weight]: [string, any]) => ({
        标签: tag,
        权重: weight.toFixed(3),
        权重条: '█'.repeat(Math.min(20, Math.floor(weight * 2)))
      }))
    console.table(tagEntries)
    console.groupEnd()
  }
  
  // 相似用户详情
  if (debugData.similarUsers?.length) {
    console.group('%c👥 相似用户分析', 'color:#1976d2;font-weight:bold;')
    console.log(`找到 ${debugData.similarUsers.length} 个相似用户`)
    
    debugData.similarUsers.forEach((user: any, index: number) => {
      console.group(`%c相似用户 #${index + 1} (ID: ${user.userId})`, 'color:#4caf50;font-weight:bold;')
      console.log('%c综合相似度:', 'color:#666;font-weight:bold', `${(user.similarity * 100).toFixed(2)}%`)
      
      if (user.adjustedCosine !== undefined) {
        console.log('%c  ├─ 调整余弦相似度:', 'color:#666', `${(user.adjustedCosine * 100).toFixed(2)}%`)
      }
      if (user.jaccard !== undefined) {
        console.log('%c  ├─ Jaccard相似度:', 'color:#666', `${(user.jaccard * 100).toFixed(2)}%`)
      }
      if (user.timeWeighted !== undefined) {
        console.log('%c  ├─ 时间加权相似度:', 'color:#666', `${(user.timeWeighted * 100).toFixed(2)}%`)
      }
      if (user.commonSpotsCount !== undefined) {
        console.log('%c  └─ 共同访问景点数:', 'color:#666', user.commonSpotsCount)
      }
      
      // 可视化相似度
      const similarityBar = '█'.repeat(Math.min(50, Math.floor(user.similarity * 50)))
      console.log(`%c相似度可视化: ${similarityBar}`, `color:${user.similarity > 0.5 ? '#4caf50' : user.similarity > 0.3 ? '#ff9800' : '#f44336'}`)
      console.groupEnd()
    })
    console.groupEnd()
  }
  
  // 候选景点得分详情
  if (debugData.candidateScores?.length) {
    console.group('%c🏔️ 候选景点得分分析', 'color:#1976d2;font-weight:bold;')
    console.log(`共 ${debugData.candidateScores.length} 个候选景点`)
    
    const topCandidates = debugData.candidateScores.slice(0, 10)
    console.table(topCandidates.map((candidate: any) => ({
      排名: debugData.candidateScores.indexOf(candidate) + 1,
      景点: candidate.spotName,
      最终得分: candidate.finalScore?.toFixed(4),
      协同得分: candidate.collaborativeScore?.toFixed(4) || '0.0000',
      标签得分: candidate.tagScore?.toFixed(4) || '0.0000',
      得分占比: {
        协同: candidate.finalScore ? `${((candidate.collaborativeScore || 0) / candidate.finalScore * 100).toFixed(1)}%` : '0%',
        标签: candidate.finalScore ? `${((candidate.tagScore || 0) / candidate.finalScore * 100).toFixed(1)}%` : '0%'
      }
    })))
    
    // 得分分布可视化
    console.group('%c📈 得分分布', 'color:#1976d2;font-weight:bold;')
    topCandidates.forEach((candidate: any, index: number) => {
      const maxScore = topCandidates[0]?.finalScore || 1
      const barLength = Math.floor((candidate.finalScore / maxScore) * 30)
      const bar = '█'.repeat(barLength) + '░'.repeat(30 - barLength)
      const color = index < 3 ? '#4caf50' : index < 6 ? '#ff9800' : '#9e9e9e'
      console.log(`%c${(index + 1).toString().padStart(2, ' ')}. ${candidate.spotName.padEnd(20, ' ')} ${bar} ${candidate.finalScore?.toFixed(4)}`, `color:${color}`)
    })
    console.groupEnd()
    
    console.groupEnd()
  }
  
  // 最终推荐结果
  if (debugData.recommendations?.length) {
    console.group('%c✨ 最终推荐结果', 'color:#4caf50;font-weight:bold;')
    debugData.recommendations.forEach((spot: any, index: number) => {
      console.log(`%c${index + 1}. ${spot.name}`, 'color:#4caf50;font-weight:bold;font-size:14px;')
    })
    console.groupEnd()
  }
  
  console.groupEnd()
  
  // 训练完成提示
  console.log('%c✅ 协同过滤算法训练完成！', 'color:#4caf50;font-weight:bold;font-size:14px;padding:4px;background:#e8f5e9;border-radius:4px;')
}

const fetchRecommendations = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      // 只调用一次推荐接口，推荐原因从推荐结果中推断，不再额外调用 debug 接口
      const recommendationRes = await api.get(`${endpoints.spots.recommendations}?userId=${user.id}`)
      recommendedSpots.value = recommendationRes.data
      
      // 为推荐景点生成推荐原因（从景点属性推断，无需额外请求）
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
    } else {
      const response = await api.get(endpoints.spots.list)
      recommendedSpots.value = response.data.slice(0, 3)
      // 未登录用户使用默认原因
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

// 视差滚动效果（Hero 背景随页面滚动轻微移动）
const initParallax = () => {
  const heroBg = document.querySelector('.hero-parallax-bg') as HTMLElement | null
  if (!heroBg) return

  let ticking = false
  const onScroll = () => {
    if (!ticking) {
      requestAnimationFrame(() => {
        const scrollY = window.scrollY
        heroBg.style.transform = `translateY(${scrollY * 0.3}px)`
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
  await fetchRecommendations()
  setTimeout(initScrollAnimations, 100)
  const cleanupParallax = initParallax()
  if (cleanupParallax) {
    onUnmounted(cleanupParallax)
  }
})
</script>
