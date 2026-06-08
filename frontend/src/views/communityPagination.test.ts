// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, type App, type Component } from 'vue'
import { createI18n } from 'vue-i18n'
import { readPaginatedResponse } from '../api/endpoints'

const testState = vi.hoisted(() => ({
  apiDelete: vi.fn(),
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  ensureSession: vi.fn(),
  hasValidSession: vi.fn(),
  refreshSession: vi.fn(),
  requireAuth: vi.fn(),
  routeParams: { id: '42' } as Record<string, string>,
  routeQuery: {} as Record<string, string>,
  routerBack: vi.fn(),
  routerPush: vi.fn(),
  routerReplace: vi.fn(),
  showConfirm: vi.fn(),
  showToast: vi.fn(),
  updateUser: vi.fn(),
  authUser: null as any
}))

const endpointsMock = vi.hoisted(() => ({
  auth: {
    me: '/auth/me',
    meStats: '/auth/me/stats',
    meComments: '/auth/me/comments',
    changePassword: '/auth/me/change-password',
    uploadAvatar: '/auth/me/upload-avatar',
    updateNickname: '/auth/me/nickname'
  },
  bookings: {
    my: '/bookings/my',
    cancel: (id: number) => `/bookings/${id}/cancel`,
    delete: (id: number) => `/bookings/${id}`
  },
  comments: {
    list: (spotId: number) => `/comments/spot/${spotId}`,
    create: '/comments',
    delete: (id: number) => `/comments/${id}`,
    liked: (id: number) => `/comments/${id}/liked`,
    like: (id: number) => `/comments/${id}/like`,
    uploadImage: '/comments/upload-image'
  },
  community: {
    questionDetail: (id: number | string) => `/community/questions/${id}`,
    questionAnswers: (id: number | string) => `/community/questions/${id}/answers`,
    questionLikeStatus: (id: number | string) => `/community/questions/${id}/like-status`,
    questionLike: (id: number | string) => `/community/questions/${id}/like`,
    createQuestionAnswer: (id: number | string) => `/community/questions/${id}/answers`,
    acceptQuestionAnswer: (questionId: number | string, answerId: number | string) =>
      `/community/questions/${questionId}/answers/${answerId}/accept`,
    deleteQuestion: (id: number | string) => `/community/questions/${id}`
  },
  hotelBookings: {
    my: '/hotel-bookings/my',
    cancel: (id: number) => `/hotel-bookings/${id}`,
    delete: (id: number) => `/hotel-bookings/${id}/permanent`
  },
  routes: {
    sharedDetail: (id: number) => `/routes/shared/${id}`,
    sharedComments: (id: number) => `/routes/shared/${id}/comments`,
    sharedLikeStatus: (id: number) => `/routes/shared/${id}/like-status`,
    sharedLike: (id: number) => `/routes/shared/${id}/like`,
    deleteSharedComment: (routeId: number, commentId: number) => `/routes/shared/${routeId}/comments/${commentId}`,
    myRoutes: '/routes/my-routes'
  },
  spots: {
    detail: (id: number) => `/spots/${id}`
  }
}))

vi.mock('../api', () => ({
  default: {
    delete: testState.apiDelete,
    get: testState.apiGet,
    post: testState.apiPost,
    put: testState.apiPut
  },
  endpoints: endpointsMock
}))

vi.mock('@/api', () => ({
  default: {
    delete: testState.apiDelete,
    get: testState.apiGet,
    post: testState.apiPost,
    put: testState.apiPut
  },
  endpoints: endpointsMock
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    fullPath: `/test/${testState.routeParams.id}`,
    params: testState.routeParams,
    query: testState.routeQuery
  }),
  useRouter: () => ({
    back: testState.routerBack,
    push: testState.routerPush,
    replace: testState.routerReplace
  })
}))

vi.mock('../stores/auth', () => ({
  useAuthStore: () => ({
    ensureSession: testState.ensureSession,
    hasValidSession: testState.hasValidSession,
    refreshSession: testState.refreshSession,
    updateUser: testState.updateUser,
    get user() {
      return testState.authUser
    }
  })
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    ensureSession: testState.ensureSession,
    hasValidSession: testState.hasValidSession,
    refreshSession: testState.refreshSession,
    updateUser: testState.updateUser,
    get user() {
      return testState.authUser
    }
  })
}))

vi.mock('../composables/useAuthGuard', () => ({
  useAuthGuard: () => ({ requireAuth: testState.requireAuth })
}))

vi.mock('../composables/useConfirm', () => ({
  showConfirm: testState.showConfirm,
  useConfirm: () => ({ showConfirm: testState.showConfirm })
}))

