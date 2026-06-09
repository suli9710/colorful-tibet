// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, nextTick, type App } from 'vue'
import { createI18n } from 'vue-i18n'
import AdminDashboard from './AdminDashboard.vue'

const { apiGet, apiPost, apiPut, apiDelete, routerPush, authStore } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiDelete: vi.fn(),
  routerPush: vi.fn(),
  authStore: {
    ensureSession: vi.fn(),
    hasValidSession: vi.fn(),
    isAdmin: true,
    user: { id: 1, username: 'admin', role: 'ADMIN' },
    logout: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

vi.mock('../stores/auth', () => ({
  useAuthStore: () => authStore
}))

vi.mock('../composables/useConfirm', () => ({
  useConfirm: () => ({ showConfirm: vi.fn().mockResolvedValue(true) })
}))

vi.mock('../composables/useToast', () => ({
  useToast: () => ({ showToast: vi.fn() })
}))

vi.mock('../components/AdminAnalyticsPanel.vue', () => ({
  __esModule: true,
  default: { template: '<section data-testid="analytics-panel" />' }
}))

vi.mock('../components/AdminCommunityPanel.vue', () => ({
  default: { template: '<section data-testid="community-panel" />' }
}))

vi.mock('../components/AdminHeritagePanel.vue', () => ({
  default: { template: '<section data-testid="heritage-panel" />' }
}))

vi.mock('../components/AdminSecurityPosturePanel.vue', () => ({
  default: {
    props: ['posture', 'loading', 'error'],
    template: '<section data-testid="security-posture-panel" />'
  }
}))

vi.mock('../components/ImageUploadField.vue', () => ({
  default: { template: '<input aria-label="image upload" />' }
}))

vi.mock('../components/motion/MotionModal.vue', () => ({
  default: {
    props: ['show'],
    template: '<div v-if="show"><slot /></div>'
  }
}))

vi.mock('../api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: apiPut,
    delete: apiDelete
  },
  clearTokenCache: vi.fn(),
  endpoints: {
    admin: {
      stats: '/admin/stats',
      securityPosture: '/admin/security-posture',
      users: '/admin/users',
      spots: '/admin/spots',
      news: '/admin/news',
      createNews: '/admin/news',
      uploadImage: '/admin/upload-image',
      updateNews: (id: number) => `/admin/news/${id}`,
      deleteNews: (id: number) => `/admin/news/${id}`,
      updateRole: (id: number) => `/admin/users/${id}/role`,
      deleteUser: (id: number) => `/admin/users/${id}`,
      unlockUser: (id: number) => `/admin/users/${id}/unlock`,
      updateSpot: (id: number) => `/admin/spots/${id}`
    },
    hotelBookings: {
      all: '/hotel-bookings',
      updateStatus: (id: number) => `/hotel-bookings/${id}/status`,
      delete: (id: number) => `/hotel-bookings/${id}/permanent`
    },
    prices: {
      fetch: (spotId: number) => `/prices/fetch/${spotId}`,
      batchUpdateJob: '/prices/batch-update/jobs',
      batchUpdateJobStatus: (jobId: string) => `/prices/batch-update/jobs/${jobId}`
    },
    carousels: {
      adminList: '/admin/carousels',
      adminCreate: '/admin/carousels',
      adminUpdate: (id: number) => `/admin/carousels/${id}`,
      adminDelete: (id: number) => `/admin/carousels/${id}`
    },
    adminRoutes: {
      list: '/admin/routes',
      create: '/admin/routes',
      update: (id: number) => `/admin/routes/${id}`,
      delete: (id: number) => `/admin/routes/${id}`
    },
    adminHotels: {
      list: '/admin/hotels',
      create: '/admin/hotels',
      update: (id: number) => `/admin/hotels/${id}`,
      delete: (id: number) => `/admin/hotels/${id}`,
      roomTypes: (hotelId: number) => `/admin/hotels/${hotelId}/room-types`,
      deleteRoomType: (id: number) => `/admin/room-types/${id}`
    }
  }
}))

const mountedApps: App[] = []

const hotelOrderFixture = {
  id: 42,
  user: { username: 'internal-account-name' },
  hotel: { id: 9, name: '拉萨瑞吉度假酒店', location: '拉萨', imageUrl: '/hotel.jpg' },
  roomName: '观景套房',
  checkInDate: '2026-06-20',
  checkOutDate: '2026-06-23',
  guests: 2,
  nights: 3,
  guestName: '卓玛',
  phone: '13812345678',
  note: '高楼层',
  totalPrice: 1888,
  status: 'PENDING',
  createdAt: '2026-06-08T09:30:00'
}

