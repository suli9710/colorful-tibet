<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { isAxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AnimatePresence, LayoutGroup, motion } from 'motion-v'
import api, { endpoints } from '@/api'
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata, type PaginatedHttpResponse } from '@/api/endpoints'
import { useAuthStore } from '@/stores/auth'
import { applyHotelImageFallback, resolveHotelBookingImage } from '@/data/hotelImages'
import MotionModal from '@/components/motion/MotionModal.vue'
import { showConfirm } from '@/composables/useConfirm'
import { showToast } from '@/composables/useToast'
import { safeClientErrorMessage, summarizeClientError } from '@/utils/errorMonitoring'
import { toIntlLocale } from '@/i18n/formatting'
import {
  cardExit,
  cardInitial,
  cardInView,
  cardTransition,
  revealInitial,
  revealInView,
  revealTransition,
  softSpring
} from '@/motion/presets'

const { t, locale } = useI18n()

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | string

interface ProfileUser {
  nickname?: string | null
  avatar?: string | null
  avatarUrl?: string | null
  role?: string | null
  mustChangePassword?: boolean | null
  createdAt?: string | null
}

interface ProfileStats {
  routeCount?: number | null
  commentCount?: number | null
  bookingCount?: number | null
}

interface ProfileRoute {
  id: number
  title: string
  days?: number | string | null
  budget?: string | null
  preference?: string | null
  viewCount?: number | null
  likeCount?: number | null
  commentCount?: number | null
  createdAt?: string | null
}

interface ProfileSpotBooking {
  id: number
  spot?: {
    id?: number
    name?: string | null
    imageUrl?: string | null
  } | null
  visitDate?: string | null
  ticketCount?: number | string | null
  totalPrice?: number | string | null
  status: BookingStatus
}

interface ProfileHotelBooking {
  id: number
  hotel?: {
    id?: number
    name?: string | null
    imageUrl?: string | null
    coverImage?: string | null
  } | null
  hotelName?: string | null
  roomName?: string | null
  nights?: number | string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  totalPrice?: number | string | null
  status: BookingStatus
}

interface ProfileSpotComment {
  id: number
  spot?: {
    id?: number
    name?: string | null
  } | null
  rating?: number | null
  content?: string | null
  imageUrl?: string | null
  likeCount?: number | null
  createdAt?: string | null
}

interface ProfileRouteComment {
  id: number
  route?: {
    id?: number
    title?: string | null
  } | null
  content?: string | null
  createdAt?: string | null
}

interface ProfileCommentsResponse {
  spotCommentsPage?: unknown
  spotComments?: unknown
  routeCommentsPage?: unknown
  routeComments?: unknown
}

interface AvatarUploadResponse {
  avatarUrl?: string
}

type ProfileListRetryMode = 'append' | 'refresh'

interface PaginatedRequestGuard {
  sequence: number
  activeAppendToken: number | null
}

const createPaginatedRequestGuard = (): PaginatedRequestGuard => ({
  sequence: 0,
  activeAppendToken: null
})

const beginPaginatedRequest = (
  guard: PaginatedRequestGuard,
  append: boolean,
  clearAppendLoading: () => void
) => {
  const token = guard.sequence + 1
  guard.sequence = token

  if (append) {
    guard.activeAppendToken = token
  } else {
    guard.activeAppendToken = null
    clearAppendLoading()
  }

  return token
}

const isLatestPaginatedRequest = (guard: PaginatedRequestGuard, token: number) =>
  guard.sequence === token

const finishAppendRequest = (
  guard: PaginatedRequestGuard,
  token: number,
  clearAppendLoading: () => void
) => {
  if (guard.activeAppendToken !== token) return

  guard.activeAppendToken = null
  clearAppendLoading()
}

const isUnauthorizedError = (error: unknown) =>
  isAxiosError(error) && error.response?.status === 401

const userInfo = ref<ProfileUser | null>(null)
const stats = ref<ProfileStats | null>(null)
const myRoutes = ref<ProfileRoute[]>([])
const myRoutesPageSize = 20
const myRoutesPageInfo = ref<PageMetadata>({
  page: 0,
  size: myRoutesPageSize,
  totalElements: 0,
  totalPages: 0
})
const myRoutesRefreshing = ref(false)
const myRoutesLoadingMore = ref(false)
const myRoutesLoadError = ref('')
const myRoutesRetryMode = ref<ProfileListRetryMode>('refresh')
const myRoutesRequestGuard = createPaginatedRequestGuard()
const bookings = ref<ProfileSpotBooking[]>([])
const bookingsPageSize = 20
const bookingsPageInfo = ref<PageMetadata>({
  page: 0,
  size: bookingsPageSize,
  totalElements: 0,
  totalPages: 0
})
const bookingsRefreshing = ref(false)
const bookingsLoadingMore = ref(false)
const bookingsLoadError = ref('')
const bookingsRetryMode = ref<ProfileListRetryMode>('refresh')
const bookingsRequestGuard = createPaginatedRequestGuard()
const hotelBookings = ref<ProfileHotelBooking[]>([])
const hotelBookingsPageSize = 20
const hotelBookingsPageInfo = ref<PageMetadata>({
  page: 0,
  size: hotelBookingsPageSize,
  totalElements: 0,
  totalPages: 0
})
const hotelBookingsRefreshing = ref(false)
const hotelBookingsLoadingMore = ref(false)
const hotelBookingsLoadError = ref('')
const hotelBookingsRetryMode = ref<ProfileListRetryMode>('refresh')
const hotelBookingsRequestGuard = createPaginatedRequestGuard()
const spotComments = ref<ProfileSpotComment[]>([])
const routeComments = ref<ProfileRouteComment[]>([])
const commentsPageSize = 20
const spotCommentsPageInfo = ref<PageMetadata>({
  page: 0,
  size: commentsPageSize,
  totalElements: 0,
  totalPages: 0
})
const routeCommentsPageInfo = ref<PageMetadata>({
  page: 0,
  size: commentsPageSize,
  totalElements: 0,
  totalPages: 0
})
const commentsRefreshing = ref(false)
const commentsLoadingMore = ref(false)
const commentsLoadError = ref('')
const commentsRetryMode = ref<ProfileListRetryMode>('refresh')
const commentsRequestGuard = createPaginatedRequestGuard()
const loading = ref(true)
const profileAuthFlowPending = ref(false)
type ProfileTabId = 'routes' | 'bookings' | 'hotel-bookings' | 'comments'
const activeTab = ref<ProfileTabId>('routes')
const profileTabId = (tabId: ProfileTabId) => `profile-tab-${tabId}`
const profileTabPanelId = (tabId: ProfileTabId) => `profile-tabpanel-${tabId}`
const activateProfileTab = (tabId: ProfileTabId) => {
  activeTab.value = tabId
}
const focusProfileTab = (tabId: ProfileTabId) => {
  if (typeof document === 'undefined') return
  const focusTab = () => document.getElementById(profileTabId(tabId))?.focus()

  if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
    window.requestAnimationFrame(focusTab)
  } else {
    focusTab()
  }
}
const moveProfileTabFocus = (currentIndex: number, nextIndex: number) => {
  const tabs = profileTabs.value
  const nextTab = tabs[(nextIndex + tabs.length) % tabs.length]

  if (!nextTab) return
  activateProfileTab(nextTab.id)
  focusProfileTab(nextTab.id)
}
const handleProfileTabKeydown = (event: KeyboardEvent, index: number) => {
  if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
    event.preventDefault()
    moveProfileTabFocus(index, index + 1)
  } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
    event.preventDefault()
    moveProfileTabFocus(index, index - 1)
  } else if (event.key === 'Home') {
    event.preventDefault()
    moveProfileTabFocus(index, 0)
  } else if (event.key === 'End') {
    event.preventDefault()
    moveProfileTabFocus(index, profileTabs.value.length - 1)
  }
}
const showPasswordModal = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordFormError = ref('')
const profilePasswordErrorId = 'profile-password-error'
const passwordFieldInvalid = computed(() => passwordFormError.value ? 'true' : undefined)
const describePasswordField = (helpId: string) => computed(() => [
  helpId,
  passwordFormError.value ? profilePasswordErrorId : ''
].filter(Boolean).join(' '))
const currentPasswordDescription = describePasswordField('profile-current-password-help')
const newPasswordDescription = describePasswordField('profile-new-password-help')
const confirmNewPasswordDescription = describePasswordField('profile-confirm-new-password-help')
const changingPassword = ref(false)
const showNicknameModal = ref(false)
const nicknameForm = ref({
  nickname: ''
})
const nicknameFormError = ref('')
const profileNicknameErrorId = 'profile-nickname-error'
const nicknameFieldInvalid = computed(() => nicknameFormError.value ? 'true' : undefined)
const nicknameDescription = computed(() => [
  'profile-nickname-help',
  nicknameFormError.value ? profileNicknameErrorId : ''
].filter(Boolean).join(' '))
const updatingNickname = ref(false)
const uploadingAvatar = ref(false)
const avatarFileInput = ref<HTMLInputElement | null>(null)