vi.mock('@/composables/useConfirm', () => ({
  showConfirm: testState.showConfirm,
  useConfirm: () => ({ showConfirm: testState.showConfirm })
}))

vi.mock('../composables/useToast', () => ({
  showToast: testState.showToast,
  useToast: () => ({ showToast: testState.showToast })
}))

vi.mock('@/composables/useToast', () => ({
  showToast: testState.showToast,
  useToast: () => ({ showToast: testState.showToast })
}))

vi.mock('../motion/presets', () => ({
  cardExit: {},
  cardInitial: {},
  cardInView: {},
  cardTransition: () => ({}),
  inViewOnce: {},
  motionEase: 'easeOut',
  revealInitial: {},
  revealInView: {},
  revealTransition: {},
  softSpring: {}
}))

vi.mock('@/motion/presets', () => ({
  cardExit: {},
  cardInitial: {},
  cardInView: {},
  cardTransition: () => ({}),
  inViewOnce: {},
  motionEase: 'easeOut',
  revealInitial: {},
  revealInView: {},
  revealTransition: {},
  softSpring: {}
}))

vi.mock('motion-v', async () => {
  const vue = await import('vue')
  const passthrough = (tag = 'div') => vue.defineComponent({
    name: `Motion${tag}`,
    setup(_, { attrs, slots }) {
      return () => vue.h(tag, attrs, slots.default?.())
    }
  })

  return {
    AnimatePresence: passthrough(),
    LayoutGroup: passthrough(),
    motion: new Proxy({}, {
      get: (_, tag) => passthrough(String(tag))
    })
  }
})

vi.mock('lucide-vue-next', async () => {
  const vue = await import('vue')
  const Icon = vue.defineComponent({
    name: 'IconStub',
    setup() {
      return () => vue.h('svg')
    }
  })

  return {
    Eye: Icon,
    Heart: Icon,
    Minus: Icon,
    Plus: Icon
  }
})

vi.mock('../utils/browserStorage', () => ({
  readBrowserStorage: () => 'zh'
}))

vi.mock('@/utils/browserStorage', () => ({
  readBrowserStorage: () => 'zh'
}))

vi.mock('../utils/errorMonitoring', () => ({
  safeClientErrorMessage: (_error: unknown, fallback: string) => fallback,
  summarizeClientError: (error: unknown) => error instanceof Error ? error.message : String(error)
}))

vi.mock('@/utils/errorMonitoring', () => ({
  safeClientErrorMessage: (_error: unknown, fallback: string) => fallback,
  summarizeClientError: (error: unknown) => error instanceof Error ? error.message : String(error)
}))

vi.mock('../utils/sanitize', () => ({
  renderMarkdownToSafeHtml: (markdown: string) => markdown
}))

vi.mock('@/data/hotelImages', () => ({
  applyHotelImageFallback: vi.fn(),
  resolveHotelBookingImage: () => ''
}))

vi.mock('@/components/motion/MotionModal.vue', async () => {
  const vue = await import('vue')

  return {
  default: vue.defineComponent({
    name: 'MotionModalStub',
    props: { show: Boolean },
    setup(props, { slots }) {
      return () => props.show ? vue.h('div', slots.default?.()) : null
    }
  })
  }
})

vi.mock('../components/MobileStickyActionBar.vue', async () => {
  const vue = await import('vue')

  return {
  default: vue.defineComponent({
    name: 'MobileStickyActionBarStub',
    setup() {
      return () => null
    }
  })
  }
})

vi.mock('../utils/externalBooking', () => ({
  openExternalBooking: vi.fn(() => true)
}))

vi.mock('../utils/domText', () => ({
  createTextPopupContent: (_title: string, body: string) => body
}))

import QuestionDetail from './QuestionDetail.vue'
import RouteDetail from './RouteDetail.vue'
import ScenicSpotDetail from './ScenicSpotDetail.vue'
import UserProfile from './UserProfile.vue'

const mountedApps: App[] = []

const RouterLinkStub = defineComponent({
  name: 'RouterLinkStub',
  props: {
    to: {
      required: true,
      type: [String, Object]
    }
  },
  setup(props, { slots }) {
    return () => h('a', { href: typeof props.to === 'string' ? props.to : '#' }, slots.default?.())
  }
})

