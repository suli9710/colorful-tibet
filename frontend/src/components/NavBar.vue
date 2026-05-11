<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Menu, X, LogOut } from 'lucide-vue-next'
import { AnimatePresence, LayoutGroup, motion, useScroll, useTransform } from 'motion-v'
import { updateMemoizedLocale, clearTokenCache } from '../api/index'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const { t, locale } = useI18n()
const auth = useAuthStore()
const isOpen = ref(false)
const { scrollY } = useScroll()
const navMaxWidth = useTransform(scrollY, [0, 120], ['1280px', '1024px'])
const navBackground = useTransform(scrollY, [0, 120], ['rgba(247, 243, 238, 0.18)', 'rgba(247, 243, 238, 0.86)'])
const navBlur = useTransform(scrollY, [0, 120], ['blur(4px) saturate(150%)', 'blur(20px) saturate(180%)'])
const navShadow = useTransform(scrollY, [0, 120], ['0 0 0 rgba(139, 46, 58, 0)', '0 4px 24px rgba(139, 46, 58, 0.12)'])
const navBorderColor = useTransform(scrollY, [0, 120], ['rgba(197, 150, 75, 0)', 'rgba(197, 150, 75, 0.22)'])
const navPaddingY = useTransform(scrollY, [0, 120], ['1rem', '0.625rem'])
const navBrandOpacity = useTransform(scrollY, [0, 120], [0.8, 1])

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
      class="pointer-events-auto will-change-transform rounded-full border py-2.5 px-6 tibet-nav-shell"
      :initial="{ y: -18, opacity: 0, scale: 0.98 }"
      :animate="{ y: 0, opacity: 1, scale: 1 }"
      :transition="{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }"
      :style="{
        maxWidth: navMaxWidth,
        backgroundColor: navBackground,
        backdropFilter: navBlur,
        boxShadow: navShadow,
        borderColor: navBorderColor,
        borderWidth: '1px',
        paddingTop: navPaddingY,
        paddingBottom: navPaddingY,
      }">
      <div class="flex justify-between items-center">
        <div class="flex-shrink-0 flex items-center">
          <router-link to="/" class="flex items-center space-x-2 group">
            <span class="tibet-brand-sigil flex-shrink-0"></span>
            <motion.span class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-tibet-yellow via-tibet-gold to-tibet-red tibetan-font"
              :style="{ opacity: navBrandOpacity }">
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
              :whileHover="{ y: -2, scale: 1.04 }"
              :whilePress="{ scale: 0.96 }"
              @click="navigate"
            >
              <motion.span
                v-if="isActive"
                layoutId="desktop-nav-active-pill"
                class="absolute inset-0 rounded-full bg-white/75 shadow-sm border border-tibet-gold/20"
                :transition="{ type: 'spring', stiffness: 430, damping: 34 }"
              />
              <span class="absolute inset-0 bg-gradient-to-r from-tibet-gold/10 to-tibet-red/10 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-300 ease-out-expo"></span>
              <span class="relative z-10">{{ item.label }}</span>
            </motion.a>
          </router-link>

          <router-link v-if="auth.user && auth.user.role === 'ADMIN'" to="/admin" custom v-slot="{ href, navigate, isActive }">
            <motion.a
              :href="href"
              class="relative px-4 py-2 rounded-full text-sm font-medium text-tibet-brown/80 hover:text-tibet-dark transition-colors duration-200"
              :whileHover="{ y: -2, scale: 1.04 }"
              :whilePress="{ scale: 0.96 }"
              @click="navigate"
            >
              <motion.span
                v-if="isActive"
                layoutId="desktop-nav-active-pill"
                class="absolute inset-0 rounded-full bg-white/75 shadow-sm border border-tibet-gold/20"
                :transition="{ type: 'spring', stiffness: 430, damping: 34 }"
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
            :whileHover="{ y: -1, scale: 1.04 }"
            :whilePress="{ scale: 0.94 }"
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
            :whileHover="{ y: -1, scale: 1.04 }"
            :whilePress="{ scale: 0.94 }"
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
            <motion.div :whileHover="{ y: -3, scale: 1.05 }" :whilePress="{ scale: 0.95 }">
              <router-link to="/register" class="bg-tibet-red hover:bg-tibet-red/90 text-tibet-yellow text-sm font-medium px-4 py-2 rounded-full transition-colors duration-300 ease-out-expo shadow-lg shadow-tibet-red/30 hover:shadow-tibet-red/50 relative overflow-hidden group will-change-transform">
                <span class="relative z-10">{{ t('common.register') }}</span>
              </router-link>
            </motion.div>
          </div>
        </div>

        <div class="flex items-center md:hidden">
          <motion.button @click="isOpen = !isOpen" class="text-tibet-brown/80 hover:text-tibet-dark p-2 rounded-lg hover:bg-white/50 transition-colors" :whileTap="{ scale: 0.9 }">
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
      :initial="{ opacity: 0, y: -14, scale: 0.96, filter: 'blur(8px)' }"
      :animate="{ opacity: 1, y: 0, scale: 1, filter: 'blur(0px)' }"
      :exit="{ opacity: 0, y: -10, scale: 0.97, filter: 'blur(6px)' }"
      :transition="{ duration: 0.32, ease: [0.16, 1, 0.3, 1] }"
    >
      <div class="px-4 pt-2 pb-6 space-y-1">
        <motion.div
          v-for="(item, index) in navItems"
          :key="item.path"
          :initial="{ opacity: 0, x: -10 }"
          :animate="{ opacity: 1, x: 0 }"
          :transition="{ delay: index * 0.035, duration: 0.28, ease: [0.16, 1, 0.3, 1] }"
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
