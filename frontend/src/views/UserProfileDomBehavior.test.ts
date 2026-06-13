// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, type App } from 'vue'
import { createI18n } from 'vue-i18n'

const testState = vi.hoisted(() => ({
  apiDelete: vi.fn(),
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  authUser: null as null | {
    avatar?: string
    avatarUrl?: string
    mustChangePassword?: boolean
    nickname?: string
    role?: string
  },
  ensureSession: vi.fn(),
  routeQuery: {} as Record<string, unknown>,
  routerPush: vi.fn(),
  routerReplace: vi.fn(),
  showConfirm: vi.fn(),
  showToast: vi.fn(),
  updateUser: vi.fn()
}))

const endpointsMock = vi.hoisted(() => ({
  auth: {
    changePassword: '/auth/me/change-password',
    me: '/auth/me',
    meComments: '/auth/me/comments',
    meStats: '/auth/me/stats',
    updateNickname: '/auth/me/nickname',
    uploadAvatar: '/auth/me/upload-avatar'
  },
  bookings: {
    cancel: (id: number) => `/bookings/${id}/cancel`,
    delete: (id: number) => `/bookings/${id}`,
    my: '/bookings/my'
  },
  comments: {
    delete: (id: number) => `/comments/${id}`
  },
  hotelBookings: {
    cancel: (id: number) => `/hotel-bookings/${id}`,
    delete: (id: number) => `/hotel-bookings/${id}/permanent`,
    my: '/hotel-bookings/my'
  },
  routes: {
    deleteSharedComment: (routeId: number, commentId: number) =>
      `/routes/shared/${routeId}/comments/${commentId}`,
    myRoutes: '/routes/my-routes',
    sharedDetail: (id: number) => `/routes/shared/${id}`
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    fullPath: '/profile',
    path: '/profile',
    query: testState.routeQuery
  }),
  useRouter: () => ({
    push: testState.routerPush,
    replace: testState.routerReplace
  })
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

vi.mock('../api', () => ({
  default: {
    delete: testState.apiDelete,
    get: testState.apiGet,
    post: testState.apiPost,
    put: testState.apiPut
  },
  endpoints: endpointsMock
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    ensureSession: testState.ensureSession,
    updateUser: testState.updateUser,
    get user() {
      return testState.authUser
    }
  })
}))

vi.mock('../stores/auth', () => ({
  useAuthStore: () => ({
    ensureSession: testState.ensureSession,
    updateUser: testState.updateUser,
    get user() {
      return testState.authUser
    }
  })
}))

vi.mock('@/composables/useConfirm', () => ({
  showConfirm: testState.showConfirm,
  useConfirm: () => ({ showConfirm: testState.showConfirm })
}))

vi.mock('../composables/useConfirm', () => ({
  showConfirm: testState.showConfirm,
  useConfirm: () => ({ showConfirm: testState.showConfirm })
}))

vi.mock('@/composables/useToast', () => ({
  showToast: testState.showToast,
  useToast: () => ({ showToast: testState.showToast })
}))

vi.mock('../composables/useToast', () => ({
  showToast: testState.showToast,
  useToast: () => ({ showToast: testState.showToast })
}))

vi.mock('@/components/motion/MotionModal.vue', async () => {
  const vue = await import('vue')

  return {
    default: vue.defineComponent({
      name: 'MotionModalStub',
      props: {
        show: Boolean
      },
      emits: ['close'],
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
      props: {
        show: Boolean
      },
      emits: ['close'],
      setup(props, { slots }) {
        return () => props.show ? vue.h('div', slots.default?.()) : null
      }
    })
  }
})

vi.mock('@/data/hotelImages', () => ({
  applyHotelImageFallback: vi.fn(),
  resolveHotelBookingImage: (booking: { hotel?: { imageUrl?: string, coverImage?: string } }) =>
    booking.hotel?.imageUrl || booking.hotel?.coverImage || '/hotel-cover.jpg'
}))

vi.mock('../data/hotelImages', () => ({
  applyHotelImageFallback: vi.fn(),
  resolveHotelBookingImage: (booking: { hotel?: { imageUrl?: string, coverImage?: string } }) =>
    booking.hotel?.imageUrl || booking.hotel?.coverImage || '/hotel-cover.jpg'
}))

