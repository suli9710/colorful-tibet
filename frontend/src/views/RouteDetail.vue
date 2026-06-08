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
          <div class="prose route-detail-markdown max-w-none overflow-x-auto prose-headings:text-tibet-dark prose-p:text-tibet-brown/80 prose-strong:text-tibet-gold sm:prose-lg">
            <div v-html="renderedContent"></div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-center gap-3 mb-8 sm:gap-6 sm:mb-12">
          <button 
            @click="toggleLike" 
            :disabled="liking"
            :aria-busy="liking"
            class="flex items-center gap-2 px-6 py-3 rounded-full transition-all duration-300 shadow-sm hover:shadow-md disabled:cursor-wait disabled:opacity-70"
            :class="isLiked ? 'bg-red-50 text-red-600 border border-red-200' : 'bg-white text-gray-600 border border-tibet-gold/25 hover:bg-gray-50'"
          >
            <Heart class="h-5 w-5" :fill="isLiked ? 'currentColor' : 'none'" />
            <span class="font-medium">{{ routeData.likeCount }}</span>
          </button>
          
          <div class="flex items-center gap-2 px-6 py-3 bg-white text-gray-600 rounded-full border border-tibet-gold/25 shadow-sm">
            <Eye class="h-5 w-5" />
            <span class="font-medium">{{ routeData.viewCount }}</span>
          </div>
        </div>

        <!-- Comments -->
        <div class="glass-card rounded-2xl p-4 sm:rounded-3xl sm:p-8">
          <h3 class="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2">
            {{ t('routeDetail.comments') }}
            <span class="text-sm font-normal text-gray-500">
              ({{ comments.length }}<template v-if="commentsTotalElements > comments.length"> / {{ commentsTotalElements }}</template>)
            </span>
          </h3>
          
          <!-- Add Comment -->
          <div class="mb-8">
            <textarea 
              v-model="newComment" 
              rows="3"
              class="w-full px-4 py-3 rounded-xl bg-white/50 border border-tibet-gold/25 focus:border-tibet-gold outline-none transition-all resize-none mb-4"
              :placeholder="t('routeDetail.commentPlaceholder')"
              :aria-label="t('routeDetail.commentPlaceholder')"
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
                <span class="font-medium text-gray-900">{{ publicUserName(comment.user) }}</span>
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

          <div
            v-if="commentsTotalPages > 1"
            class="mt-6 flex flex-wrap items-center justify-center gap-3"
            role="navigation"
            :aria-label="t('routeDetail.comments')"
          >
            <span class="text-sm text-gray-500" role="status" aria-live="polite">
              {{ commentsPage + 1 }} / {{ commentsTotalPages }}
            </span>
            <button
              type="button"
              @click="loadNextCommentsPage"
              :disabled="commentsLoadingMore || !hasMoreComments"
              :aria-busy="commentsLoadingMore"
              class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50"
            >
              {{ commentsLoadingMore ? t('common.loading') : t('community.nextPage') }}
            </button>
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
import { renderMarkdownToSafeHtml } from '../utils/sanitize'
import { motion } from 'motion-v'
import { revealInitial, revealInView, revealTransition } from '../motion/presets'
import api, { endpoints } from '../api'
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata } from '../api/endpoints'
import { useAuthStore } from '../stores/auth'
import { showToast } from '../composables/useToast'
import { showConfirm } from '../composables/useConfirm'
import { readBrowserStorage } from '../utils/browserStorage'
import { summarizeClientError } from '../utils/errorMonitoring'
import { Eye, Heart } from 'lucide-vue-next'

const { t } = useI18n()

const currentRoute = useRoute()
const router = useRouter()
const auth = useAuthStore()
const routeId = Number(currentRoute.params.id)

const loading = ref(true)
const routeData = ref<any>(null)
const comments = ref<any[]>([])
const routeCommentsPageSize = 20
const commentsPageInfo = ref<PageMetadata>({
  page: 0,
  size: routeCommentsPageSize,
  totalElements: 0,
  totalPages: 0
})
const commentsLoadingMore = ref(false)
const isLiked = ref(false)
const liking = ref(false)
const newComment = ref('')
const submitting = ref(false)
const currentUser = computed(() => auth.user)

const renderedContent = computed(() => {
  return routeData.value ? renderMarkdownToSafeHtml(routeData.value.content) : ''
})
const commentsPage = computed(() => commentsPageInfo.value.page)
const commentsTotalPages = computed(() => commentsPageInfo.value.totalPages)
const commentsTotalElements = computed(() => commentsPageInfo.value.totalElements)
const hasMoreComments = computed(() => hasNextPage(commentsPageInfo.value))

const redirectToLogin = () => {
  router.push({ path: '/login', query: { redirect: currentRoute.fullPath } })
}

