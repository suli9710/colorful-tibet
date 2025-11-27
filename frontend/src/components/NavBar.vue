<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Menu, X, LogOut, User } from 'lucide-vue-next'

const router = useRouter()
const isOpen = ref(false)
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const isScrolled = ref(false)

const checkScroll = () => {
  isScrolled.value = window.scrollY > 20
}

onMounted(() => {
  window.addEventListener('scroll', checkScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', checkScroll)
})

const logout = () => {
  localStorage.removeItem('user')
  user.value = null
  router.push('/login')
}

const isLoggedIn = ref(!!user.value)
</script>

<template>
  <div class="fixed top-0 left-0 w-full z-50 flex justify-center pt-4 px-4 pointer-events-none">
    <nav :class="[
      'pointer-events-auto transition-all duration-700 ease-out-expo will-change-transform',
      isScrolled 
        ? 'w-full max-w-5xl bg-white/80 backdrop-blur-xl shadow-xl rounded-full border border-white/30 py-2.5 px-6' 
        : 'w-full max-w-7xl bg-transparent py-4'
    ]">
      <div class="flex justify-between items-center">
        <!-- Logo -->
        <div class="flex-shrink-0 flex items-center">
          <router-link to="/" class="flex items-center space-x-2 group">
            <span :class="[
              'text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-purple-600 transition-opacity',
              isScrolled ? 'opacity-100' : 'group-hover:opacity-80'
            ]">
              七彩西藏
            </span>
          </router-link>
        </div>

        <!-- Desktop Menu -->
        <div class="hidden md:flex items-center space-x-1">
          <router-link v-for="item in [
            { path: '/', label: '首页' },
            { path: '/spots', label: '景点导览' },
            { path: '/route-planner', label: 'AI行程' },
            { path: '/community', label: '社区' },
            { path: '/heritage', label: '非遗文化' },
            { path: '/news', label: '资讯' },
          ]" :key="item.path" :to="item.path" 
          class="relative px-4 py-2 rounded-full text-sm font-medium text-apple-gray-600 hover:text-apple-gray-900 hover:bg-white/60 transition-all duration-300 ease-out-expo active:scale-95 group will-change-transform">
            <span class="relative z-10">{{ item.label }}</span>
            <span class="absolute inset-0 bg-gradient-to-r from-blue-500/10 to-purple-500/10 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-300 ease-out-expo"></span>
          </router-link>
          
          <router-link v-if="user && user.role === 'ADMIN'" to="/admin" class="px-4 py-2 rounded-full text-sm font-medium text-apple-gray-600 hover:text-apple-gray-900 hover:bg-white/50 transition-all duration-200 active:scale-95">
            管理
          </router-link>
        </div>
        
        <!-- User Menu -->
        <div class="hidden md:flex items-center space-x-4">
          <div v-if="user" class="flex items-center space-x-3 bg-white/50 backdrop-blur-sm px-4 py-1.5 rounded-full border border-white/20 shadow-sm">
            <span class="text-sm font-medium text-apple-gray-700">{{ user.nickname || user.username }}</span>
            <button @click="logout" class="text-apple-gray-500 hover:text-red-500 transition-colors active:scale-90">
              <LogOut class="w-4 h-4" />
            </button>
          </div>
          <div v-else class="flex items-center space-x-3">
            <router-link to="/login" class="text-sm font-medium text-apple-gray-600 hover:text-apple-gray-900 transition-colors">
              登录
            </router-link>
            <router-link to="/register" class="bg-apple-blue hover:bg-apple-blue-hover text-white text-sm font-medium px-4 py-2 rounded-full transition-all duration-300 ease-out-expo shadow-lg shadow-blue-500/30 hover:shadow-blue-500/50 hover:-translate-y-1 hover:scale-105 active:scale-95 relative overflow-hidden group will-change-transform">
              <span class="relative z-10">注册</span>
              <span class="absolute inset-0 bg-gradient-to-r from-blue-400 to-purple-400 opacity-0 group-hover:opacity-100 transition-opacity duration-300 ease-out-expo"></span>
            </router-link>
          </div>
        </div>

        <!-- Mobile menu button -->
        <div class="flex items-center md:hidden">
          <button @click="isOpen = !isOpen" class="text-apple-gray-600 hover:text-apple-gray-900 p-2 rounded-lg hover:bg-white/50 transition-colors">
            <Menu v-if="!isOpen" class="w-6 h-6" />
            <X v-else class="w-6 h-6" />
          </button>
        </div>
      </div>
    </nav>
  </div>

  <!-- Mobile menu overlay -->
  <transition
    enter-active-class="transition duration-300 ease-out-expo"
    enter-from-class="transform -translate-y-8 opacity-0 scale-95"
    enter-to-class="transform translate-y-0 opacity-100 scale-100"
    leave-active-class="transition duration-200 ease-in"
    leave-from-class="transform translate-y-0 opacity-100 scale-100"
    leave-to-class="transform -translate-y-8 opacity-0 scale-95"
  >
    <div v-if="isOpen" class="fixed top-20 left-4 right-4 z-40 md:hidden glass rounded-3xl border border-white/20 shadow-2xl">
      <div class="px-4 pt-2 pb-6 space-y-1">
        <router-link v-for="item in [
          { path: '/', label: '首页' },
          { path: '/spots', label: '景点导览' },
          { path: '/route-planner', label: 'AI行程' },
          { path: '/community', label: '社区' },
          { path: '/heritage', label: '非遗文化' },
          { path: '/news', label: '资讯' },
        ]" :key="item.path" :to="item.path" 
        class="block px-4 py-3 rounded-xl text-base font-medium text-apple-gray-600 hover:text-apple-gray-900 hover:bg-white/60 transition-all duration-300 ease-out-expo active:scale-98 hover:translate-x-2 group will-change-transform"
        @click="isOpen = false">
          <span class="flex items-center">
            <span class="flex-1">{{ item.label }}</span>
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 opacity-0 group-hover:opacity-100 transform group-hover:translate-x-1 transition-all duration-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </span>
        </router-link>
        
        <router-link v-if="user && user.role === 'ADMIN'" to="/admin"
          class="block px-4 py-3 rounded-xl text-base font-medium text-apple-gray-600 hover:text-apple-gray-900 hover:bg-white/50 transition-all active:scale-98"
          @click="isOpen = false">
          管理
        </router-link>
        
        <div class="pt-4 border-t border-gray-100 mt-2">
          <div v-if="user" class="flex items-center justify-between px-4">
            <span class="font-medium text-apple-gray-700">{{ user.nickname || user.username }}</span>
            <button @click="logout" class="text-red-500 text-sm font-medium">退出登录</button>
          </div>
          <div v-else class="grid grid-cols-2 gap-4 px-4">
            <router-link to="/login" class="text-center py-2 rounded-xl bg-gray-100 text-apple-gray-700 font-medium">登录</router-link>
            <router-link to="/register" class="text-center py-2 rounded-xl bg-apple-blue text-white font-medium">注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>