vi.mock('@/i18n/formatting', () => ({
  toIntlLocale: () => 'en-US'
}))

vi.mock('../i18n/formatting', () => ({
  toIntlLocale: () => 'en-US'
}))

vi.mock('@/motion/presets', () => ({
  cardExit: {},
  cardInitial: {},
  cardInView: {},
  cardTransition: () => ({}),
  revealInitial: {},
  revealInView: {},
  revealTransition: {},
  softSpring: {}
}))

vi.mock('../motion/presets', () => ({
  cardExit: {},
  cardInitial: {},
  cardInView: {},
  cardTransition: () => ({}),
  revealInitial: {},
  revealInView: {},
  revealTransition: {},
  softSpring: {}
}))

vi.mock('@/utils/errorMonitoring', () => ({
  safeClientErrorMessage: (_error: unknown, fallback: string) => fallback,
  summarizeClientError: (error: unknown) => error instanceof Error ? error.message : String(error)
}))

vi.mock('../utils/errorMonitoring', () => ({
  safeClientErrorMessage: (_error: unknown, fallback: string) => fallback,
  summarizeClientError: (error: unknown) => error instanceof Error ? error.message : String(error)
}))

vi.mock('motion-v', async () => {
  const vue = await import('vue')
  const passthroughComponents = new Map<string, ReturnType<typeof vue.defineComponent>>()

  const passthrough = (tag = 'div') => {
    const existing = passthroughComponents.get(tag)
    if (existing) return existing

    const component = vue.defineComponent({
      inheritAttrs: false,
      setup(_, { attrs, slots }) {
        return () => {
          const {
            animate,
            exit,
            initial,
            layout,
            layoutId,
            mode,
            transition,
            whileHover,
            whileInView,
            whileTap,
            ...domAttrs
          } = attrs

          return vue.h(tag, domAttrs, slots.default?.())
        }
      }
    })
    passthroughComponents.set(tag, component)

    return component
  }

  const fragment = vue.defineComponent({
    inheritAttrs: false,
    setup(_, { slots }) {
      return () => vue.h(vue.Fragment, slots.default?.())
    }
  })

  return {
    AnimatePresence: fragment,
    LayoutGroup: fragment,
    motion: new Proxy({}, {
      get: (_target, tag) => passthrough(String(tag))
    })
  }
})

import UserProfile from './UserProfile.vue'

const mountedApps: App[] = []
const pageSize = 20

type DeferredPromise<T> = {
  promise: Promise<T>
  reject: (reason?: unknown) => void
  resolve: (value: T | PromiseLike<T>) => void
}

const createDeferred = <T,>(): DeferredPromise<T> => {
  let resolve!: DeferredPromise<T>['resolve']
  let reject!: DeferredPromise<T>['reject']
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, reject, resolve }
}

