<template>
  <div class="min-h-screen bg-apple-gray-50 py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-apple-blue mx-auto"></div>
      </div>
      
      <div v-else-if="route" class="animate-fade-in">
        <!-- Header -->
        <div class="mb-8">
          <button @click="router.back()" class="text-gray-500 hover:text-gray-900 mb-4 flex items-center">
            ← 返回列表
          </button>
          <h1 class="text-3xl font-bold text-gray-900 mb-4">{{ route.title }}</h1>
          <div class="flex items-center justify-between text-sm text-gray-500">
            <div class="flex items-center gap-4">
              <span>作者：{{ route.author?.username || '匿名用户' }}</span>
              <span>发布于：{{ formatDate(route.createdAt) }}</span>
            </div>
            <div class="flex gap-2">
              <span class="px-2 py-1 bg-blue-50 text-blue-600 rounded-lg">{{ route.days }}天</span>
              <span class="px-2 py-1 bg-gray-100 rounded-lg">{{ route.budget }}</span>
              <span class="px-2 py-1 bg-gray-100 rounded-lg">{{ route.preference }}</span>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="glass rounded-3xl p-8 mb-8">
          <div class="prose prose-lg max-w-none prose-headings:text-apple-gray-900 prose-p:text-apple-gray-600 prose-strong:text-apple-blue">
            <div v-html="renderedContent"></div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-center gap-6 mb-12">
          <button 
            @click="toggleLike" 
            class="flex items-center gap-2 px-6 py-3 rounded-full transition-all duration-300 shadow-sm hover:shadow-md"
            :class="isLiked ? 'bg-red-50 text-red-600 border border-red-200' : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'"
          >
            <span class="text-xl">{{ isLiked ? '❤️' : '🤍' }}</span>
            <span class="font-medium">{{ route.likeCount }}</span>
          </button>
          
          <div class="flex items-center gap-2 px-6 py-3 bg-white text-gray-600 rounded-full border border-gray-200 shadow-sm">
            <span class="text-xl">👁️</span>
            <span class="font-medium">{{ route.viewCount }}</span>
          </div>
        </div>

        <!-- Comments -->
        <div class="glass rounded-3xl p-8">
          <h3 class="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2">
            评论 <span class="text-sm font-normal text-gray-500">({{ comments.length }})</span>
          </h3>
          
          <!-- Add Comment -->
          <div class="mb-8">
            <textarea 
              v-model="newComment" 
              rows="3"
              class="w-full px-4 py-3 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue outline-none transition-all resize-none mb-4"
              placeholder="写下你的想法..."
            ></textarea>
            <div class="flex justify-end">
              <button 
                @click="submitComment" 
                :disabled="!newComment.trim() || submitting"
                class="px-6 py-2 bg-apple-blue text-white rounded-xl hover:bg-apple-blue-hover transition-colors disabled:opacity-50"
              >
                {{ submitting ? '发送中...' : '发表评论' }}
              </button>
            </div>
          </div>
          
          <!-- Comment List -->
          <div class="space-y-6">
            <div v-for="comment in comments" :key="comment.id" class="border-b border-gray-100 last:border-0 pb-6 last:pb-0">
              <div class="flex justify-between items-start mb-2">
                <span class="font-medium text-gray-900">{{ comment.user?.username || '匿名用户' }}</span>
                <span class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <p class="text-gray-600">{{ comment.content }}</p>
            </div>
            
            <div v-if="comments.length === 0" class="text-center text-gray-400 py-4">
              暂无评论，快来抢沙发吧！
            </div>
          </div>
        </div>
      </div>
      
      <div v-else class="text-center py-12 text-gray-500">
        未找到该路线
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import api from '../api'

const route = useRoute()
const router = useRouter()
const routeId = route.params.id

const loading = ref(true)
const routeData = ref<any>(null)
const comments = ref<any[]>([])
const isLiked = ref(false)
const newComment = ref('')
const submitting = ref(false)

const renderedContent = computed(() => {
  return routeData.value ? marked(routeData.value.content) : ''
})

const loadRouteDetail = async () => {
  loading.value = true
  try {
    const [routeRes, commentsRes, likeRes] = await Promise.all([
      api.get(`/routes/shared/${routeId}`),
      api.get(`/routes/shared/${routeId}/comments`),
      api.get(`/routes/shared/${routeId}/like-status`).catch(() => ({ data: { liked: false } }))
    ])
    
    routeData.value = routeRes.data
    comments.value = commentsRes.data
    isLiked.value = likeRes.data.liked
  } catch (error) {
    console.error('Failed to load route detail:', error)
  } finally {
    loading.value = false
  }
}

const toggleLike = async () => {
  try {
    if (isLiked.value) {
      await api.delete(`/routes/shared/${routeId}/like`)
      routeData.value.likeCount--
      isLiked.value = false
    } else {
      await api.post(`/routes/shared/${routeId}/like`)
      routeData.value.likeCount++
      isLiked.value = true
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      alert('请先登录后再点赞')
      router.push('/login')
    } else {
      alert('操作失败，请重试')
    }
  }
}

const submitComment = async () => {
  if (!newComment.value.trim()) return
  
  submitting.value = true
  try {
    const response = await api.post(`/routes/shared/${routeId}/comments`, {
      content: newComment.value
    })
    
    comments.value.unshift(response.data)
    routeData.value.commentCount++
    newComment.value = ''
  } catch (error: any) {
    if (error.response?.status === 401) {
      alert('请先登录后再评论')
      router.push('/login')
    } else {
      alert('评论失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  loadRouteDetail()
})
</script>
