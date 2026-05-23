<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Bot, Compass, Home, MapPinned, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const auth = useAuthStore()

const hiddenRouteNames = new Set(['privacy', 'terms'])
const hiddenPathPrefixes = ['/login', '/register', '/admin', '/privacy', '/terms']

const isVisible = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  return !hiddenRouteNames.has(routeName) && !hiddenPathPrefixes.some(prefix => route.path.startsWith(prefix))
})

const tabs = computed(() => [
  {
    key: 'home',
    label: t('common.home'),
    to: '/',
    icon: Home,
    active: route.path === '/'
  },
  {
    key: 'spots',
    label: t('mobileNav.spots', '景点'),
    to: '/spots',
    icon: MapPinned,
    active: route.path.startsWith('/spots')
  },
  {
    key: 'ai',
    label: t('mobileNav.ai', 'AI'),
    to: '/route-planner',
    icon: Bot,
    active: route.path.startsWith('/route-planner') || route.path.startsWith('/route')
  },
  {
    key: 'community',
    label: t('common.community'),
    to: '/community',
    icon: Compass,
    active: route.path.startsWith('/community') || route.path.startsWith('/create-route')
  },
  {
    key: 'mine',
    label: t('mobileNav.mine', t('common.profile')),
    to: auth.user ? '/profile' : '/login',
    icon: UserRound,
    active: ['/profile', '/orders', '/hotel-orders', '/favorites'].some(prefix => route.path.startsWith(prefix))
  }
])

const go = (to: string) => {
  router.push(to)
}
</script>

<template>
  <Teleport to="body">
    <nav
      v-if="isVisible"
      class="mobile-bottom-nav fixed inset-x-0 bottom-0 z-[95] md:hidden"
      aria-label="Mobile primary navigation"
    >
      <div class="mx-3 mb-2 rounded-[1.35rem] border border-tibet-gold/20 bg-white/[0.92] shadow-2xl shadow-tibet-dark/15 backdrop-blur-xl">
        <div class="tibet-prayer-flag h-1 rounded-t-[1.35rem] opacity-75"></div>
        <div class="grid grid-cols-5 px-1.5 py-1.5">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="relative flex min-w-0 flex-col items-center justify-center gap-1 rounded-2xl px-1 py-2 text-[11px] font-semibold transition active:scale-95"
            :class="tab.active ? 'text-tibet-red' : 'text-tibet-brown/62'"
            @click="go(tab.to)"
          >
            <span
              v-if="tab.active"
              class="absolute inset-x-1 top-1 bottom-1 rounded-2xl bg-tibet-red/8"
              aria-hidden="true"
            ></span>
            <component :is="tab.icon" class="relative h-5 w-5" :stroke-width="tab.active ? 2.6 : 2.1" />
            <span class="relative w-full truncate leading-none">{{ tab.label }}</span>
          </button>
        </div>
      </div>
    </nav>
  </Teleport>
</template>

<style scoped>
.mobile-bottom-nav {
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