const settleVue = async (turns = 10) => {
  for (let index = 0; index < turns; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const RouterLinkStub = defineComponent({
  name: 'RouterLinkStub',
  props: {
    to: {
      required: true,
      type: [String, Object]
    }
  },
  setup(props, { slots }) {
    return () => h('a', {
      href: typeof props.to === 'string' ? props.to : '#'
    }, slots.default?.())
  }
})

const createTestI18n = () => createI18n({
  fallbackLocale: 'en',
  fallbackWarn: false,
  legacy: false,
  locale: 'en',
  missingWarn: false,
  messages: {
    en: {
      common: {
        delete: 'Delete',
        edit: 'Edit',
        loading: 'Loading',
        pendingConfirm: 'Pending'
      },
      community: {
        nextPage: 'Next page'
      },
      profile: {
        admin: 'Admin',
        avatar: 'Avatar',
        bookingsCount: 'bookings',
        bookingsLoadFailed: 'Could not load scenic bookings',
        browseHotelsLink: 'Browse hotels',
        browseSpotsLink: 'Browse spots',
        cancel: 'Cancel',
        cancelBooking: 'Cancel booking',
        cancelFailed: 'Cancel failed',
        cancelHotelBookingFailed: 'Cancel hotel booking failed',
        cancelled: 'Cancelled',
        changePassword: 'Change password',
        changing: 'Changing',
        checkIn: 'Check-in',
        checkOut: 'Check-out',
        commentImageAlt: 'Comment image',
        commentsCount: 'comments',
        commentsLoadFailed: 'Could not load comments',
        confirmCancelBooking: 'Cancel scenic booking?',
        confirmCancelHotelBooking: 'Cancel hotel booking?',
        confirmChange: 'Confirm change',
        confirmDeleteBooking: 'Delete booking?',
        confirmDeleteComment: 'Delete comment?',
        confirmDeleteRoute: 'Delete route?',
        confirmNewPassword: 'Confirm new password',
        confirmNewPasswordPlaceholder: 'Confirm new password',
        createRouteLink: 'Create route',
        currentPassword: 'Current password',
        currentPasswordPlaceholder: 'Current password',
        days: ' days',
        delete: 'Delete',
        deleteBooking: 'Delete booking',
        deleteBookingFailed: 'Delete booking failed',
        deleteFailed: 'Delete failed',
        deleteSuccess: 'Deleted',
        editNickname: 'Edit nickname',
        fillAllFields: 'Fill all fields',
        hotelBookingsLoadFailed: 'Could not load hotel bookings',
        loadFailed: 'Load failed',
        member: 'Member',
        myBookingsTab: 'Scenic bookings',
        myCommentsTab: 'Comments',
        myHotelBookingsTab: 'Hotel bookings',
        myRoutesTab: 'Routes',
        newPassword: 'New password',
        newPasswordPlaceholder: 'New password',
        nights: 'Nights',
        noBookings: 'No scenic bookings',
        noComments: 'No comments',
        noHotelBookings: 'No hotel bookings',
        noRoutes: 'No routes',
        retryLoad: 'Retry',
        passwordChangeFailed: 'Password change failed',
        passwordChangeSuccess: 'Password changed',
        passwordMinLength: 'Password is too short',
        passwordMismatch: 'Passwords do not match',
        pending: 'Pending',
        registeredAt: 'Registered at',
        room: 'Room',
        routeComments: 'Route comments',
        routeCount: 'routes',
        routesCount: 'routes',
        spotComments: 'Spot comments',
        tickets: ' tickets',
        title: 'Profile',
        visitDate: 'Visit date'
      }
    }
  }
})

const pageResponse = <T,>(
  content: T[],
  page = 0,
  totalPages = content.length > 0 ? 1 : 0,
  totalElements = totalPages > 1 ? totalPages * pageSize : content.length
) => ({
  data: {
    content,
    page,
    size: pageSize,
    totalElements,
    totalPages
  }
})

const commentsResponse = (
  spotComments: unknown[],
  routeComments: unknown[],
  page = 0,
  totalPages = spotComments.length > 0 || routeComments.length > 0 ? 1 : 0
) => ({
  data: {
    routeCommentsPage: {
      content: routeComments,
      page,
      size: pageSize,
      totalElements: routeComments.length > 0 && totalPages > 1 ? totalPages * pageSize : routeComments.length,
      totalPages: routeComments.length > 0 ? totalPages : 0
    },
    spotCommentsPage: {
      content: spotComments,
      page,
      size: pageSize,
      totalElements: spotComments.length > 0 && totalPages > 1 ? totalPages * pageSize : spotComments.length,
      totalPages: spotComments.length > 0 ? totalPages : 0
    }
  }
})

const scenicBooking = (id: number, name: string) => ({
  id,
  spot: {
    id: id + 1000,
    imageUrl: `/spots/${id}.jpg`,
    name
  },
  status: 'PENDING',
  ticketCount: 2,
  totalPrice: 120,
  visitDate: '2026-06-20'
})

const hotelBooking = (id: number, name: string) => ({
  checkInDate: '2026-06-20',
  checkOutDate: '2026-06-22',
  hotel: {
    coverImage: `/hotels/${id}.jpg`,
    id: id + 2000,
    imageUrl: `/hotels/${id}.jpg`,
    name
  },
  hotelName: name,
  id,
  nights: 2,
  roomName: 'Panorama room',
  status: 'PENDING',
  totalPrice: 880
})

const spotComment = (id: number, spotName: string, content: string) => ({
  content,
  createdAt: '2026-06-08T09:30:00Z',
  id,
  likeCount: 3,
  rating: 5,
  spot: {
    id: id + 3000,
    name: spotName
  }
})

const routeComment = (id: number, routeTitle: string, content: string) => ({
  content,
  createdAt: '2026-06-08T10:30:00Z',
  id,
  route: {
    id: id + 4000,
    title: routeTitle
  }
})

type QueuedResponse = Promise<unknown> | Record<string, unknown>
type ResponseQueues = Partial<Record<string, QueuedResponse[]>>

const installProfileGetMock = (queues: ResponseQueues = {}) => {
  testState.apiGet.mockImplementation((url: string) => {
    const queue = queues[url]

    if (queue?.length) {
      return Promise.resolve(queue.shift())
    }

    if (url === endpointsMock.auth.me) {
      return Promise.resolve({
        data: {
          createdAt: '2026-01-01T00:00:00Z',
          nickname: 'Traveler',
          role: 'USER'
        }
      })
    }

    if (url === endpointsMock.auth.meStats) {
      return Promise.resolve({
        data: {
          bookingCount: 0,
          commentCount: 0,
          routeCount: 0
        }
      })
    }

    if (
      url === endpointsMock.routes.myRoutes ||
      url === endpointsMock.bookings.my ||
      url === endpointsMock.hotelBookings.my
    ) {
      return Promise.resolve(pageResponse([]))
    }

    if (url === endpointsMock.auth.meComments) {
      return Promise.resolve(commentsResponse([], []))
    }

    return Promise.reject(new Error(`Unexpected GET ${url}`))
  })
}

const mountUserProfile = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(UserProfile)
  app.component('router-link', RouterLinkStub)
  app.use(createTestI18n())
  mountedApps.push(app)
  app.mount(root)
  await settleVue()

  return root
}

const findButtonByText = (root: ParentNode, text: string) =>
  Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find(button =>
    button.textContent?.includes(text)
  ) ?? null

const clickButtonByText = async (root: ParentNode, text: string, turns?: number) => {
  const button = findButtonByText(root, text)

  expect(button).toBeTruthy()
  button!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  await settleVue(turns)

  return button!
}

const clickProfileTab = async (root: ParentNode, label: string) => {
  const tab = Array.from(root.querySelectorAll<HTMLButtonElement>('[role="tab"]')).find(button =>
    button.textContent?.includes(label)
  )

  expect(tab).toBeTruthy()
  tab!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  await settleVue()

  return tab!
}

const findLoadMoreButton = (root: ParentNode) =>
  Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find(button =>
    button.textContent?.includes('Next page') || button.textContent?.includes('Loading')
  ) ?? null

const setFieldValue = async (root: ParentNode, selector: string, value: string) => {
  const input = root.querySelector<HTMLInputElement>(selector)

  expect(input).toBeTruthy()
  input!.value = value
  input!.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }))
  await settleVue(2)
}

