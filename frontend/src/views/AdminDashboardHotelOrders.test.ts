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

describe('AdminDashboard hotel order table', () => {
  beforeEach(() => {
    authStore.ensureSession.mockResolvedValue(true)
    authStore.hasValidSession.mockReturnValue(true)
    routerPush.mockResolvedValue(undefined)
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
      if (url === '/hotel-bookings') return Promise.resolve({ data: [hotelOrderFixture] })
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

    const hotelOrdersHeader = Array.from(root.querySelectorAll<HTMLElement>('.cursor-pointer'))
      .find(element => element.textContent?.includes('酒店咨询单'))

    expect(hotelOrdersHeader).toBeTruthy()
    hotelOrdersHeader?.click()
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
})

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
          loading: '加载中...',
          delete: '删除',
          unknown: '未知',
          noData: '暂无数据',
          countUnit: '个',
          peopleUnit: '人',
          priceCny: '¥{price}',
          nightsUnit: '晚',
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
          newsManagement: '旅游资讯管理',
          carouselManagement: '轮播图管理',
          routeManagement: '线路管理',
          hotelManagement: '酒店管理',
          loadingSpots: '正在加载景点数据...',
          batchFetchPrices: '批量获取价格',
          fetchingPrices: '获取中...',
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
