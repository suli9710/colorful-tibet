<template>
  <div class="min-h-screen bg-tibet-white py-12 pt-24 sm:py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-red mx-auto"></div>
      </div>

      <motion.div v-else-if="question"
        :initial="revealInitial"
        :animate="revealInView"
        :transition="revealTransition"
      >
        <!-- Back -->
        <button @click="router.back()" class="text-gray-500 hover:text-gray-900 mb-6 flex items-center gap-1 text-sm">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
          {{ t('questionDetail.backToCommunity') }}
        </button>

        <!-- Question Header -->
        <div class="glass-card rounded-2xl p-4 mb-6 sm:rounded-3xl sm:p-8">
          <div class="flex items-start justify-between gap-4 mb-4">
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2 mb-3">
                <span v-if="question.isResolved" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-600 text-xs font-semibold">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                  {{ t('community.solved') }}
                </span>
                <span v-else class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-amber-50 text-amber-600 text-xs font-semibold">
                  {{ t('community.unsolved') }}
                </span>
                <span v-if="question.tags" v-for="tag in parseTags(question.tags)" :key="tag" class="px-2 py-1 rounded-lg text-xs font-medium text-white" :style="{ backgroundColor: getTagColor(tag) }">{{ tag }}</span>
              </div>
              <h1 class="break-words text-xl font-bold text-gray-900 mb-3 sm:text-2xl">{{ question.title }}</h1>
              <div class="flex flex-wrap items-center gap-2 text-sm text-gray-400 sm:gap-3">
                <span>{{ publicUserName(question.author) }}</span>
                <span>·</span>
                <span>{{ formatDate(question.createdAt) }}</span>
                <span>·</span>
                <span>{{ t('questionDetail.viewCount', { count: question.viewCount }) }}</span>
              </div>
            </div>
          </div>
          <div class="prose prose-slate max-w-none break-words text-gray-700 leading-relaxed">{{ question.content }}</div>

          <!-- Question Actions -->
          <div class="flex flex-wrap items-center gap-3 mt-6 pt-6 border-t border-tibet-gold/20 sm:gap-4">
            <button type="button"
                    @click="toggleLike"
                    :disabled="liking"
                    :aria-label="questionLikeAccessibleName"
                    :aria-pressed="isLiked"
                    :aria-busy="liking"
                    class="flex items-center gap-2 px-4 py-2 rounded-xl transition-all text-sm font-medium disabled:cursor-wait disabled:opacity-70"
                    :class="isLiked ? 'bg-red-50 text-red-600' : 'bg-gray-50 text-gray-500 hover:bg-gray-100'">
              <Heart class="h-4 w-4" :fill="isLiked ? 'currentColor' : 'none'" aria-hidden="true" />
              <span>{{ question.likeCount }}</span>
            </button>
            <button v-if="isAuthor"
                    @click="deleteQuestion"
                    :disabled="deletingQuestion"
                    :aria-busy="deletingQuestion"
                    class="ml-auto px-4 py-2 rounded-xl bg-gray-50 text-gray-400 hover:bg-red-50 hover:text-red-500 transition-colors text-sm disabled:cursor-wait disabled:opacity-70">
              {{ t('questionDetail.deleteQuestion') }}
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
                 class="glass-card rounded-2xl p-4 transition-all sm:p-6"
                 :class="answer.isAccepted ? 'border-2 border-emerald-300 bg-emerald-50/40' : 'border border-white/20'">
              <div class="flex flex-col gap-2 mb-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-semibold text-gray-900 text-sm">{{ publicUserName(answer.user) }}</span>
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
                        :disabled="acceptingAnswerId !== null"
                        :aria-busy="acceptingAnswerId === answer.id"
                        class="ml-auto px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-600 hover:bg-emerald-100 text-xs font-medium transition-colors disabled:cursor-wait disabled:opacity-70">
                  {{ t('community.acceptAnswer') }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Write Answer -->
        <div class="glass-card rounded-2xl p-4 sm:rounded-3xl sm:p-8">
          <h3 class="text-lg font-bold text-gray-900 mb-4">{{ t('community.writeAnswer') }}</h3>
          <textarea v-model="newAnswer" rows="4"
                    :placeholder="t('community.yourAnswer')"
                    :aria-label="t('community.yourAnswer')"
                    class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-red focus:ring-4 focus:ring-red-50 outline-none transition-all text-sm resize-none mb-4"></textarea>
          <div class="flex justify-end">
            <button @click="submitAnswer"
                    :disabled="isSubmitAnswerDisabled"
                    :aria-busy="answering"
                    class="px-6 py-2.5 bg-gradient-to-r from-tibet-red to-rose-600 text-white rounded-xl shadow-lg shadow-red-500/20 hover:shadow-xl hover:shadow-red-500/30 transition-all disabled:cursor-wait disabled:opacity-50 text-sm font-medium">
              {{ answering ? t('community.submittingAnswer') : t('community.submitAnswer') }}
            </button>
          </div>
        </div>
      </motion.div>

      <div v-else class="text-center py-12 text-gray-500">{{ t('questionDetail.questionNotFound') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { motion } from 'motion-v'
import { revealInitial, revealInView, revealTransition } from '../motion/presets'
import api, { endpoints } from '../api'
import { useAuthStore } from '../stores/auth'
import { useAuthGuard } from '../composables/useAuthGuard'
import { showConfirm } from '../composables/useConfirm'
import { showToast } from '../composables/useToast'
import { readBrowserStorage } from '../utils/browserStorage'
import { summarizeClientError } from '../utils/errorMonitoring'
import { Heart } from 'lucide-vue-next'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { requireAuth } = useAuthGuard()

const question = ref<any>(null)
const answers = ref<any[]>([])
const loading = ref(true)
const answering = ref(false)
const newAnswer = ref('')
const isLiked = ref(false)
const liking = ref(false)
const acceptingAnswerId = ref<number | null>(null)
const deletingQuestion = ref(false)

const isAuthor = computed(() => Boolean(question.value?.author?.owner))
const isQuestionAuthor = isAuthor
const isSubmitAnswerDisabled = computed(() => !newAnswer.value.trim() || answering.value)
const questionLikeAccessibleName = computed(() => {
  const count = question.value?.likeCount ?? 0

  return t('questionDetail.likeQuestionAria', {
    action: t(isLiked.value ? 'questionDetail.unlikeQuestionAction' : 'questionDetail.likeQuestionAction'),
    state: t(isLiked.value ? 'questionDetail.likedState' : 'questionDetail.notLikedState'),
    count
  })
})

const normalizePublicUserName = (value: unknown) => typeof value === 'string' ? value.trim() : ''
const publicUserName = (user: any) =>
  normalizePublicUserName(user?.nickname) || t('questionDetail.anonymousUser')

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
      api.get(endpoints.community.questionDetail(String(id))),
      api.get(endpoints.community.questionAnswers(String(id)))
    ])
    question.value = qRes.data
    answers.value = aRes.data || []

    // Check like status
    if (auth.hasValidSession()) {
      try {
        const likeRes = await api.get(endpoints.community.questionLikeStatus(String(id)))
        isLiked.value = likeRes.data.liked
      } catch { /* ignore */ }
    }
  } catch (error) {
    console.error('Failed to load question:', summarizeClientError(error))
    question.value = null
  } finally {
    loading.value = false
  }
}

