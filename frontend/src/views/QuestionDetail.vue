<template>
  <div class="min-h-screen bg-tibet-white py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-red mx-auto"></div>
      </div>

      <div v-else-if="question" class="animate-fade-in">
        <!-- Back -->
        <button @click="router.back()" class="text-gray-500 hover:text-gray-900 mb-6 flex items-center gap-1 text-sm">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
          返回社区
        </button>

        <!-- Question Header -->
        <div class="glass-card rounded-3xl p-8 mb-6">
          <div class="flex items-start justify-between gap-4 mb-4">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-3">
                <span v-if="question.isResolved" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-600 text-xs font-semibold">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                  {{ t('community.solved') }}
                </span>
                <span v-else class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-amber-50 text-amber-600 text-xs font-semibold">
                  {{ t('community.unsolved') }}
                </span>
                <span v-if="question.tags" v-for="tag in parseTags(question.tags)" :key="tag" class="px-2 py-1 rounded-lg text-xs font-medium text-white" :style="{ backgroundColor: getTagColor(tag) }">{{ tag }}</span>
              </div>
              <h1 class="text-2xl font-bold text-gray-900 mb-3">{{ question.title }}</h1>
              <div class="flex items-center gap-3 text-sm text-gray-400">
                <span>{{ question.author?.username || '匿名用户' }}</span>
                <span>·</span>
                <span>{{ formatDate(question.createdAt) }}</span>
                <span>·</span>
                <span>{{ question.viewCount }} 次浏览</span>
              </div>
            </div>
          </div>
          <div class="prose prose-slate max-w-none text-gray-700 leading-relaxed">{{ question.content }}</div>

          <!-- Question Actions -->
          <div class="flex items-center gap-4 mt-6 pt-6 border-t border-tibet-gold/20">
            <button @click="toggleLike" class="flex items-center gap-2 px-4 py-2 rounded-xl transition-all text-sm font-medium"
                    :class="isLiked ? 'bg-red-50 text-red-600' : 'bg-gray-50 text-gray-500 hover:bg-gray-100'">
              <span>{{ isLiked ? '❤️' : '🤍' }}</span>
              <span>{{ question.likeCount }}</span>
            </button>
            <button v-if="isAuthor" @click="deleteQuestion" class="ml-auto px-4 py-2 rounded-xl bg-gray-50 text-gray-400 hover:bg-red-50 hover:text-red-500 transition-colors text-sm">
              删除问题
            </button>
          </div>
        </div>

        <!-- Answers Section -->
        <div class="mb-6">
          <h3 class="text-lg font-bold text-gray-900 mb-4">
            {{ t('community.answers') }} ({{ answers.length }})
          </h3>

          <div v-if="answers.length === 0" class="text-center py-8 text-gray-400 text-sm">
            {{ t('community.noAnswers') }}
          </div>

          <div v-else class="space-y-4">
            <div v-for="answer in answers" :key="answer.id"
                 class="glass-card rounded-2xl p-6 transition-all"
                 :class="answer.isAccepted ? 'border-2 border-emerald-300 bg-emerald-50/40' : 'border border-white/20'">
              <div class="flex items-start justify-between gap-4 mb-3">
                <div class="flex items-center gap-2">
                  <span class="font-semibold text-gray-900 text-sm">{{ answer.user?.username || '匿名用户' }}</span>
                  <span v-if="answer.isAccepted" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-600 text-xs font-medium">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    {{ t('community.accepted') }}
                  </span>
                </div>
                <span class="text-xs text-gray-400">{{ formatDate(answer.createdAt) }}</span>
              </div>
              <p class="text-gray-700 text-sm leading-relaxed">{{ answer.content }}</p>
              <div class="flex items-center gap-4 mt-4 pt-4 border-t border-tibet-gold/20">
                <span class="text-xs text-gray-400 flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                  {{ answer.likeCount }}
                </span>
                <button v-if="isQuestionAuthor && !answer.isAccepted && !question.isResolved"
                        @click="acceptAnswer(answer.id)"
                        class="ml-auto px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-600 hover:bg-emerald-100 text-xs font-medium transition-colors">
                  {{ t('community.acceptAnswer') }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Write Answer -->
        <div class="glass-card rounded-3xl p-8">
          <h3 class="text-lg font-bold text-gray-900 mb-4">{{ t('community.writeAnswer') }}</h3>
          <textarea v-model="newAnswer" rows="4"
                    :placeholder="t('community.yourAnswer')"
                    class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-red focus:ring-4 focus:ring-red-50 outline-none transition-all text-sm resize-none mb-4"></textarea>
          <div class="flex justify-end">
            <button @click="submitAnswer" :disabled="!newAnswer.trim() || answering"
                    class="px-6 py-2.5 bg-gradient-to-r from-tibet-red to-rose-600 text-white rounded-xl shadow-lg shadow-red-500/20 hover:shadow-xl hover:shadow-red-500/30 transition-all disabled:opacity-50 text-sm font-medium">
              {{ answering ? t('community.submittingAnswer') : t('community.submitAnswer') }}
            </button>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-12 text-gray-500">问题不存在</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '../api'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const question = ref<any>(null)
const answers = ref<any[]>([])
const loading = ref(true)
const answering = ref(false)
const newAnswer = ref('')
const isLiked = ref(false)

const currentUserId = computed(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return null
  try {
    const user = JSON.parse(userStr)
    return user?.id || user?.data?.id || null
  } catch { return null }
})