const applyCommentsPage = (response: any, append = false) => {
  const page = readPaginatedResponse<any>(response, {
    page: append ? commentsPageInfo.value.page + 1 : 0,
    size: routeCommentsPageSize
  })

  comments.value = append ? mergeUniqueById(comments.value, page.content) : page.content
  commentsPageInfo.value = {
    page: page.page,
    size: page.size || routeCommentsPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const fetchCommentsPage = async (page = 0, append = false) => {
  const response = await api.get(endpoints.routes.sharedComments(routeId), {
    params: { page, size: routeCommentsPageSize }
  })
  applyCommentsPage(response, append)
}

const loadRouteDetail = async () => {
  loading.value = true
  try {
    const [routeRes, commentsRes, likeRes] = await Promise.all([
      api.get(endpoints.routes.sharedDetail(routeId)),
      api.get(endpoints.routes.sharedComments(routeId), {
        params: { page: 0, size: routeCommentsPageSize }
      }),
      api.get(endpoints.routes.sharedLikeStatus(routeId)).catch(() => ({ data: { liked: false } }))
    ])
    
    routeData.value = routeRes.data
    applyCommentsPage(commentsRes)
    isLiked.value = likeRes.data.liked
  } catch (error) {
    console.error('Failed to load route detail:', summarizeClientError(error))
  } finally {
    loading.value = false
  }
}

const toggleLike = async () => {
  if (liking.value || !routeData.value) return
  liking.value = true

  try {
    if (!(await auth.ensureSession())) {
      showToast(t('routeDetail.loginRequiredLike'), 'warning')
      redirectToLogin()
      return
    }

    if (isLiked.value) {
      await api.delete(endpoints.routes.sharedLike(routeId))
      routeData.value.likeCount--
      isLiked.value = false
    } else {
      await api.post(endpoints.routes.sharedLike(routeId))
      routeData.value.likeCount++
      isLiked.value = true
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      showToast(t('routeDetail.loginRequiredLike'), 'warning')
      redirectToLogin()
    } else {
      showToast(t('routeDetail.operationFailed'), 'error')
    }
  } finally {
    liking.value = false
  }
}

const loadNextCommentsPage = async () => {
  if (commentsLoadingMore.value || !hasMoreComments.value) return
  commentsLoadingMore.value = true
  try {
    await fetchCommentsPage(commentsPageInfo.value.page + 1, true)
  } catch (error) {
    console.error('Failed to load more route comments:', summarizeClientError(error))
    showToast(t('routeDetail.commentFailed'), 'error')
  } finally {
    commentsLoadingMore.value = false
  }
}

const submitComment = async () => {
  const content = newComment.value.trim()
  if (!content || submitting.value) return
  submitting.value = true
  try {
    if (!(await auth.ensureSession())) {
      showToast(t('routeDetail.loginRequiredComment'), 'warning')
      redirectToLogin()
      return
    }

    const response = await api.post(endpoints.routes.sharedComments(routeId), {
      content
    })
    
    comments.value = [response.data, ...comments.value.filter(comment => comment.id !== response.data?.id)]
    routeData.value.commentCount++
    commentsPageInfo.value = {
      ...commentsPageInfo.value,
      totalElements: commentsPageInfo.value.totalElements + 1,
      totalPages: Math.max(
        commentsPageInfo.value.totalPages,
        Math.ceil((commentsPageInfo.value.totalElements + 1) / commentsPageInfo.value.size)
      )
    }
    newComment.value = ''
    showToast(t('routeDetail.commentSuccess'), 'success')
  } catch (error: any) {
    if (error.response?.status === 401) {
      showToast(t('routeDetail.loginRequiredComment'), 'warning')
      redirectToLogin()
    } else {
      showToast(t('routeDetail.commentFailed'), 'error')
    }
  } finally {
    submitting.value = false
  }
}

const isOwnComment = (comment: any) => {
  return Boolean(currentUser.value && comment?.user?.owner)
}

const publicUserName = (user: any) => user?.nickname || t('routeDetail.anonymous')

const routeAuthorName = (route: any) => {
  if (route?.sourceType === 'OFFICIAL') {
    return t('community.officialRoute')
  }
  return publicUserName(route?.author)
}

const deleteComment = async (comment: any) => {
  const confirmed = await showConfirm({
    message: t('routeDetail.confirmDeleteComment'),
    confirmLabel: t('common.delete'),
    cancelLabel: t('common.cancel'),
    tone: 'danger'
  })
  if (!confirmed) return

  try {
    await api.delete(endpoints.routes.deleteSharedComment(routeId, comment.id))
    comments.value = comments.value.filter(item => item.id !== comment.id)
    if (routeData.value?.commentCount > 0) {
      routeData.value.commentCount--
    }
    if (commentsPageInfo.value.totalElements > 0) {
      const totalElements = commentsPageInfo.value.totalElements - 1
      commentsPageInfo.value = {
        ...commentsPageInfo.value,
        totalElements,
        totalPages: commentsPageInfo.value.size > 0 ? Math.ceil(totalElements / commentsPageInfo.value.size) : 0
      }
    }
    showToast(t('routeDetail.deleteCommentSuccess'), 'success')
  } catch (error) {
    console.error('Failed to delete comment:', summarizeClientError(error))
    showToast(t('routeDetail.deleteCommentFailed'), 'error')
  }
}

const formatDate = (dateStr: string) => {
  const locale = readBrowserStorage('localStorage', 'locale', 'zh')
  return new Date(dateStr).toLocaleString(locale === 'bo' ? 'bo-CN' : 'zh-CN')
}

onMounted(async () => {
  await auth.refreshSession()
  loadRouteDetail()
})
</script>

<style scoped>
.route-detail-markdown :deep(a),
.route-detail-markdown :deep(code) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.route-detail-markdown :deep(pre),
.route-detail-markdown :deep(table) {
  max-width: 100%;
  overflow-x: auto;
}
</style>