const submitAnswer = async () => {
  const content = newAnswer.value.trim()
  if (!content || answering.value || !question.value) return
  answering.value = true
  try {
    if (!(await requireAuth())) return

    await api.post(endpoints.community.createQuestionAnswer(question.value.id), { content })
    newAnswer.value = ''
    const aRes = await api.get(endpoints.community.questionAnswers(question.value.id))
    answers.value = aRes.data || []
    if (question.value) {
      question.value.answerCount = answers.value.length
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      if (!(await requireAuth())) return
    } else {
      showToast(t('questionDetail.answerFailed'), 'error')
    }
  } finally {
    answering.value = false
  }
}

const toggleLike = async () => {
  if (liking.value || !question.value) return
  liking.value = true
  try {
    if (!(await requireAuth())) return

    if (isLiked.value) {
      await api.delete(endpoints.community.questionLike(question.value.id))
      isLiked.value = false
      question.value.likeCount = Math.max(0, (question.value.likeCount || 1) - 1)
    } else {
      const res = await api.post(endpoints.community.questionLike(question.value.id))
      isLiked.value = true
      question.value.likeCount = res.data.likeCount
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      if (!(await requireAuth())) return
    }
  } finally {
    liking.value = false
  }
}

const acceptAnswer = async (answerId: number) => {
  if (acceptingAnswerId.value !== null || !question.value) return
  acceptingAnswerId.value = answerId
  try {
    if (!(await requireAuth())) return

    await api.post(endpoints.community.acceptQuestionAnswer(question.value.id, answerId))
    question.value.isResolved = true
    await loadQuestion()
  } catch (error: any) {
    showToast(t('questionDetail.acceptFailed'), 'error')
  } finally {
    acceptingAnswerId.value = null
  }
}

const deleteQuestion = async () => {
  if (deletingQuestion.value || !question.value) return
  deletingQuestion.value = true
  try {
    if (!(await requireAuth())) return

    const confirmed = await showConfirm({
      message: t('questionDetail.confirmDelete'),
      confirmLabel: t('common.delete'),
      cancelLabel: t('common.cancel'),
      tone: 'danger'
    })
    if (!confirmed) return

    await api.delete(endpoints.community.deleteQuestion(question.value.id))
    router.push('/community')
  } catch (error: any) {
    showToast(t('questionDetail.deleteFailed'), 'error')
  } finally {
    deletingQuestion.value = false
  }
}

const formatDate = (dateStr: string) => {
  const locale = readBrowserStorage('localStorage', 'locale', 'zh')
  return new Date(dateStr).toLocaleDateString(locale === 'bo' ? 'bo-CN' : 'zh-CN')
}

onMounted(async () => {
  await auth.refreshSession()
  loadQuestion()
})
</script>