const submitPasswordChange = async (root: ParentNode) => {
  await clickButtonByText(root, 'Change password', 2)
  await setFieldValue(root, '#profile-current-password', 'oldpass1')
  await setFieldValue(root, '#profile-new-password', 'newpass1')
  await setFieldValue(root, '#profile-confirm-new-password', 'newpass1')
  await clickButtonByText(root, 'Confirm change', 4)
}

beforeEach(() => {
  testState.apiDelete.mockReset().mockResolvedValue({ data: {} })
  testState.apiGet.mockReset()
  testState.apiPost.mockReset().mockResolvedValue({ data: {} })
  testState.apiPut.mockReset().mockResolvedValue({ data: {} })
  testState.authUser = {
    nickname: 'Traveler',
    role: 'USER'
  }
  testState.ensureSession.mockReset().mockResolvedValue(true)
  testState.routerPush.mockReset().mockResolvedValue(undefined)
  testState.routerReplace.mockReset().mockResolvedValue(undefined)
  testState.showConfirm.mockReset().mockResolvedValue(true)
  testState.showToast.mockReset()
  testState.updateUser.mockReset()
  Object.keys(testState.routeQuery).forEach(key => delete testState.routeQuery[key])
})

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

describe('UserProfile stale-response DOM behavior', () => {
  it('ignores an older scenic booking refresh that returns after a newer refresh', async () => {
    const oldRefresh = createDeferred<ReturnType<typeof pageResponse>>()
    const newRefresh = createDeferred<ReturnType<typeof pageResponse>>()

    installProfileGetMock({
      [endpointsMock.bookings.my]: [
        pageResponse([scenicBooking(1, 'Initial Scenic Booking')]),
        oldRefresh.promise,
        newRefresh.promise
      ]
    })

    const root = await mountUserProfile()
    await clickProfileTab(root, 'Scenic bookings')

    expect(root.textContent).toContain('Initial Scenic Booking')

    await clickButtonByText(root, 'Cancel booking', 4)
    await clickButtonByText(root, 'Cancel booking', 4)

    newRefresh.resolve(pageResponse([scenicBooking(2, 'Fresh Scenic Booking')]))
    await settleVue()

    oldRefresh.resolve(pageResponse([scenicBooking(3, 'Stale Scenic Booking')]))
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('Fresh Scenic Booking')
    expect(renderedText).not.toContain('Stale Scenic Booking')
  })

  it('keeps cancel-triggered scenic refresh authoritative while load-more is clicked', async () => {
    const cancelRefresh = createDeferred<ReturnType<typeof pageResponse>>()

    installProfileGetMock({
      [endpointsMock.bookings.my]: [
        pageResponse([scenicBooking(1, 'Cancelable Scenic Booking')], 0, 2),
        cancelRefresh.promise
      ]
    })

    const root = await mountUserProfile()
    await clickProfileTab(root, 'Scenic bookings')

    expect(root.textContent).toContain('Cancelable Scenic Booking')

    await clickButtonByText(root, 'Cancel booking', 2)

    const busyLoadMore = findLoadMoreButton(root)
    expect(busyLoadMore).toBeTruthy()
    expect(busyLoadMore?.disabled).toBe(true)
    expect(busyLoadMore?.getAttribute('aria-busy')).toBe('true')

    busyLoadMore!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await settleVue(2)

    const scenicBookingRequests = testState.apiGet.mock.calls.filter(([url]) =>
      url === endpointsMock.bookings.my
    )
    expect(scenicBookingRequests).toHaveLength(2)

    cancelRefresh.resolve(pageResponse([scenicBooking(2, 'Fresh Cancel Result')], 0, 2))
    await settleVue()

    const renderedText = root.textContent || ''
    const readyLoadMore = findLoadMoreButton(root)
    expect(renderedText).toContain('Fresh Cancel Result')
    expect(renderedText).not.toContain('Cancelable Scenic Booking')
    expect(readyLoadMore?.disabled).toBe(false)
    expect(readyLoadMore?.getAttribute('aria-busy')).toBe('false')
  })

  it('refreshes scenic bookings after delete so pagination is clamped and backfilled', async () => {
    const authoritativeRefresh = createDeferred<ReturnType<typeof pageResponse>>()
    const deletedBooking = {
      ...scenicBooking(10, 'Deleted Scenic Booking'),
      status: 'CANCELLED'
    }

    installProfileGetMock({
      [endpointsMock.bookings.my]: [
        pageResponse([deletedBooking, scenicBooking(11, 'Visible Scenic Booking')], 2, 3, 41),
        authoritativeRefresh.promise
      ]
    })

    const root = await mountUserProfile()
    await clickProfileTab(root, 'Scenic bookings')

    expect(root.textContent).toContain('Deleted Scenic Booking')
    expect(root.textContent).toContain('3 / 3')

    await clickButtonByText(root, 'Delete booking', 2)

    let renderedText = root.textContent || ''
    expect(renderedText).not.toContain('3 / 2')

    authoritativeRefresh.resolve(pageResponse([
      scenicBooking(12, 'Backfilled Scenic Booking')
    ], 0, 2, 40))
    await settleVue()

    renderedText = root.textContent || ''
    expect(renderedText).toContain('Backfilled Scenic Booking')
    expect(renderedText).not.toContain('Deleted Scenic Booking')
    expect(renderedText).toContain('1 / 2')
  })

  it('ignores a stale hotel booking append after a full refresh supersedes it and restores loadingMore', async () => {
    const appendLoad = createDeferred<ReturnType<typeof pageResponse>>()
    const refreshLoad = createDeferred<ReturnType<typeof pageResponse>>()

    installProfileGetMock({
      [endpointsMock.hotelBookings.my]: [
        pageResponse([hotelBooking(1, 'Initial Hotel Booking')], 0, 2),
        appendLoad.promise,
        refreshLoad.promise
      ]
    })

    const root = await mountUserProfile()
    await clickProfileTab(root, 'Hotel bookings')

    expect(root.textContent).toContain('Initial Hotel Booking')

    const initialLoadMore = findLoadMoreButton(root)
    expect(initialLoadMore).toBeTruthy()

    initialLoadMore!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await settleVue(2)

    const busyLoadMore = findLoadMoreButton(root)
    expect(busyLoadMore?.disabled).toBe(true)
    expect(busyLoadMore?.getAttribute('aria-busy')).toBe('true')

    await clickButtonByText(root, 'Cancel booking', 4)

    refreshLoad.resolve(pageResponse([hotelBooking(2, 'Fresh Hotel Booking')], 0, 2))
    await settleVue()

    const refreshedLoadMore = findLoadMoreButton(root)
    expect(root.textContent).toContain('Fresh Hotel Booking')
    expect(refreshedLoadMore?.disabled).toBe(false)
    expect(refreshedLoadMore?.getAttribute('aria-busy')).toBe('false')

    appendLoad.resolve(pageResponse([hotelBooking(3, 'Stale Hotel Append')], 1, 2))
    await settleVue()

    const renderedText = root.textContent || ''
    const finalLoadMore = findLoadMoreButton(root)
    expect(renderedText).toContain('Fresh Hotel Booking')
    expect(renderedText).not.toContain('Stale Hotel Append')
    expect(finalLoadMore?.disabled).toBe(false)
    expect(finalLoadMore?.getAttribute('aria-busy')).toBe('false')
  })

  it('ignores stale comments refresh and append responses after newer comment pages win', async () => {
    const oldInitialRefresh = createDeferred<ReturnType<typeof commentsResponse>>()
    const newerRefresh = createDeferred<ReturnType<typeof commentsResponse>>()
    const staleAppend = createDeferred<ReturnType<typeof commentsResponse>>()
    const latestRefresh = createDeferred<ReturnType<typeof commentsResponse>>()

    installProfileGetMock({
      [endpointsMock.auth.meComments]: [
        oldInitialRefresh.promise,
        newerRefresh.promise,
        staleAppend.promise,
        latestRefresh.promise
      ]
    })

    const root = await mountUserProfile()

    await submitPasswordChange(root)

    newerRefresh.resolve(commentsResponse([
      spotComment(1, 'Fresh Comment Spot', 'Fresh spot comment')
    ], [
      routeComment(2, 'Fresh Route', 'Fresh route comment')
    ], 0, 2))
    await settleVue()

    oldInitialRefresh.resolve(commentsResponse([
      spotComment(3, 'Stale Comment Spot', 'Stale spot comment')
    ], [
      routeComment(4, 'Stale Route', 'Stale route comment')
    ], 0, 2))
    await settleVue()

    await clickProfileTab(root, 'Comments')

    let renderedText = root.textContent || ''
    expect(renderedText).toContain('Fresh Comment Spot')
    expect(renderedText).toContain('Fresh Route')
    expect(renderedText).not.toContain('Stale Comment Spot')
    expect(renderedText).not.toContain('Stale Route')

    const commentsLoadMore = findLoadMoreButton(root)
    expect(commentsLoadMore).toBeTruthy()

    commentsLoadMore!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await settleVue(2)

    await submitPasswordChange(root)

    latestRefresh.resolve(commentsResponse([
      spotComment(5, 'Latest Comment Spot', 'Latest spot comment')
    ], [
      routeComment(6, 'Latest Route', 'Latest route comment')
    ], 0, 2))
    await settleVue()

    staleAppend.resolve(commentsResponse([
      spotComment(7, 'Stale Append Spot', 'Stale appended spot comment')
    ], [
      routeComment(8, 'Stale Append Route', 'Stale appended route comment')
    ], 1, 2))
    await settleVue()

    renderedText = root.textContent || ''
    expect(renderedText).toContain('Latest Comment Spot')
    expect(renderedText).toContain('Latest Route')
    expect(renderedText).not.toContain('Stale Append Spot')
    expect(renderedText).not.toContain('Stale Append Route')
    expect(renderedText).not.toContain('Stale Comment Spot')
    expect(renderedText).not.toContain('Stale Route')
  })
})