const createTestI18n = () =>
  createI18n({
    legacy: false,
    locale: 'zh',
    messages: {
      zh: {
        common: {
          cancel: '取消',
          delete: '删除',
          loading: '加载中',
          priceCny: '¥{price}',
          freeTicket: '免费'
        },
        community: {
          accepted: '已采纳',
          acceptAnswer: '采纳',
          answers: '回答',
          nextPage: '下一页',
          noAnswers: '暂无回答',
          officialRoute: '官方路线',
          previousPage: '上一页',
          solved: '已解决',
          submittingAnswer: '提交中',
          submitAnswer: '提交回答',
          tagLabels: [],
          unsolved: '待解决',
          writeAnswer: '写回答',
          yourAnswer: '你的回答'
        },
        profile: {
          admin: '管理员',
          avatar: '头像',
          browseHotelsLink: '浏览酒店',
          browseSpotsLink: '浏览景点',
          bookingsCount: '咨询',
          cancel: '取消',
          cancelBooking: '取消咨询',
          changePassword: '修改密码',
          commentsCount: '评论',
          confirmDeleteBooking: '删除咨询?',
          confirmDeleteComment: '删除评论?',
          confirmDeleteRoute: '删除路线?',
          createRouteLink: '创建路线',
          days: '天',
          delete: '删除',
          deleteBooking: '删除咨询',
          editNickname: '编辑昵称',
          loadingOrders: '加载中',
          loadFailed: '加载失败',
          member: '会员',
          myBookingsTab: '景点咨询',
          myCommentsTab: '我的评论',
          myHotelBookingsTab: '酒店咨询',
          myRoutesTab: '我的路线',
          noBookings: '暂无景点咨询',
          noComments: '暂无评论',
          noHotelBookings: '暂无酒店咨询',
          noRoutes: '暂无路线',
          registeredAt: '注册于',
          routeCount: '路线',
          routesCount: '路线'
        },
        questionDetail: {
          anonymousUser: '匿名用户',
          answerFailed: '回答失败',
          backToCommunity: '返回社区',
          deleteFailed: '删除失败',
          deleteQuestion: '删除问题',
          likedState: '已赞',
          likeQuestionAction: '点赞',
          likeQuestionAria: '{action}，{state}，{count}',
          notLikedState: '未赞',
          questionNotFound: '问题不存在',
          unlikeQuestionAction: '取消赞',
          viewCount: '{count} 次浏览'
        },
        routeDetail: {
          anonymous: '匿名用户',
          author: '作者',
          backToList: '返回列表',
          commentFailed: '评论失败',
          commentPlaceholder: '写评论',
          comments: '评论',
          commentSuccess: '评论成功',
          confirmDeleteComment: '删除评论?',
          days: '天',
          deleteCommentFailed: '删除失败',
          deleteCommentSuccess: '已删除',
          loginRequiredComment: '请登录后评论',
          loginRequiredLike: '请登录后点赞',
          noComments: '暂无评论',
          notFound: '未找到路线',
          operationFailed: '操作失败',
          postComment: '发表评论',
          publishedAt: '发布于',
          submitting: '提交中'
        },
        spotDetail: {
          addPhoto: '添加照片',
          backToSpots: '返回景点',
          basePriceHint: '基础票价',
          commentFailed: '评论失败',
          commentImageAlt: '{name} 的评论图片',
          commentRating: '{rating} 星',
          comments: '评论',
          commentsLoadFailed: '评论加载失败',
          commentsLoading: '评论加载中',
          confirmDeleteComment: '删除评论?',
          cultural: '人文',
          deleteCommentAria: '删除评论',
          detailErrorMessage: '景点加载失败',
          detailErrorTitle: '加载失败',
          externalBook: '外部预订',
          imageFormats: '支持图片',
          imageSizeHint: '5MB 以内',
          imageUploaded: '已上传',
          introduction: '介绍',
          likeComment: '点赞评论',
          likeCount: '{count} 个赞',
          loadingSpot: '加载景点',
          loginToComment: '登录后评论',
          natural: '自然',
          noComments: '暂无评论',
          noDescription: '暂无介绍',
          noPlatformPaymentHint: '将跳转外部平台',
          offSeasonHint: '淡季',
          openCommentImage: '打开图片',
          operationFailed: '操作失败',
          peakSeasonHint: '旺季',
          postComment: '发表评论',
          publishComment: '发布评论',
          rating: '评分',
          removeSelectedImage: '移除图片',
          retryComments: '重试评论',
          retryLoad: '重试',
          selectImage: '选择图片',
          setRating: '{rating} 星',
          shareExperience: '分享体验',
          submitting: '提交中',
          tibetAutonomousRegion: '西藏自治区',
          totalAmount: '合计',
          unlikeComment: '取消点赞',
          uploading: '上传中',
          winterFreeHint: '冬季免票'
        }
      }
    }
  })

