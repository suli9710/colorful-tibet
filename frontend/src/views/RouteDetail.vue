<template>
  <div class="min-h-screen bg-tibet-white py-12 pt-24 sm:py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold mx-auto"></div>
      </div>
      
      <motion.div v-else-if="routeData"
        :initial="revealInitial"
        :animate="revealInView"
        :transition="revealTransition"
      >
        <!-- Header -->
        <div class="mb-6 sm:mb-8">
          <button @click="router.back()" class="text-gray-500 hover:text-gray-900 mb-4 flex items-center">
            ← {{ t('routeDetail.backToList') }}
          </button>
          <h1 class="break-words text-2xl font-bold text-gray-900 mb-4 sm:text-3xl">{{ routeData.title }}</h1>
          <div class="flex flex-col gap-3 text-sm text-gray-500 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex flex-wrap items-center gap-3 sm:gap-4">
              <span>{{ t('routeDetail.author') }}：{{ routeAuthorName(routeData) }}</span>
              <span>{{ t('routeDetail.publishedAt') }}：{{ formatDate(routeData.createdAt) }}</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <span class="px-2 py-1 bg-blue-50 text-blue-600 rounded-lg">{{ routeData.days }}{{ t('routeDetail.days') }}</span>
              <span class="px-2 py-1 bg-gray-100 rounded-lg">{{ routeData.budget }}</span>
              <span class="px-2 py-1 bg-gray-100 rounded-lg">{{ routeData.preference }}</span>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="glass-card rounded-2xl p-4 mb-6 sm:rounded-3xl sm:p-8 sm:mb-8">
          <div class="prose max-w-none overflow-x-auto prose-headings:text-tibet-dark prose-p:text-tibet-brown/80 prose-strong:text-tibet-gold sm:prose-lg">
            <div v-html="renderedContent"></div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-center gap-3 mb-8 sm:gap-6 sm:mb-12">
          <button 
            @click="toggleLike" 
            class="flex items-center gap-2 px-6 py-3 rounded-full transition-all duration-300 shadow-sm hover:shadow-md"
            :class="isLiked ? 'bg-red-50 text-red-600 border border-red-200' : 'bg-white text-gray-600 border border-tibet-gold/25 hover:bg-gray-50'"
          >
            <span class="text-xl">{{ isLiked ? '❤️' : '🤍' }}</span>
            <span class="font-medium">{{ routeData.likeCount }}</span>
          </button>
          
          <div class="flex items-center gap-2 px-6 py-3 bg-white text-gray-600 rounded-full border border-tibet-gold/25 shadow-sm">
            <span class="text-xl">👁️</span>
            <span class="font-medium">{{ routeData.viewCount }}</span>
          </div>
        </div>

        <!-- Comments -->
        <div class="glass-card rounded-2xl p-4 sm:rounded-3xl sm:p-8">
          <h3 class="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2">
            {{ t('routeDetail.comments') }} <span class="text-sm font-normal text-gray-500">({{ comments.length }})</span>
          </h3>
          
          <!-- Add Comment -->
          <div class="mb-8">
            <textarea 
              v-model="newComment" 
              rows="3"
              class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none transition-all resize-none mb-4"
              :placeholder="t('routeDetail.commentPlaceholder')"
            ></textarea>
            <div class="flex justify-end">
              <button 
                @click="submitComment" 
                :disabled="!newComment.trim() || submitting"
                class="px-6 py-2 bg-tibet-gold text-white rounded-xl hover:bg-tibet-gold/80 transition-colors disabled:opacity-50"
              >
                {{ submitting ? t('routeDetail.submitting') : t('routeDetail.postComment') }}
              </button>
            </div>
          </div>
          
          <!-- Comment List -->
          <div class="space-y-6">
            <div v-for="comment in comments" :key="comment.id" class="border-b border-tibet-gold/20 last:border-0 pb-6 last:pb-0">
              <div class="flex flex-col gap-1 mb-2 sm:flex-row sm:items-start sm:justify-between">
                <span class="font-medium text-gray-900">{{ comment.user?.username || t('routeDetail.anonymous') }}</span>
                <div class="flex flex-wrap items-center gap-3">
                  <span class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</span>
                  <button
                    v-if="isOwnComment(comment)"
                    @click="deleteComment(comment)"
                    class="text-xs font-medium text-red-500 hover:text-red-700 transition-colors"
                  >
                    {{ t('common.delete') }}
                  </button>
                </div>
              </div>
              <p class="text-gray-600">{{ comment.content }}</p>
            </div>
            
            <div v-if="comments.length === 0" class="text-center text-gray-400 py-4">
              {{ t('routeDetail.noComments') }}
            </div>
          </div>
        </div>
      </motion.div>

      <div v-else class="text-center py-12 text-gray-500">
        {{ t('routeDetail.notFound') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { motion } from 'motion-v'
import { revealInitial, revealInView, revealTransition } from '../motion/presets'
import api, { endpoints } from '../api'
import { useAuthStore } from '../stores/auth'

const { t } = useI18n()

const currentRoute = useRoute()
const router = useRouter()
const auth = useAuthStore()
const routeId = currentRoute.params.id

const loading = ref(true)
const routeData = ref<any>(null)
const comments = ref<any[]>([])
const isLiked = ref(false)
const newComment = ref('')
const submitting = ref(false)
const currentUser = computed(() => auth.user)

const renderedContent = computed(() => {
  return routeData.value ? DOMPurify.sanitize(marked(routeData.value.content) as string) : ''
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
  if (!(await auth.ensureSession())) {
    alert(t('routeDetail.loginRequiredLike'))
    router.push('/login')
    return
  }

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
      alert(t('routeDetail.loginRequiredLike'))
      router.push('/login')
    } else {
      alert(t('routeDetail.operationFailed'))
    }
  }
}

const submitComment = async () => {
  if (!newComment.value.trim()) return
  if (!(await auth.ensureSession())) {
    alert(t('routeDetail.loginRequiredComment'))
    router.push('/login')
    return
  }
  
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
      alert(t('routeDetail.loginRequiredComment'))
      router.push('/login')
    } else {
      alert(t('routeDetail.commentFailed'))
    }
  } finally {
    submitting.value = false
  }
}

const isOwnComment = (comment: any) => {
  if (!currentUser.value || !comment?.user) return false
  return Number(comment.user.id) === Number(currentUser.value.id) || comment.user.username === currentUser.value.username
}

const routeAuthorName = (route: any) => {
  if (route?.sourceType === 'OFFICIAL' && !route?.author?.username) {
    return t('community.officialRoute')
  }
  return route?.author?.nickname || route?.author?.username || t('routeDetail.anonymous')
}

const deleteComment = async (comment: any) => {
  if (!confirm(t('routeDetail.confirmDeleteComment'))) return

  try {
    await api.delete(endpoints.routes.deleteSharedComment(Number(routeId), comment.id))
    comments.value = comments.value.filter(item => item.id !== comment.id)
    if (routeData.value?.commentCount > 0) {
      routeData.value.commentCount--
    }
  } catch (error) {
    console.error('Failed to delete comment:', error)
    alert(t('routeDetail.deleteCommentFailed'))
  }
}

const formatDate = (dateStr: string) => {
  const locale = localStorage.getItem('locale') || 'zh'
  return new Date(dateStr).toLocaleString(locale === 'bo' ? 'bo-CN' : 'zh-CN')
}

onMounted(async () => {
  await auth.refreshSession()
  loadRouteDetail()
})
</script>
