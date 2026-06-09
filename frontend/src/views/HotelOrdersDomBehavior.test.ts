// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, type App } from 'vue'
import { createI18n } from 'vue-i18n'

const testState = vi.hoisted(() => ({
  apiGet: vi.fn(),
  clearHotelOrderClientStorage: vi.fn(),
  route: null as null | {
    path: string
    query: Record<string, unknown>
  },
  routerReplace: vi.fn()
}))

vi.mock('vue-router', async () => {
  const vue = await import('vue')

  testState.route ??= vue.reactive({
    path: '/hotel-orders',
    query: {}
  })

  return {
    useRoute: () => testState.route,
    useRouter: () => ({
      replace: testState.routerReplace
    })
  }
})

vi.mock('../api', () => ({
  default: {
    get: testState.apiGet
  },
  endpoints: {
    hotelBookings: {
      my: '/hotel-bookings/my'
    }
  }
}))

vi.mock('../api/cache', () => ({
  clearHotelOrderClientStorage: testState.clearHotelOrderClientStorage
}))

vi.mock('../data/hotelImages', () => ({
  applyHotelImageFallback: vi.fn(),
  resolveHotelBookingImage: () => '/hotel-cover.jpg'
}))

vi.mock('../motion/presets', () => ({
  cardInitial: {},
  cardInView: {},
  cardTransition: () => ({}),
  inViewOnce: {},
  revealInitial: {},
  revealInView: {},
  revealTransition: {}
}))

vi.mock('../utils/errorMonitoring', () => ({
  safeClientErrorMessage: (_error: unknown, fallback: string) => fallback
}))

vi.mock('motion-v', async () => {
  const vue = await import('vue')

  const passthrough = (tag = 'div') => vue.defineComponent({
    inheritAttrs: false,
    setup(_, { attrs, slots }) {
      return () => {
        const {
          animate,
          inViewOptions,
          initial,
          transition,
          whileInView,
          ...domAttrs
        } = attrs

        return vue.h(tag, domAttrs, slots.default?.())
      }
    }
  })

  return {
    motion: {
      div: passthrough('div')
    }
  }
})

import HotelOrders from './HotelOrders.vue'

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
    return () => h('a', {
      href: typeof props.to === 'string' ? props.to : '#'
    }, slots.default?.())
  }
})

const createTestI18n = () => createI18n({
  legacy: false,
  locale: 'zh',
  messages: {
    zh: {
      common: {
        close: 'Close',
        loading: 'Loading',
        reload: 'Reload'
      },
      community: {
        nextPage: 'Next page'
      },
      hotel: {
        booker: 'Booker',
        bookingFailed: 'Hotel orders failed to load',
        bookNow: 'Book a hotel',
        dateConnector: 'to',
        guests: ' guests',
        noOrders: 'No hotel orders',
        ordersEmptyHint: 'Return to hotels to make a booking.',
        ordersLoading: 'Loading hotel orders',
        ordersSubtitle: 'Track your hotel bookings.',
        ordersTitle: 'Hotel orders',
        status: {
          cancelled: 'Cancelled',
          confirmed: 'Confirmed',
          pending: 'Pending'
        }
      }
    }
  }
})

const settleVue = async (turns = 8) => {
  for (let index = 0; index < turns; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const hotelOrdersPage = (content: unknown[], page = 0, totalPages = content.length > 0 ? 1 : 0) => ({
  data: {
    content,
    page,
    size: 20,
    totalElements: Math.max(content.length, totalPages * 20),
    totalPages
  }
})

const deferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, reject, resolve }
}

const mountHotelOrders = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(HotelOrders)
  app.component('router-link', RouterLinkStub)
  app.use(createTestI18n())
  mountedApps.push(app)
  app.mount(root)
  await settleVue()

  return root
}

const findBookingConfirmation = (root: ParentNode) =>
  Array.from(root.querySelectorAll<HTMLElement>('[role="status"]'))
    .find(status => status.querySelector('button[aria-label="Close"]')) ?? null

const findLoadMoreButton = (root: ParentNode) =>
  Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find(button =>
    button.textContent?.includes('Next page') || button.textContent?.includes('Loading')
  ) ?? null

