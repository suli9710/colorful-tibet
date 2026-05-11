<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import Footer from './components/Footer.vue'
import { ref } from 'vue'
import { AnimatePresence, MotionConfig, motion, useScroll, useSpring } from 'motion-v'
import { pageAnimate, pageExit, pageInitial, pageTransition } from './motion/presets'

const isNavigating = ref(false)
const { scrollYProgress } = useScroll({ trackContentSize: true })
const pageScrollScale = useSpring(scrollYProgress, {
  stiffness: 180,
  damping: 28,
  mass: 0.2
})

// Listen for route changes to trigger navigation indicator
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    isNavigating.value = true
  })
}
</script>

<template>
  <MotionConfig reducedMotion="user" :transition="{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }">
  <div class="flex flex-col min-h-screen">
    <!-- 经幡色彩顶条 -->
    <div class="fixed top-0 left-0 right-0 h-1 z-[101] tibet-prayer-flag opacity-35"></div>
    <motion.div
      class="fixed top-0 left-0 right-0 h-1 z-[102] tibet-prayer-flag origin-left"
      :style="{ scaleX: pageScrollScale }"
    />

    <!-- Navigation loading bar -->
    <div
      v-if="isNavigating"
      class="fixed top-1 left-0 right-0 h-0.5 z-[100] overflow-hidden"
    >
      <div class="h-full bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red shimmer-gradient"></div>
    </div>

    <NavBar />
    <main class="flex-grow pt-20 md:pt-24">
      <router-view v-slot="{ Component, route }">
        <AnimatePresence mode="wait" :initial="false">
          <motion.div
            :key="route.fullPath"
            class="min-h-[calc(100vh-6rem)]"
            :initial="pageInitial"
            :animate="pageAnimate"
            :exit="pageExit"
            :transition="pageTransition"
          >
            <keep-alive include="RoutePlanner">
              <component :is="Component" :key="route.name || route.path" />
            </keep-alive>
          </motion.div>
        </AnimatePresence>
      </router-view>
    </main>
    <Footer />
  </div>
  </MotionConfig>
</template>