const isAuthor = computed(() => currentUserId.value && question.value?.author?.id === currentUserId.value)
const isQuestionAuthor = computed(() => currentUserId.value && question.value?.author?.id === currentUserId.value)

interface TagOption { value: string; color: string }

const getTagColor = (tagValue: string): string => {
  const tags = t('community.tagLabels')
  if (Array.isArray(tags)) {
    const tag = (tags as TagOption[]).find(t => t.value === tagValue)
    if (tag) return tag.color
  }
  return '#6b7280'
}

const getTagLabel = (tagValue: string) => tagValue

const parseTags = (tagsStr: string): string[] => {
  if (!tagsStr) return []
  return tagsStr.split(',').map(s => s.trim()).filter(Boolean)
}

const loadQuestion = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const [qRes, aRes] = await Promise.all([
      api.get(`/community/questions/${id}`),
      api.get(`/community/questions/${id}/answers`)
    ])
    question.value = qRes.data
    answers.value = aRes.data || []

    // Check like status
    if (currentUserId.value) {
      try {
        const likeRes = await api.get(`/community/questions/${id}/like-status`)
        isLiked.value = likeRes.data.liked
      } catch { /* ignore */ }
    }
  } catch (error) {
    console.error('Failed to load question:', error)
    question.value = null
  } finally {
    loading.value = false
  }
}

const submitAnswer = async () => {
  if (!newAnswer.value.trim() || answering.value) return
  answering.value = true
  try {
    await api.post(`/community/questions/${question.value.id}/answers`, { content: newAnswer.value })
    newAnswer.value = ''
    const aRes = await api.get(`/community/questions/${question.value.id}/answers`)
    answers.value = aRes.data || []
    if (question.value) {
      question.value.answerCount = answers.value.length
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      if (confirm(t('routePlanner.loginRequired'))) router.push('/login')
    } else {
      alert('回答失败，请稍后重试')
    }
  } finally {
    answering.value = false
  }
}

const toggleLike = async () => {
  if (!currentUserId.value) {
    if (confirm(t('routePlanner.loginRequired'))) router.push('/login')
    return
  }
  try {
    if (isLiked.value) {
      await api.delete(`/community/questions/${question.value.id}/like`)
      isLiked.value = false
      question.value.likeCount = Math.max(0, (question.value.likeCount || 1) - 1)
    } else {
      const res = await api.post(`/community/questions/${question.value.id}/like`)
      isLiked.value = true
      question.value.likeCount = res.data.likeCount
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      if (confirm(t('routePlanner.loginRequired'))) router.push('/login')
    }
  }
}

const acceptAnswer = async (answerId: number) => {
  try {
    await api.post(`/community/questions/${question.value.id}/answers/${answerId}/accept`)
    question.value.isResolved = true
    loadQuestion()
  } catch (error: any) {
    alert('采纳失败')
  }
}

const deleteQuestion = async () => {
  if (!confirm('确定要删除这个问题吗？')) return
  try {
    await api.delete(`/community/questions/${question.value.id}`)
    router.push('/community')
  } catch (error: any) {
    alert('删除失败')
  }
}

const formatDate = (dateStr: string) => {
  const locale = localStorage.getItem('locale') || 'zh'
  return new Date(dateStr).toLocaleDateString(locale === 'bo' ? 'bo-CN' : 'zh-CN')
}

onMounted(() => { loadQuestion() })
</script>