const adminUserFixture = {
  id: 7,
  username: 'ops-admin',
  nickname: '运营管理员',
  role: 'ADMIN',
  locked: false,
  protectedAccount: false,
  createdAt: '2026-06-01T08:00:00'
}

const spotFixture = {
  id: 101,
  name: '布达拉宫',
  city: '拉萨',
  ticketPrice: 200,
  visitCount: 88,
  description: '拉萨地标'
}

const newsFixture = {
  id: 201,
  title: '藏历新年交通提示',
  content: '节日期间请提前规划出行。',
  category: 'NOTICE',
  viewCount: 12,
  createdAt: '2026-06-02T10:00:00'
}

const carouselFixture = {
  id: 301,
  title: '高原湖泊首图',
  subtitle: '湖光与雪山',
  imageUrl: '/banner.jpg',
  active: true
}

const routeFixture = {
  id: 401,
  title: '拉萨经典四日线',
  sourceType: 'OFFICIAL',
  days: 4,
  budget: '舒适型',
  preference: '人文历史',
  content: '布达拉宫、大昭寺与八廓街。',
  viewCount: 9,
  likeCount: 2,
  commentCount: 1
}

const hotelFixture = {
  id: 501,
  name: '雪域观景酒店',
  location: '拉萨',
  phone: '0891-1234567',
  priceRange: '¥500 - ¥900',
  rating: 4.7,
  imageUrl: '/hotel-admin.jpg',
  facilities: 'WiFi,停车场'
}

type ApiGetHandler = () => Promise<{ data: unknown }>
type DeferredResponse = {
  promise: Promise<{ data: unknown }>
  reject: (reason?: unknown) => void
  resolve: (value: { data: unknown }) => void
}

const createDeferredResponse = (): DeferredResponse => {
  let resolve!: DeferredResponse['resolve']
  let reject!: DeferredResponse['reject']
  const promise = new Promise<{ data: unknown }>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, reject, resolve }
}

let usersGet: ApiGetHandler
let hotelBookingsGet: ApiGetHandler
let spotsGet: ApiGetHandler
let newsGet: ApiGetHandler
let carouselsGet: ApiGetHandler
let routesGet: ApiGetHandler
let hotelsGet: ApiGetHandler

type AdminManagedListCase = {
  emptyText: string
  errorText: string
  header: string
  initialText: string
  name: string
  nextItem: unknown
  nextText: string
  setGet: (handler: ApiGetHandler) => void
  staleItem: unknown
  staleText: string
}

const adminManagedListCases: AdminManagedListCase[] = [
  {
    name: 'spots',
    header: '景点管理',
    initialText: '布达拉宫',
    nextText: '纳木错',
    staleText: '羊卓雍错',
    errorText: '景点列表加载失败，请稍后重试',
    emptyText: '暂无景点数据',
    setGet: handler => { spotsGet = handler },
    nextItem: { ...spotFixture, id: 102, name: '纳木错' },
    staleItem: { ...spotFixture, id: 103, name: '羊卓雍错' }
  },
  {
    name: 'news',
    header: '旅游资讯管理',
    initialText: '藏历新年交通提示',
    nextText: '边境通行提醒',
    staleText: '旧版资讯',
    errorText: '资讯列表加载失败，请稍后重试',
    emptyText: '暂无资讯数据',
    setGet: handler => { newsGet = handler },
    nextItem: { ...newsFixture, id: 202, title: '边境通行提醒' },
    staleItem: { ...newsFixture, id: 203, title: '旧版资讯' }
  },
  {
    name: 'carousels',
    header: '轮播图管理',
    initialText: '高原湖泊首图',
    nextText: '林芝桃花轮播',
    staleText: '旧轮播图',
    errorText: '轮播图列表加载失败，请稍后重试',
    emptyText: '暂无轮播图',
    setGet: handler => { carouselsGet = handler },
    nextItem: { ...carouselFixture, id: 302, title: '林芝桃花轮播' },
    staleItem: { ...carouselFixture, id: 303, title: '旧轮播图' }
  },
  {
    name: 'routes',
    header: '线路管理',
    initialText: '拉萨经典四日线',
    nextText: '林芝三日线',
    staleText: '旧线路',
    errorText: '线路列表加载失败，请稍后重试',
    emptyText: '暂无分享路线',
    setGet: handler => { routesGet = handler },
    nextItem: { ...routeFixture, id: 402, title: '林芝三日线' },
    staleItem: { ...routeFixture, id: 403, title: '旧线路' }
  },
  {
    name: 'hotels',
    header: '酒店管理',
    initialText: '雪域观景酒店',
    nextText: '林芝雪山酒店',
    staleText: '旧酒店',
    errorText: '酒店列表加载失败，请稍后重试',
    emptyText: '暂无酒店数据',
    setGet: handler => { hotelsGet = handler },
    nextItem: { ...hotelFixture, id: 502, name: '林芝雪山酒店' },
    staleItem: { ...hotelFixture, id: 503, name: '旧酒店' }
  }
]