const normalizeProfileText = (value: unknown) => typeof value === 'string' ? value.trim() : ''
const profileRole = computed(() =>
  normalizeProfileText(userInfo.value?.role) ||
  normalizeProfileText(auth.user?.role)
)
const profileRoleLabel = computed(() => profileRole.value === 'ADMIN' ? t('profile.admin') : t('profile.member'))
const profileDisplayName = computed(() =>
  normalizeProfileText(userInfo.value?.nickname) ||
  normalizeProfileText(auth.user?.nickname) ||
  profileRoleLabel.value
)
const profileAvatarUrl = computed(() =>
  normalizeProfileText(userInfo.value?.avatar) ||
  normalizeProfileText(userInfo.value?.avatarUrl) ||
  normalizeProfileText(auth.user?.avatar) ||
  normalizeProfileText(auth.user?.avatarUrl) ||
  undefined
)
const profileAvatarInitial = computed(() => profileDisplayName.value.charAt(0).toUpperCase())

const profileTabs = computed<Array<{ id: ProfileTabId; label: string; count: number }>>(() => [
  { id: 'routes', label: t('profile.myRoutesTab'), count: myRoutesPageInfo.value.totalElements || myRoutes.value.length },
  { id: 'bookings', label: t('profile.myBookingsTab'), count: bookingsPageInfo.value.totalElements || bookings.value.length },
  { id: 'hotel-bookings', label: t('profile.myHotelBookingsTab'), count: hotelBookingsPageInfo.value.totalElements || hotelBookings.value.length },
  {
    id: 'comments',
    label: t('profile.myCommentsTab'),
    count: (spotCommentsPageInfo.value.totalElements + routeCommentsPageInfo.value.totalElements) || (spotComments.value.length + routeComments.value.length)
  }
])

const myRoutesPage = computed(() => myRoutesPageInfo.value.page)
const myRoutesTotalPages = computed(() => myRoutesPageInfo.value.totalPages)
const myRoutesTotalElements = computed(() => myRoutesPageInfo.value.totalElements)
const hasMoreMyRoutes = computed(() => hasNextPage(myRoutesPageInfo.value))
const myRoutesLoadMoreBusy = computed(() => myRoutesLoadingMore.value || myRoutesRefreshing.value)
const bookingsPage = computed(() => bookingsPageInfo.value.page)
const bookingsTotalPages = computed(() => bookingsPageInfo.value.totalPages)
const hasMoreBookings = computed(() => hasNextPage(bookingsPageInfo.value))
const bookingsLoadMoreBusy = computed(() => bookingsLoadingMore.value || bookingsRefreshing.value)
const hotelBookingsPage = computed(() => hotelBookingsPageInfo.value.page)
const hotelBookingsTotalPages = computed(() => hotelBookingsPageInfo.value.totalPages)
const hasMoreHotelBookings = computed(() => hasNextPage(hotelBookingsPageInfo.value))
const hotelBookingsLoadMoreBusy = computed(() => hotelBookingsLoadingMore.value || hotelBookingsRefreshing.value)
const commentsPage = computed(() => Math.max(spotCommentsPageInfo.value.page, routeCommentsPageInfo.value.page))
const commentsTotalPages = computed(() => Math.max(spotCommentsPageInfo.value.totalPages, routeCommentsPageInfo.value.totalPages))
const hasMoreComments = computed(() => hasNextPage(spotCommentsPageInfo.value) || hasNextPage(routeCommentsPageInfo.value))
const commentsLoadMoreBusy = computed(() => commentsLoadingMore.value || commentsRefreshing.value)
const profileText = (key: string, fallbackKey: string) => {
  const translated = t(key)
  return translated === key ? t(fallbackKey) : translated
}
const profileListLoadErrorMessage = (key: string) => profileText(key, 'profile.loadFailed')
const profileRetryText = (key: string) => profileText(key, 'profile.retryListLoading')

onMounted(async () => {
  if (!(await auth.ensureSession())) {
    await router.push('/login')
    return
  }
  profileAuthFlowPending.value = false

  try {
    await fetchUserInfo()
    const mustChangePassword = Boolean(userInfo.value?.mustChangePassword || auth.user?.mustChangePassword)
    if (mustChangePassword) {
      auth.updateUser({ mustChangePassword: true })
    }
    if (mustChangePassword || route.query.changePassword === '1') {
      showPasswordModal.value = true
      return
    }

    await loadProfileDetails()
  } catch (e: unknown) {
    // 如果API调用失败（特别是401），响应拦截器会处理跳转
    console.error('Failed to load user profile:', summarizeClientError(e))
    if (isUnauthorizedError(e)) {
      profileAuthFlowPending.value = true
    } else {
      // 如果不是401错误，显示错误信息
      showToast(t('profile.loadFailed'), 'error')
    }
  } finally {
    loading.value = false
  }
})

const loadProfileDetails = async () => {
  await Promise.all([
    fetchStats(),
    fetchMyRoutes(),
    fetchBookings(),
    fetchHotelBookings(),
    fetchMyComments()
  ])
}

const fetchUserInfo = async () => {
  try {
    const response = await api.get<ProfileUser>(endpoints.auth.me)
    userInfo.value = response.data
  } catch (e: unknown) {
    console.error('Failed to fetch user info:', summarizeClientError(e))
    // 如果是401错误，说明token无效，让响应拦截器处理跳转
    if (isUnauthorizedError(e)) {
      profileAuthFlowPending.value = true
      throw e // 重新抛出，让响应拦截器处理
    }
  }
}

const fetchStats = async () => {
  try {
    const response = await api.get<ProfileStats>(endpoints.auth.meStats)
    stats.value = response.data
  } catch (e: unknown) {
    console.error('Failed to fetch stats:', summarizeClientError(e))
    // 如果是401错误，说明token无效，让响应拦截器处理跳转
    if (isUnauthorizedError(e)) {
      profileAuthFlowPending.value = true
      throw e // 重新抛出，让响应拦截器处理
    }
  }
}

