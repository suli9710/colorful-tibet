<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Bot, Compass, Home, MapPinned, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const { t } = useI18n()
const auth = useAuthStore()

const hiddenRouteNames = new Set(['privacy', 'terms'])
const hiddenPathPrefixes = ['/login', '/register', '/admin', '/privacy', '/terms']

const isVisible = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  return !hiddenRouteNames.has(routeName) && !hiddenPathPrefixes.some(prefix => route.path.startsWith(prefix))
})

const getTabAriaLabel = (label: string, active: boolean) => {
  const baseLabel = t('mobileNav.goTo', { label })
  return active ? `${baseLabel}, ${t('mobileNav.currentPage', 'current page')}` : baseLabel
}

const getTabTitle = (label: string, active: boolean) => {
  return active ? `${label} (${t('mobileNav.currentPage', 'current page')})` : label
}

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
</script>

<template>
  <Teleport to="body">
    <nav
      v-if="isVisible"
      class="mobile-bottom-nav fixed inset-x-0 bottom-0 z-[95] md:hidden"
      :aria-label="t('mobileNav.ariaLabel')"
    >
      <div class="mobile-bottom-nav__shell rounded-[1.35rem] border border-tibet-gold/20 bg-white/[0.92] shadow-2xl shadow-tibet-dark/15 backdrop-blur-xl">
        <div class="tibet-prayer-flag h-1 rounded-t-[1.35rem] opacity-75"></div>
        <div class="mobile-bottom-nav__grid grid grid-cols-5 px-1.5 py-1.5">
          <router-link
            v-for="tab in tabs"
            :key="tab.key"
            :to="tab.to"
            custom
            v-slot="{ href, navigate }"
          >
            <a
              :href="href"
              class="mobile-bottom-nav__tab relative flex min-h-[56px] min-w-0 flex-col items-center justify-center gap-1 rounded-2xl px-1 py-2 text-[10px] font-semibold leading-none transition active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white"
              :class="tab.active ? 'text-tibet-red' : 'text-tibet-brown/62'"
              :aria-current="tab.active ? 'page' : undefined"
              :aria-label="getTabAriaLabel(tab.label, tab.active)"
              :title="getTabTitle(tab.label, tab.active)"
              @click="navigate"
            >
              <span
                v-if="tab.active"
                class="absolute inset-x-1 top-1 bottom-1 rounded-2xl bg-tibet-red/8"
                aria-hidden="true"
              ></span>
              <component :is="tab.icon" class="relative h-5 w-5 shrink-0" :stroke-width="tab.active ? 2.6 : 2.1" aria-hidden="true" />
              <span class="relative w-full truncate leading-[1.1]">{{ tab.label }}</span>
            </a>
          </router-link>
        </div>
      </div>
    </nav>
  </Teleport>
</template>

<style scoped>
.mobile-bottom-nav {
  padding-bottom: env(safe-area-inset-bottom);
  padding-left: max(0.75rem, env(safe-area-inset-left));
  padding-right: max(0.75rem, env(safe-area-inset-right));
  pointer-events: none;
}

.mobile-bottom-nav__shell {
  margin-bottom: 0.5rem;
  pointer-events: auto;
}

.mobile-bottom-nav__tab {
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
}

@media (max-width: 360px) {
  .mobile-bottom-nav {
    padding-left: max(0.5rem, env(safe-area-inset-left));
    padding-right: max(0.5rem, env(safe-area-inset-right));
  }

  .mobile-bottom-nav__grid {
    padding: 0.25rem;
  }

  .mobile-bottom-nav__tab {
    min-height: 50px;
    border-radius: 1rem;
  }
}

@media (max-height: 420px) and (max-width: 767px) {
  .mobile-bottom-nav {
    display: none;
  }
}
</style>
