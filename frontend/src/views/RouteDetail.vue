<template>
  <div class="min-h-screen bg-tibet-white py-12 pt-24 sm:py-24">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <div v-if="loading" class="text-center py-12" role="status" aria-live="polite" aria-busy="true">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold mx-auto" aria-hidden="true"></div>
        <span class="sr-only">{{ t('routeDetail.loadingRoute') }}</span>
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
          
          <div v-if="commentsLoading" class="py-6 text-center" role="status" aria-live="polite" aria-busy="true">
            <div class="mx-auto h-8 w-8 animate-spin rounded-full border-b-2 border-tibet-gold" aria-hidden="true"></div>
            <span class="sr-only">{{ t('routeDetail.commentsLoading') }}</span>
          </div>

          <div v-else-if="commentsLoadError" class="mb-6 rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-red-700" role="alert">
            <p class="mb-3 font-medium">{{ commentsLoadError }}</p>
            <button
              type="button"
              @click="retryLoadComments"
              class="min-h-11 rounded-xl bg-tibet-red px-5 py-2.5 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
            >
              {{ t('common.retry') }}
            </button>
          </div>

          <!-- Comment List -->
          <div v-else class="space-y-6">
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
            v-if="!commentsLoading && !commentsLoadError && commentsTotalPages > 1"
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

      <div v-else-if="routeLoadError" class="rounded-2xl border border-red-100 bg-red-50 px-5 py-8 text-center text-red-700" role="alert">
        <h1 class="mb-2 text-xl font-bold">{{ t('common.error') }}</h1>
        <p class="mb-4 font-medium">{{ routeLoadError }}</p>
        <button
          type="button"
          @click="loadRouteDetail"
          class="min-h-11 rounded-xl bg-tibet-red px-5 py-2.5 text-sm font-semibold text-tibet-yellow transition-colors hover:bg-tibet-red/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold focus-visible:ring-offset-2"
        >
          {{ t('common.retry') }}
        </button>
      </div>

      <div v-else-if="routeNotFound" class="text-center py-12 text-gray-500">
        <h1 class="text-xl font-bold">{{ t('routeDetail.notFound') }}</h1>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { renderMarkdownToSafeHtml } from '../utils/sanitize'
import { motion } from 'motion-v'
import { revealInitial, revealInView, revealTransition } from '../motion/presets'
import api, {
  endpoints,
  type PublicUserResponse,
  type RouteCommentResponse,
  type RouteLikeMutationResponse,
  type RouteLikeStatusResponse,
  type SharedRouteResponse
} from '../api'
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata, type PaginatedHttpResponse } from '../api/endpoints'
import { useAuthStore } from '../stores/auth'
import { showToast } from '../composables/useToast'
import { showConfirm } from '../composables/useConfirm'
import { summarizeClientError } from '../utils/errorMonitoring'
import { Eye, Heart } from 'lucide-vue-next'
import { toIntlLocale } from '../i18n/formatting'

const { t, locale } = useI18n()

const currentRoute = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const routeLoadError = ref('')
const routeNotFound = ref(false)
const routeData = ref<SharedRouteResponse | null>(null)
const comments = ref<RouteCommentResponse[]>([])
const routeCommentsPageSize = 20
const commentsPageInfo = ref<PageMetadata>({
  page: 0,
  size: routeCommentsPageSize,
  totalElements: 0,
  totalPages: 0
})
const commentsLoadingMore = ref(false)
const commentsLoading = ref(false)
const commentsLoadError = ref('')
const isLiked = ref(false)
const liking = ref(false)
const newComment = ref('')
const submitting = ref(false)
let routeDetailRequestId = 0
let commentsRequestId = 0
let likeStatusRequestId = 0
let routeLikeMutationId = 0
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

const currentRouteId = () => Number(currentRoute.params.id)
const isCurrentRouteId = (routeId: number) => currentRouteId() === routeId
const isCurrentRouteDetailRequest = (requestId: number, routeId: number) =>
  requestId === routeDetailRequestId && isCurrentRouteId(routeId)
const isCurrentCommentsRequest = (requestId: number, routeId: number) =>
  requestId === commentsRequestId && isCurrentRouteId(routeId) && routeData.value?.id === routeId
const isCurrentRouteLikeMutation = (mutationId: number, routeId: number) =>
  mutationId === routeLikeMutationId && isCurrentRouteId(routeId) && routeData.value?.id === routeId

