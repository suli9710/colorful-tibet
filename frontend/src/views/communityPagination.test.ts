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
    questions: '/community/questions',
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
    shared: '/routes/shared',
    sharedDetail: (id: number) => `/routes/shared/${id}`,
    sharedComments: (id: number) => `/routes/shared/${id}/comments`,
    sharedLikeStatus: (id: number) => `/routes/shared/${id}/like-status`,
    sharedLike: (id: number) => `/routes/shared/${id}/like`,
    deleteSharedComment: (routeId: number, commentId: number) => `/routes/shared/${routeId}/comments/${commentId}`,
    myRoutes: '/routes/my-routes'
  },
  spots: {
    list: '/spots',
    search: '/spots/search',
    detail: (id: number) => `/spots/${id}`
  },
  news: {
    list: '/news'
  },
  heritage: {
    list: '/heritage',
    detail: (id: number) => `/heritage/${id}`,
    like: (id: number) => `/heritage/${id}/like`,
    likeStatus: (id: number) => `/heritage/${id}/like-status`,
    comments: (id: number) => `/heritage/${id}/comments`,
    deleteComment: (heritageId: number, commentId: number) => `/heritage/${heritageId}/comments/${commentId}`,
    inheritors: (id: number) => `/heritage/${id}/inheritors`,
    events: (id: number) => `/heritage/${id}/events`,
    upcomingEvents: '/heritage/events/upcoming'
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
    get isLoggedIn() {
      return Boolean(testState.authUser)
    },
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
    get isLoggedIn() {
      return Boolean(testState.authUser)
    },
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
    ArrowUpRight: Icon,
    BookOpen: Icon,
    CalendarDays: Icon,
    Eye: Icon,
    Heart: Icon,
    MessageCircle: Icon,
    Minus: Icon,
    Plus: Icon,
    RefreshCw: Icon,
    Search: Icon,
    SlidersHorizontal: Icon,
    UserRound: Icon,
    Video: Icon
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

vi.mock('../components/motion/MotionModal.vue', async () => {
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
  createTextCardPopupContent: (_title: string, body: string) => body,
  createTextPopupContent: (_title: string, body: string) => body
}))

import QuestionDetail from './QuestionDetail.vue'
import RouteCommunity from './RouteCommunity.vue'
import RouteDetail from './RouteDetail.vue'
import ScenicSpotDetail from './ScenicSpotDetail.vue'
import UserProfile from './UserProfile.vue'
import News from './News.vue'
import ScenicSpots from './ScenicSpots.vue'
import Heritage from './Heritage.vue'

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
          retry: '重试',
          search: '搜索',
          priceCny: '¥{price}',
          freeTicket: '免费'
        },
        spots: {
          cardAria: '{name}',
          category: {
            all: '全部',
            natural: '自然景观',
            cultural: '人文历史'
          },
          filterByCategory: '按{category}筛选',
          loadingLabel: '加载景点',
          paginationLabel: '景点分页',
          resultSummary: '{category} · {count} 个景点',
          searchPlaceholder: '搜索景点、地区或标签',
          subtitle: '探索西藏景点',
          ticketFrom: '起',
          title: '景点',
          viewDetails: '查看详情'
        },
        community: {
          accepted: '已采纳',
          acceptAnswer: '采纳',
          allBudget: '全部预算',
          allDays: '全部天数',
          allPreference: '全部偏好',
          allTags: '全部标签',
          askQuestion: '提问',
          answers: '回答',
          createMyRoute: '创建路线',
          days: '天',
          loadingQuestions: '问答列表加载中',
          loadingRoutes: '路线列表加载中',
          nextPage: '下一页',
          noAnswers: '暂无回答',
          noQuestions: '暂无问题',
          noRoutes: '暂无路线',
          officialRoute: '官方路线',
          postingQuestion: '发布中',
          postQuestion: '发布问题',
          previousPage: '上一页',
          publishFailedRetry: '发布失败，请重试',
          questionContent: '问题内容',
          questionContentPlaceholder: '请描述问题',
          questionTags: '问题标签',
          questionTagsHint: '最多选择 3 个标签',
          questionTitle: '问题标题',
          questionTitlePlaceholder: '请输入问题标题',
          questionsLoadFailed: '问答列表加载失败，请稍后重试。',
          retryQuestions: '重试问答',
          retryRoutes: '重试路线',
          routesLoadFailed: '路线列表加载失败，请稍后重试。',
          solved: '已解决',
          sharedRoutes: '路线分享',
          sortHot: '热门',
          sortLatest: '最新',
          sortUnanswered: '未回答',
          submittingAnswer: '提交中',
          submitAnswer: '提交回答',
          subtitle: '分享与问答',
          tagLabels: [],
          title: '社区',
          travelQA: '旅行问答',
          unsolved: '待解决',
          viewCount: '浏览',
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
          commentsLoadFailed: '评论加载失败，请稍后重试。',
          commentsLoading: '评论加载中',
          commentPlaceholder: '写评论',
          comments: '评论',
          commentSuccess: '评论成功',
          confirmDeleteComment: '删除评论?',
          days: '天',
          deleteCommentFailed: '删除失败',
          deleteCommentSuccess: '已删除',
          loginRequiredComment: '请登录后评论',
          loginRequiredLike: '请登录后点赞',
          loadingRoute: '路线详情加载中',
          loadFailed: '路线详情加载失败，请稍后重试。',
          noComments: '暂无评论',
          notFound: '未找到路线',
          operationFailed: '操作失败',
          postComment: '发表评论',
          publishedAt: '发布于',
          submitting: '提交中'
        },
        heritage: {
          anonymous: '匿名用户',
          commentPlaceholder: '分享你对这个非遗项目的感受...',
          commentsTitle: '用户评论',
          deleteComment: '删除',
          interaction: {
            like: '点赞',
            liked: '已点赞'
          },
          loginToComment: '登录后可以评论',
          noComments: '暂无评论，快来发表第一条吧',
          submitComment: '发表'
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

const paginatedArray = <T,>(content: T[], page: number, size: number, totalElements: number, totalPages = Math.ceil(totalElements / size)) => ({
  data: content,
  headers: {
    'x-page': String(page),
    'x-size': String(size),
    'x-total-elements': String(totalElements),
    'x-total-pages': String(totalPages)
  }
})

const createDeferred = <T = unknown>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}

