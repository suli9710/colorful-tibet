<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Menu, X, LogOut } from 'lucide-vue-next'
import api, { updateMemoizedLocale, clearTokenCache, endpoints } from '../api/index'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const auth = useAuthStore()
const isOpen = ref(false)
const isScrolled = ref(false)
const menuButton = ref<HTMLButtonElement | null>(null)
let ticking = false

const updateScrolledState = () => {
  isScrolled.value = window.scrollY > 80
  ticking = false
}

const handleScroll = () => {
  if (ticking) return
  ticking = true
  window.requestAnimationFrame(updateScrolledState)
}

const closeMobileMenu = (restoreFocus = false) => {
  if (!isOpen.value) return
  isOpen.value = false

  if (restoreFocus) {
    void nextTick(() => menuButton.value?.focus())
  }
}

const toggleMobileMenu = () => {
  isOpen.value = !isOpen.value
}

const handleGlobalKeydown = (event: KeyboardEvent) => {
  if (event.key !== 'Escape' || !isOpen.value) return
  event.preventDefault()
  closeMobileMenu(true)
}

watch(() => route.fullPath, () => {
  closeMobileMenu()
})

onMounted(() => {
  updateScrolledState()
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('keydown', handleGlobalKeydown)
})

const currentLocale = computed(() => locale.value)
const currentUserAvatar = computed(() => auth.user?.avatar || auth.user?.avatarUrl || '')
const currentUserLabel = computed(() => {
  const nickname = auth.user?.nickname?.trim()
  if (nickname) return nickname
  return auth.user?.role === 'ADMIN' ? t('profile.admin') : t('profile.member')
})
const currentUserInitial = computed(() => currentUserLabel.value.charAt(0).toUpperCase())
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
    await api.post(endpoints.auth.logout)
  } finally {
    clearTokenCache()
    auth.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="nav-safe-frame fixed top-0 left-0 w-full z-50 flex justify-center px-3 sm:px-4 pointer-events-none">
    <nav
      class="pointer-events-auto rounded-[1.35rem] sm:rounded-full border px-3 sm:px-6 tibet-nav-shell nav-shell-optimized"
      :class="isScrolled ? 'nav-shell-scrolled' : 'nav-shell-top'"
      :aria-label="t('mobileNav.ariaLabel')"
    >
      <div class="flex justify-between items-center">
        <div class="flex-shrink-0 flex items-center">
          <router-link to="/" class="flex min-w-0 items-center space-x-2 rounded-full group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80">
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
              class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-all duration-300 ease-out-expo group will-change-transform hover:-translate-y-0.5 hover:scale-[1.03] active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80"
              :aria-current="isActive ? 'page' : undefined"
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
              class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-all duration-200 hover:-translate-y-0.5 hover:scale-[1.03] active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80"
              :aria-current="isActive ? 'page' : undefined"
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
            type="button"
            @click="switchLanguage('zh')"
            :aria-pressed="currentLocale === 'zh'"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200 hover:-translate-y-0.5 active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80',
              currentLocale === 'zh'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-white/50 text-tibet-brown/80 hover:bg-white/70'
            ]">
            {{ t('common.chinese') }}
          </button>
          <button
            type="button"
            @click="switchLanguage('bo')"
            :aria-pressed="currentLocale === 'bo'"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200 hover:-translate-y-0.5 active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80',
              currentLocale === 'bo'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-white/50 text-tibet-brown/80 hover:bg-white/70'
            ]">
            {{ t('common.tibetan') }}
          </button>
        </div>

        <div class="hidden md:flex items-center space-x-4">
          <div v-if="auth.user" class="flex items-center space-x-3 bg-white/50 backdrop-blur-sm px-4 py-1.5 rounded-full border border-white/20 shadow-sm">
            <img
              v-if="currentUserAvatar"
              :src="currentUserAvatar"
              :alt="currentUserLabel"
              class="h-7 w-7 rounded-full object-cover"
            >
            <span v-else class="flex h-7 w-7 items-center justify-center rounded-full bg-tibet-red text-xs font-bold text-tibet-yellow">
              {{ currentUserInitial }}
            </span>
            <span class="text-sm font-medium text-tibet-dark/80">{{ currentUserLabel }}</span>
            <button type="button" @click="logout" class="rounded-full text-tibet-brown/70 hover:text-red-500 transition-transform hover:-rotate-6 hover:scale-110 active:scale-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80" :aria-label="t('common.logout')" :title="t('common.logout')">
              <LogOut class="w-4 h-4" aria-hidden="true" />
            </button>
          </div>
          <div v-else class="flex items-center space-x-3">
            <router-link to="/login" class="rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80">
              {{ t('common.login') }}
            </router-link>
            <router-link to="/register" class="bg-tibet-red hover:bg-tibet-red/90 text-tibet-yellow text-sm font-medium px-4 py-2 rounded-full transition-all duration-300 ease-out-expo shadow-lg shadow-tibet-red/30 hover:shadow-tibet-red/50 relative overflow-hidden group will-change-transform hover:-translate-y-0.5 hover:scale-[1.03] active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80">
              <span class="relative z-10">{{ t('common.register') }}</span>
            </router-link>
          </div>
        </div>

        <div class="flex items-center md:hidden">
          <button
            ref="menuButton"
            type="button"
            @click="toggleMobileMenu"
            class="flex h-11 w-11 items-center justify-center rounded-xl text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/50 transition-colors active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80"
            :aria-label="isOpen ? t('common.closeMenu') : t('common.openMenu')"
            :title="isOpen ? t('common.closeMenu') : t('common.openMenu')"
            :aria-expanded="isOpen"
            aria-controls="mobile-primary-menu"
          >
            <Menu v-if="!isOpen" class="w-6 h-6" aria-hidden="true" />
            <X v-else class="w-6 h-6" aria-hidden="true" />
          </button>
        </div>
      </div>
    </nav>
  </div>

  <nav
      v-if="isOpen"
      id="mobile-primary-menu"
      key="mobile-nav"
      class="mobile-menu-panel fixed z-40 origin-top overflow-y-auto overscroll-contain rounded-3xl border border-white/20 shadow-2xl md:hidden glass animate-fade-in"
      :aria-label="t('mobileNav.ariaLabel')"
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
            custom
            v-slot="{ href, navigate, isActive }">
            <a
              :href="href"
              class="block rounded-xl px-4 py-3 text-base font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/60 transition-all duration-300 ease-out-expo active:scale-98 sm:hover:translate-x-2 group will-change-transform focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80"
              :class="isActive ? 'bg-white/75 text-tibet-red shadow-sm' : ''"
              :aria-current="isActive ? 'page' : undefined"
              @click="(event) => { navigate(event); closeMobileMenu() }"
            >
              <span class="flex items-center">
                <span class="flex-1">{{ item.label }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 opacity-0 group-hover:opacity-100 group-focus-visible:opacity-100 transform group-hover:translate-x-1 group-focus-visible:translate-x-1 transition-all duration-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </span>
            </a>
          </router-link>
        </div>

        <router-link v-if="auth.user && auth.user.role === 'ADMIN'" to="/admin" custom v-slot="{ href, navigate, isActive }">
          <a
            :href="href"
            class="block px-4 py-3 rounded-xl text-base font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/50 transition-all active:scale-98 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80"
            :class="isActive ? 'bg-white/75 text-tibet-red shadow-sm' : ''"
            :aria-current="isActive ? 'page' : undefined"
            @click="(event) => { navigate(event); closeMobileMenu() }"
          >
            {{ t('common.admin') }}
          </a>
        </router-link>

        <div class="mt-2 flex items-center justify-center gap-2 border-t border-tibet-gold/20 px-2 py-3 sm:px-4">
          <button
            type="button"
            @click="switchLanguage('zh')"
            :aria-pressed="currentLocale === 'zh'"
            :class="[
              'min-h-11 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80',
              currentLocale === 'zh'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-gray-100 text-tibet-brown/80 hover:bg-gray-200'
            ]">
            {{ t('common.chinese') }}
          </button>
          <button
            type="button"
            @click="switchLanguage('bo')"
            :aria-pressed="currentLocale === 'bo'"
            :class="[
              'min-h-11 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80',
              currentLocale === 'bo'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-gray-100 text-tibet-brown/80 hover:bg-gray-200'
            ]">
            {{ t('common.tibetan') }}
          </button>
        </div>

        <div class="pt-4 border-t border-tibet-gold/20 mt-2">
          <div v-if="auth.user" class="flex items-center justify-between px-4">
            <span class="flex min-w-0 items-center gap-2 font-medium text-tibet-dark/80">
              <img
                v-if="currentUserAvatar"
                :src="currentUserAvatar"
                :alt="currentUserLabel"
                class="h-8 w-8 rounded-full object-cover"
              >
              <span v-else class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-tibet-red text-xs font-bold text-tibet-yellow">
                {{ currentUserInitial }}
              </span>
              <span class="truncate">{{ currentUserLabel }}</span>
            </span>
            <button type="button" @click="logout" class="min-h-11 rounded-xl px-3 text-red-500 text-sm font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80">{{ t('common.logout') }}</button>
          </div>
          <div v-else class="grid grid-cols-2 gap-4 px-4">
            <router-link to="/login" class="flex min-h-11 items-center justify-center rounded-xl bg-gray-100 px-3 py-2 text-center font-medium text-tibet-dark/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80" @click="closeMobileMenu()">{{ t('common.login') }}</router-link>
            <router-link to="/register" class="flex min-h-11 items-center justify-center rounded-xl bg-tibet-red px-3 py-2 text-center font-medium text-tibet-yellow focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white/80" @click="closeMobileMenu()">{{ t('common.register') }}</router-link>
          </div>
        </div>
      </div>
    </nav>
</template>

<style scoped>
.nav-safe-frame {
  padding-top: calc(0.75rem + env(safe-area-inset-top));
}

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

.mobile-menu-panel {
  left: max(0.75rem, env(safe-area-inset-left));
  right: max(0.75rem, env(safe-area-inset-right));
  top: calc(4.75rem + env(safe-area-inset-top));
  max-height: calc(100dvh - 5.5rem - env(safe-area-inset-top) - env(safe-area-inset-bottom));
  scroll-padding-block: 0.75rem;
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

@media (min-width: 640px) {
  .nav-safe-frame {
    padding-top: calc(1rem + env(safe-area-inset-top));
  }
}

@media (max-height: 420px) and (max-width: 767px) {
  .mobile-menu-panel {
    top: calc(3.75rem + env(safe-area-inset-top));
    max-height: calc(100dvh - 4.25rem - env(safe-area-inset-top));
  }
}
</style>