const hotelOrderFixture = {
  id: 'hotel-order-42',
  hotelId: 9,
  hotel: {
    id: 9,
    name: 'Lhasa Summit Hotel',
    coverImage: '/hotel-cover.jpg',
    imageUrl: '/hotel-cover.jpg'
  },
  hotelName: 'Fallback hotel name',
  roomId: 3,
  roomName: 'Panorama suite',
  checkInDate: '2026-06-20',
  checkOutDate: '2026-06-23',
  guests: 2,
  guestName: 'Traveler Chen',
  phone: '13812345678',
  note: '',
  nights: 3,
  subtotal: 1888,
  discount: 0,
  totalPrice: 1888,
  status: 'PENDING',
  createdAt: '2026-06-08T09:30:00'
}

beforeEach(() => {
  testState.apiGet.mockReset()
  testState.clearHotelOrderClientStorage.mockReset()
  testState.routerReplace.mockReset()
  testState.route!.path = '/hotel-orders'
  testState.route!.query = {}
  testState.routerReplace.mockImplementation((location: { path?: string, query?: Record<string, unknown> }) => {
    testState.route!.path = location.path ?? testState.route!.path
    testState.route!.query = location.query ?? {}
    return Promise.resolve()
  })
  testState.apiGet.mockResolvedValue(hotelOrdersPage([]))
})

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

