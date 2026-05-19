<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import Footer from './components/Footer.vue'
import AiRouteFloatingBall from './components/AiRouteFloatingBall.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AnimatePresence, MotionConfig, motion } from 'motion-v'
import {
  motionEase,
  pageAnimate,
  pageExit,
  pageInitial,
  pageTransition,
  progressBarAnimate,
  progressBarExit,
  progressBarInitial,
  progressBarTransition
} from './motion/presets'

const router = useRouter()
const isNavigating = ref(false)
let finishTimer: number | undefined
let removeBeforeEach: (() => void) | undefined
let removeAfterEach: (() => void) | undefined
let removeOnError: (() => void) | undefined

const handleBeforeUnload = () => {
  isNavigating.value = true
}

const finishNavigation = () => {
  if (finishTimer) window.clearTimeout(finishTimer)
  finishTimer = window.setTimeout(() => {
    isNavigating.value = false
  }, 160)
}

onMounted(() => {
  removeBeforeEach = router.beforeEach(() => {
    if (finishTimer) window.clearTimeout(finishTimer)
    isNavigating.value = true
    return true
  })
  removeAfterEach = router.afterEach(finishNavigation)
  removeOnError = router.onError(finishNavigation)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  removeBeforeEach?.()
  removeAfterEach?.()
  removeOnError?.()
  if (finishTimer) window.clearTimeout(finishTimer)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <MotionConfig reducedMotion="user" :transition="{ duration: 0.45, ease: motionEase }">
  <div class="flex flex-col min-h-screen">
    <!-- 经幡色彩顶条 -->
    <div class="fixed top-0 left-0 right-0 h-1 z-[101] tibet-prayer-flag opacity-35"></div>

    <!-- Navigation loading bar -->
    <AnimatePresence>
      <motion.div
        v-if="isNavigating"
        key="navigation-progress"
        class="fixed top-1 left-0 right-0 h-0.5 z-[100] overflow-hidden origin-left"
        :initial="progressBarInitial"
        :animate="progressBarAnimate"
        :exit="progressBarExit"
        :transition="progressBarTransition"
      >
        <div class="h-full bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red shimmer-gradient"></div>
      </motion.div>
    </AnimatePresence>

    <NavBar />
    <main class="flex-grow pt-20 md:pt-24">
      <router-view v-slot="{ Component, route }">
        <keep-alive include="RoutePlanner">
          <component
            v-if="route.name === 'route-planner'"
            :is="Component"
            :key="route.name || route.path"
          />
        </keep-alive>
        <motion.div
          v-if="route.name !== 'route-planner'"
          :key="route.fullPath"
          class="min-h-[calc(100vh-6rem)]"
          :initial="pageInitial"
          :animate="pageAnimate"
          :exit="pageExit"
          :transition="pageTransition"
        >
          <component :is="Component" :key="route.name || route.path" />
        </motion.div>
      </router-view>
    </main>
    <Footer />
    <AiRouteFloatingBall />
  </div>
  </MotionConfig>
</template>
