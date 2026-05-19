<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Menu, X, LogOut } from 'lucide-vue-next'
import api, { updateMemoizedLocale, clearTokenCache } from '../api/index'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const auth = useAuthStore()
const isOpen = ref(false)
const isScrolled = ref(false)
let ticking = false

watch(() => route.fullPath, () => {
  isOpen.value = false
})

const updateScrolledState = () => {
  isScrolled.value = window.scrollY > 80
  ticking = false
}

const handleScroll = () => {
  if (ticking) return
  ticking = true
  window.requestAnimationFrame(updateScrolledState)
}

onMounted(() => {
  updateScrolledState()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const currentLocale = computed(() => locale.value)
const navItems = computed(() => [
  { path: '/', label: t('common.home') },
  { path: '/spots', label: t('common.spots') },
  { path: '/route-planner', label: t('common.routePlanner') },
  { path: '/community', label: t('common.community') },
  { path: '/hotels', label: t('common.hotels') },
  { path: '/heritage', label: t('common.heritage') },
  { path: '/news', label: t('common.news') },
  ...(auth.user ? [
    { path: '/orders', label: t('common.orders') },
    { path: '/profile', label: t('common.profile') }
  ] : []),
])

const switchLanguage = (lang: string) => {
  locale.value = lang
  updateMemoizedLocale(lang)
  document.documentElement.lang = lang
}

const logout = async () => {
  try {
    await api.post('/auth/logout')
  } finally {
    clearTokenCache()
    auth.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="fixed top-0 left-0 w-full z-50 flex justify-center pt-3 px-3 sm:pt-4 sm:px-4 pointer-events-none">
    <nav
      class="pointer-events-auto rounded-[1.35rem] sm:rounded-full border px-3 sm:px-6 tibet-nav-shell nav-shell-optimized"
      :class="isScrolled ? 'nav-shell-scrolled' : 'nav-shell-top'"
    >
      <div class="flex justify-between items-center">
        <div class="flex-shrink-0 flex items-center">
          <router-link to="/" class="flex min-w-0 items-center space-x-2 group">
            <span class="tibet-brand-sigil flex-shrink-0"></span>
            <span class="max-w-[12rem] truncate text-[1.05rem] font-bold bg-clip-text text-transparent bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red sm:max-w-none sm:text-2xl tibetan-font"
              :class="isScrolled ? 'opacity-100' : 'opacity-90'">
              {{ t('common.brandName') }}
            </span>
          </router-link>
        </div>

        <div class="hidden md:flex items-center space-x-1">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path" custom v-slot="{ href, navigate, isActive }">
            <a
              :href="href"
              class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-all duration-300 ease-out-expo group will-change-transform hover:-translate-y-0.5 hover:scale-[1.03] active:scale-95"
              @click="navigate"
            >
              <span
                v-if="isActive"
                class="absolute inset-0 rounded-full bg-white/75 shadow-sm border border-tibet-gold/20"
              />
              <span class="absolute inset-0 bg-gradient-to-r from-tibet-gold/10 to-tibet-red/10 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-300 ease-out-expo"></span>
              <span class="relative z-10">{{ item.label }}</span>
            </a>
          </router-link>

          <router-link v-if="auth.user && auth.user.role === 'ADMIN'" to="/admin" custom v-slot="{ href, navigate, isActive }">
            <a
              :href="href"
              class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-all duration-200 hover:-translate-y-0.5 hover:scale-[1.03] active:scale-95"
              @click="navigate"
            >
              <span
                v-if="isActive"
                class="absolute inset-0 rounded-full bg-white/75 shadow-sm border border-tibet-gold/20"
              />
              <span class="relative z-10">{{ t('common.admin') }}</span>
            </a>
          </router-link>
        </div>

        <div class="hidden md:flex items-center space-x-2 mr-4">
          <button
            @click="switchLanguage('zh')"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200 hover:-translate-y-0.5 active:scale-95',
              currentLocale === 'zh'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-white/50 text-tibet-brown/80 hover:bg-white/70'
            ]">
            {{ t('common.chinese') }}
          </button>
          <button
            @click="switchLanguage('bo')"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200 hover:-translate-y-0.5 active:scale-95',
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
            <button @click="logout" class="text-tibet-brown/70 hover:text-red-500 transition-transform hover:-rotate-6 hover:scale-110 active:scale-90">
              <LogOut class="w-4 h-4" />
            </button>
          </div>
          <div v-else class="flex items-center space-x-3">
            <router-link to="/login" class="text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-colors">
              {{ t('common.login') }}
            </router-link>
            <router-link to="/register" class="bg-tibet-red hover:bg-tibet-red/90 text-tibet-yellow text-sm font-medium px-4 py-2 rounded-full transition-all duration-300 ease-out-expo shadow-lg shadow-tibet-red/30 hover:shadow-tibet-red/50 relative overflow-hidden group will-change-transform hover:-translate-y-0.5 hover:scale-[1.03] active:scale-95">
              <span class="relative z-10">{{ t('common.register') }}</span>
            </router-link>
          </div>
        </div>

        <div class="flex items-center md:hidden">
          <button
            @click="isOpen = !isOpen"
            class="flex h-11 w-11 items-center justify-center rounded-xl text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/50 transition-colors active:scale-95"
            :aria-label="isOpen ? '关闭菜单' : '打开菜单'"
            :aria-expanded="isOpen"
          >
            <Menu v-if="!isOpen" class="w-6 h-6" />
            <X v-else class="w-6 h-6" />
          </button>
        </div>
      </div>
    </nav>
  </div>

  <div
      v-if="isOpen"
      key="mobile-nav"
      class="fixed left-3 right-3 top-[4.75rem] z-40 max-h-[calc(100dvh-5.5rem)] origin-top overflow-y-auto overscroll-contain rounded-3xl border border-white/20 shadow-2xl md:hidden glass animate-fade-in"
    >
      <div class="px-3 pt-2 pb-5 space-y-1 sm:px-4 sm:pb-6">
        <div
          v-for="(item, index) in navItems"
          :key="item.path"
          :style="{ animationDelay: `${Math.min(index, 8) * 25}ms` }"
          class="animate-fade-slide-in"
        >
          <router-link
            :to="item.path"
            class="block rounded-xl px-4 py-3 text-base font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/60 transition-all duration-300 ease-out-expo active:scale-98 sm:hover:translate-x-2 group will-change-transform"
            @click="isOpen = false">
            <span class="flex items-center">
              <span class="flex-1">{{ item.label }}</span>
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 opacity-0 group-hover:opacity-100 transform group-hover:translate-x-1 transition-all duration-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </span>
          </router-link>
        </div>

        <router-link v-if="auth.user && auth.user.role === 'ADMIN'" to="/admin"
          class="block px-4 py-3 rounded-xl text-base font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/50 transition-all active:scale-98"
          @click="isOpen = false">
          {{ t('common.admin') }}
        </router-link>

        <div class="mt-2 flex items-center justify-center gap-2 border-t border-tibet-gold/20 px-2 py-3 sm:px-4">
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
</template>

<style scoped>
.nav-shell-optimized {
  width: min(100%, 1280px);
  border-width: 1px;
  transition:
    width 180ms ease,
    background-color 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease,
    padding 180ms ease;
}

.nav-shell-top {
  padding-top: 1rem;
  padding-bottom: 1rem;
  background-color: rgba(247, 243, 238, 0.5);
  border-color: rgba(197, 150, 75, 0.08);
  box-shadow: none;
}

.nav-shell-scrolled {
  width: min(100%, 1024px);
  padding-top: 0.625rem;
  padding-bottom: 0.625rem;
  background-color: rgba(247, 243, 238, 0.92);
  border-color: rgba(197, 150, 75, 0.24);
  box-shadow: 0 4px 22px rgba(139, 46, 58, 0.1);
}

.animate-fade-in {
  animation: fadeIn 180ms ease-out both;
}

.animate-fade-slide-in {
  animation: fadeSlideIn 220ms ease-out both;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-8px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}
</style>