const applyCommentsPage = (response: PaginatedHttpResponse, append = false) => {
  const page = readPaginatedResponse<RouteCommentResponse>(response, {
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

const resetComments = () => {
  comments.value = []
  commentsPageInfo.value = {
    page: 0,
    size: routeCommentsPageSize,
    totalElements: 0,
    totalPages: 0
  }
}

const fetchCommentsPage = async (page = 0, append = false, routeId = routeData.value?.id, requestId = commentsRequestId) => {
  if (!routeId) return
  const response = await api.get<unknown>(endpoints.routes.sharedComments(routeId), {
    params: { page, size: routeCommentsPageSize }
  })
  if (!isCurrentCommentsRequest(requestId, routeId)) return
  applyCommentsPage(response, append)
}

const loadInitialComments = async (routeId = routeData.value?.id) => {
  if (!routeId) return
  const requestId = ++commentsRequestId
  commentsLoading.value = true
  commentsLoadError.value = ''
  try {
    await fetchCommentsPage(0, false, routeId, requestId)
  } catch (error) {
    if (!isCurrentCommentsRequest(requestId, routeId)) return
    console.error('Failed to load route comments:', summarizeClientError(error))
    resetComments()
    commentsLoadError.value = t('routeDetail.commentsLoadFailed')
  } finally {
    if (isCurrentCommentsRequest(requestId, routeId)) {
      commentsLoading.value = false
    }
  }
}

const retryLoadComments = () => {
  if (!routeData.value || commentsLoading.value) return
  loadInitialComments(routeData.value.id)
}

const loadLikeStatus = async (routeId = routeData.value?.id) => {
  if (!routeId) return
  const requestId = ++likeStatusRequestId
  if (!currentUser.value) {
    isLiked.value = false
    return
  }

  try {
    const likeRes = await api.get<RouteLikeStatusResponse>(endpoints.routes.sharedLikeStatus(routeId), { skipAuthRedirect: true })
    if (requestId !== likeStatusRequestId || !isCurrentRouteId(routeId) || routeData.value?.id !== routeId) return
    isLiked.value = Boolean(likeRes.data.liked)
  } catch {
    if (requestId !== likeStatusRequestId || !isCurrentRouteId(routeId) || routeData.value?.id !== routeId) return
    isLiked.value = false
  }
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value)

const responseStatus = (error: unknown) => {
  if (!isRecord(error) || !isRecord(error.response)) return undefined
  const status = error.response.status
  if (typeof status === 'number' && Number.isFinite(status)) return status
  if (typeof status === 'string' && status.trim()) {
    const parsedStatus = Number(status)
    return Number.isFinite(parsedStatus) ? parsedStatus : undefined
  }
  return undefined
}

const isNotFoundError = (error: unknown) => responseStatus(error) === 404

const applyRouteLikeResult = (
  responseData: RouteLikeMutationResponse | undefined,
  fallbackLiked: boolean,
  fallbackLikeCount: number,
  routeId: number
) => {
  if (!routeData.value || routeData.value.id !== routeId) return
  isLiked.value = typeof responseData?.liked === 'boolean' ? responseData.liked : fallbackLiked
  const nextLikeCount = Number(responseData?.likeCount)
  routeData.value.likeCount = Number.isFinite(nextLikeCount)
    ? nextLikeCount
    : Math.max(0, fallbackLikeCount)
}

const loadRouteDetail = async () => {
  const requestId = ++routeDetailRequestId
  const routeId = currentRouteId()
  commentsRequestId += 1
  likeStatusRequestId += 1
  routeLikeMutationId += 1
  loading.value = true
  liking.value = false
  commentsLoading.value = false
  commentsLoadingMore.value = false
  routeLoadError.value = ''
  routeNotFound.value = false
  commentsLoadError.value = ''
  try {
    const routeRes = await api.get<SharedRouteResponse | null>(endpoints.routes.sharedDetail(routeId))
    if (!isCurrentRouteDetailRequest(requestId, routeId)) return
    if (!routeRes.data) {
      routeData.value = null
      routeNotFound.value = true
      resetComments()
      return
    }

    routeData.value = routeRes.data
    loading.value = false
    await Promise.all([loadInitialComments(routeId), loadLikeStatus(routeId)])
  } catch (error) {
    if (!isCurrentRouteDetailRequest(requestId, routeId)) return
    console.error('Failed to load route detail:', summarizeClientError(error))
    routeData.value = null
    resetComments()
    if (isNotFoundError(error)) {
      routeNotFound.value = true
    } else {
      routeLoadError.value = t('routeDetail.loadFailed')
    }
  } finally {
    if (isCurrentRouteDetailRequest(requestId, routeId)) {
      loading.value = false
    }
  }
}

const toggleLike = async () => {
  if (liking.value || !routeData.value) return
  const routeId = routeData.value.id
  const mutationId = ++routeLikeMutationId
  liking.value = true

  try {
    if (!(await auth.ensureSession())) {
      showToast(t('routeDetail.loginRequiredLike'), 'warning')
      redirectToLogin()
      return
    }
    if (!isCurrentRouteLikeMutation(mutationId, routeId)) return

    const previousLikeCount = Number(routeData.value.likeCount) || 0
    if (isLiked.value) {
      const response = await api.delete<RouteLikeMutationResponse>(endpoints.routes.sharedLike(routeId))
      if (!isCurrentRouteLikeMutation(mutationId, routeId)) return
      applyRouteLikeResult(response.data, false, previousLikeCount - 1, routeId)
    } else {
      const response = await api.post<RouteLikeMutationResponse>(endpoints.routes.sharedLike(routeId))
      if (!isCurrentRouteLikeMutation(mutationId, routeId)) return
      applyRouteLikeResult(response.data, true, previousLikeCount + 1, routeId)
    }
  } catch (error) {
    if (responseStatus(error) === 401) {
      showToast(t('routeDetail.loginRequiredLike'), 'warning')
      redirectToLogin()
    } else {
      showToast(t('routeDetail.operationFailed'), 'error')
    }
  } finally {
    if (mutationId === routeLikeMutationId) {
      liking.value = false
    }
  }
}

const loadNextCommentsPage = async () => {
  if (commentsLoadingMore.value || !hasMoreComments.value || !routeData.value) return
  const routeId = routeData.value.id
  const requestId = ++commentsRequestId
  commentsLoadingMore.value = true
  try {
    await fetchCommentsPage(commentsPageInfo.value.page + 1, true, routeId, requestId)
  } catch (error) {
    if (!isCurrentCommentsRequest(requestId, routeId)) return
    console.error('Failed to load more route comments:', summarizeClientError(error))
    showToast(t('routeDetail.commentFailed'), 'error')
  } finally {
    if (isCurrentCommentsRequest(requestId, routeId)) {
      commentsLoadingMore.value = false
    }
  }
}

const submitComment = async () => {
  const content = newComment.value.trim()
  if (!content || submitting.value || !routeData.value) return
  const routeId = routeData.value.id
  submitting.value = true
  try {
    if (!(await auth.ensureSession())) {
      showToast(t('routeDetail.loginRequiredComment'), 'warning')
      redirectToLogin()
      return
    }
    if (!isCurrentRouteId(routeId) || routeData.value?.id !== routeId) return

    const response = await api.post<RouteCommentResponse>(endpoints.routes.sharedComments(routeId), {
      content
    })
    if (!isCurrentRouteId(routeId) || routeData.value?.id !== routeId) return
    
    comments.value = [response.data, ...comments.value.filter(comment => comment.id !== response.data.id)]
    if (routeData.value) {
      routeData.value.commentCount++
    }
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
  } catch (error) {
    if (responseStatus(error) === 401) {
      showToast(t('routeDetail.loginRequiredComment'), 'warning')
      redirectToLogin()
    } else {
      showToast(t('routeDetail.commentFailed'), 'error')
    }
  } finally {
    if (isCurrentRouteId(routeId)) {
      submitting.value = false
    }
  }
}

const isOwnComment = (comment: RouteCommentResponse) => {
  return Boolean(currentUser.value && comment?.user?.owner)
}

const publicUserName = (user?: PublicUserResponse | null) => user?.nickname || t('routeDetail.anonymous')

const routeAuthorName = (route: SharedRouteResponse | null | undefined) => {
  if (route?.sourceType === 'OFFICIAL') {
    return t('community.officialRoute')
  }
  return publicUserName(route?.author)
}

const deleteComment = async (comment: RouteCommentResponse) => {
  const routeId = routeData.value?.id
  if (!routeId) return
  const confirmed = await showConfirm({
    message: t('routeDetail.confirmDeleteComment'),
    confirmLabel: t('common.delete'),
    cancelLabel: t('common.cancel'),
    tone: 'danger'
  })
  if (!confirmed) return
  if (!isCurrentRouteId(routeId) || routeData.value?.id !== routeId) return

  try {
    await api.delete(endpoints.routes.deleteSharedComment(routeId, comment.id))
    if (!isCurrentRouteId(routeId) || routeData.value?.id !== routeId) return
    comments.value = comments.value.filter(item => item.id !== comment.id)
    const route = routeData.value
    if (route && route.commentCount > 0) {
      route.commentCount--
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
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return t('common.pendingConfirm')
  return date.toLocaleString(toIntlLocale(locale.value))
}

onMounted(async () => {
  await auth.refreshSession()
  loadRouteDetail()
})

watch(
  () => currentRoute.params.id,
  (id, oldId) => {
    if (String(id ?? '') !== String(oldId ?? '')) {
      loadRouteDetail()
    }
  }
)
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
