<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import Footer from './components/Footer.vue'
import { ref } from 'vue'

const isNavigating = ref(false)

// Listen for route changes to trigger navigation indicator
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    isNavigating.value = true
  })
}
</script>

<template>
  <div class="flex flex-col min-h-screen">
    <!-- 经幡色彩顶条 -->
    <div class="fixed top-0 left-0 right-0 h-1 z-[101] tibet-prayer-flag"></div>

    <!-- Navigation loading bar -->
    <div
      v-if="isNavigating"
      class="fixed top-1 left-0 right-0 h-0.5 z-[100] overflow-hidden"
    >
      <div class="h-full bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red shimmer-gradient"></div>
    </div>

    <NavBar />
    <main class="flex-grow pt-24 md:pt-28">
      <router-view v-slot="{ Component, route }">
        <transition name="page" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </main>
    <Footer />
  </div>
</template>
