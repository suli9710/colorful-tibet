<template>
  <div class="min-h-screen flex items-center justify-center bg-cover bg-center py-12 px-4 sm:px-6 lg:px-8 relative overflow-hidden"
       style="background-image: url('/heritage/布达拉宫3.jpg')">
    
    <!-- Overlay -->
    <div class="absolute inset-0 bg-black/30 backdrop-blur-[2px]"></div>

    <div class="max-w-md w-full space-y-8 relative z-10 glass p-10 rounded-3xl animate-scale-in shadow-2xl gpu-accelerated">
      <div class="text-center animate-slide-up" style="animation-delay: 0.1s">
        <h2 class="text-3xl font-bold text-white mb-2">欢迎回来</h2>
        <p class="text-gray-200 animate-fade-in" style="animation-delay: 0.2s">登录您的七彩西藏账号</p>
      </div>
      
      <form class="mt-8 space-y-6 animate-slide-up" style="animation-delay: 0.2s" @submit.prevent="handleLogin">
        <div class="rounded-md shadow-sm -space-y-px">
          <div class="mb-4 animate-slide-up" style="animation-delay: 0.3s">
            <label for="username" class="sr-only">用户名</label>
            <input id="username" name="username" type="text" required v-model="form.username"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-white/30 placeholder-gray-300 text-white bg-white/10 focus:outline-none focus:ring-2 focus:ring-apple-blue focus:border-transparent focus:z-10 sm:text-sm backdrop-blur-sm input-focus"
                   placeholder="用户名">
          </div>
          <div class="animate-slide-up" style="animation-delay: 0.4s">
            <label for="password" class="sr-only">密码</label>
            <input id="password" name="password" type="password" required v-model="form.password"
                   class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-white/30 placeholder-gray-300 text-white bg-white/10 focus:outline-none focus:ring-2 focus:ring-apple-blue focus:border-transparent focus:z-10 sm:text-sm backdrop-blur-sm input-focus"
                   placeholder="密码">
          </div>
        </div>

        <div class="animate-slide-up" style="animation-delay: 0.5s">
          <button type="submit" :disabled="loading"
                  class="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-xl text-white bg-apple-blue hover:bg-apple-blue-hover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-apple-blue transition-all duration-300 ease-out-expo shadow-lg hover:shadow-blue-500/40 transform hover:-translate-y-1 hover:scale-105 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none overflow-hidden will-change-transform">
            <span v-if="loading" class="absolute left-0 inset-y-0 flex items-center pl-3 z-10">
              <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            <span class="relative z-10">{{ loading ? '登录中...' : '登录' }}</span>
            <span class="absolute inset-0 bg-gradient-to-r from-blue-400 to-purple-400 opacity-0 group-hover:opacity-100 transition-opacity duration-300 ease-out-expo"></span>
          </button>
        </div>
      </form>
      
      <div class="text-center mt-4 animate-fade-in" style="animation-delay: 0.6s">
        <p class="text-sm text-gray-200">
          还没有账号？
          <router-link to="/register" class="font-medium text-white hover:text-apple-blue transition-colors duration-300 ease-out-expo underline decoration-apple-blue/50 hover:decoration-apple-blue">
            立即注册
          </router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  loading.value = true
  try {
    const response = await api.post('/auth/login', form.value)
    const user = response.data
    localStorage.setItem('user', JSON.stringify(user))
    // Trigger a custom event or use a store to update NavBar state
    window.dispatchEvent(new Event('storage')) 
    router.push('/')
  } catch (error) {
    console.error('Login failed:', error)
    alert('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>
