<template>
  <div class="min-h-screen bg-apple-gray-50 py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="text-center mb-12 animate-fade-in">
        <h1 class="text-4xl font-bold text-apple-gray-900 mb-4">AI 智能行程规划</h1>
        <p class="text-lg text-apple-gray-500">
          告诉我们您的偏好，AI 导游将为您定制专属西藏之旅。
        </p>
      </div>

      <!-- Form Section -->
      <div class="glass rounded-3xl p-8 mb-12 animate-slide-up" style="animation-delay: 0.1s">
        <form @submit.prevent="generateRoute" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
            <!-- Days -->
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">计划天数</label>
              <div class="flex items-center space-x-4">
                <button type="button" @click="form.days > 1 && form.days--" 
                        class="w-10 h-10 rounded-full bg-apple-gray-100 hover:bg-apple-gray-200 flex items-center justify-center text-apple-gray-600 transition-colors">
                  -
                </button>
                <span class="text-xl font-bold text-apple-gray-900 w-8 text-center">{{ form.days }}</span>
                <button type="button" @click="form.days < 30 && form.days++" 
                        class="w-10 h-10 rounded-full bg-apple-gray-100 hover:bg-apple-gray-200 flex items-center justify-center text-apple-gray-600 transition-colors">
                  +
                </button>
              </div>
            </div>

            <!-- Budget -->
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">预算范围</label>
              <select v-model="form.budget" class="w-full px-4 py-2.5 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none transition-all appearance-none">
                <option value="经济型">经济型 (穷游)</option>
                <option value="舒适型">舒适型 (性价比)</option>
                <option value="豪华型">豪华型 (高端)</option>
              </select>
            </div>

            <!-- Preference -->
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">游玩偏好</label>
              <select v-model="form.preference" class="w-full px-4 py-2.5 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none transition-all appearance-none">
                <option value="自然风光">自然风光 (雪山/湖泊)</option>
                <option value="人文历史">人文历史 (寺庙/古迹)</option>
                <option value="深度摄影">深度摄影 (日照金山)</option>
                <option value="休闲度假">休闲度假 (林芝氧吧)</option>
              </select>
            </div>
          </div>

          <button type="submit" :disabled="loading"
                  class="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-bold py-4 px-6 rounded-2xl transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] shadow-lg hover:shadow-blue-500/30 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center">
            <span v-if="loading" class="flex flex-col items-center">
              <span class="flex items-center">
                <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                AI 正在规划路线...
              </span>
              <span class="text-sm text-white/80 mt-2">通常需要 20-30 秒，请耐心等待</span>
            </span>
            <span v-else>生成专属行程</span>
          </button>
        </form>
      </div>

      <!-- Result Section -->
      <div v-if="result" class="glass rounded-3xl p-8 md:p-12 animate-slide-up shadow-xl border border-white/50">
        <div class="prose prose-lg max-w-none prose-headings:text-apple-gray-900 prose-p:text-apple-gray-600 prose-strong:text-apple-blue">
          <div v-html="renderedResult"></div>
        </div>
        
        <div class="mt-8 pt-8 border-t border-gray-200 flex justify-end gap-4">
          <button @click="saveRoute" class="px-6 py-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-700 font-medium transition-colors flex items-center" :disabled="saving">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
            </svg>
            {{ saving ? '保存中...' : '保存行程' }}
          </button>
          <button @click="shareRoute" class="px-6 py-2 rounded-xl bg-apple-blue hover:bg-apple-blue-hover text-white font-medium transition-colors flex items-center shadow-lg shadow-blue-500/30" :disabled="sharing">
             <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            {{ sharing ? '分享中...' : '分享到社区' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import api from '../api'

const router = useRouter()

const form = ref({
  days: 7,
  budget: '舒适型',
  preference: '自然风光'
})

const loading = ref(false)
const saving = ref(false)
const sharing = ref(false)
const result = ref('')

const renderedResult = computed(() => {
  return marked(result.value)
})

const generateRoute = async () => {
  loading.value = true
  result.value = ''
  
  try {
    const response = await api.post('/routes/generate', form.value)
    result.value = response.data
  } catch (error: any) {
    console.error('Failed to generate route:', error)
    
    let errorMessage = '生成失败，请稍后重试'
    
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      errorMessage = 'AI 生成超时，请稍后重试。如果问题持续，请联系管理员。'
    } else if (error.response) {
      const status = error.response.status
      if (status === 500) {
        errorMessage = 'AI 服务暂时不可用，请稍后重试'
      } else if (status === 401) {
        errorMessage = '认证失败，请重新登录'
      } else {
        errorMessage = `服务器错误 (${status})，请稍后重试`
      }
    } else if (error.request) {
      errorMessage = '无法连接到服务器，请检查网络连接'
    }
    
    alert(errorMessage)
  } finally {
    loading.value = false
  }
}

const saveRoute = () => {
  const blob = new Blob([result.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `西藏${form.value.days}日游行程.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const shareRoute = async () => {
    if (!result.value) return
    
    // 检查是否登录
    const userStr = localStorage.getItem('user')
    if (!userStr) {
        if (confirm('分享路线需要登录账号。是否现在去登录？')) {
            router.push('/login')
        }
        return
    }
    
    sharing.value = true
    try {
        await api.post('/routes/share', {
            title: `${form.value.days}天${form.value.preference}之旅`,
            content: result.value,
            days: form.value.days,
            budget: form.value.budget,
            preference: form.value.preference
        })
        alert('分享成功！快去社区看看吧')
        router.push('/community')
    } catch (error: any) {
        console.error('Share failed:', error)
        if (error.response && error.response.status === 401) {
            if (confirm('登录已过期，请重新登录后再分享。是否现在去登录？')) {
                router.push('/login')
            }
        } else {
            alert('分享失败，请重试')
        }
    } finally {
        sharing.value = false
    }
}
</script>
