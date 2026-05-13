<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Menu, X, LogOut } from 'lucide-vue-next'
import { AnimatePresence, LayoutGroup, motion } from 'motion-v'
import { updateMemoizedLocale, clearTokenCache } from '../api/index'
import { useAuthStore } from '../stores/auth'
import {
  mobileMenuAnimate,
  mobileMenuExit,
  mobileMenuInitial,
  mobileMenuItemAnimate,
  mobileMenuItemInitial,
  mobileMenuItemTransition,
  mobileMenuTransition,
  navItemHover,
  navItemPress,
  navShellAnimate,
  navShellInitial,
  navShellTransition,
  primaryActionHover,
  primaryActionPress,
  softSpring,
  subtleButtonHover,
  subtleButtonPress
} from '../motion/presets'

const router = useRouter()
const { t, locale } = useI18n()
const auth = useAuthStore()
const isOpen = ref(false)
const isScrolled = ref(false)
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
  ...(auth.user ? [{ path: '/profile', label: t('common.profile') }] : []),
])

const switchLanguage = (lang: string) => {
  locale.value = lang
  updateMemoizedLocale(lang)
  document.documentElement.lang = lang
}

const logout = () => {
  clearTokenCache()
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="fixed top-0 left-0 w-full z-50 flex justify-center pt-4 px-4 pointer-events-none">
    <motion.nav
      layout
      class="pointer-events-auto rounded-full border px-6 tibet-nav-shell nav-shell-optimized"
      :class="isScrolled ? 'nav-shell-scrolled' : 'nav-shell-top'"
      :initial="navShellInitial"
      :animate="navShellAnimate"
      :transition="navShellTransition"
    >
      <div class="flex justify-between items-center">
        <div class="flex-shrink-0 flex items-center">
          <router-link to="/" class="flex items-center space-x-2 group">
            <span class="tibet-brand-sigil flex-shrink-0"></span>
            <motion.span class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red tibetan-font"
              :animate="{ opacity: isScrolled ? 1 : 0.86 }"
              :transition="{ duration: 0.2 }">
              {{ t('common.brandName') }}
            </motion.span>
          </router-link>
        </div>

        <LayoutGroup>
        <div class="hidden md:flex items-center space-x-1">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path" custom v-slot="{ href, navigate, isActive }">
            <motion.a
              :href="href"
              class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-colors duration-300 ease-out-expo group will-change-transform"
              :whileHover="navItemHover"
              :whilePress="navItemPress"
              @click="navigate"
            >
              <motion.span
                v-if="isActive"
                layoutId="desktop-nav-active-pill"
                class="absolute inset-0 rounded-full bg-white/75 shadow-sm border border-tibet-gold/20"
                :transition="softSpring"
              />
              <span class="absolute inset-0 bg-gradient-to-r from-tibet-gold/10 to-tibet-red/10 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-300 ease-out-expo"></span>
              <span class="relative z-10">{{ item.label }}</span>
            </motion.a>
          </router-link>

          <router-link v-if="auth.user && auth.user.role === 'ADMIN'" to="/admin" custom v-slot="{ href, navigate, isActive }">
            <motion.a
              :href="href"
              class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-colors duration-200"
              :whileHover="navItemHover"
              :whilePress="navItemPress"
              @click="navigate"
            >
              <motion.span
                v-if="isActive"
                layoutId="desktop-nav-active-pill"
                class="absolute inset-0 rounded-full bg-white/75 shadow-sm border border-tibet-gold/20"
                :transition="softSpring"
              />
              <span class="relative z-10">{{ t('common.admin') }}</span>
            </motion.a>
          </router-link>
        </div>
        </LayoutGroup>

        <div class="hidden md:flex items-center space-x-2 mr-4">
          <motion.button
            @click="switchLanguage('zh')"
            layout
            :whileHover="subtleButtonHover"
            :whilePress="subtleButtonPress"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200',
              currentLocale === 'zh'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-white/50 text-tibet-brown/80 hover:bg-white/70'
            ]">
            {{ t('common.chinese') }}
          </motion.button>
          <motion.button
            @click="switchLanguage('bo')"
            layout
            :whileHover="subtleButtonHover"
            :whilePress="subtleButtonPress"
            :class="[
              'px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200',
              currentLocale === 'bo'
                ? 'bg-tibet-red text-tibet-yellow shadow-md'
                : 'bg-white/50 text-tibet-brown/80 hover:bg-white/70'
            ]">
            {{ t('common.tibetan') }}
          </motion.button>
        </div>

        <div class="hidden md:flex items-center space-x-4">
          <div v-if="auth.user" class="flex items-center space-x-3 bg-white/50 backdrop-blur-sm px-4 py-1.5 rounded-full border border-white/20 shadow-sm">
            <span class="text-sm font-medium text-tibet-dark/80">{{ auth.user.nickname || auth.user.username }}</span>
            <motion.button @click="logout" class="text-tibet-brown/70 hover:text-red-500 transition-colors" :whileHover="{ rotate: -8, scale: 1.1 }" :whilePress="{ scale: 0.9 }">
              <LogOut class="w-4 h-4" />
            </motion.button>
          </div>
          <div v-else class="flex items-center space-x-3">
            <router-link to="/login" class="text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-colors">
              {{ t('common.login') }}
            </router-link>
            <motion.div :whileHover="primaryActionHover" :whilePress="primaryActionPress">
              <router-link to="/register" class="bg-tibet-red hover:bg-tibet-red/90 text-tibet-yellow text-sm font-medium px-4 py-2 rounded-full transition-colors duration-300 ease-out-expo shadow-lg shadow-tibet-red/30 hover:shadow-tibet-red/50 relative overflow-hidden group will-change-transform">
                <span class="relative z-10">{{ t('common.register') }}</span>
              </router-link>
            </motion.div>
          </div>
        </div>

        <div class="flex items-center md:hidden">
          <motion.button @click="isOpen = !isOpen" class="text-tibet-brown/80 hover:text-tibet-dark p-2 rounded-lg hover:bg-white/50 transition-colors" :whileTap="subtleButtonPress">
            <Menu v-if="!isOpen" class="w-6 h-6" />
            <X v-else class="w-6 h-6" />
          </motion.button>
        </div>
      </div>
    </motion.nav>
  </div>

  <AnimatePresence>
    <motion.div
      v-if="isOpen"
      key="mobile-nav"
      class="fixed top-20 left-4 right-4 z-40 md:hidden glass rounded-3xl border border-white/20 shadow-2xl origin-top"
      :initial="mobileMenuInitial"
      :animate="mobileMenuAnimate"
      :exit="mobileMenuExit"
      :transition="mobileMenuTransition"
    >
      <div class="px-4 pt-2 pb-6 space-y-1">
        <motion.div
          v-for="(item, index) in navItems"
          :key="item.path"
          :initial="mobileMenuItemInitial"
          :animate="mobileMenuItemAnimate"
          :transition="mobileMenuItemTransition(index)"
        >
          <router-link
            :to="item.path"
            class="block px-4 py-3 rounded-xl text-base font-medium text-tibet-brown/80 hover:text-tibet-dark hover:bg-white/60 transition-all duration-300 ease-out-expo active:scale-98 hover:translate-x-2 group will-change-transform"
            @click="isOpen = false">
            <span class="flex items-center">
              <span class="flex-1">{{ item.label }}</span>
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 opacity-0 group-hover:opacity-100 transform group-hover:translate-x-1 transition-all duration-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </span>
          </router-link>
        </motion.div>

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
    </motion.div>
  </AnimatePresence>
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
</style>
