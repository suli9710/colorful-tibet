<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Menu, X, LogOut } from 'lucide-vue-next'
import { updateMemoizedLocale, clearTokenCache } from '../api/index'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const { t, locale } = useI18n()
const auth = useAuthStore()
const isOpen = ref(false)
const scrollProgress = ref(0)

const currentLocale = computed(() => locale.value)
const switchLanguage = (lang: string) => {
  locale.value = lang
  updateMemoizedLocale(lang)
  document.documentElement.lang = lang
}

let scrollTicking = false
const checkScroll = () => {
  if (!scrollTicking) {
    requestAnimationFrame(() => {
      const maxScroll = 120
      scrollProgress.value = Math.min(window.scrollY / maxScroll, 1)
      scrollTicking = false
    })
    scrollTicking = true
  }
}

onMounted(() => {
  window.addEventListener('scroll', checkScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', checkScroll)
})

const logout = () => {
  clearTokenCache()
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="fixed top-0 left-0 w-full z-50 flex justify-center pt-4 px-4 pointer-events-none">
    <nav class="pointer-events-auto will-change-transform rounded-full border py-2.5 px-6"
      :style="{
        maxWidth: 1280 - scrollProgress * 256 + 'px',
        backgroundColor: `rgba(247, 243, 238, ${Math.max(0.15, scrollProgress * 0.85)})`,
        backdropFilter: `blur(${Math.max(4, scrollProgress * 20)}px) saturate(180%)`,
        boxShadow: scrollProgress > 0.05 ? `0 4px 24px rgba(139, 46, 58, ${scrollProgress * 0.12})` : 'none',
        borderColor: `rgba(197, 150, 75, ${scrollProgress * 0.2})`,
        borderWidth: scrollProgress > 0.05 ? '1px' : '0px',
        paddingTop: scrollProgress > 0.5 ? '0.625rem' : '1rem',
        paddingBottom: scrollProgress > 0.5 ? '0.625rem' : '1rem',
      }">
      <div class="flex justify-between items-center">
        <div class="flex-shrink-0 flex items-center">
          <router-link to="/" class="flex items-center space-x-2 group">
            <span class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red tibetan-font"
              :style="{ opacity: 0.8 + scrollProgress * 0.2 }">
              {{ t('common.brandName') }}
            </span>
          </router-link>
        </div>

        <div class="hidden md:flex items-center space-x-1">
          <router-link v-for="item in [
            { path: '/', label: t('common.home') },
            { path: '/spots', label: t('common.spots') },
            { path: '/route-planner', label: t('common.routePlanner') },
            { path: '/community', label: t('common.community') },
            { path: '/hotels', label: t('common.hotels') },
            { path: '/heritage', label: t('common.heritage') },
            { path: '/news', label: t('common.news') },
            ...(auth.user ? [{ path: '/profile', label: t('common.profile') }] : []),
          ]" :key="item.path" :to="item.path"
            class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/60 transition-all duration-300 ease-out-expo active:scale-95 group will-change-transform">
            <span class="relative z-10">{{ item.label }}</span>
            <span class="absolute inset-0 bg-gradient-to-r from-tibet-gold/10 to-tibet-red/10 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-300 ease-out-expo"></span>
          </router-link>

          <router-link v-if="auth.user && auth.user.role === 'ADMIN'" to="/admin" class="px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/50 transition-all duration-200 active:scale-95">
            {{ t('common.admin') }}
          </router-link>
        </div>

        <div class="hidden md:flex items-center space-x-2 mr-4">
          <button
            @click="switchLanguage('zh')"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200',
              currentLocale === 'zh'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-white/50 text-tibet-brown/80 hover:bg-white/70'
            ]">
            {{ t('common.chinese') }}
          </button>
          <button
            @click="switchLanguage('bo')"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200',
              currentLocale === 'bo'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-white/50 text-tibet-brown/80 hover:bg-white/70'
            ]">
            {{ t('common.tibetan') }}
          </button>
        </div>

        <div class="hidden md:flex items-center space-x-4">
          <div v-if="auth.user" class="flex items-center space-x-3 bg-white/50 backdrop-blur-sm px-4 py-1.5 rounded-full border border-white/20 shadow-sm">
            <span class="text-sm font-medium text-tibet-dark/80">{{ auth.user.nickname || auth.user.username }}</span>
            <button @click="logout" class="text-tibet-brown/70 hover:text-red-500 transition-colors active:scale-90">
              <LogOut class="w-4 h-4" />
            </button>
          </div>
          <div v-else class="flex items-center space-x-3">
            <router-link to="/login" class="text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-colors">
              {{ t('common.login') }}
            </router-link>
            <router-link to="/register" class="bg-tibet-red hover:bg-tibet-red/90 text-tibet-yellow text-sm font-medium px-4 py-2 rounded-full transition-all duration-300 ease-out-expo shadow-lg shadow-tibet-red/30 hover:shadow-tibet-red/50 hover:-translate-y-1 hover:scale-105 active:scale-95 relative overflow-hidden group will-change-transform">
              <span class="relative z-10">{{ t('common.register') }}</span>
            </router-link>
          </div>
        </div>

        <div class="flex items-center md:hidden">
          <button @click="isOpen = !isOpen" class="text-tibet-brown/80 hover:text-tibet-dark p-2 rounded-lg hover:bg-white/50 transition-colors">
            <Menu v-if="!isOpen" class="w-6 h-6" />
            <X v-else class="w-6 h-6" />
          </button>
        </div>
      </div>
    </nav>
  </div>

  <transition
    enter-active-class="mobile-menu-enter-active"
    enter-from-class="mobile-menu-enter-from"
    leave-active-class="mobile-menu-leave-active"
    leave-to-class="mobile-menu-leave-to"
  >
    <div v-if="isOpen" class="fixed top-20 left-4 right-4 z-40 md:hidden glass rounded-3xl border border-white/20 shadow-2xl">
      <div class="px-4 pt-2 pb-6 space-y-1">
        <router-link v-for="item in [
          { path: '/', label: t('common.home') },
          { path: '/spots', label: t('common.spots') },
          { path: '/route-planner', label: t('common.routePlanner') },
          { path: '/community', label: t('common.community') },
          { path: '/hotels', label: t('common.hotels') },
          { path: '/heritage', label: t('common.heritage') },
          { path: '/news', label: t('common.news') },
          ...(auth.user ? [{ path: '/profile', label: t('common.profile') }] : []),
        ]" :key="item.path" :to="item.path"
          class="block px-4 py-3 rounded-xl text-base font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/60 transition-all duration-300 ease-out-expo active:scale-98 hover:translate-x-2 group will-change-transform"
          @click="isOpen = false">
          <span class="flex items-center">
            <span class="flex-1">{{ item.label }}</span>
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 opacity-0 group-hover:opacity-100 transform group-hover:translate-x-1 transition-all duration-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </span>
        </router-link>

        <router-link v-if="auth.user && auth.user.role === 'ADMIN'" to="/admin"
          class="block px-4 py-3 rounded-xl text-base font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/50 transition-all active:scale-98"
          @click="isOpen = false">
          {{ t('common.admin') }}
        </router-link>

        <div class="px-4 py-3 flex items-center justify-center space-x-2 border-t border-tibet-gold/20 mt-2">
          <button
            @click="switchLanguage('zh')"
            :class="[
              'px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200',
              currentLocale === 'zh'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-gray-100 text-tibet-brown/80 hover:bg-gray-200'
            ]">
            {{ t('common.chinese') }}
          </button>
          <button
            @click="switchLanguage('bo')"
            :class="[
              'px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200',
              currentLocale === 'bo'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-gray-100 text-tibet-brown/80 hover:bg-gray-200'
            ]">
            {{ t('common.tibetan') }}
          </button>
        </div>

        <div class="pt-4 border-t border-tibet-gold/20 mt-2">
          <div v-if="auth.user" class="flex items-center justify-between px-4">
            <span class="font-medium text-tibet-dark/80">{{ auth.user.nickname || auth.user.username }}</span>
            <button @click="logout" class="text-red-500 text-sm font-medium">{{ t('common.logout') }}</button>
          </div>
          <div v-else class="grid grid-cols-2 gap-4 px-4">
            <router-link to="/login" class="text-center py-2 rounded-xl bg-gray-100 text-tibet-dark/80 font-medium">{{ t('common.login') }}</router-link>
            <router-link to="/register" class="text-center py-2 rounded-xl bg-tibet-red text-tibet-yellow font-medium">{{ t('common.register') }}</router-link>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>