const applyMyRoutesPage = (response: PaginatedHttpResponse, append = false) => {
  const page = readPaginatedResponse<ProfileRoute>(response, {
    page: append ? myRoutesPageInfo.value.page + 1 : 0,
    size: myRoutesPageSize
  })

  myRoutes.value = append ? mergeUniqueById(myRoutes.value, page.content) : page.content
  myRoutesPageInfo.value = {
    page: page.page,
    size: page.size || myRoutesPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const fetchMyRoutes = async (page = 0, append = false) => {
  if (append && myRoutesRefreshing.value) return

  const requestToken = beginPaginatedRequest(
    myRoutesRequestGuard,
    append,
    () => {
      myRoutesLoadingMore.value = false
    }
  )

  if (append) {
    myRoutesLoadingMore.value = true
  } else {
    myRoutesRefreshing.value = true
  }

  try {
    const response = await api.get(endpoints.routes.myRoutes, {
      params: { page, size: myRoutesPageSize }
    })
    if (!isLatestPaginatedRequest(myRoutesRequestGuard, requestToken)) return

    applyMyRoutesPage(response, append)
    myRoutesLoadError.value = ''
    myRoutesRetryMode.value = 'refresh'
  } catch (e: unknown) {
    if (isUnauthorizedError(e)) {
      profileAuthFlowPending.value = true
      throw e
    }
    if (!isLatestPaginatedRequest(myRoutesRequestGuard, requestToken)) return

    console.error('Failed to fetch my routes:', summarizeClientError(e))
    myRoutesLoadError.value = profileListLoadErrorMessage(
      append ? 'profile.routesAppendLoadFailed' : 'profile.routesLoadFailed'
    )
    myRoutesRetryMode.value = append ? 'append' : 'refresh'
  } finally {
    if (append) {
      finishAppendRequest(myRoutesRequestGuard, requestToken, () => {
        myRoutesLoadingMore.value = false
      })
    } else if (isLatestPaginatedRequest(myRoutesRequestGuard, requestToken)) {
      myRoutesRefreshing.value = false
    }
  }
}

const loadNextMyRoutesPage = async () => {
  if (myRoutesLoadMoreBusy.value || !hasMoreMyRoutes.value) return
  await fetchMyRoutes(myRoutesPageInfo.value.page + 1, true)
}

const retryMyRoutes = async () => {
  if (myRoutesLoadMoreBusy.value) return
  if (myRoutesRetryMode.value === 'append' && hasMoreMyRoutes.value) {
    await fetchMyRoutes(myRoutesPageInfo.value.page + 1, true)
    return
  }

  await fetchMyRoutes()
}

const applyBookingsPage = (response: PaginatedHttpResponse, append = false) => {
  const page = readPaginatedResponse<ProfileSpotBooking>(response, {
    page: append ? bookingsPageInfo.value.page + 1 : 0,
    size: bookingsPageSize
  })

  bookings.value = append ? mergeUniqueById(bookings.value, page.content) : page.content
  bookingsPageInfo.value = {
    page: page.page,
    size: page.size || bookingsPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const fetchBookings = async (page = 0, append = false) => {
  if (append && bookingsRefreshing.value) return

  const requestToken = beginPaginatedRequest(
    bookingsRequestGuard,
    append,
    () => {
      bookingsLoadingMore.value = false
    }
  )

  if (append) {
    bookingsLoadingMore.value = true
  } else {
    bookingsRefreshing.value = true
  }

  try {
    const response = await api.get(endpoints.bookings.my, {
      params: { page, size: bookingsPageSize }
    })
    if (!isLatestPaginatedRequest(bookingsRequestGuard, requestToken)) return

    applyBookingsPage(response, append)
    bookingsLoadError.value = ''
    bookingsRetryMode.value = 'refresh'
  } catch (e) {
    if (isUnauthorizedError(e)) {
      profileAuthFlowPending.value = true
      throw e
    }
    if (!isLatestPaginatedRequest(bookingsRequestGuard, requestToken)) return

    console.error('Failed to fetch bookings:', summarizeClientError(e))
    bookingsLoadError.value = profileListLoadErrorMessage(
      append ? 'profile.bookingsAppendLoadFailed' : 'profile.bookingsLoadFailed'
    )
    bookingsRetryMode.value = append ? 'append' : 'refresh'
  } finally {
    if (append) {
      finishAppendRequest(bookingsRequestGuard, requestToken, () => {
        bookingsLoadingMore.value = false
      })
    } else if (isLatestPaginatedRequest(bookingsRequestGuard, requestToken)) {
      bookingsRefreshing.value = false
    }
  }
}

const loadNextBookingsPage = async () => {
  if (bookingsLoadMoreBusy.value || !hasMoreBookings.value) return
  await fetchBookings(bookingsPageInfo.value.page + 1, true)
}

const retryBookings = async () => {
  if (bookingsLoadMoreBusy.value) return
  if (bookingsRetryMode.value === 'append' && hasMoreBookings.value) {
    await fetchBookings(bookingsPageInfo.value.page + 1, true)
    return
  }

  await fetchBookings()
}

const emptyPageInfo = (size: number): PageMetadata => ({
  page: 0,
  size,
  totalElements: 0,
  totalPages: 0
})

const pageAfterItemRemoval = (pageInfo: PageMetadata) => {
  const totalElements = Math.max(0, pageInfo.totalElements - 1)
  const size = pageInfo.size || 1
  const totalPages = size > 0 && totalElements > 0 ? Math.ceil(totalElements / size) : 0

  return {
    ...pageInfo,
    page: totalPages > 0 ? Math.min(pageInfo.page, totalPages - 1) : 0,
    totalElements,
    totalPages
  }
}

const applyHotelBookingsPage = (response: PaginatedHttpResponse, append = false) => {
  const page = readPaginatedResponse<ProfileHotelBooking>(response, {
    page: append ? hotelBookingsPageInfo.value.page + 1 : 0,
    size: hotelBookingsPageSize
  })

  hotelBookings.value = append ? mergeUniqueById(hotelBookings.value, page.content) : page.content
  hotelBookingsPageInfo.value = {
    page: page.page,
    size: page.size || hotelBookingsPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const fetchHotelBookings = async (page = 0, append = false) => {
  if (append && hotelBookingsRefreshing.value) return

  const requestToken = beginPaginatedRequest(
    hotelBookingsRequestGuard,
    append,
    () => {
      hotelBookingsLoadingMore.value = false
    }
  )

  if (append) {
    hotelBookingsLoadingMore.value = true
  } else {
    hotelBookingsRefreshing.value = true
  }

  try {
    const response = await api.get(endpoints.hotelBookings.my, {
      params: { page, size: hotelBookingsPageSize }
    })
    if (!isLatestPaginatedRequest(hotelBookingsRequestGuard, requestToken)) return

    applyHotelBookingsPage(response, append)
    hotelBookingsLoadError.value = ''
    hotelBookingsRetryMode.value = 'refresh'
  } catch (e) {
    if (isUnauthorizedError(e)) {
      profileAuthFlowPending.value = true
      throw e
    }
    if (!isLatestPaginatedRequest(hotelBookingsRequestGuard, requestToken)) return

    console.error('Failed to fetch hotel bookings:', summarizeClientError(e))
    hotelBookingsLoadError.value = profileListLoadErrorMessage(
      append ? 'profile.hotelBookingsAppendLoadFailed' : 'profile.hotelBookingsLoadFailed'
    )
    hotelBookingsRetryMode.value = append ? 'append' : 'refresh'
  } finally {
    if (append) {
      finishAppendRequest(hotelBookingsRequestGuard, requestToken, () => {
        hotelBookingsLoadingMore.value = false
      })
    } else if (isLatestPaginatedRequest(hotelBookingsRequestGuard, requestToken)) {
      hotelBookingsRefreshing.value = false
    }
  }
}

const loadNextHotelBookingsPage = async () => {
  if (hotelBookingsLoadMoreBusy.value || !hasMoreHotelBookings.value) return
  await fetchHotelBookings(hotelBookingsPageInfo.value.page + 1, true)
}

const retryHotelBookings = async () => {
  if (hotelBookingsLoadMoreBusy.value) return
  if (hotelBookingsRetryMode.value === 'append' && hasMoreHotelBookings.value) {
    await fetchHotelBookings(hotelBookingsPageInfo.value.page + 1, true)
    return
  }

  await fetchHotelBookings()
}

const getApiErrorMessage = (error: unknown, fallback: string) => {
  return safeClientErrorMessage(error, fallback)
}

const confirmDangerousAction = (message: string) => showConfirm({ message, tone: 'danger' })

const cancelHotelBooking = async (id: number) => {
  if (!(await confirmDangerousAction(t('profile.confirmCancelHotelBooking')))) return

  try {
    await api.delete(endpoints.hotelBookings.cancel(id))
    await fetchHotelBookings()
  } catch (e) {
    console.error('Failed to cancel hotel booking:', summarizeClientError(e))
    showToast(t('profile.cancelHotelBookingFailed'), 'error')
  }
}

const deleteHotelBooking = async (id: number) => {
  if (!(await confirmDangerousAction(t('profile.confirmDeleteBooking')))) return

  try {
    await api.delete(endpoints.hotelBookings.delete(id))
    hotelBookings.value = hotelBookings.value.filter(booking => booking.id !== id)
    hotelBookingsPageInfo.value = pageAfterItemRemoval(hotelBookingsPageInfo.value)
    await fetchHotelBookings()
  } catch (e: unknown) {
    console.error('Failed to delete hotel booking:', summarizeClientError(e))
    showToast(getApiErrorMessage(e, t('profile.deleteBookingFailed')), 'error')
  }
}

const applyCommentsPage = (response: PaginatedHttpResponse, append = false) => {
  const body = response.data && typeof response.data === 'object'
    ? response.data as ProfileCommentsResponse
    : {}
  const fallbackPage = append ? commentsPage.value + 1 : 0
  const spotPage = readPaginatedResponse<ProfileSpotComment>({
    data: body.spotCommentsPage || body.spotComments || []
  }, {
    page: fallbackPage,
    size: commentsPageSize
  })
  const routePage = readPaginatedResponse<ProfileRouteComment>({
    data: body.routeCommentsPage || body.routeComments || []
  }, {
    page: fallbackPage,
    size: commentsPageSize
  })

  spotComments.value = append ? mergeUniqueById(spotComments.value, spotPage.content) : spotPage.content
  routeComments.value = append ? mergeUniqueById(routeComments.value, routePage.content) : routePage.content
  spotCommentsPageInfo.value = {
    page: spotPage.page,
    size: spotPage.size || commentsPageSize,
    totalElements: spotPage.totalElements,
    totalPages: spotPage.totalPages
  }
  routeCommentsPageInfo.value = {
    page: routePage.page,
    size: routePage.size || commentsPageSize,
    totalElements: routePage.totalElements,
    totalPages: routePage.totalPages
  }
}

const fetchMyComments = async (page = 0, append = false) => {
  if (append && commentsRefreshing.value) return

  const requestToken = beginPaginatedRequest(
    commentsRequestGuard,
    append,
    () => {
      commentsLoadingMore.value = false
    }
  )

  if (append) {
    commentsLoadingMore.value = true
  } else {
    commentsRefreshing.value = true
  }

  try {
    const response = await api.get(endpoints.auth.meComments, {
      params: { page, size: commentsPageSize }
    })
    if (!isLatestPaginatedRequest(commentsRequestGuard, requestToken)) return

    applyCommentsPage(response, append)
    commentsLoadError.value = ''
    commentsRetryMode.value = 'refresh'
  } catch (e: unknown) {
    if (isUnauthorizedError(e)) {
      profileAuthFlowPending.value = true
      throw e
    }
    if (!isLatestPaginatedRequest(commentsRequestGuard, requestToken)) return

    console.error('Failed to fetch my comments:', summarizeClientError(e))
    commentsLoadError.value = profileListLoadErrorMessage(
      append ? 'profile.commentsAppendLoadFailed' : 'profile.commentsLoadFailed'
    )
    commentsRetryMode.value = append ? 'append' : 'refresh'
  } finally {
    if (append) {
      finishAppendRequest(commentsRequestGuard, requestToken, () => {
        commentsLoadingMore.value = false
      })
    } else if (isLatestPaginatedRequest(commentsRequestGuard, requestToken)) {
      commentsRefreshing.value = false
    }
  }
}

const loadNextCommentsPage = async () => {
  if (commentsLoadMoreBusy.value || !hasMoreComments.value) return
  await fetchMyComments(commentsPage.value + 1, true)
}

const retryMyComments = async () => {
  if (commentsLoadMoreBusy.value) return
  if (commentsRetryMode.value === 'append' && hasMoreComments.value) {
    await fetchMyComments(commentsPage.value + 1, true)
    return
  }

  await fetchMyComments()
}

const cancelBooking = async (id: number) => {
  if (!(await confirmDangerousAction(t('profile.confirmCancelBooking')))) return

  try {
    await api.post(endpoints.bookings.cancel(id))
    await fetchBookings()
  } catch (e: unknown) {
    console.error('Failed to cancel booking:', summarizeClientError(e))
    showToast(getApiErrorMessage(e, t('profile.cancelFailed')), 'error')
  }
}

const deleteBooking = async (id: number) => {
  if (!(await confirmDangerousAction(t('profile.confirmDeleteBooking')))) return

  try {
    await api.delete(endpoints.bookings.delete(id))
    bookings.value = bookings.value.filter(booking => booking.id !== id)
    bookingsPageInfo.value = pageAfterItemRemoval(bookingsPageInfo.value)
    await fetchBookings()
  } catch (e: unknown) {
    console.error('Failed to delete booking:', summarizeClientError(e))
    showToast(getApiErrorMessage(e, t('profile.deleteBookingFailed')), 'error')
  }
}

const deleteRoute = async (id: number) => {
  if (!(await confirmDangerousAction(t('profile.confirmDeleteRoute')))) return
  
  try {
    await api.delete(endpoints.routes.sharedDetail(id))
    showToast(t('profile.deleteSuccess'), 'success')
    await fetchMyRoutes()
  } catch (e) {
    console.error('Failed to delete route:', summarizeClientError(e))
    showToast(t('profile.deleteFailed'), 'error')
  }
}

const deleteSpotComment = async (id: number) => {
  if (!(await confirmDangerousAction(t('profile.confirmDeleteComment')))) return

  try {
    await api.delete(endpoints.comments.delete(id))
    spotComments.value = spotComments.value.filter(comment => comment.id !== id)
    spotCommentsPageInfo.value = pageAfterItemRemoval(spotCommentsPageInfo.value)
    const currentStats = stats.value
    if (currentStats && (currentStats.commentCount ?? 0) > 0) {
      currentStats.commentCount = (currentStats.commentCount ?? 0) - 1
    }
    await Promise.all([fetchMyComments(), fetchStats()])
  } catch (e) {
    console.error('Failed to delete spot comment:', summarizeClientError(e))
    showToast(t('profile.deleteFailed'), 'error')
  }
}

const deleteRouteComment = async (comment: ProfileRouteComment) => {
  if (!(await confirmDangerousAction(t('profile.confirmDeleteComment')))) return
  const routeId = comment.route?.id

  if (!routeId) {
    showToast(t('profile.deleteFailed'), 'error')
    return
  }

  try {
    await api.delete(endpoints.routes.deleteSharedComment(routeId, comment.id))
    routeComments.value = routeComments.value.filter(item => item.id !== comment.id)
    routeCommentsPageInfo.value = pageAfterItemRemoval(routeCommentsPageInfo.value)
    const currentStats = stats.value
    if (currentStats && (currentStats.commentCount ?? 0) > 0) {
      currentStats.commentCount = (currentStats.commentCount ?? 0) - 1
    }
    await Promise.all([fetchMyComments(), fetchStats()])
  } catch (e) {
    console.error('Failed to delete route comment:', summarizeClientError(e))
    showToast(t('profile.deleteFailed'), 'error')
  }
}

const clearPasswordFormError = () => {
  passwordFormError.value = ''
}

const showPasswordFormError = (message: string, tone: 'warning' | 'error') => {
  passwordFormError.value = message
  showToast(message, tone)
}

const changePassword = async () => {
  if (changingPassword.value) return

  clearPasswordFormError()

  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    showPasswordFormError(t('profile.fillAllFields'), 'warning')
    return
  }
  
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showPasswordFormError(t('profile.passwordMismatch'), 'warning')
    return
  }
  
  if (passwordForm.value.newPassword.length < 6) {
    showPasswordFormError(t('profile.passwordMinLength'), 'warning')
    return
  }
  
  changingPassword.value = true
  try {
    await api.post(endpoints.auth.changePassword, {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    showToast(t('profile.passwordChangeSuccess'), 'success')
    showPasswordModal.value = false
    auth.updateUser({ mustChangePassword: false })
    if (userInfo.value) {
      userInfo.value.mustChangePassword = false
    }
    if (route.query.changePassword === '1') {
      await router.replace({ path: '/profile' })
    }
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (e: unknown) {
    console.error('Failed to change password:', summarizeClientError(e))
    const errorMsg = safeClientErrorMessage(e, t('profile.passwordChangeFailed'))
    showPasswordFormError(errorMsg, 'error')
  } finally {
    changingPassword.value = false
  }
}

const formatDate = (dateStr?: string | null) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return t('common.pendingConfirm')
  return date.toLocaleDateString(toIntlLocale(locale.value))
}

const openPasswordModal = () => {
  clearPasswordFormError()
  showPasswordModal.value = true
}

const closePasswordModal = () => {
  if (changingPassword.value) return
  showPasswordModal.value = false
  clearPasswordFormError()
}

const clearNicknameFormError = () => {
  nicknameFormError.value = ''
}

const showNicknameFormError = (message: string, tone: 'warning' | 'error') => {
  nicknameFormError.value = message
  showToast(message, tone)
}

const openNicknameModal = () => {
  clearNicknameFormError()
  nicknameForm.value.nickname =
    normalizeProfileText(userInfo.value?.nickname) ||
    normalizeProfileText(auth.user?.nickname)
  showNicknameModal.value = true
}

const closeNicknameModal = () => {
  if (updatingNickname.value) return
  showNicknameModal.value = false
  clearNicknameFormError()
}

const updateNickname = async () => {
  if (updatingNickname.value) return

  clearNicknameFormError()
  const nickname = nicknameForm.value.nickname.trim()
  if (!nickname) {
    showNicknameFormError(t('profile.nicknameRequired'), 'warning')
    return
  }
  
  updatingNickname.value = true
  try {
    await api.put(endpoints.auth.updateNickname, {
      nickname
    })
    showToast(t('profile.nicknameUpdateSuccess'), 'success')
    showNicknameModal.value = false
    await fetchUserInfo()
    auth.updateUser({ nickname })
  } catch (e: unknown) {
    console.error('Failed to update nickname:', summarizeClientError(e))
    const errorMsg = safeClientErrorMessage(e, t('profile.nicknameUpdateFailed'))
    showNicknameFormError(errorMsg, 'error')
  } finally {
    updatingNickname.value = false
  }
}

const handleAvatarClick = () => {
  if (uploadingAvatar.value) return
  avatarFileInput.value?.click()
}

const handleAvatarUpload = async (event: Event) => {
  if (uploadingAvatar.value) return
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    showToast(t('profile.selectImageFile'), 'warning')
    target.value = ''
    return
  }
  
  // 验证文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    showToast(t('profile.imageTooLarge'), 'warning')
    target.value = ''
    return
  }
  
  uploadingAvatar.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await api.post<AvatarUploadResponse>(endpoints.auth.uploadAvatar, formData)
    
    showToast(t('profile.avatarUploadSuccess'), 'success')
    await fetchUserInfo()
    auth.updateUser({ avatar: response.data.avatarUrl })
  } catch (e: unknown) {
    console.error('Failed to upload avatar:', summarizeClientError(e))
    const errorMsg = safeClientErrorMessage(e, t('profile.avatarUploadFailed'))
    showToast(errorMsg, 'error')
  } finally {
    uploadingAvatar.value = false
    // 清空input
    if (target) {
      target.value = ''
    }
  }
}

const getAvatarUrl = (): string | undefined => profileAvatarUrl.value || undefined
</script>

<template>
  <div class="min-h-screen bg-tibet-white">
    <motion.main
      class="max-w-7xl mx-auto px-3 py-6 sm:px-4 sm:py-8"
      :initial="revealInitial"
      :animate="revealInView"
      :transition="revealTransition"
    >
      <!-- User Info Card -->
      <motion.section
        class="glass-card rounded-2xl p-5 mb-6 shadow-xl border border-white/50 sm:rounded-3xl sm:p-8 sm:mb-8"
        :initial="cardInitial"
        :animate="cardInView"
        :transition="cardTransition(0, 0.04)"
      >
        <div class="flex flex-col md:flex-row items-center md:items-start gap-6">
          <div class="relative group">
            <motion.button
              type="button"
              @click="handleAvatarClick"
              class="w-20 h-20 rounded-full flex items-center justify-center text-2xl text-white font-bold shadow-lg cursor-pointer overflow-hidden transition-all hover:ring-4 hover:ring-tibet-gold/60/50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-tibet-gold/70 disabled:cursor-wait disabled:opacity-75 sm:h-24 sm:w-24 sm:text-3xl"
              :whileHover="{ scale: 1.04, rotate: -1 }"
              :whileTap="{ scale: 0.96 }"
              :class="getAvatarUrl() ? '' : 'bg-gradient-to-br from-blue-500 to-purple-600'"
              :disabled="uploadingAvatar"
              :aria-busy="uploadingAvatar"
              :aria-label="`${t('common.edit')} ${t('profile.avatar')}`"
              :title="`${t('common.edit')} ${t('profile.avatar')}`"
            >
              <img 
                v-if="getAvatarUrl()" 
                :src="getAvatarUrl()" 
                :alt="t('profile.avatar')" 
                class="w-full h-full object-cover"
              >
              <span v-else>
                {{ profileAvatarInitial }}
              </span>
            </motion.button>
            <div class="pointer-events-none absolute inset-0 bg-black/0 group-hover:bg-black/20 rounded-full flex items-center justify-center transition-all" aria-hidden="true">
              <svg v-if="!uploadingAvatar" xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-white opacity-0 group-hover:opacity-100 transition-opacity" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <div v-else class="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
            </div>
            <input 
              id="avatar-upload"
              ref="avatarFileInput"
              type="file" 
              accept="image/*" 
              :aria-label="t('profile.avatar')"
              @change="handleAvatarUpload"
              class="hidden"
            >
          </div>
          <div class="flex-1 text-center md:text-left">
            <div class="flex items-center justify-center md:justify-start gap-2 mb-2">
              <h1 class="min-w-0 break-words text-2xl font-bold text-tibet-dark sm:text-3xl">
                {{ profileDisplayName }}
              </h1>
              <motion.button
                type="button"
                @click="openNicknameModal"
                class="text-tibet-brown/70 hover:text-tibet-gold transition-colors"
                :whileHover="{ scale: 1.12, rotate: -4 }"
                :whileTap="{ scale: 0.9 }"
                :aria-label="t('profile.editNickname')"
                :title="t('profile.editNickname')"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </motion.button>
            </div>
            <p class="text-tibet-brown/70 mb-4">
              {{ profileRoleLabel }} ·
              {{ t('profile.registeredAt') }} {{ userInfo?.createdAt ? formatDate(userInfo.createdAt) : '' }}
            </p>
            <div class="flex flex-wrap gap-2 justify-center md:justify-start mb-4 sm:gap-4">
              <motion.div
                class="flex min-w-[9rem] items-center justify-center gap-2 px-3 py-2 bg-white/50 rounded-xl sm:min-w-0 sm:px-4"
                :initial="{ opacity: 0, y: 10 }"
                :animate="{ opacity: 1, y: 0 }"
                :transition="cardTransition(0, 0.16)"
                :whileHover="{ y: -2, scale: 1.02 }"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-tibet-gold" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                </svg>
                <span class="text-sm font-medium text-tibet-dark/80">
                  <span class="text-tibet-gold font-bold">{{ stats?.routeCount || 0 }}</span> {{ t('profile.routesCount') }}
                </span>
              </motion.div>
              <motion.div
                class="flex min-w-[9rem] items-center justify-center gap-2 px-3 py-2 bg-white/50 rounded-xl sm:min-w-0 sm:px-4"
                :initial="{ opacity: 0, y: 10 }"
                :animate="{ opacity: 1, y: 0 }"
                :transition="cardTransition(1, 0.16)"
                :whileHover="{ y: -2, scale: 1.02 }"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
                <span class="text-sm font-medium text-tibet-dark/80">
                  <span class="text-green-500 font-bold">{{ stats?.commentCount || 0 }}</span> {{ t('profile.commentsCount') }}
                </span>
              </motion.div>
              <motion.div
                class="flex min-w-[9rem] items-center justify-center gap-2 px-3 py-2 bg-white/50 rounded-xl sm:min-w-0 sm:px-4"
                :initial="{ opacity: 0, y: 10 }"
                :animate="{ opacity: 1, y: 0 }"
                :transition="cardTransition(2, 0.16)"
                :whileHover="{ y: -2, scale: 1.02 }"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-orange-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
                <span class="text-sm font-medium text-tibet-dark/80">
                  <span class="text-orange-500 font-bold">{{ stats?.bookingCount || 0 }}</span> {{ t('profile.bookingsCount') }}
                </span>
              </motion.div>
            </div>
            <motion.button
              type="button"
              @click="openPasswordModal"
              class="min-w-0 whitespace-normal break-words rounded-xl bg-tibet-gold px-4 py-2 text-center text-sm font-medium leading-snug text-white transition-colors hover:bg-tibet-gold/80"
              :whileHover="{ y: -2, scale: 1.02 }"
              :whileTap="{ scale: 0.96 }"
            >
              {{ t('profile.changePassword') }}
            </motion.button>
          </div>
        </div>
      </motion.section>

      <!-- Nickname Edit Modal -->
      <MotionModal
        :show="showNicknameModal"
        modal-key="nickname-modal"
        panel-class="glass rounded-3xl p-5 sm:p-8 max-w-md mx-4 border border-white/50"
        backdrop-class="bg-black/50"
        labelled-by="nickname-modal-title"
        @close="closeNicknameModal"
      >
          <h2 id="nickname-modal-title" class="text-2xl font-bold text-tibet-dark mb-6">{{ t('profile.editNickname') }}</h2>
          <form
            @submit.prevent="updateNickname"
            class="space-y-4"
            :aria-busy="updatingNickname"
            :aria-describedby="nicknameFormError ? profileNicknameErrorId : undefined"
          >
            <div>
              <label for="profile-nickname" class="block text-sm font-medium text-tibet-dark/80 mb-2">{{ t('profile.nickname') }}</label>
              <input 
                id="profile-nickname"
                v-model="nicknameForm.nickname" 
                name="nickname"
                type="text" 
                required
                maxlength="20"
                autocomplete="nickname"
                :disabled="updatingNickname"
                :aria-describedby="nicknameDescription"
                :aria-invalid="nicknameFieldInvalid"
                class="w-full rounded-xl border border-tibet-gold/25 bg-white/50 px-4 py-2 outline-none focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:opacity-70"
                :placeholder="t('profile.nicknamePlaceholder')"
                @input="clearNicknameFormError"
              />
              <p id="profile-nickname-help" class="mt-1 text-xs text-gray-500">{{ nicknameForm.nickname.length }}/20</p>
            </div>
            <div
              v-if="nicknameFormError"
              :id="profileNicknameErrorId"
              role="alert"
              aria-live="assertive"
              class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {{ nicknameFormError }}
            </div>
            <div class="flex flex-col gap-3 pt-4 sm:flex-row">
              <motion.button
                type="button"
                :disabled="updatingNickname"
                @click="closeNicknameModal"
                class="min-w-0 flex-1 whitespace-normal break-words rounded-xl border border-tibet-gold/25 px-4 py-2 text-center font-medium leading-snug text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                :whileHover="updatingNickname ? {} : { y: -1, scale: 1.02 }"
                :whileTap="updatingNickname ? {} : { scale: 0.96 }"
              >
                {{ t('profile.cancel') }}
              </motion.button>
              <motion.button
                type="submit"
                :disabled="updatingNickname"
                class="min-w-0 flex-1 whitespace-normal break-words rounded-xl bg-tibet-gold px-4 py-2 text-center font-medium leading-snug text-white transition-colors hover:bg-tibet-gold/80 disabled:cursor-not-allowed disabled:opacity-50"
                :whileHover="updatingNickname ? {} : { y: -1, scale: 1.02 }"
                :whileTap="updatingNickname ? {} : { scale: 0.96 }"
              >
                {{ updatingNickname ? t('profile.updating') : t('profile.confirmUpdate') }}
              </motion.button>
            </div>
          </form>
      </MotionModal>

      <!-- Password Change Modal -->
      <MotionModal
        :show="showPasswordModal"
        modal-key="password-modal"
        panel-class="glass rounded-3xl p-5 sm:p-8 max-w-md mx-4 border border-white/50"
        backdrop-class="bg-black/50"
        labelled-by="password-modal-title"
        @close="closePasswordModal"
      >
          <h2 id="password-modal-title" class="text-2xl font-bold text-tibet-dark mb-6">{{ t('profile.changePassword') }}</h2>
          <form
            @submit.prevent="changePassword"
            class="space-y-4"
            :aria-busy="changingPassword"
            :aria-describedby="passwordFormError ? profilePasswordErrorId : undefined"
          >
            <div>
              <label for="profile-current-password" class="block text-sm font-medium text-tibet-dark/80 mb-2">{{ t('profile.currentPassword') }}</label>
              <input 
                id="profile-current-password"
                v-model="passwordForm.oldPassword" 
                name="current-password"
                type="password" 
                required
                autocomplete="current-password"
                :disabled="changingPassword"
                :aria-describedby="currentPasswordDescription"
                :aria-invalid="passwordFieldInvalid"
                class="w-full rounded-xl border border-tibet-gold/25 bg-white/50 px-4 py-2 outline-none focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:opacity-70"
                :placeholder="t('profile.currentPasswordPlaceholder')"
                @input="clearPasswordFormError"
              />
              <p id="profile-current-password-help" class="sr-only">{{ t('profile.currentPassword') }}</p>
            </div>
            <div>
              <label for="profile-new-password" class="block text-sm font-medium text-tibet-dark/80 mb-2">{{ t('profile.newPassword') }}</label>
              <input 
                id="profile-new-password"
                v-model="passwordForm.newPassword" 
                name="new-password"
                type="password" 
                required
                autocomplete="new-password"
                minlength="6"
                :disabled="changingPassword"
                :aria-describedby="newPasswordDescription"
                :aria-invalid="passwordFieldInvalid"
                class="w-full rounded-xl border border-tibet-gold/25 bg-white/50 px-4 py-2 outline-none focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:opacity-70"
                :placeholder="t('profile.newPasswordPlaceholder')"
                @input="clearPasswordFormError"
              />
              <p id="profile-new-password-help" class="sr-only">{{ t('profile.newPassword') }}</p>
            </div>
            <div>
              <label for="profile-confirm-new-password" class="block text-sm font-medium text-tibet-dark/80 mb-2">{{ t('profile.confirmNewPassword') }}</label>
              <input 
                id="profile-confirm-new-password"
                v-model="passwordForm.confirmPassword" 
                name="confirm-new-password"
                type="password" 
                required
                autocomplete="new-password"
                minlength="6"
                :disabled="changingPassword"
                :aria-describedby="confirmNewPasswordDescription"
                :aria-invalid="passwordFieldInvalid"
                class="w-full rounded-xl border border-tibet-gold/25 bg-white/50 px-4 py-2 outline-none focus:border-tibet-gold focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:opacity-70"
                :placeholder="t('profile.confirmNewPasswordPlaceholder')"
                @input="clearPasswordFormError"
              />
              <p id="profile-confirm-new-password-help" class="sr-only">{{ t('profile.confirmNewPassword') }}</p>
            </div>
            <div
              v-if="passwordFormError"
              :id="profilePasswordErrorId"
              role="alert"
              aria-live="assertive"
              class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {{ passwordFormError }}
            </div>
            <div class="flex flex-col gap-3 pt-4 sm:flex-row">
              <motion.button
                type="button"
                :disabled="changingPassword"
                @click="closePasswordModal"
                class="min-w-0 flex-1 whitespace-normal break-words rounded-xl border border-tibet-gold/25 px-4 py-2 text-center font-medium leading-snug text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                :whileHover="changingPassword ? {} : { y: -1, scale: 1.02 }"
                :whileTap="changingPassword ? {} : { scale: 0.96 }"
              >
                {{ t('profile.cancel') }}
              </motion.button>
              <motion.button
                type="submit"
                :disabled="changingPassword"
                class="min-w-0 flex-1 whitespace-normal break-words rounded-xl bg-tibet-gold px-4 py-2 text-center font-medium leading-snug text-white transition-colors hover:bg-tibet-gold/80 disabled:cursor-not-allowed disabled:opacity-50"
                :whileHover="changingPassword ? {} : { y: -1, scale: 1.02 }"
                :whileTap="changingPassword ? {} : { scale: 0.96 }"
              >
                {{ changingPassword ? t('profile.changing') : t('profile.confirmChange') }}
              </motion.button>
            </div>
          </form>
      </MotionModal>

      <!-- Tabs -->
      <motion.section
        class="glass-card rounded-2xl p-4 mb-8 shadow-xl border border-white/50 sm:p-6"
        :initial="cardInitial"
        :animate="cardInView"
        :transition="cardTransition(1, 0.08)"
      >
        <LayoutGroup>
        <div class="-mx-4 flex gap-2 overflow-x-auto border-b border-tibet-gold/25 px-4 pb-1 mb-5 sm:mx-0 sm:gap-4 sm:px-0 sm:pb-0 sm:mb-6" role="tablist" :aria-label="t('profile.title')">
          <motion.button
            v-for="(tab, index) in profileTabs"
            :key="tab.id"
            :id="profileTabId(tab.id)"
            type="button"
            role="tab"
            :aria-selected="activeTab === tab.id"
            :aria-controls="profileTabPanelId(tab.id)"
            :tabindex="activeTab === tab.id ? 0 : -1"
            @click="activateProfileTab(tab.id)"
            @keydown="handleProfileTabKeydown($event, index)"
            class="relative shrink-0 px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors sm:px-6 sm:text-base"
            :class="activeTab === tab.id ? 'text-tibet-gold' : 'text-tibet-brown/70 hover:text-tibet-dark/80'"
            :whileHover="{ y: -1 }"
            :whileTap="{ scale: 0.96 }"
          >
            {{ tab.label }} ({{ tab.count }})
            <motion.span
              v-if="activeTab === tab.id"
              layoutId="profile-tab-underline"
              class="absolute left-0 right-0 -bottom-px h-0.5 rounded-full bg-tibet-gold"
              :transition="softSpring"
            />
          </motion.button>
        </div>
        </LayoutGroup>

        <!-- Loading -->
        <AnimatePresence mode="wait">
        <motion.div
          v-if="loading"
          key="profile-loading"
          class="text-center py-12"
          :initial="{ opacity: 0, y: 14 }"
          :animate="{ opacity: 1, y: 0 }"
          :exit="{ opacity: 0, y: -10 }"
          :transition="revealTransition"
        >
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold mx-auto"></div>
        </motion.div>

        <motion.div
          v-else
          :key="activeTab"
          :initial="{ opacity: 0, y: 18, scale: 0.99 }"
          :animate="{ opacity: 1, y: 0, scale: 1 }"
          :exit="{ opacity: 0, y: -12, scale: 0.99 }"
          :transition="revealTransition"
        >

        <!-- My Routes -->
        <div v-if="activeTab === 'routes'" role="tabpanel" :id="profileTabPanelId('routes')" :aria-labelledby="profileTabId('routes')" tabindex="0" :aria-busy="myRoutesLoadMoreBusy">
          <div
            v-if="myRoutesLoadError"
            class="mb-6 rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-red-700"
            role="alert"
          >
            <p class="mb-3 font-medium">{{ myRoutesLoadError }}</p>
            <button
              type="button"
              @click="retryMyRoutes"
              :disabled="myRoutesLoadMoreBusy"
              :aria-busy="myRoutesLoadMoreBusy"
              class="min-h-11 rounded-xl bg-red-600 px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:cursor-wait disabled:opacity-60"
            >
              {{ myRoutesLoadMoreBusy ? t('common.loading') : profileRetryText('profile.retryRoutes') }}
            </button>
          </div>

          <div v-if="!profileAuthFlowPending && !myRoutesLoadError && myRoutes.length === 0" class="text-center py-12">
            <p class="text-gray-500 mb-4">{{ t('profile.noRoutes') }}</p>
            <router-link to="/create-route" class="text-tibet-gold hover:text-tibet-gold/80 font-medium">
              {{ t('profile.createRouteLink') }}
            </router-link>
          </div>

          <template v-if="myRoutes.length > 0">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <motion.div
              v-for="(route, index) in myRoutes"
              :key="route.id"
              layout
              class="glass-card rounded-2xl p-4 hover:shadow-2xl transition-all duration-500 ease-out border border-white/20 group sm:p-6"
              :initial="cardInitial"
              :animate="cardInView"
              :exit="cardExit"
              :transition="cardTransition(index)"
              :whileHover="{ y: -4, scale: 1.01 }"
            >
              <div class="flex justify-between items-start mb-4">
                <h3 class="min-w-0 flex-1 text-lg font-bold text-gray-900 sm:text-xl">
                  <router-link
                    :to="`/community/${route.id}`"
                    class="block line-clamp-2 rounded-lg transition-colors duration-300 group-hover:text-tibet-gold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/60 focus-visible:ring-offset-2"
                  >
                    {{ route.title }}
                  </router-link>
                </h3>
                <motion.button
                  type="button"
                  @click="deleteRoute(route.id)"
                  class="ml-2 text-red-500 hover:text-red-700 transition-colors"
                  :whileHover="{ scale: 1.12, rotate: -4 }"
                  :whileTap="{ scale: 0.9 }"
                  :aria-label="`${t('profile.delete')} ${route.title}`"
                  :title="t('profile.delete')"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </motion.button>
              </div>
              
              <div class="flex flex-wrap gap-2 mb-4 text-sm text-gray-600">
                <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ route.days }}{{ t('profile.days') }}</span>
                <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ route.budget }}</span>
                <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ route.preference }}</span>
              </div>
              
              <div class="flex flex-col gap-3 text-sm text-gray-500 pt-4 border-t border-tibet-gold/20 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex flex-wrap items-center gap-3 sm:gap-4">
                  <span class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    {{ route.viewCount }}
                  </span>
                  <span class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                    {{ route.likeCount }}
                  </span>
                  <span class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                    {{ route.commentCount }}
                  </span>
                </div>
                <span class="text-xs">{{ formatDate(route.createdAt) }}</span>
              </div>
            </motion.div>
          </div>
          <div
            v-if="myRoutes.length > 0 && myRoutesTotalPages > 1"
            class="mt-8 flex flex-wrap items-center justify-center gap-3"
            role="navigation"
            :aria-label="t('profile.myRoutesTab')"
          >
            <span class="text-sm text-gray-500" role="status" aria-live="polite">
              {{ myRoutesPage + 1 }} / {{ myRoutesTotalPages }}
            </span>
            <button
              type="button"
              @click="loadNextMyRoutesPage"
              :disabled="myRoutesLoadMoreBusy || !hasMoreMyRoutes"
              :aria-busy="myRoutesLoadMoreBusy"
              class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-medium text-tibet-brown/80 transition-colors hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50"
            >
              {{ myRoutesLoadMoreBusy ? t('common.loading') : t('community.nextPage') }}
            </button>
          </div>
          </template>
        </div>

        <!-- My Bookings -->
        <div v-if="activeTab === 'bookings'" role="tabpanel" :id="profileTabPanelId('bookings')" :aria-labelledby="profileTabId('bookings')" tabindex="0" :aria-busy="bookingsLoadMoreBusy">
          <div
            v-if="bookingsLoadError"
            class="mb-6 rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-red-700"
            role="alert"
          >
            <p class="mb-3 font-medium">{{ bookingsLoadError }}</p>
            <button
              type="button"
              @click="retryBookings"
              :disabled="bookingsLoadMoreBusy"
              :aria-busy="bookingsLoadMoreBusy"
              class="min-h-11 rounded-xl bg-red-600 px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:cursor-wait disabled:opacity-60"
            >
              {{ bookingsLoadMoreBusy ? t('common.loading') : profileRetryText('profile.retryBookings') }}
            </button>
          </div>

          <div v-if="!profileAuthFlowPending && !bookingsLoadError && bookings.length === 0" class="text-center py-12">
            <p class="text-gray-500 mb-4">{{ t('profile.noBookings') }}</p>
            <router-link to="/spots" class="text-tibet-gold hover:text-tibet-gold/80 font-medium">
              {{ t('profile.browseSpotsLink') }}
            </router-link>
          </div>

          <template v-if="bookings.length > 0">
          <div class="space-y-4">
            <motion.div
              v-for="(booking, index) in bookings"
              :key="booking.id"
              layout
              class="glass-card rounded-2xl p-4 flex flex-col md:flex-row justify-between items-start md:items-center transition-all hover:shadow-lg border border-white/20 sm:p-6"
              :initial="cardInitial"
              :animate="cardInView"
              :exit="cardExit"
              :transition="cardTransition(index)"
              :whileHover="{ y: -3, scale: 1.006 }"
            >
              <div class="flex w-full min-w-0 items-start gap-3 mb-4 md:mb-0 md:w-auto sm:items-center sm:gap-4">
                <div class="h-16 w-16 rounded-xl bg-gray-100 overflow-hidden flex-shrink-0">
                  <img :src="booking.spot?.imageUrl || ''" class="w-full h-full object-cover" alt="">
                </div>
                <div class="min-w-0">
                  <h3 class="truncate text-lg font-bold text-gray-900 mb-1">{{ booking.spot?.name }}</h3>
                  <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-gray-500 text-sm">
                    <span>{{ t('profile.visitDate') }} {{ booking.visitDate }}</span>
                    <span>🎫 {{ booking.ticketCount }}{{ t('profile.tickets') }}</span>
                  </div>
                </div>
              </div>
              
              <div class="flex w-full flex-wrap items-center justify-between gap-3 md:w-auto md:justify-end md:gap-6">
                <div class="text-right">
                  <div class="text-xl font-bold text-tibet-gold mb-1">¥{{ booking.totalPrice }}</div>
                  <span :class="{
                    'bg-green-100 text-green-800': booking.status === 'CONFIRMED',
                    'bg-yellow-100 text-yellow-800': booking.status === 'PENDING',
                    'bg-red-100 text-red-800': booking.status === 'CANCELLED'
                  }" class="px-2.5 py-0.5 rounded-full text-xs font-medium">
                    {{ booking.status === 'CONFIRMED' ? t('profile.bookingSuccess') : (booking.status === 'PENDING' ? t('profile.pending') : t('profile.cancelled')) }}
                  </span>
                </div>
                
                <motion.button
                  v-if="booking.status === 'CONFIRMED' || booking.status === 'PENDING'"
                  @click="cancelBooking(booking.id)"
                  class="px-4 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-xl transition-colors"
                  :whileHover="{ y: -1, scale: 1.02 }"
                  :whileTap="{ scale: 0.96 }"
                >
                  {{ t('profile.cancelBooking') }}
                </motion.button>
                <motion.button
                  v-else-if="booking.status === 'CANCELLED'"
                  @click="deleteBooking(booking.id)"
                  class="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-xl transition-colors"
                  :whileHover="{ y: -1, scale: 1.02 }"
                  :whileTap="{ scale: 0.96 }"
                >
                  {{ t('profile.deleteBooking') }}
                </motion.button>
              </div>
            </motion.div>
          </div>
          <div
            v-if="bookingsTotalPages > 1"
            class="mt-8 flex flex-wrap items-center justify-center gap-3"
            role="navigation"
            :aria-label="t('profile.myBookingsTab')"
          >
            <span class="text-sm text-gray-500" role="status" aria-live="polite">
              {{ bookingsPage + 1 }} / {{ bookingsTotalPages }}
            </span>
            <button
              type="button"
              @click="loadNextBookingsPage"
              :disabled="bookingsLoadMoreBusy || !hasMoreBookings"
              :aria-busy="bookingsLoadMoreBusy"
              class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-medium text-tibet-brown/80 transition-colors hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50"
            >
              {{ bookingsLoadMoreBusy ? t('common.loading') : t('community.nextPage') }}
            </button>
          </div>
          </template>
        </div>

        <!-- My Hotel Bookings -->
        <div v-if="activeTab === 'hotel-bookings'" role="tabpanel" :id="profileTabPanelId('hotel-bookings')" :aria-labelledby="profileTabId('hotel-bookings')" tabindex="0" :aria-busy="hotelBookingsLoadMoreBusy">
          <div
            v-if="hotelBookingsLoadError"
            class="mb-6 rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-red-700"
            role="alert"
          >
            <p class="mb-3 font-medium">{{ hotelBookingsLoadError }}</p>
            <button
              type="button"
              @click="retryHotelBookings"
              :disabled="hotelBookingsLoadMoreBusy"
              :aria-busy="hotelBookingsLoadMoreBusy"
              class="min-h-11 rounded-xl bg-red-600 px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:cursor-wait disabled:opacity-60"
            >
              {{ hotelBookingsLoadMoreBusy ? t('common.loading') : profileRetryText('profile.retryHotelBookings') }}
            </button>
          </div>

          <div v-if="!profileAuthFlowPending && !hotelBookingsLoadError && hotelBookings.length === 0" class="text-center py-12">
            <p class="text-gray-500 mb-4">{{ t('profile.noHotelBookings') }}</p>
            <router-link to="/hotels" class="text-tibet-gold hover:text-tibet-gold/80 font-medium">
              {{ t('profile.browseHotelsLink') }}
            </router-link>
          </div>

          <div v-if="hotelBookings.length > 0" class="space-y-4">
            <motion.div
              v-for="(booking, index) in hotelBookings"
              :key="booking.id"
              layout
              class="glass-card rounded-2xl p-4 flex flex-col md:flex-row justify-between items-start md:items-center transition-all hover:shadow-lg border border-white/20 sm:p-6"
              :initial="cardInitial"
              :animate="cardInView"
              :exit="cardExit"
              :transition="cardTransition(index)"
              :whileHover="{ y: -3, scale: 1.006 }"
            >
              <div class="flex w-full min-w-0 items-start gap-3 mb-4 md:mb-0 md:w-auto sm:items-center sm:gap-4">
                <div class="h-16 w-16 rounded-xl bg-gray-100 overflow-hidden flex-shrink-0">
                  <img :src="resolveHotelBookingImage(booking)" class="w-full h-full object-cover" alt="" @error="applyHotelImageFallback">
                </div>
                <div class="min-w-0">
                  <h3 class="truncate text-lg font-bold text-gray-900 mb-1">{{ booking.hotel?.name || '-' }}</h3>
                  <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-gray-500 text-sm">
                    <span>{{ t('profile.room') }} {{ booking.roomName }}</span>
                    <span>{{ t('profile.nights') }} {{ booking.nights }}</span>
                  </div>
                  <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-gray-500 text-sm mt-1">
                    <span>{{ t('profile.checkIn') }} {{ booking.checkInDate }}</span>
                    <span>{{ t('profile.checkOut') }} {{ booking.checkOutDate }}</span>
                  </div>
                </div>
              </div>
              
              <div class="flex w-full flex-wrap items-center justify-between gap-3 md:w-auto md:justify-end md:gap-6">
                <div class="text-right">
                  <div class="text-xl font-bold text-tibet-gold mb-1">¥{{ booking.totalPrice }}</div>
                  <span :class="{
                    'bg-green-100 text-green-800': booking.status === 'CONFIRMED',
                    'bg-yellow-100 text-yellow-800': booking.status === 'PENDING',
                    'bg-red-100 text-red-800': booking.status === 'CANCELLED'
                  }" class="px-2.5 py-0.5 rounded-full text-xs font-medium">
                    {{ booking.status === 'CONFIRMED' ? t('profile.bookingSuccess') : (booking.status === 'PENDING' ? t('profile.pending') : t('profile.cancelled')) }}
                  </span>
                </div>
                
                <motion.button
                  v-if="booking.status === 'CONFIRMED' || booking.status === 'PENDING'"
                  @click="cancelHotelBooking(booking.id)"
                  class="px-4 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-xl transition-colors"
                  :whileHover="{ y: -1, scale: 1.02 }"
                  :whileTap="{ scale: 0.96 }"
                >
                  {{ t('profile.cancelBooking') }}
                </motion.button>
                <motion.button
                  v-else-if="booking.status === 'CANCELLED'"
                  @click="deleteHotelBooking(booking.id)"
                  class="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-xl transition-colors"
                  :whileHover="{ y: -1, scale: 1.02 }"
                  :whileTap="{ scale: 0.96 }"
                >
                  {{ t('profile.deleteBooking') }}
                </motion.button>
              </div>
            </motion.div>
          </div>
          <div
            v-if="hotelBookings.length > 0 && hotelBookingsTotalPages > 1"
            class="mt-8 flex flex-wrap items-center justify-center gap-3"
            role="navigation"
            :aria-label="t('profile.myHotelBookingsTab')"
          >
            <span class="text-sm text-gray-500" role="status" aria-live="polite">
              {{ hotelBookingsPage + 1 }} / {{ hotelBookingsTotalPages }}
            </span>
            <button
              type="button"
              @click="loadNextHotelBookingsPage"
              :disabled="hotelBookingsLoadMoreBusy || !hasMoreHotelBookings"
              :aria-busy="hotelBookingsLoadMoreBusy"
              class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-medium text-tibet-brown/80 transition-colors hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50"
            >
              {{ hotelBookingsLoadMoreBusy ? t('common.loading') : t('community.nextPage') }}
            </button>
          </div>
        </div>

        <!-- My Comments -->
        <div v-if="activeTab === 'comments'" role="tabpanel" :id="profileTabPanelId('comments')" :aria-labelledby="profileTabId('comments')" tabindex="0" :aria-busy="commentsLoadMoreBusy">
          <div
            v-if="commentsLoadError"
            class="mb-6 rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-red-700"
            role="alert"
          >
            <p class="mb-3 font-medium">{{ commentsLoadError }}</p>
            <button
              type="button"
              @click="retryMyComments"
              :disabled="commentsLoadMoreBusy"
              :aria-busy="commentsLoadMoreBusy"
              class="min-h-11 rounded-xl bg-red-600 px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:cursor-wait disabled:opacity-60"
            >
              {{ commentsLoadMoreBusy ? t('common.loading') : profileRetryText('profile.retryComments') }}
            </button>
          </div>

          <div v-if="!profileAuthFlowPending && !commentsLoadError && spotComments.length === 0 && routeComments.length === 0" class="text-center py-12">
            <p class="text-gray-500 mb-4">{{ t('profile.noComments') }}</p>
            <router-link to="/spots" class="text-tibet-gold hover:text-tibet-gold/80 font-medium">
              {{ t('profile.browseSpotsLink') }}
            </router-link>
          </div>

          <div v-if="spotComments.length > 0 || routeComments.length > 0" class="space-y-6">
            <!-- Spot Comments -->
            <div v-if="spotComments.length > 0">
              <h3 class="text-lg font-bold text-tibet-dark mb-4">{{ t('profile.spotComments') }} ({{ spotComments.length }})</h3>
              <div class="space-y-4">
                <motion.div
                  v-for="(comment, index) in spotComments"
                  :key="comment.id"
                  class="glass-card rounded-2xl p-4 border border-white/20 hover:shadow-lg transition-all sm:p-6"
                  :initial="cardInitial"
                  :animate="cardInView"
                  :exit="cardExit"
                  :transition="cardTransition(index)"
                  :whileHover="{ y: -3, scale: 1.006 }"
                >
                  <div class="flex items-start gap-4">
                    <div class="min-w-0 flex-1">
                      <div class="flex flex-wrap items-center gap-2 mb-2">
                        <router-link 
                          :to="`/spots/${comment.spot?.id}`"
                          class="font-bold text-tibet-gold hover:text-tibet-gold/80"
                        >
                          {{ comment.spot?.name }}
                        </router-link>
                        <div class="flex items-center gap-1 text-yellow-500">
                          <span v-for="i in 5" :key="i" class="text-sm">
                            {{ i <= (comment.rating || 0) ? '★' : '☆' }}
                          </span>
                        </div>
                      </div>
                      <p class="text-gray-700 mb-2">{{ comment.content }}</p>
                      <div v-if="comment.imageUrl" class="mb-2">
                        <img :src="comment.imageUrl" :alt="t('profile.commentImageAlt')" class="max-w-full rounded-lg sm:max-w-xs">
                      </div>
                      <div class="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                        <span>👍 {{ comment.likeCount || 0 }}</span>
                        <span>{{ formatDate(comment.createdAt) }}</span>
                        <button
                          @click="deleteSpotComment(comment.id)"
                          class="font-medium text-red-500 hover:text-red-700 transition-colors"
                        >
                          {{ t('common.delete') }}
                        </button>
                      </div>
                    </div>
                  </div>
                </motion.div>
              </div>
            </div>

            <!-- Route Comments -->
            <div v-if="routeComments.length > 0">
              <h3 class="text-lg font-bold text-tibet-dark mb-4 mt-6">{{ t('profile.routeComments') }} ({{ routeComments.length }})</h3>
              <div class="space-y-4">
                <motion.div
                  v-for="(comment, index) in routeComments"
                  :key="comment.id"
                  class="glass-card rounded-2xl p-4 border border-white/20 hover:shadow-lg transition-all sm:p-6"
                  :initial="cardInitial"
                  :animate="cardInView"
                  :exit="cardExit"
                  :transition="cardTransition(index + spotComments.length)"
                  :whileHover="{ y: -3, scale: 1.006 }"
                >
                  <div class="flex items-start gap-4">
                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-2 mb-2">
                        <router-link 
                          :to="`/community/${comment.route?.id}`"
                          class="font-bold text-tibet-gold hover:text-tibet-gold/80"
                        >
                          {{ comment.route?.title }}
                        </router-link>
                      </div>
                      <p class="text-gray-700 mb-2">{{ comment.content }}</p>
                      <div class="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                        <span>{{ formatDate(comment.createdAt) }}</span>
                        <button
                          @click="deleteRouteComment(comment)"
                          class="font-medium text-red-500 hover:text-red-700 transition-colors"
                        >
                          {{ t('common.delete') }}
                        </button>
                      </div>
                    </div>
                  </div>
                </motion.div>
              </div>
            </div>

            <div
              v-if="commentsTotalPages > 1"
              class="mt-8 flex flex-wrap items-center justify-center gap-3"
              role="navigation"
              :aria-label="t('profile.myCommentsTab')"
            >
              <span class="text-sm text-gray-500" role="status" aria-live="polite">
                {{ commentsPage + 1 }} / {{ commentsTotalPages }}
              </span>
              <button
                type="button"
                @click="loadNextCommentsPage"
                :disabled="commentsLoadMoreBusy || !hasMoreComments"
                :aria-busy="commentsLoadMoreBusy"
                class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-medium text-tibet-brown/80 transition-colors hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50"
              >
                {{ commentsLoadMoreBusy ? t('common.loading') : t('community.nextPage') }}
              </button>
            </div>
          </div>
        </div>
        </motion.div>
        </AnimatePresence>
      </motion.section>
    </motion.main>
  </div>
</template>