describe('HotelOrders DOM behavior', () => {
  it('shows and dismisses the created booking confirmation from route query', async () => {
    testState.route!.query = {
      created: '1',
      ref: 'checkout'
    }

    const root = await mountHotelOrders()
    const confirmation = findBookingConfirmation(root)

    expect(confirmation).not.toBeNull()
    expect(confirmation?.getAttribute('aria-live')).toBe('polite')
    expect(confirmation?.textContent).toContain('Hotel orders')
    expect(confirmation?.textContent).toContain('Pending')

    confirmation?.querySelector<HTMLButtonElement>('button[aria-label="Close"]')?.click()
    await settleVue()

    expect(testState.routerReplace).toHaveBeenCalledWith({
      path: '/hotel-orders',
      query: {
        ref: 'checkout'
      }
    })
    expect(testState.route!.query).toEqual({
      ref: 'checkout'
    })
    expect(findBookingConfirmation(root)).toBeNull()
  })

  it('renders an accessible alert when hotel orders fail to load', async () => {
    testState.apiGet.mockRejectedValueOnce(new Error('network down'))

    const root = await mountHotelOrders()
    const alert = root.querySelector<HTMLElement>('[role="alert"]')

    expect(alert).not.toBeNull()
    expect(alert?.getAttribute('aria-live')).toBe('assertive')
    expect(alert?.textContent).toContain('Hotel orders failed to load')
  })

  it('shows an empty state with a booking entry point back to hotels', async () => {
    const root = await mountHotelOrders()
    const bookingLink = root.querySelector<HTMLAnchorElement>('a[href="/hotels"]')

    expect(root.textContent).toContain('No hotel orders')
    expect(root.textContent).toContain('Return to hotels to make a booking.')
    expect(bookingLink).not.toBeNull()
    expect(bookingLink?.textContent).toContain('Book a hotel')
  })

  it('renders masked guest phone numbers instead of the full raw phone', async () => {
    testState.apiGet.mockResolvedValueOnce(hotelOrdersPage([hotelOrderFixture]))

    const root = await mountHotelOrders()
    const renderedText = root.textContent || ''

    expect(renderedText).toContain('Lhasa Summit Hotel')
    expect(renderedText).toContain('Traveler Chen')
    expect(renderedText).toContain('*******5678')
    expect(renderedText).not.toContain('13812345678')
  })

  it('ignores stale refresh responses that return after a newer hotel order load', async () => {
    const firstLoad = deferred<ReturnType<typeof hotelOrdersPage>>()
    const secondLoad = deferred<ReturnType<typeof hotelOrdersPage>>()
    testState.apiGet
      .mockReturnValueOnce(firstLoad.promise)
      .mockReturnValueOnce(secondLoad.promise)

    const root = await mountHotelOrders()

    window.dispatchEvent(new Event('hotel-orders-updated'))
    await settleVue(2)

    secondLoad.resolve(hotelOrdersPage([
      {
        ...hotelOrderFixture,
        id: 'new-order',
        hotel: { ...hotelOrderFixture.hotel, name: 'Fresh Lhasa Hotel' },
        hotelName: 'Fresh Lhasa Hotel'
      }
    ]))
    await settleVue()

    firstLoad.resolve(hotelOrdersPage([
      {
        ...hotelOrderFixture,
        id: 'stale-order',
        hotel: { ...hotelOrderFixture.hotel, name: 'Stale Lhasa Hotel' },
        hotelName: 'Stale Lhasa Hotel'
      }
    ]))
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('Fresh Lhasa Hotel')
    expect(renderedText).not.toContain('Stale Lhasa Hotel')
  })

  it('ignores stale refresh errors after a newer hotel order load succeeds', async () => {
    const firstLoad = deferred<ReturnType<typeof hotelOrdersPage>>()
    const secondLoad = deferred<ReturnType<typeof hotelOrdersPage>>()
    testState.apiGet
      .mockReturnValueOnce(firstLoad.promise)
      .mockReturnValueOnce(secondLoad.promise)

    const root = await mountHotelOrders()

    window.dispatchEvent(new Event('hotel-orders-updated'))
    await settleVue(2)

    secondLoad.resolve(hotelOrdersPage([
      {
        ...hotelOrderFixture,
        id: 'fresh-after-stale-error',
        hotel: { ...hotelOrderFixture.hotel, name: 'Fresh Error Guard Hotel' },
        hotelName: 'Fresh Error Guard Hotel'
      }
    ]))
    await settleVue()

    firstLoad.reject(new Error('late stale failure'))
    await settleVue()

    const renderedText = root.textContent || ''
    expect(renderedText).toContain('Fresh Error Guard Hotel')
    expect(renderedText).not.toContain('Hotel orders failed to load')
    expect(root.querySelector('[role="alert"]')).toBeNull()
  })

  it('ignores stale hotel order append responses after a full refresh supersedes them', async () => {
    const appendLoad = deferred<ReturnType<typeof hotelOrdersPage>>()
    const refreshLoad = deferred<ReturnType<typeof hotelOrdersPage>>()
    testState.apiGet
      .mockResolvedValueOnce(hotelOrdersPage([hotelOrderFixture], 0, 2))
      .mockReturnValueOnce(appendLoad.promise)
      .mockReturnValueOnce(refreshLoad.promise)

    const root = await mountHotelOrders()
    const nextPageButton = findLoadMoreButton(root)

    expect(nextPageButton).toBeTruthy()
    nextPageButton?.click()
    await settleVue(2)

    const busyLoadMoreButton = findLoadMoreButton(root)
    expect(busyLoadMoreButton?.disabled).toBe(true)
    expect(busyLoadMoreButton?.getAttribute('aria-busy')).toBe('true')

    window.dispatchEvent(new Event('hotel-orders-updated'))
    await settleVue(2)

    refreshLoad.resolve(hotelOrdersPage([
      {
        ...hotelOrderFixture,
        id: 'refreshed-order',
        hotel: { ...hotelOrderFixture.hotel, name: 'Refreshed Hotel' },
        hotelName: 'Refreshed Hotel'
      }
    ], 0, 2))
    await settleVue()

    const refreshedLoadMoreButton = findLoadMoreButton(root)
    expect(refreshedLoadMoreButton?.disabled).toBe(false)
    expect(refreshedLoadMoreButton?.getAttribute('aria-busy')).toBe('false')

    appendLoad.resolve(hotelOrdersPage([
      {
        ...hotelOrderFixture,
        id: 'stale-append-order',
        hotel: { ...hotelOrderFixture.hotel, name: 'Stale Append Hotel' },
        hotelName: 'Stale Append Hotel'
      }
    ], 1, 2))
    await settleVue()

    const renderedText = root.textContent || ''
    const finalLoadMoreButton = findLoadMoreButton(root)
    expect(renderedText).toContain('Refreshed Hotel')
    expect(renderedText).not.toContain('Stale Append Hotel')
    expect(finalLoadMoreButton?.disabled).toBe(false)
    expect(finalLoadMoreButton?.getAttribute('aria-busy')).toBe('false')
  })
})