describe('AdminDashboard hotel order table', () => {
  beforeEach(() => {
    authStore.ensureSession.mockResolvedValue(true)
    authStore.hasValidSession.mockReturnValue(true)
    routerPush.mockResolvedValue(undefined)

    usersGet = () => Promise.resolve({ data: [adminUserFixture] })
    hotelBookingsGet = () => Promise.resolve({ data: [hotelOrderFixture] })
    spotsGet = () => Promise.resolve({ data: [spotFixture] })
    newsGet = () => Promise.resolve({ data: [newsFixture] })
    carouselsGet = () => Promise.resolve({ data: [carouselFixture] })
    routesGet = () => Promise.resolve({ data: [routeFixture] })
    hotelsGet = () => Promise.resolve({ data: [hotelFixture] })

    apiGet.mockImplementation((url: string) => {
      if (url === '/admin/stats') {
        return Promise.resolve({
          data: {
            userCount: 0,
            orderCount: 1,
            totalRevenue: 1888,
            recentBookings: [],
            recentHotelBookings: [],
            popularSpots: []
          }
        })
      }
      if (url === '/admin/security-posture') return Promise.resolve({ data: null })
      if (url === '/admin/users') return usersGet()
      if (url === '/hotel-bookings') return hotelBookingsGet()
      if (url === '/admin/spots') return spotsGet()
      if (url === '/admin/news') return newsGet()
      if (url === '/admin/carousels') return carouselsGet()
      if (url === '/admin/routes') return routesGet()
      if (url === '/admin/hotels') return hotelsGet()
      return Promise.resolve({ data: [] })
    })
  })

  afterEach(() => {
    mountedApps.splice(0).forEach(app => app.unmount())
    document.body.innerHTML = ''
    vi.clearAllMocks()
  })

  it('renders useful hotel order details without internal usernames or raw phones', async () => {
    const { root } = await mountDashboard()

    findPanelHeader(root, '酒店咨询单').click()
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('卓玛')
    expect(renderedText).toContain('138 **** 5678')
    expect(renderedText).toContain('拉萨瑞吉度假酒店')
    expect(renderedText).toContain('观景套房')
    expect(renderedText).toContain('2人')
    expect(renderedText).toContain('3晚')
    expect(renderedText).not.toContain('internal-account-name')
    expect(renderedText).not.toContain('13812345678')
  })

  it('rolls back the visible status and shows row error when status update fails', async () => {
    const { root } = await mountDashboard()

    findPanelHeader(root, '酒店咨询单').click()
    await settleVue()

    const statusSelect = root.querySelector<HTMLSelectElement>('select[aria-label="修改订单 42 的状态"]')
    expect(statusSelect).toBeTruthy()
    expect(statusSelect?.value).toBe('PENDING')

    apiPut.mockRejectedValueOnce(new Error('status service unavailable'))
    statusSelect!.value = 'CONFIRMED'
    statusSelect!.dispatchEvent(new Event('change', { bubbles: true }))
    await settleVue()

    expect(apiPut).toHaveBeenCalledWith('/hotel-bookings/42/status', { status: 'CONFIRMED' })
    expect(statusSelect?.value).toBe('PENDING')
    expect(statusSelect?.disabled).toBe(false)
    expect(root.textContent).toContain('状态更新失败，已恢复为原状态')
    expect(root.textContent).toContain('卓玛')
  })

  it('keeps hotel order rows and shows retry when a refresh fails', async () => {
    const { root } = await mountDashboard()

    const hotelOrdersHeader = findPanelHeader(root, '酒店咨询单')
    hotelOrdersHeader.click()
    await settleVue()
    expect(root.textContent).toContain('卓玛')

    hotelBookingsGet = () => Promise.reject(new Error('temporary hotel order outage'))
    clickHeaderButton(hotelOrdersHeader, '刷新')
    await settleVue()

    expect(root.textContent).toContain('酒店咨询单加载失败，请稍后重试')
    expect(root.textContent).toContain('重试酒店咨询单')
    expect(root.textContent).toContain('卓玛')
    expect(root.textContent).not.toContain('暂无酒店咨询单')

    hotelBookingsGet = () => Promise.resolve({
      data: [{ ...hotelOrderFixture, id: 43, guestName: '央金', hotel: { ...hotelOrderFixture.hotel, name: '林芝雪山酒店' } }]
    })
    clickButtonByText(root, '重试酒店咨询单')
    await settleVue()

    expect(root.textContent).toContain('央金')
    expect(root.textContent).toContain('林芝雪山酒店')
    expect(root.textContent).not.toContain('酒店咨询单加载失败，请稍后重试')
  })

  it('ignores stale hotel-order failures after a newer refresh succeeds', async () => {
    const { root } = await mountDashboard()

    const hotelOrdersHeader = findPanelHeader(root, '酒店咨询单')
    hotelOrdersHeader.click()
    await settleVue()
    expect(root.textContent).toContain('卓玛')

    const staleRefresh = createDeferredResponse()
    const latestRefresh = createDeferredResponse()

    hotelBookingsGet = () => staleRefresh.promise
    clickHeaderButton(hotelOrdersHeader, '刷新')
    hotelBookingsGet = () => latestRefresh.promise
    clickHeaderButton(hotelOrdersHeader, '刷新')
    await settleVue()

    latestRefresh.resolve({
      data: [{ ...hotelOrderFixture, id: 44, guestName: '央金', hotel: { ...hotelOrderFixture.hotel, name: '林芝雪山酒店' } }]
    })
    await settleVue()

    staleRefresh.reject(new Error('stale hotel order outage'))
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('央金')
    expect(renderedText).toContain('林芝雪山酒店')
    expect(renderedText).not.toContain('酒店咨询单加载失败，请稍后重试')
    expect(renderedText).not.toContain('卓玛')
  })

  it('keeps user rows and shows retry when a refresh fails', async () => {
    const { root } = await mountDashboard()

    const usersHeader = findPanelHeader(root, '用户管理')
    usersHeader.click()
    await settleVue()
    expect(root.textContent).toContain('ops-admin')

    usersGet = () => Promise.reject(new Error('temporary users outage'))
    clickHeaderButton(usersHeader, '刷新')
    await settleVue()

    expect(root.textContent).toContain('用户列表加载失败，请稍后重试')
    expect(root.textContent).toContain('重试用户列表')
    expect(root.textContent).toContain('ops-admin')
    expect(root.textContent).not.toContain('用户管理 (0人)')

    usersGet = () => Promise.resolve({
      data: [{ ...adminUserFixture, id: 8, username: 'ops-reviewer', nickname: '复核员', role: 'USER' }]
    })
    clickButtonByText(root, '重试用户列表')
    await settleVue()

    expect(root.textContent).toContain('ops-reviewer')
    expect(root.textContent).not.toContain('用户列表加载失败，请稍后重试')
  })

  it('ignores stale user refresh responses after a newer refresh succeeds', async () => {
    const { root } = await mountDashboard()

    const usersHeader = findPanelHeader(root, '用户管理')
    usersHeader.click()
    await settleVue()
    expect(root.textContent).toContain('ops-admin')

    const staleRefresh = createDeferredResponse()
    const latestRefresh = createDeferredResponse()

    usersGet = () => staleRefresh.promise
    clickHeaderButton(usersHeader, '刷新')
    usersGet = () => latestRefresh.promise
    clickHeaderButton(usersHeader, '刷新')
    await settleVue()

    latestRefresh.resolve({
      data: [{ ...adminUserFixture, id: 8, username: 'ops-reviewer', nickname: '复核员', role: 'USER' }]
    })
    await settleVue()

    staleRefresh.resolve({
      data: [{ ...adminUserFixture, id: 9, username: 'stale-admin', nickname: '旧响应', role: 'ADMIN' }]
    })
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('ops-reviewer')
    expect(renderedText).not.toContain('stale-admin')
    expect(renderedText).not.toContain('用户列表加载失败，请稍后重试')
  })

  it.each(adminManagedListCases)('keeps $name rows and shows retry when a refresh fails', async (listCase) => {
    const { root } = await mountDashboard()

    const header = findPanelHeader(root, listCase.header)
    header.click()
    await settleVue()
    expect(root.textContent).toContain(listCase.initialText)

    listCase.setGet(() => Promise.reject(new Error(`temporary ${listCase.name} outage`)))
    clickHeaderButton(header, '刷新')
    await settleVue()

    expect(root.textContent).toContain(listCase.errorText)
    expect(root.textContent).toContain('重试')
    expect(root.textContent).toContain(listCase.initialText)
    expect(root.textContent).not.toContain(listCase.emptyText)

    listCase.setGet(() => Promise.resolve({ data: [listCase.nextItem] }))
    clickButtonByText(root, '重试')
    await settleVue()

    expect(root.textContent).toContain(listCase.nextText)
    expect(root.textContent).not.toContain(listCase.errorText)
  })

  it.each(adminManagedListCases)('ignores stale $name refresh responses after a newer refresh succeeds', async (listCase) => {
    const { root } = await mountDashboard()

    const header = findPanelHeader(root, listCase.header)
    header.click()
    await settleVue()
    expect(root.textContent).toContain(listCase.initialText)

    const staleRefresh = createDeferredResponse()
    const latestRefresh = createDeferredResponse()

    listCase.setGet(() => staleRefresh.promise)
    clickHeaderButton(header, '刷新')
    listCase.setGet(() => latestRefresh.promise)
    clickHeaderButton(header, '刷新')
    await settleVue()

    latestRefresh.resolve({ data: [listCase.nextItem] })
    await settleVue()

    staleRefresh.resolve({ data: [listCase.staleItem] })
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain(listCase.nextText)
    expect(renderedText).not.toContain(listCase.staleText)
    expect(renderedText).not.toContain(listCase.errorText)
  })
})

