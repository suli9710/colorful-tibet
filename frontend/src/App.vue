<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import Footer from './components/Footer.vue'
import MobileBottomNav from './components/MobileBottomNav.vue'
import ConfirmHost from './components/ConfirmHost.vue'
import ToastHost from './components/ToastHost.vue'
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AnimatePresence, MotionConfig, motion } from 'motion-v'
import { showToast } from './composables/useToast'
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
const { t } = useI18n()
const AiRouteFloatingBall = defineAsyncComponent(() => import('./components/AiRouteFloatingBall.vue'))
const isNavigating = ref(false)
const mainContent = ref<HTMLElement | null>(null)
const routeStatusMessage = computed(() => isNavigating.value ? t('common.loading') : '')
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

const focusMainContent = () => {
  void nextTick(() => {
    mainContent.value?.focus({ preventScroll: true })
  })
}

onMounted(() => {
  removeBeforeEach = router.beforeEach(() => {
    if (finishTimer) window.clearTimeout(finishTimer)
    isNavigating.value = true
    return true
  })
  removeAfterEach = router.afterEach((to, from) => {
    finishNavigation()
    if (to.fullPath !== from.fullPath) {
      focusMainContent()
    }
  })
  removeOnError = router.onError(() => {
    showToast(t('toast.pageLoadFailed'), 'error')
    finishNavigation()
  })
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
    <div class="flex min-h-screen flex-col">
      <a href="#main-content" class="skip-link">
        {{ t('common.skipToContent', 'Skip to main content') }}
      </a>
      <div class="tibet-prayer-flag fixed left-0 right-0 top-0 z-[101] h-1 opacity-35"></div>

      <AnimatePresence>
        <motion.div
          v-if="isNavigating"
          key="navigation-progress"
          class="fixed left-0 right-0 top-1 z-[100] h-0.5 origin-left overflow-hidden"
          role="progressbar"
          :aria-label="t('common.loading')"
          :initial="progressBarInitial"
          :animate="progressBarAnimate"
          :exit="progressBarExit"
          :transition="progressBarTransition"
        >
          <div class="shimmer-gradient h-full bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red"></div>
        </motion.div>
      </AnimatePresence>

      <p class="sr-only" role="status" aria-live="polite" aria-atomic="true">
        {{ routeStatusMessage }}
      </p>

      <NavBar />
      <main
        id="main-content"
        ref="mainContent"
        class="app-main mobile-shell-main flex-grow pt-20 md:pt-24"
        tabindex="-1"
        :aria-busy="isNavigating"
      >
        <router-view v-slot="{ Component, route }">
          <keep-alive include="RoutePlanner">
            <component
              :is="Component"
              v-if="route.name === 'route-planner'"
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
      <MobileBottomNav />
      <ConfirmHost />
      <ToastHost />
    </div>
  </MotionConfig>
</template>