const clickByText = (root: ParentNode, text: string) => {
  const button = Array.from(root.querySelectorAll('button')).find(candidate =>
    candidate.textContent?.includes(text)
  )

  expect(button).toBeTruthy()
  button?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
}

const setInputValue = async (root: ParentNode, selector: string, value: string) => {
  const input = root.querySelector<HTMLInputElement>(selector)

  expect(input).toBeTruthy()
  input!.value = value
  input!.dispatchEvent(new Event('input', { bubbles: true }))
  await settleVue()
}

const setSelectValue = async (select: HTMLSelectElement | null, value: string, turns = 10) => {
  expect(select).toBeTruthy()
  select!.value = value
  select!.dispatchEvent(new Event('change', { bubbles: true }))
  await settleVue(turns)
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

  it('shows a retryable shared route list failure instead of the empty state', async () => {
    let routeAttempts = 0

    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/routes/shared') {
        routeAttempts += 1
        if (routeAttempts === 1) {
          return Promise.reject(new Error('routes unavailable'))
        }

        return Promise.resolve({
          data: {
            content: [
              {
                budget: '经济型',
                commentCount: 0,
                createdAt: '2026-06-08T00:00:00Z',
                days: 3,
                id: 1,
                likeCount: 0,
                preference: '自然风光',
                title: 'Recovered route',
                viewCount: 1
              }
            ],
            totalPages: 1
          }
        })
      }
      if (url === '/community/questions') {
        return Promise.resolve({ data: { content: [], totalPages: 0 } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteCommunity)

    expect(root.textContent).toContain('路线列表加载失败，请稍后重试。')
    expect(root.textContent).not.toContain('暂无路线')

    clickByText(root, '重试')
    await settleVue()

    expect(root.textContent).toContain('Recovered route')
    expect(root.textContent).not.toContain('路线列表加载失败，请稍后重试。')
  })

  it('shows a retryable Q&A list failure instead of the empty state', async () => {
    let questionAttempts = 0

    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/routes/shared') {
        return Promise.resolve({ data: { content: [], totalPages: 0 } })
      }
      if (url === '/community/questions') {
        questionAttempts += 1
        if (questionAttempts === 1) {
          return Promise.reject(new Error('questions unavailable'))
        }

        return Promise.resolve({
          data: {
            content: [
              {
                answerCount: 1,
                content: 'Need local advice',
                createdAt: '2026-06-08T00:00:00Z',
                id: 2,
                isResolved: false,
                likeCount: 0,
                tags: '',
                title: 'Recovered question',
                viewCount: 3
              }
            ],
            totalPages: 1
          }
        })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteCommunity)

    expect(root.textContent).toContain('问答列表加载失败，请稍后重试。')
    expect(root.textContent).not.toContain('暂无问题')

    clickByText(root, '重试')
    await settleVue()

    expect(root.textContent).toContain('Recovered question')
    expect(root.textContent).not.toContain('问答列表加载失败，请稍后重试。')
  })

  it('keeps only the latest shared route request state after out-of-order responses', async () => {
    const initialRoutes = createDeferred()
    const filteredRoutes = createDeferred()
    const staleRouteFailure = createDeferred()
    const newestRoutes = createDeferred()

    const routeResponse = (title: string) => ({
      data: {
        content: [
          {
            budget: 'budget',
            commentCount: 0,
            createdAt: '2026-06-08T00:00:00Z',
            days: 3,
            id: title.length,
            likeCount: 0,
            preference: 'preference',
            title,
            viewCount: 1
          }
        ],
        totalPages: 1
      }
    })

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared') {
        const days = Number(config?.params?.days ?? 0)
        if (days === 3) return filteredRoutes.promise
        if (days === 5) return staleRouteFailure.promise
        if (days === 7) return newestRoutes.promise
        return initialRoutes.promise
      }
      if (url === '/community/questions') {
        return Promise.resolve({ data: { content: [], totalPages: 0 } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteCommunity)
    const routeDaysSelect = root.querySelector<HTMLSelectElement>('#community-panel-routes select')

    await setSelectValue(routeDaysSelect, '3', 2)
    initialRoutes.resolve(routeResponse('Stale route'))
    await settleVue()

    expect(root.textContent).not.toContain('Stale route')
    expect(root.textContent).toContain('路线列表加载中')
    expect(root.textContent).not.toContain('路线列表加载失败，请稍后重试。')

    filteredRoutes.resolve(routeResponse('Fresh route'))
    await settleVue()

    expect(root.textContent).toContain('Fresh route')
    expect(root.textContent).not.toContain('Stale route')
    expect(root.textContent).not.toContain('路线列表加载中')

    await setSelectValue(routeDaysSelect, '5', 2)
    await setSelectValue(routeDaysSelect, '7', 2)
    staleRouteFailure.reject(new Error('stale route failure'))
    await settleVue()

    expect(root.textContent).not.toContain('路线列表加载失败，请稍后重试。')
    expect(root.textContent).toContain('路线列表加载中')

    newestRoutes.resolve(routeResponse('Newest route'))
    await settleVue()

    expect(root.textContent).toContain('Newest route')
    expect(root.textContent).not.toContain('Fresh route')
    expect(root.textContent).not.toContain('路线列表加载中')
  })

  it('resets shared route filters to the first page after browsing later pages', async () => {
    const routeResponse = (title: string, totalPages = 3) => ({
      data: {
        content: [
          {
            budget: '经济型',
            commentCount: 0,
            createdAt: '2026-06-08T00:00:00Z',
            days: 3,
            id: title.length,
            likeCount: 0,
            preference: '自然风光',
            title,
            viewCount: 1
          }
        ],
        totalPages
      }
    })

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared') {
        const days = Number(config?.params?.days ?? 0)
        const page = Number(config?.params?.page ?? 0)

        if (days === 5) {
          return Promise.resolve(routeResponse(page === 0 ? 'Filtered first page route' : 'Wrong filtered page route', 1))
        }

        return Promise.resolve(routeResponse(page === 1 ? 'Second page route' : 'First page route', 3))
      }
      if (url === '/community/questions') {
        return Promise.resolve({ data: { content: [], totalPages: 0 } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteCommunity)
    const routePanel = root.querySelector<HTMLElement>('#community-panel-routes')
    const routeNextButton = Array.from(routePanel?.querySelectorAll<HTMLButtonElement>('button') ?? [])
      .find(button => button.textContent?.includes('下一页'))

    expect(root.textContent).toContain('First page route')
    expect(routeNextButton).toBeTruthy()

    routeNextButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue()

    expect(root.textContent).toContain('Second page route')
    expect(root.textContent).toContain('2 / 3')

    await setSelectValue(routePanel?.querySelector<HTMLSelectElement>('select') ?? null, '5')

    const filteredCalls = pageParamCalls('/routes/shared').filter(params => params?.days === 5)
    expect(filteredCalls).toContainEqual({ days: 5, page: 0, size: 9, sortField: 'createdAt' })
    expect(filteredCalls.some(params => params?.page === 1)).toBe(false)
    expect(root.textContent).toContain('Filtered first page route')
    expect(root.textContent).not.toContain('Wrong filtered page route')
    expect(root.textContent).not.toContain('Second page route')
  })

  it('keeps only the latest Q&A request state after out-of-order responses', async () => {
    const initialQuestions = createDeferred()
    const sortedQuestions = createDeferred()
    const staleQuestionFailure = createDeferred()
    const newestQuestions = createDeferred()
    let latestQuestionRequests = 0

    const questionResponse = (title: string) => ({
      data: {
        content: [
          {
            answerCount: 0,
            content: title,
            createdAt: '2026-06-08T00:00:00Z',
            id: title.length,
            isResolved: false,
            likeCount: 0,
            tags: '',
            title,
            viewCount: 1
          }
        ],
        totalPages: 1
      }
    })

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared') {
        return Promise.resolve({ data: { content: [], totalPages: 0 } })
      }
      if (url === '/community/questions') {
        const sort = String(config?.params?.sort ?? 'latest')
        if (sort === 'hot') return sortedQuestions.promise
        if (sort === 'unsolved') return staleQuestionFailure.promise
        latestQuestionRequests += 1
        return latestQuestionRequests === 1 ? initialQuestions.promise : newestQuestions.promise
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteCommunity)
    const qaSortSelect = root.querySelector<HTMLSelectElement>('#community-panel-qa select')

    await setSelectValue(qaSortSelect, 'hot', 2)
    initialQuestions.resolve(questionResponse('Stale question'))
    await settleVue()

    expect(root.textContent).not.toContain('Stale question')
    expect(root.textContent).toContain('问答列表加载中')
    expect(root.textContent).not.toContain('问答列表加载失败，请稍后重试。')

    sortedQuestions.resolve(questionResponse('Fresh question'))
    await settleVue()

    expect(root.textContent).toContain('Fresh question')
    expect(root.textContent).not.toContain('Stale question')
    expect(root.textContent).not.toContain('问答列表加载中')

    await setSelectValue(qaSortSelect, 'unsolved', 2)
    await setSelectValue(qaSortSelect, 'latest', 2)
    staleQuestionFailure.reject(new Error('stale question failure'))
    await settleVue()

    expect(root.textContent).not.toContain('问答列表加载失败，请稍后重试。')
    expect(root.textContent).toContain('问答列表加载中')

    newestQuestions.resolve(questionResponse('Newest question'))
    await settleVue()

    expect(root.textContent).toContain('Newest question')
    expect(root.textContent).not.toContain('Fresh question')
    expect(root.textContent).not.toContain('问答列表加载中')
  })

  it('resets Q&A sort changes to the first page after browsing later pages', async () => {
    const questionResponse = (title: string, totalPages = 3) => ({
      data: {
        content: [
          {
            answerCount: 0,
            content: title,
            createdAt: '2026-06-08T00:00:00Z',
            id: title.length,
            isResolved: false,
            likeCount: 0,
            tags: '',
            title,
            viewCount: 1
          }
        ],
        totalPages
      }
    })

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared') {
        return Promise.resolve({ data: { content: [], totalPages: 0 } })
      }
      if (url === '/community/questions') {
        const page = Number(config?.params?.page ?? 0)
        const sort = String(config?.params?.sort ?? 'latest')

        if (sort === 'hot') {
          return Promise.resolve(questionResponse(page === 0 ? 'Hot first page question' : 'Wrong hot page question', 1))
        }

        return Promise.resolve(questionResponse(page === 1 ? 'Second page question' : 'First page question', 3))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteCommunity)
    clickByText(root, '旅行问答')
    await settleVue()

    const qaPanel = root.querySelector<HTMLElement>('#community-panel-qa')
    const qaNextButton = Array.from(qaPanel?.querySelectorAll<HTMLButtonElement>('button') ?? [])
      .find(button => button.textContent?.includes('下一页'))

    expect(root.textContent).toContain('First page question')
    expect(qaNextButton).toBeTruthy()

    qaNextButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue()

    expect(root.textContent).toContain('Second page question')
    expect(root.textContent).toContain('2 / 3')

    await setSelectValue(qaPanel?.querySelector<HTMLSelectElement>('select') ?? null, 'hot')

    const sortedCalls = pageParamCalls('/community/questions').filter(params => params?.sort === 'hot')
    expect(sortedCalls).toContainEqual({ page: 0, size: 10, sort: 'hot' })
    expect(sortedCalls.some(params => params?.page === 1)).toBe(false)
    expect(root.textContent).toContain('Hot first page question')
    expect(root.textContent).not.toContain('Wrong hot page question')
    expect(root.textContent).not.toContain('Second page question')
  })

  it('loads additional news pages and resets search to the first page', async () => {
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/news') {
        const page = Number(config?.params?.page ?? 0)
        return Promise.resolve(page === 0
          ? {
              data: {
                content: [
                  { id: 1, title: 'First news item', content: 'Policy update', category: 'POLICY', createdAt: '2026-06-08T00:00:00Z', viewCount: 1 }
                ],
                page: 0,
                size: 20,
                totalElements: 2,
                totalPages: 2
              }
            }
          : paginatedArray([
              { id: 2, title: 'Second news item', content: 'Festival notice', category: 'NOTICE', createdAt: '2026-06-08T00:01:00Z', viewCount: 2 }
            ], 1, 20, 2, 2))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(News)

    expect(root.textContent).toContain('First news item')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/news')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/news')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First news item')
    expect(root.textContent).toContain('Second news item')

    await setInputValue(root, '#news-search', 'First')

    expect(pageParamCalls('/news').filter(params => params?.page === 0 && params?.size === 20)).toHaveLength(2)
    expect(pageParamCalls('/news')).toContainEqual({ page: 0, size: 20, keyword: 'first' })
  })

  it('clears stale news load-more state after search starts a newer request', async () => {
    let resolveStalePage: ((value: unknown) => void) | undefined
    const firstPage = {
      data: {
        content: [
          { id: 1, title: 'First news item', content: 'Policy update', category: 'POLICY', createdAt: '2026-06-08T00:00:00Z', viewCount: 1 }
        ],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 2
      }
    }

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/news') {
        const page = Number(config?.params?.page ?? 0)
        if (page === 1) {
          return new Promise(resolve => {
            resolveStalePage = resolve
          })
        }
        return Promise.resolve(firstPage)
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(News)
    clickByText(root, '下一页')
    await settleVue(2)
    await setInputValue(root, '#news-search', 'First')

    resolveStalePage?.(paginatedArray([
      { id: 2, title: 'Stale news item', content: 'Late page', category: 'NOTICE', createdAt: '2026-06-08T00:01:00Z', viewCount: 2 }
    ], 1, 20, 2, 2))
    await settleVue()

    const nextButtons = Array.from(root.querySelectorAll('button')).filter(button =>
      button.textContent?.includes('下一页')
    )
    expect(nextButtons.length).toBeGreaterThan(0)
    expect(nextButtons.every(button => !button.disabled)).toBe(true)
  })

  it('loads additional scenic spot pages and resets search to the first page', async () => {
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/spots') {
        const page = Number(config?.params?.page ?? 0)
        return Promise.resolve(page === 0
          ? paginatedArray([
              { id: 10, name: 'First scenic spot', description: 'Mountain trail', category: 'NATURAL', createdAt: '2026-06-08T00:00:00Z', ticketPrice: 10 }
            ], 0, 20, 2, 2)
          : paginatedArray([
              { id: 11, name: 'Second scenic spot', description: 'Temple route', category: 'CULTURAL', createdAt: '2026-06-08T00:01:00Z', ticketPrice: 20 }
            ], 1, 20, 2, 2))
      }
      if (url === '/spots/search') {
        return Promise.resolve({
          data: {
            content: [
              { id: 12, name: 'Lake search result', description: 'Blue lake', category: 'NATURAL', createdAt: '2026-06-08T00:02:00Z', ticketPrice: 0 }
            ],
            page: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1
          }
        })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(ScenicSpots)

    expect(root.textContent).toContain('First scenic spot')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/spots')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/spots')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First scenic spot')
    expect(root.textContent).toContain('Second scenic spot')

    clickByText(root, '自然景观')
    await settleVue()
    await setInputValue(root, '#spot-search', 'lake')

    expect(pageParamCalls('/spots/search')).toContainEqual({ page: 0, size: 20, keyword: 'lake', category: 'NATURAL' })
    expect(root.textContent).toContain('Lake search result')
  })

  it('clears stale scenic spot load-more state after search starts a newer request', async () => {
    let resolveStalePage: ((value: unknown) => void) | undefined

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/spots') {
        const page = Number(config?.params?.page ?? 0)
        if (page === 1) {
          return new Promise(resolve => {
            resolveStalePage = resolve
          })
        }
        return Promise.resolve(paginatedArray([
          { id: 10, name: 'First scenic spot', description: 'Mountain trail', category: 'NATURAL', createdAt: '2026-06-08T00:00:00Z', ticketPrice: 10 }
        ], 0, 20, 2, 2))
      }
      if (url === '/spots/search') {
        return Promise.resolve(paginatedArray([
          { id: 12, name: 'Lake search result', description: 'Blue lake', category: 'NATURAL', createdAt: '2026-06-08T00:02:00Z', ticketPrice: 0 }
        ], 0, 20, 2, 2))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(ScenicSpots)
    clickByText(root, '下一页')
    await settleVue(2)
    await setInputValue(root, '#spot-search', 'lake')

    resolveStalePage?.(paginatedArray([
      { id: 11, name: 'Stale scenic spot', description: 'Late page', category: 'CULTURAL', createdAt: '2026-06-08T00:01:00Z', ticketPrice: 20 }
    ], 1, 20, 2, 2))
    await settleVue()

    const nextButtons = Array.from(root.querySelectorAll('button')).filter(button =>
      button.textContent?.includes('下一页')
    )
    expect(nextButtons.length).toBeGreaterThan(0)
    expect(nextButtons.every(button => !button.disabled)).toBe(true)
  })

  it('loads the next heritage detail comment page instead of hiding later comments', async () => {
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/heritage') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, name: 'Thangka craft', category: '传统技艺', description: 'Painted heritage', commentCount: 2, likeCount: 0, viewCount: 1 }
            ],
            page: 0,
            size: 100,
            totalElements: 1,
            totalPages: 1
          }
        })
      }
      if (url === '/heritage/events/upcoming') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1') {
        return Promise.resolve({ data: { id: 1, name: 'Thangka craft', category: '传统技艺', description: 'Painted heritage', commentCount: 2 } })
      }
      if (url === '/heritage/1/comments') {
        const page = Number(config?.params?.page ?? 0)
        return Promise.resolve(page === 0
          ? paginatedArray([{ id: 101, content: 'First heritage comment', createdAt: '2026-06-08T00:00:00Z', nickname: 'A', rating: 5 }], 0, 20, 2, 2)
          : paginatedArray([{ id: 102, content: 'Second heritage comment', createdAt: '2026-06-08T00:01:00Z', nickname: 'B', rating: 4 }], 1, 20, 2, 2))
      }
      if (url === '/heritage/1/inheritors' || url === '/heritage/1/events') {
        return Promise.resolve({ data: { content: [] } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(Heritage)
    clickByText(root, 'Thangka craft')
    await settleVue()

    expect(root.textContent).toContain('First heritage comment')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/heritage/1/comments')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/heritage/1/comments')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First heritage comment')
    expect(root.textContent).toContain('Second heritage comment')
  })

  it('ignores stale heritage like completions after switching selected items', async () => {
    const staleLike = createDeferred<{ data: { liked: boolean; likeCount: number } }>()
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/heritage') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, name: 'Slow heritage', category: '传统技艺', description: 'First detail', commentCount: 0, likeCount: 4, viewCount: 1 },
              { id: 2, name: 'Current heritage', category: '传统技艺', description: 'Second detail', commentCount: 0, likeCount: 7, viewCount: 1 }
            ],
            page: 0,
            size: 100,
            totalElements: 2,
            totalPages: 1
          }
        })
      }
      if (url === '/heritage/events/upcoming') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1') {
        return Promise.resolve({ data: { id: 1, name: 'Slow heritage', category: '传统技艺', description: 'First detail', commentCount: 0, likeCount: 4 } })
      }
      if (url === '/heritage/2') {
        return Promise.resolve({ data: { id: 2, name: 'Current heritage', category: '传统技艺', description: 'Second detail', commentCount: 0, likeCount: 7 } })
      }
      if (url === '/heritage/1/comments' || url === '/heritage/2/comments') {
        return Promise.resolve(paginatedArray([], Number(config?.params?.page ?? 0), 20, 0, 0))
      }
      if (url === '/heritage/1/inheritors' || url === '/heritage/1/events' || url === '/heritage/2/inheritors' || url === '/heritage/2/events') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1/like-status' || url === '/heritage/2/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    testState.apiPost.mockImplementation((url: string) => {
      if (url === '/heritage/1/like') return staleLike.promise

      return Promise.reject(new Error(`Unexpected POST ${url}`))
    })

    const root = await mountView(Heritage)
    clickByText(root, 'Slow heritage')
    await settleVue()

    clickByText(root, '点赞')
    await settleVue(2)
    clickByText(root, 'Current heritage')
    await settleVue()

    expect(root.textContent).toContain('Current heritage')
    expect(root.textContent).toContain('7')

    staleLike.resolve({ data: { liked: true, likeCount: 99 } })
    await settleVue()

    expect(testState.apiPost).toHaveBeenCalledWith('/heritage/1/like')
    expect(root.textContent).toContain('Current heritage')
    expect(root.textContent).toContain('7')
    expect(root.textContent).not.toContain('99')
  })

  it('ignores older same-item heritage like completions after a newer toggle finishes', async () => {
    const firstLike = createDeferred<{ data: { liked: boolean; likeCount: number } }>()
    const secondLike = createDeferred<{ data: { liked: boolean; likeCount: number } }>()
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/heritage') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, name: 'Thangka craft', category: '传统技艺', description: 'Painted heritage', commentCount: 0, likeCount: 4, viewCount: 1 }
            ],
            page: 0,
            size: 100,
            totalElements: 1,
            totalPages: 1
          }
        })
      }
      if (url === '/heritage/events/upcoming') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1') {
        return Promise.resolve({ data: { id: 1, name: 'Thangka craft', category: '传统技艺', description: 'Painted heritage', commentCount: 0, likeCount: 4 } })
      }
      if (url === '/heritage/1/comments') {
        return Promise.resolve(paginatedArray([], Number(config?.params?.page ?? 0), 20, 0, 0))
      }
      if (url === '/heritage/1/inheritors' || url === '/heritage/1/events') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    testState.apiPost
      .mockReturnValueOnce(firstLike.promise)
      .mockReturnValueOnce(secondLike.promise)

    const root = await mountView(Heritage)
    clickByText(root, 'Thangka craft')
    await settleVue()

    clickByText(root, '点赞')
    await settleVue(2)
    clickByText(root, '点赞')
    await settleVue(2)

    secondLike.resolve({ data: { liked: false, likeCount: 4 } })
    await settleVue()
    firstLike.resolve({ data: { liked: true, likeCount: 5 } })
    await settleVue()

    expect(testState.apiPost).toHaveBeenCalledTimes(2)
    expect(root.textContent).toContain('点赞')
    expect(root.textContent).not.toContain('已点赞')
  })

  it('prevents duplicate heritage comment submissions while one is in flight', async () => {
    const pendingComment = createDeferred<{ data: { id: number; content: string; createdAt: string; nickname: string; rating: number } }>()
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/heritage') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, name: 'Commentable heritage', category: '传统技艺', description: 'Detail', commentCount: 0, likeCount: 0, viewCount: 1 }
            ],
            page: 0,
            size: 100,
            totalElements: 1,
            totalPages: 1
          }
        })
      }
      if (url === '/heritage/events/upcoming') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1') {
        return Promise.resolve({ data: { id: 1, name: 'Commentable heritage', category: '传统技艺', description: 'Detail', commentCount: 0 } })
      }
      if (url === '/heritage/1/comments') {
        return Promise.resolve(paginatedArray([], Number(config?.params?.page ?? 0), 20, 0, 0))
      }
      if (url === '/heritage/1/inheritors' || url === '/heritage/1/events') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    testState.apiPost.mockImplementation((url: string) => {
      if (url === '/heritage/1/comments') return pendingComment.promise

      return Promise.reject(new Error(`Unexpected POST ${url}`))
    })

    const root = await mountView(Heritage)
    clickByText(root, 'Commentable heritage')
    await settleVue()
    await setInputValue(root, 'input[maxlength="1000"]', 'A careful comment')

    const submitButton = Array.from(root.querySelectorAll('button')).find(button =>
      button.textContent?.includes('发表')
    )
    expect(submitButton).toBeTruthy()
    submitButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    submitButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue(2)

    expect(testState.apiPost).toHaveBeenCalledTimes(1)
    expect(testState.apiPost).toHaveBeenCalledWith('/heritage/1/comments', {
      content: 'A careful comment',
      rating: 5
    })

    pendingComment.resolve({
      data: {
        id: 201,
        content: 'A careful comment',
        createdAt: '2026-06-08T00:02:00Z',
        nickname: 'Traveler',
        rating: 5
      }
    })
    await settleVue()

    expect(root.textContent).toContain('A careful comment')
  })

  it('prevents duplicate heritage comment deletes while one is in flight', async () => {
    const pendingDelete = createDeferred()
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/heritage') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, name: 'Owned-comment heritage', category: '传统技艺', description: 'Detail', commentCount: 1, likeCount: 0, viewCount: 1 }
            ],
            page: 0,
            size: 100,
            totalElements: 1,
            totalPages: 1
          }
        })
      }
      if (url === '/heritage/events/upcoming') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1') {
        return Promise.resolve({ data: { id: 1, name: 'Owned-comment heritage', category: '传统技艺', description: 'Detail', commentCount: 1 } })
      }
      if (url === '/heritage/1/comments') {
        return Promise.resolve(paginatedArray([
          { id: 301, content: 'Owned heritage comment', createdAt: '2026-06-08T00:00:00Z', nickname: 'Traveler', owner: true, rating: 5 }
        ], Number(config?.params?.page ?? 0), 20, 1, 1))
      }
      if (url === '/heritage/1/inheritors' || url === '/heritage/1/events') {
        return Promise.resolve({ data: { content: [] } })
      }
      if (url === '/heritage/1/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    testState.apiDelete.mockImplementation((url: string) => {
      if (url === '/heritage/1/comments/301') return pendingDelete.promise

      return Promise.reject(new Error(`Unexpected DELETE ${url}`))
    })

    const root = await mountView(Heritage)
    clickByText(root, 'Owned-comment heritage')
    await settleVue()

    expect(root.textContent).toContain('Owned heritage comment')
    clickByText(root, '删除')
    clickByText(root, '删除')
    await settleVue(2)

    expect(testState.apiDelete).toHaveBeenCalledTimes(1)
    expect(testState.apiDelete).toHaveBeenCalledWith('/heritage/1/comments/301')

    pendingDelete.resolve({})
    await settleVue()

    expect(root.textContent).not.toContain('Owned heritage comment')
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

    expect(testState.apiGet).toHaveBeenCalledWith('/routes/shared/42/like-status', { skipAuthRedirect: true })
    expect(root.textContent).toContain('First route comment')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/routes/shared/42/comments')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/routes/shared/42/comments')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First route comment')
    expect(root.textContent).toContain('Second route comment')
  })

  it('uses backend authoritative shared route like counts after toggling like', async () => {
    testState.routeParams.id = '42'
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared/42') {
        return Promise.resolve({
          data: {
            author: { nickname: 'Guide' },
            budget: 'moderate',
            commentCount: 0,
            content: 'route body',
            createdAt: '2026-06-08T00:00:00Z',
            days: 3,
            id: 42,
            likeCount: 4,
            preference: 'culture',
            title: 'Shared Route',
            viewCount: 1
          }
        })
      }
      if (url === '/routes/shared/42/comments') {
        return Promise.resolve(paginatedArray([], Number(config?.params?.page ?? 0), 20, 0, 0))
      }
      if (url === '/routes/shared/42/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    testState.apiPost.mockResolvedValueOnce({ data: { liked: true, likeCount: 99 } })

    const root = await mountView(RouteDetail)
    const likeButton = Array.from(root.querySelectorAll('button')).find(button =>
      button.textContent?.includes('4')
    )

    expect(likeButton).toBeTruthy()
    likeButton!.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue()

    expect(testState.apiPost).toHaveBeenCalledWith('/routes/shared/42/like')
    expect(root.textContent).toContain('99')
  })

  it('shows route detail load failures separately from true not-found responses', async () => {
    testState.routeParams.id = '42'
    let detailAttempts = 0

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared/42') {
        detailAttempts += 1
        if (detailAttempts === 1) {
          return Promise.reject(Object.assign(new Error('server unavailable'), { response: { status: 500 } }))
        }

        return Promise.resolve({
          data: {
            author: { nickname: 'Guide' },
            budget: 'moderate',
            commentCount: 0,
            content: 'route body',
            createdAt: '2026-06-08T00:00:00Z',
            days: 3,
            id: 42,
            likeCount: 0,
            preference: 'culture',
            title: 'Recovered detail route',
            viewCount: 1
          }
        })
      }
      if (url === '/routes/shared/42/comments') {
        return Promise.resolve(paginatedArray([], Number(config?.params?.page ?? 0), 20, 0, 0))
      }
      if (url === '/routes/shared/42/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteDetail)

    expect(root.textContent).toContain('路线详情加载失败，请稍后重试。')
    expect(root.textContent).not.toContain('未找到路线')

    clickByText(root, '重试')
    await settleVue()

    expect(root.textContent).toContain('Recovered detail route')
    expect(root.textContent).not.toContain('路线详情加载失败，请稍后重试。')
  })

  it('renders true shared route 404 responses as not found', async () => {
    testState.routeParams.id = '42'
    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/routes/shared/42') {
        return Promise.reject(Object.assign(new Error('not found'), { response: { status: 404 } }))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteDetail)

    expect(root.textContent).toContain('未找到路线')
    expect(root.textContent).not.toContain('路线详情加载失败，请稍后重试。')
  })

  it('keeps a loaded route detail visible when the initial comments request fails', async () => {
    testState.routeParams.id = '42'
    let commentsAttempts = 0

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared/42') {
        return Promise.resolve({
          data: {
            author: { nickname: 'Guide' },
            budget: 'moderate',
            commentCount: 1,
            content: 'route body',
            createdAt: '2026-06-08T00:00:00Z',
            days: 3,
            id: 42,
            likeCount: 0,
            preference: 'culture',
            title: 'Route stays visible',
            viewCount: 1
          }
        })
      }
      if (url === '/routes/shared/42/comments') {
        commentsAttempts += 1
        if (commentsAttempts === 1) {
          return Promise.reject(new Error('comments unavailable'))
        }

        return Promise.resolve(paginatedArray([
          { id: 1, content: 'Recovered comment', createdAt: '2026-06-08T00:00:00Z', user: { nickname: 'A' } }
        ], Number(config?.params?.page ?? 0), 20, 1, 1))
      }
      if (url === '/routes/shared/42/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteDetail)

    expect(root.textContent).toContain('Route stays visible')
    expect(root.textContent).toContain('评论加载失败，请稍后重试。')
    expect(root.textContent).not.toContain('未找到路线')

    clickByText(root, '重试')
    await settleVue()

    expect(root.textContent).toContain('Route stays visible')
    expect(root.textContent).toContain('Recovered comment')
    expect(root.textContent).not.toContain('评论加载失败，请稍后重试。')
  })

  it('does not request shared route like status for anonymous readers', async () => {
    testState.authUser = null
    testState.routeParams.id = '42'
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared/42') {
        return Promise.resolve({
          data: {
            author: { nickname: 'Guide' },
            budget: 'moderate',
            commentCount: 1,
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
        return Promise.resolve(paginatedArray([
          { id: 1, content: 'Public route comment', createdAt: '2026-06-08T00:00:00Z', user: { nickname: 'A' } }
        ], Number(config?.params?.page ?? 0), 20, 1, 1))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteDetail)

    expect(root.textContent).toContain('Public route comment')
    expect(testState.apiGet.mock.calls.map(call => call[0])).not.toContain('/routes/shared/42/like-status')
  })

  it('uses the route comment PublicUser owner contract for deleting own comments', async () => {
    testState.routeParams.id = '42'
    testState.showConfirm.mockResolvedValueOnce(true)
    testState.apiDelete.mockResolvedValueOnce({ data: {} })
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/routes/shared/42') {
        return Promise.resolve({
          data: {
            author: { nickname: 'Guide', owner: false },
            budget: 'moderate',
            commentCount: 1,
            content: 'route body',
            createdAt: '2026-06-08T00:00:00Z',
            days: 3,
            id: 42,
            likeCount: 0,
            preference: 'culture',
            sourceType: 'USER',
            title: 'Shared Route',
            viewCount: 1
          }
        })
      }
      if (url === '/routes/shared/42/comments') {
        return Promise.resolve(paginatedArray([
          {
            content: 'Owned route comment',
            createdAt: '2026-06-08T00:00:00Z',
            id: 7,
            route: { id: 42, title: 'Shared Route' },
            user: { avatar: null, nickname: 'Traveler', owner: true }
          }
        ], Number(config?.params?.page ?? 0), 20, 1, 1))
      }
      if (url === '/routes/shared/42/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(RouteDetail)

    expect(root.textContent).toContain('Owned route comment')
    const deleteButton = root.querySelector<HTMLButtonElement>('button.text-red-500')
    expect(deleteButton).toBeTruthy()
    deleteButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue()

    expect(testState.showConfirm).toHaveBeenCalled()
    expect(testState.apiDelete).toHaveBeenCalledWith('/routes/shared/42/comments/7')
    expect(root.textContent).not.toContain('Owned route comment')
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

  it('ignores stale question like completions after the route question id changes', async () => {
    const staleLike = createDeferred<{ data: { liked: boolean; likeCount: number } }>()
    testState.routeParams.id = '99'
    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/community/questions/99') {
        return Promise.resolve({
          data: {
            answerCount: 0,
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
        return Promise.resolve(paginatedArray([], Number(config?.params?.page ?? 0), 20, 0, 0))
      }
      if (url === '/community/questions/99/like-status') {
        return Promise.resolve({ data: { liked: false } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    testState.apiPost.mockImplementation((url: string) => {
      if (url === '/community/questions/99/like') return staleLike.promise

      return Promise.reject(new Error(`Unexpected POST ${url}`))
    })

    const root = await mountView(QuestionDetail)
    const likeButton = root.querySelector<HTMLButtonElement>('button[aria-pressed="false"]')
    expect(likeButton).toBeTruthy()

    likeButton!.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue(2)
    testState.routeParams.id = '100'
    staleLike.resolve({ data: { liked: true, likeCount: 99 } })
    await settleVue()

    expect(testState.apiPost).toHaveBeenCalledWith('/community/questions/99/like')
    expect(root.textContent).toContain('Travel question')
    expect(root.textContent).not.toContain('99')
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

  it('loads the next profile scenic booking page and updates metadata after deletion', async () => {
    let scenicBookingDeleted = false

    testState.apiGet.mockImplementation((url: string, config?: any) => {
      if (url === '/auth/me') {
        return Promise.resolve({ data: { createdAt: '2026-06-08T00:00:00Z', nickname: 'Traveler', role: 'USER' } })
      }
      if (url === '/auth/me/stats') {
        return Promise.resolve({ data: { bookingCount: 2, commentCount: 0, routeCount: 0 } })
      }
      if (url === '/routes/my-routes') {
        return Promise.resolve(paginatedArray([], 0, 20, 0, 0))
      }
      if (url === '/bookings/my') {
        const page = Number(config?.params?.page ?? 0)
        const content = scenicBookingDeleted
          ? [{ id: 80, spot: { name: 'First scenic booking', imageUrl: '' }, visitDate: '2026-06-10', ticketCount: 1, totalPrice: 100, status: 'CONFIRMED' }]
          : page === 0
            ? [{ id: 80, spot: { name: 'First scenic booking', imageUrl: '' }, visitDate: '2026-06-10', ticketCount: 1, totalPrice: 100, status: 'CONFIRMED' }]
            : [{ id: 81, spot: { name: 'Second scenic booking', imageUrl: '' }, visitDate: '2026-06-11', ticketCount: 2, totalPrice: 200, status: 'CANCELLED' }]

        return Promise.resolve({
          data: {
            content,
            page: scenicBookingDeleted ? 0 : page,
            size: 20,
            totalElements: scenicBookingDeleted ? 1 : 2,
            totalPages: scenicBookingDeleted ? 1 : 2
          }
        })
      }
      if (url === '/hotel-bookings/my') {
        return Promise.resolve({ data: [] })
      }
      if (url === '/auth/me/comments') {
        return Promise.resolve({ data: { routeComments: [], spotComments: [] } })
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(UserProfile)

    clickByText(root, '景点咨询')
    await settleVue()

    expect(root.textContent).toContain('First scenic booking')
    expect(root.textContent).toContain('1 / 2')
    expect(pageParamCalls('/bookings/my')).toContainEqual({ page: 0, size: 20 })

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/bookings/my')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First scenic booking')
    expect(root.textContent).toContain('Second scenic booking')

    testState.showConfirm.mockResolvedValueOnce(true)
    testState.apiDelete.mockImplementationOnce(() => {
      scenicBookingDeleted = true
      return Promise.resolve({})
    })
    clickByText(root, '删除咨询')
    await settleVue()

    expect(testState.apiDelete).toHaveBeenCalledWith('/bookings/81')
    expect(root.textContent).toContain('景点咨询 (1)')
    expect(root.textContent).not.toContain('Second scenic booking')
    expect(root.textContent).not.toContain('2 / 2')
  })

  it('loads the next scenic spot comment page with liked state from the page payload', async () => {
    testState.routeParams.id = '9'

    testState.apiGet.mockImplementation((url: string, config?: any) => {
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
        const page = Number(config?.params?.page ?? 0)
        return Promise.resolve(page === 0
          ? {
              data: {
                content: [
                  { id: 20, content: 'First scenic comment', createdAt: '2026-06-08T00:00:00Z', liked: true, likeCount: 1, rating: 5, user: { nickname: 'A' } }
                ],
                page: 0,
                size: 20,
                totalElements: 2,
                totalPages: 2
              }
            }
          : paginatedArray([
              { id: 21, content: 'Second scenic comment', createdAt: '2026-06-08T00:01:00Z', liked: false, likeCount: 0, rating: 4, user: { nickname: 'B' } }
            ], 1, 20, 2, 2))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(ScenicSpotDetail)

    expect(root.textContent).toContain('First scenic comment')
    expect(root.textContent).toContain('1 / 2')
    expect(root.textContent).not.toContain('Second scenic comment')
    expect(pageParamCalls('/comments/spot/9')).toContainEqual({ page: 0, size: 20 })
    expect(root.querySelector('[aria-pressed="true"]')).not.toBeNull()

    clickByText(root, '下一页')
    await settleVue()

    expect(pageParamCalls('/comments/spot/9')).toContainEqual({ page: 1, size: 20 })
    expect(root.textContent).toContain('First scenic comment')
    expect(root.textContent).toContain('Second scenic comment')
    expect(root.textContent).toContain('2 / 2')
    expect(testState.apiGet.mock.calls.some(([url]) => /^\/comments\/\d+\/liked$/.test(String(url)))).toBe(false)
  })

  it('renders scenic comment liked states from the page payload without per-comment status requests', async () => {
    testState.routeParams.id = '9'

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
              { id: 10, content: 'Liked comment', createdAt: '2026-06-08T00:00:00Z', liked: true, likeCount: 1, rating: 5, user: { nickname: 'A' } },
              { id: 11, content: 'Missing liked comment', createdAt: '2026-06-08T00:01:00Z', likeCount: 2, rating: 4, user: { nickname: 'B' } },
              { id: 12, content: 'Unliked comment', createdAt: '2026-06-08T00:02:00Z', liked: false, likeCount: 3, rating: 3, user: { nickname: 'C' } }
            ],
            page: 0,
            size: 20,
            totalElements: 3,
            totalPages: 1
          }
        })
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountView(ScenicSpotDetail)
    await settleVue()

    expect(root.textContent).toContain('Liked comment')
    expect(root.textContent).toContain('Missing liked comment')
    expect(root.textContent).toContain('Unliked comment')
    expect(root.querySelector('[aria-pressed="true"]')).not.toBeNull()
    expect(testState.apiGet.mock.calls.some(([url]) => /^\/comments\/\d+\/liked$/.test(String(url)))).toBe(false)
  })
})