const findPanelHeader = (root: HTMLElement, label: string) => {
  const header = Array.from(root.querySelectorAll<HTMLElement>('[role="button"]'))
    .find(element => element.textContent?.includes(label))
  expect(header).toBeTruthy()
  return header as HTMLElement
}

const clickHeaderButton = (header: HTMLElement, label: string) => {
  const button = Array.from(header.querySelectorAll<HTMLButtonElement>('button'))
    .find(element => element.textContent?.includes(label))
  expect(button).toBeTruthy()
  button?.click()
}

const clickButtonByText = (root: HTMLElement, label: string) => {
  const button = Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
    .find(element => element.textContent?.includes(label))
  expect(button).toBeTruthy()
  button?.click()
}

const mountDashboard = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(AdminDashboard)
  app.use(createI18n({
    legacy: false,
    locale: 'zh',
    fallbackLocale: 'zh',
    messages: {
      zh: {
        common: {
          items: '条',
          refresh: '刷新',
          retry: '重试',
          loading: '加载中...',
          delete: '删除',
          edit: '编辑',
          create: '创建',
          enabled: '启用',
          disabled: '禁用',
          unknown: '未知',
          unknownLocation: '未知位置',
          noData: '暂无数据',
          countUnit: '个',
          peopleUnit: '人',
          hotelsUnit: '家',
          imagesUnit: '张',
          priceCny: '¥{price}',
          nightsUnit: '晚',
          consult: '咨询',
          collapse: '收起',
          expandAll: '查看全部'
        },
        admin: {
          title: '后台管理看板',
          subtitle: '数据概览与运营统计',
          totalUsers: '总用户数',
          totalOrders: '总订单数',
          totalRevenue: '总营收',
          hotelOrders: '酒店咨询单',
          loadingHotelOrders: '正在加载酒店咨询单...',
          hotelOrdersLoadFailed: '酒店咨询单加载失败，请稍后重试',
          retryHotelOrders: '重试酒店咨询单',
          orderId: '订单ID',
          booker: '预订人',
          hotelAndRoom: '酒店 / 房型',
          checkInOut: '入住 - 离店',
          amount: '金额',
          status: '状态',
          orderedAt: '下单时间',
          action: '操作',
          unknownHotel: '未知酒店',
          toDate: '至 {date}',
          noHotelOrders: '暂无酒店咨询单',
          expandHotelOrders: '点击上方标题栏展开查看全部订单（共{count}条）',
          latestOrders: '最新订单',
          hotel: '酒店',
          spot: '景点',
          noOrders: '暂无订单',
          popularSpots: '热门景点',
          visits: '{count}次',
          spotManagement: '景点管理',
          userManagement: '用户管理',
          loadingUsers: '正在加载用户列表...',
          usersLoadFailed: '用户列表加载失败，请稍后重试',
          retryUsers: '重试用户列表',
          listUnavailable: '加载失败',
          username: '用户名',
          password: '密码',
          nickname: '昵称',
          registeredAt: '注册时间',
          adminRole: '管理员',
          userRole: '普通用户',
          locked: '封禁中',
          passwordHashTitle: '密码仅保存 BCrypt 哈希，无法查看明文',
          bcryptHash: 'BCrypt 哈希',
          unlock: '解封',
          setAdmin: '设为管理员',
          unsetAdmin: '取消管理员',
          notOperable: '不可操作',
          newsManagement: '旅游资讯管理',
          createNews: '+ 创建',
          loadingNews: '正在加载资讯数据...',
          newsLoadFailed: '资讯列表加载失败，请稍后重试',
          viewCount: '浏览量: {count}',
          noNews: '暂无资讯数据',
          expandNews: '点击上方标题栏展开查看全部资讯（共{count}条）',
          categoryPolicy: '政策',
          categoryEvent: '活动',
          categoryNotice: '通知',
          carouselManagement: '轮播图管理',
          loadingCarousels: '正在加载轮播图...',
          carouselsLoadFailed: '轮播图列表加载失败，请稍后重试',
          noCarousels: '暂无轮播图',
          addCarousel: '+ 添加',
          routeManagement: '线路管理',
          loadingRoutes: '正在加载线路...',
          routesLoadFailed: '线路列表加载失败，请稍后重试',
          routeSource: '来源',
          officialRoute: '官方推荐',
          userSharedRoute: '用户分享',
          officialAuthor: '七彩西藏官方',
          routeStats: '互动',
          communityMeta: '信息',
          communityCounts: '浏览 {views} · 点赞 {likes} · 评论 {comments}',
          noCommunityRoutes: '暂无分享路线',
          days: '天数',
          daysValue: '{count}天',
          hotelManagement: '酒店管理',
          loadingHotels: '正在加载酒店...',
          hotelsLoadFailed: '酒店列表加载失败，请稍后重试',
          addHotel: '+ 新增酒店',
          roomManagement: '房型管理',
          roomTypesCount: '{count}个房型',
          noRoomTypes: '暂无房型，请在下方添加',
          pricePerNightWithCapacity: '¥{price} / 晚 · {capacity}人',
          roomNamePlaceholder: '名称',
          capacityPlaceholder: '人',
          roomTypes: '房型',
          noHotels: '暂无酒店数据',
          addFirstHotel: '+ 添加第一家酒店',
          expandHotels: '点击上方标题栏展开管理酒店（共{count}家）',
          loadingSpots: '正在加载景点数据...',
          spotsLoadFailed: '景点列表加载失败，请稍后重试',
          noSpotData: '暂无景点数据',
          clickEdit: '点击编辑',
          editArrow: '编辑 →',
          expandSpots: '点击上方标题栏展开管理景点（共{count}个）',
          addNew: '+ 新增',
          titleLabel: '标题',
          content: '内容',
          batchFetchPrices: '批量获取价格',
          fetchingPrices: '获取中...',
          serverError: '服务器错误 ({status})',
          unknownError: '未知错误',
          connectionFailed: '无法连接到后端服务，请稍后重试',
          unauthorizedAdmin: '未授权：请先登录管理员账户',
          forbiddenAdmin: '禁止访问：您没有管理员权限',
          changeHotelOrderStatus: '修改订单 {id} 的状态',
          statusUpdateFailedInline: '状态更新失败，已恢复为原状态',
          statusLabel: {
            PENDING: '待确认',
            CONFIRMED: '已确认',
            CANCELLED: '已取消'
          },
          securityPosture: {
            loadFailed: '安全态势加载失败'
          }
        },
        hotel: {
          guests: '人',
          nights: '晚数'
        }
      }
    }
  }))

  mountedApps.push(app)
  app.mount(root)
  await settleVue()

  return { root }
}

const settleVue = async () => {
  for (let index = 0; index < 12; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}