const mountView = async (component: Component) => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(component)
  app.component('router-link', RouterLinkStub)
  app.use(createTestI18n())
  mountedApps.push(app)
  app.mount(root)
  await settleVue()

  return root
}

const settleVue = async (turns = 10) => {
  for (let index = 0; index < turns; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

const paginatedArray = <T,>(content: T[], page: number, size: number, totalElements: number, totalPages = Math.ceil(totalElements / size)) => ({
  data: content,
  headers: {
    'x-page': String(page),
    'x-size': String(size),
    'x-total-elements': String(totalElements),
    'x-total-pages': String(totalPages)
  }
})

const clickByText = (root: ParentNode, text: string) => {
  const button = Array.from(root.querySelectorAll('button')).find(candidate =>
    candidate.textContent?.includes(text)
  )

  expect(button).toBeTruthy()
  button?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
}

const pageParamCalls = (url: string) =>
  testState.apiGet.mock.calls
    .filter(call => call[0] === url)
    .map(call => call[1]?.params)

beforeEach(() => {
  testState.apiDelete.mockReset()
  testState.apiGet.mockReset()
  testState.apiPost.mockReset()
  testState.apiPut.mockReset()
  testState.ensureSession.mockReset().mockResolvedValue(true)
  testState.hasValidSession.mockReset().mockReturnValue(true)
  testState.refreshSession.mockReset().mockResolvedValue(undefined)
  testState.requireAuth.mockReset().mockResolvedValue(true)
  testState.routerBack.mockReset()
  testState.routerPush.mockReset()
  testState.routerReplace.mockReset()
  testState.showConfirm.mockReset().mockResolvedValue(false)
  testState.showToast.mockReset()
  testState.updateUser.mockReset()
  testState.authUser = { nickname: 'Traveler', role: 'USER' }
  testState.routeParams.id = '42'
  Object.keys(testState.routeQuery).forEach(key => delete testState.routeQuery[key])
})

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

describe('community detail pagination', () => {
  it('normalizes pagination from either headers or PageResponse bodies', () => {
    expect(readPaginatedResponse<{ id: number }>(paginatedArray([{ id: 1 }], 1, 20, 41))).toMatchObject({
      content: [{ id: 1 }],
      page: 1,
      size: 20,
      totalElements: 41,
      totalPages: 3
    })

    expect(readPaginatedResponse<{ id: number }>({
      data: {
        content: [{ id: 2 }],
        page: 2,
        size: 10,
        totalElements: 25,
        totalPages: 3
      }
    })).toMatchObject({
      content: [{ id: 2 }],
      page: 2,
      size: 10,
      totalElements: 25,
      totalPages: 3
    })
  })

  it('loads the next shared route comment page instead of hiding later comments', async () => {
    testState.routeParams.id = '42'
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared/42') {
        return Promise.resolve({
          data: {
            author: { nickname: 'Guide' },
            budget: 'moderate',
            commentCount: 2,
            content: 'route body',
            createdAt: '2026-06-08T00:00:00Z',
            days: 3,
            id: 42,
            likeCount: 0,
            preference: 'culture',
            title: 'Shared Route',
            viewCount: 1
          }
        })
      }
      if (url === '/routes/shared/42/comments') {
        const page = Number(config?.params?.page ?? 0)
        return Promise.resolve(page === 0
          ? paginatedArray([{ id: 1, content: 'First route comment', createdAt: '2026-06-08T00:00:00Z', user: { nickname: 'A' } }], 0, 20, 2, 2)
          : paginatedArray([{ id: 2, content: 'Second route comment', createdAt: '2026-06-08T00:01:00Z', user: { nickname: 'B' } }], 1, 20, 2, 2))
      }
      if (url === '/routes/shared/42/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteDetail)

    expect(root.textContent).toContain('First route comment')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/routes/shared/42/comments')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/routes/shared/42/comments')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First route comment')
    expect(root.textContent).toContain('Second route comment')
  })

  it('loads the next answer page for question detail', async () => {
    testState.routeParams.id = '99'
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/community/questions/99') {
        return Promise.resolve({
          data: {
            answerCount: 2,
            author: { nickname: 'Questioner', owner: false },
            content: 'Where should I go?',
            createdAt: '2026-06-08T00:00:00Z',
            id: 99,
            isResolved: false,
            likeCount: 0,
            tags: '',
            title: 'Travel question',
            viewCount: 8
          }
        })
      }
      if (url === '/community/questions/99/answers') {
        const page = Number(config?.params?.page ?? 0)
        return Promise.resolve(page === 0
          ? paginatedArray([{ id: 10, content: 'First answer', createdAt: '2026-06-08T00:00:00Z', isAccepted: false, likeCount: 0, user: { nickname: 'A' } }], 0, 20, 2, 2)
          : paginatedArray([{ id: 11, content: 'Second answer', createdAt: '2026-06-08T00:01:00Z', isAccepted: false, likeCount: 0, user: { nickname: 'B' } }], 1, 20, 2, 2))
      }
      if (url === '/community/questions/99/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(QuestionDetail)

    expect(root.textContent).toContain('First answer')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/community/questions/99/answers')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/community/questions/99/answers')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First answer')
    expect(root.textContent).toContain('Second answer')
  })

  it('loads the next my-routes page on the profile routes tab', async () => {
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/auth/me') {
        return Promise.resolve({ data: { createdAt: '2026-06-08T00:00:00Z', nickname: 'Traveler', role: 'USER' } })
      }
      if (url === '/auth/me/stats') {
        return Promise.resolve({ data: { bookingCount: 0, commentCount: 0, routeCount: 2 } })
      }
      if (url === '/routes/my-routes') {
        const page = Number(config?.params?.page ?? 0)
        return Promise.resolve(page === 0
          ? paginatedArray([{ id: 70, title: 'First profile route', days: 2, budget: 'economy', preference: 'nature', viewCount: 0, likeCount: 0, commentCount: 0, createdAt: '2026-06-08T00:00:00Z' }], 0, 20, 2, 2)
          : paginatedArray([{ id: 71, title: 'Second profile route', days: 4, budget: 'comfort', preference: 'culture', viewCount: 1, likeCount: 1, commentCount: 1, createdAt: '2026-06-08T00:01:00Z' }], 1, 20, 2, 2))
      }
      if (url === '/bookings/my' || url === '/hotel-bookings/my') {
        return Promise.resolve({ data: [] })
      }
      if (url === '/auth/me/comments') {
        return Promise.resolve({ data: { routeComments: [], spotComments: [] } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(UserProfile)

    expect(root.textContent).toContain('First profile route')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/routes/my-routes')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/routes/my-routes')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First profile route')
    expect(root.textContent).toContain('Second profile route')
  })

  it('hydrates scenic comment liked states concurrently and keeps comments visible when one status fails', async () => {
    testState.routeParams.id = '9'
    let inFlightLikedChecks = 0
    let maxInFlightLikedChecks = 0
    const likedStatusCalls: number[] = []

    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/spots/9') {
        return Promise.resolve({
          data: {
            category: 'NATURAL',
            description: 'Beautiful spot',
            id: 9,
            imageUrl: '',
            name: 'Scenic Spot',
            ticketPrice: 20
          }
        })
      }
      if (url === '/comments/spot/9') {
        return Promise.resolve({
          data: {
            content: [
              { id: 10, content: 'Liked comment', createdAt: '2026-06-08T00:00:00Z', likeCount: 1, rating: 5, user: { nickname: 'A' } },
              { id: 11, content: 'Status failure comment', createdAt: '2026-06-08T00:01:00Z', likeCount: 2, rating: 4, user: { nickname: 'B' } },
              { id: 12, content: 'Unliked comment', createdAt: '2026-06-08T00:02:00Z', likeCount: 3, rating: 3, user: { nickname: 'C' } }
            ],
            page: 0,
            size: 20,
            totalElements: 3,
            totalPages: 1
          }
        })
      }
      if (/^\/comments\/\d+\/liked$/.test(url)) {
        const id = Number(url.split('/')[2])
        likedStatusCalls.push(id)
        inFlightLikedChecks += 1
        maxInFlightLikedChecks = Math.max(maxInFlightLikedChecks, inFlightLikedChecks)

        return new Promise((resolve, reject) => {
          setTimeout(() => {
            inFlightLikedChecks -= 1
            if (id === 11) {
              reject(new Error('liked status failed'))
              return
            }
            resolve({ data: { liked: id === 10 } })
          }, 5)
        })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(ScenicSpotDetail)
    await wait(30)
    await settleVue()

    expect(likedStatusCalls.sort((a, b) => a - b)).toEqual([10, 11, 12])
    expect(maxInFlightLikedChecks).toBeGreaterThan(1)
    expect(root.textContent).toContain('Liked comment')
    expect(root.textContent).toContain('Status failure comment')
    expect(root.textContent).toContain('Unliked comment')
    expect(root.querySelector('[aria-pressed="true"]')).not.toBeNull()
  })
})