const findAlert = (root: ParentNode) =>
  root.querySelector<HTMLElement>('[role="alert"]')

describe('UserProfile list failure error UX', () => {
  it('shows an accessible scenic bookings error with a working retry when the latest request fails', async () => {
    const failingLoad = createDeferred<ReturnType<typeof pageResponse>>()

    installProfileGetMock({
      [endpointsMock.bookings.my]: [
        failingLoad.promise,
        pageResponse([scenicBooking(1, 'Recovered Scenic Booking')])
      ]
    })

    const root = await mountUserProfile()
    failingLoad.reject(new Error('scenic bookings boom'))
    await settleVue()

    await clickProfileTab(root, 'Scenic bookings')

    const alert = findAlert(root)
    expect(alert).toBeTruthy()
    expect(alert?.textContent).toContain('Could not load scenic bookings')
    expect(alert?.getAttribute('aria-live')).toBe('assertive')
    expect(root.textContent).not.toContain('No scenic bookings')

    await clickButtonByText(root, 'Retry', 6)

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('Recovered Scenic Booking')
    expect(renderedText).not.toContain('Could not load scenic bookings')
    expect(findAlert(root)).toBeFalsy()
  })

  it('ignores a stale failed scenic bookings refresh after a newer refresh succeeds', async () => {
    const olderRefresh = createDeferred<ReturnType<typeof pageResponse>>()
    const newerRefresh = createDeferred<ReturnType<typeof pageResponse>>()

    installProfileGetMock({
      [endpointsMock.bookings.my]: [
        pageResponse([scenicBooking(1, 'Initial Scenic Booking')]),
        olderRefresh.promise,
        newerRefresh.promise
      ]
    })

    const root = await mountUserProfile()
    await clickProfileTab(root, 'Scenic bookings')

    expect(root.textContent).toContain('Initial Scenic Booking')

    await clickButtonByText(root, 'Cancel booking', 4)
    await clickButtonByText(root, 'Cancel booking', 4)

    newerRefresh.resolve(pageResponse([scenicBooking(2, 'Fresh Scenic Booking')]))
    await settleVue()

    olderRefresh.reject(new Error('stale scenic failure'))
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('Fresh Scenic Booking')
    expect(renderedText).not.toContain('Could not load scenic bookings')
    expect(findAlert(root)).toBeFalsy()
  })

  it('shows an accessible hotel bookings error when the latest request fails', async () => {
    const failingLoad = createDeferred<ReturnType<typeof pageResponse>>()

    installProfileGetMock({
      [endpointsMock.hotelBookings.my]: [failingLoad.promise]
    })

    const root = await mountUserProfile()
    failingLoad.reject(new Error('hotel bookings boom'))
    await settleVue()

    await clickProfileTab(root, 'Hotel bookings')

    const alert = findAlert(root)
    expect(alert).toBeTruthy()
    expect(alert?.textContent).toContain('Could not load hotel bookings')
    expect(root.textContent).not.toContain('No hotel bookings')
    expect(findButtonByText(root, 'Retry')).toBeTruthy()
  })

  it('shows an accessible comments error when the latest request fails', async () => {
    const failingLoad = createDeferred<ReturnType<typeof commentsResponse>>()

    installProfileGetMock({
      [endpointsMock.auth.meComments]: [failingLoad.promise]
    })

    const root = await mountUserProfile()
    failingLoad.reject(new Error('comments boom'))
    await settleVue()

    await clickProfileTab(root, 'Comments')

    const alert = findAlert(root)
    expect(alert).toBeTruthy()
    expect(alert?.textContent).toContain('Could not load comments')
    expect(root.textContent).not.toContain('No comments')
    expect(findButtonByText(root, 'Retry')).toBeTruthy()
  })
})
