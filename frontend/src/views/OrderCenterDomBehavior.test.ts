// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, type App } from 'vue'
import { createI18n } from 'vue-i18n'
import OrderCenter from './OrderCenter.vue'

const { apiDelete, apiGet, apiPost, ensureSession, routerPush, showConfirm, showToast } = vi.hoisted(() => ({
  apiDelete: vi.fn(),
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  ensureSession: vi.fn(),
  routerPush: vi.fn(),
  showConfirm: vi.fn(),
  showToast: vi.fn()
}))

const endpointsMock = vi.hoisted(() => ({
  orders: {
    my: '/orders/my',
    detail: (id: number) => `/orders/${id}`,
    cancel: (id: number) => `/orders/${id}/cancel`,
    delete: (id: number) => `/orders/${id}`
  }
}))

vi.mock('../api', () => ({
  default: {
    delete: apiDelete,
    get: apiGet,
    post: apiPost
  },
  endpoints: endpointsMock
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

vi.mock('../stores/auth', () => ({
  useAuthStore: () => ({ ensureSession })
}))

vi.mock('../composables/useConfirm', () => ({
  useConfirm: () => ({ showConfirm })
}))

vi.mock('../composables/useToast', () => ({
  useToast: () => ({ showToast })
}))

vi.mock('../utils/errorMonitoring', () => ({
  safeClientErrorMessage: (_error: unknown, fallback: string) => fallback,
  summarizeClientError: (error: unknown) => error instanceof Error ? error.message : String(error)
}))

vi.mock('lucide-vue-next', async () => {
  const vue = await import('vue')
  const Icon = vue.defineComponent({
    name: 'IconStub',
    setup() {
      return () => vue.h('svg', { 'aria-hidden': 'true' })
    }
  })

  return {
    Ban: Icon,
    CalendarDays: Icon,
    FileText: Icon,
    Hotel: Icon,
    Landmark: Icon,
    PackageCheck: Icon,
    ReceiptText: Icon,
    RefreshCw: Icon,
    Search: Icon,
    Sparkles: Icon,
    TicketCheck: Icon,
    Trash2: Icon,
    X: Icon
  }
})

interface OrderFixture {
  id: number
  orderNo: string
  status: string
  paymentStatus: string
  productSummary: string
  payableAmount: number
  currency: string
  createdAt: string
  items: Array<{
    id: number
    productType: string
    productName: string
    skuName?: string
    serviceStartDate: string
    quantity: number
    subtotal: number
  }>
  paymentTransactions: Array<{
    id: number
    provider: string
    amount: number
    status: string
  }>
  vouchers: Array<{
    id: number
    voucherCode: string
    status: string
    validFrom: string
    validUntil: string
  }>
}

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

const orders = {
  pending: createOrder({
    id: 101,
    orderNo: 'CT-ORDER-101',
    productSummary: 'Everest base camp consultation',
    status: 'PENDING_PAYMENT'
  }),
  confirmed: createOrder({
    id: 102,
    orderNo: 'CT-ORDER-102',
    productSummary: 'Namtso lakeside hotel package',
    status: 'CONFIRMED'
  }),
  closed: createOrder({
    id: 103,
    orderNo: 'CT-ORDER-103',
    productSummary: 'Closed archive route',
    status: 'CANCELLED',
    paymentStatus: 'REFUNDED'
  })
}

const createTestI18n = () => createI18n({
  legacy: false,
  locale: 'zh',
  messages: {
    zh: {
      common: {
        cancel: 'Cancel',
        delete: 'Delete',
        loading: 'Loading',
        pendingConfirm: 'Pending',
        refresh: 'Refresh',
        reload: 'Reload',
        retry: 'Retry',
        unknown: 'Unknown'
      },
      community: {
        nextPage: 'Next page'
      },
      orderCenter: {
        actionFailed: 'Action failed',
        browseHotels: 'Browse hotels',
        cancelConsultation: 'Cancel consultation',
        cancelReason: 'Reason',
        cancelReasonPlaceholder: 'Optional reason',
        cancelSubmitted: 'Cancel submitted',
        clearFilters: 'Clear filters',
        closeDetail: 'Close detail',
        collapse: 'Collapse',
        confirmation: {
          failed: 'Failed',
          paid: 'Paid',
          partiallyRefunded: 'Partially refunded',
          refunded: 'Refunded',
          unknown: 'Unknown',
          unpaid: 'Unpaid'
        },
        confirmationStatus: 'Confirmation',
        confirmCancel: 'Confirm cancel',
        continuePlanning: 'Continue planning',
        createItinerary: 'Create itinerary',
        createdAt: 'Created',
        dateRangeSeparator: 'to',
        defaultProductSummary: 'Travel consultation',
        deleteConfirm: 'Delete {orderNo}?',
        deleteFailed: 'Delete failed',
        deleteRecord: 'Delete record',
        deleteSuccess: 'Deleted',
        deleting: 'Deleting',
        detailFallback: 'Details pending',
        detailLoadFailed: 'Detail failed',
        detailLoading: 'Loading detail',
        emptyDescription: 'No orders yet',
        emptyTitle: 'No orders',
        itemTypes: {
          hotelRoom: 'Hotel',
          routePackage: 'Route',
          scenicSpot: 'Spot'
        },
        itemsTitle: 'Items',
        loadErrorTitle: 'Load failed',
        loadFailed: 'Load failed',
        loading: 'Loading orders',
        noFilteredResults: 'No matching orders',
        noItemDetails: 'No item details',
        noPlatformPaymentBadge: 'No platform payment',
        noThirdPartyConfirmations: 'No third party confirmations',
        noVouchers: 'No vouchers',
        orderCardAria: 'Open {summary}',
        paginationLabel: 'Order pages',
        pendingDate: 'Pending date',
        platformDisclaimer: 'Payment is handled offline.',
        processing: 'Processing',
        quantity: 'x {count}',
        referencePrice: 'Reference price',
        reloadDetail: 'Reload detail',
        searchPlaceholder: 'Search orders',
        standardConsultation: 'Standard consultation',
        stats: {
          all: 'All',
          allHint: 'All orders',
          archive: 'Archive',
          archiveHint: 'Confirmed and closed',
          confirmed: 'Confirmed',
          confirmedHint: 'Ready',
          pending: 'Pending',
          pendingHint: 'Needs action'
        },
        status: {
          cancelled: 'Cancelled',
          confirmed: 'Confirmed',
          expired: 'Expired',
          paid: 'Paid',
          pendingPayment: 'Pending payment',
          refunded: 'Refunded',
          refundPending: 'Refund pending',
          unknown: 'Unknown'
        },
        subtitle: 'Track consultation orders',
        tabs: {
          afterSale: 'After sale',
          all: 'All',
          closed: 'Closed',
          confirmed: 'Confirmed',
          pending: 'Pending'
        },
        thirdPartyCount: '{count} confirmations',
        thirdPartyDetails: 'Confirmation details',
        thirdPartyTitle: 'Third party',
        title: 'Order center',
        transactionStatus: {
          failed: 'Failed',
          pending: 'Pending',
          refunded: 'Refunded',
          success: 'Success'
        },
        voucherCount: '{count} vouchers',
        voucherDetails: 'Voucher details',
        vouchersTitle: 'Vouchers',
        voucherStatus: {
          cancelled: 'Cancelled',
          consumed: 'Consumed',
          issued: 'Issued'
        }
      }
    }
  }
})

const mountOrderCenter = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(OrderCenter)
  app.component('router-link', RouterLinkStub)
  app.use(createTestI18n())
  mountedApps.push(app)
  app.mount(root)
  await settleVue()

  return root
}

const settleVue = async (turns = 8) => {
  for (let index = 0; index < turns; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const pageResponse = (content: OrderFixture[], page = 0, totalPages = 1) => ({
  data: {
    content,
    page,
    size: 20,
    totalElements: Math.max(content.length, totalPages * 20),
    totalPages
  }
})

const mockOrdersPage = (content: OrderFixture[], totalPages = 1) => {
  apiGet.mockImplementation((url: string, config?: { params?: { page?: number } }) => {
    if (url === '/orders/my') {
      return Promise.resolve(pageResponse(content, Number(config?.params?.page ?? 0), totalPages))
    }

    const detailMatch = String(url).match(/^\/orders\/(\d+)$/)
    if (detailMatch) {
      const order = content.find(candidate => candidate.id === Number(detailMatch[1]))
      return order
        ? Promise.resolve({ data: order })
        : Promise.reject(new Error(`Unknown order ${detailMatch[1]}`))
    }

    return Promise.reject(new Error(`Unexpected GET ${url}`))
  })
}

const getOrderCard = (summary: string) => {
  const card = Array.from(document.querySelectorAll<HTMLElement>('article[role="button"]'))
    .find(element => element.getAttribute('aria-label') === `Open ${summary}`)

  expect(card).toBeTruthy()
  return card!
}

const dispatchKey = (target: Element, key: string) => {
  target.dispatchEvent(new KeyboardEvent('keydown', {
    bubbles: true,
    cancelable: true,
    key
  }))
}

const findButtonByText = (root: ParentNode, text: string) => {
  const button = Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
    .find(candidate => candidate.textContent?.includes(text))

  expect(button).toBeTruthy()
  return button!
}

const clickButtonByText = (root: ParentNode, text: string) => {
  const button = findButtonByText(root, text)

  button.dispatchEvent(new MouseEvent('click', { bubbles: true }))
  return button
}

const setSearchValue = async (root: ParentNode, value: string) => {
  const input = root.querySelector<HTMLInputElement>('input[type="search"]')

  expect(input).toBeTruthy()
  input!.value = value
  input!.dispatchEvent(new Event('input', { bubbles: true }))
  await settleVue()
  return input!
}

const deferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, reject, resolve }
}

beforeEach(() => {
  apiDelete.mockReset()
  apiGet.mockReset()
  apiPost.mockReset()
  ensureSession.mockReset().mockResolvedValue(true)
  routerPush.mockReset()
  showConfirm.mockReset()
  showToast.mockReset()
})

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

describe('OrderCenter DOM behavior', () => {
  it('announces initial loading and exposes selected status filter state', async () => {
    const ordersRequest = deferred<ReturnType<typeof pageResponse>>()
    apiGet.mockImplementation((url: string) => {
      if (url === '/orders/my') return ordersRequest.promise
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountOrderCenter()
    const loadingStatus = root.querySelector<HTMLElement>('[role="status"][aria-busy="true"]')
    const allFilter = findButtonByText(root, 'All')
    const pendingFilter = findButtonByText(root, 'Pending')

    expect(loadingStatus).not.toBeNull()
    expect(loadingStatus?.getAttribute('aria-live')).toBe('polite')
    expect(loadingStatus?.textContent).toContain('Loading orders')
    expect(allFilter.getAttribute('aria-pressed')).toBe('true')
    expect(pendingFilter.getAttribute('aria-pressed')).toBe('false')

    ordersRequest.resolve(pageResponse([orders.pending]))
    await settleVue()
  })

  it('selects order cards with Enter and Space and exposes the detail panel relationship', async () => {
    mockOrdersPage([orders.pending, orders.confirmed])
    await mountOrderCenter()

    const pendingCard = getOrderCard(orders.pending.productSummary)
    const confirmedCard = getOrderCard(orders.confirmed.productSummary)

    expect(document.getElementById('order-detail-panel')).toBeNull()

    dispatchKey(pendingCard, 'Enter')
    await settleVue()

    let detailPanel = document.getElementById('order-detail-panel')
    expect(detailPanel).not.toBeNull()
    expect(detailPanel?.textContent).toContain(orders.pending.orderNo)
    expect(pendingCard.getAttribute('aria-controls')).toBe('order-detail-panel')
    expect(pendingCard.getAttribute('aria-expanded')).toBe('true')
    expect(confirmedCard.getAttribute('aria-expanded')).toBe('false')

    dispatchKey(confirmedCard, 'Space')
    await settleVue()

    detailPanel = document.getElementById('order-detail-panel')
    expect(detailPanel).not.toBeNull()
    expect(detailPanel?.textContent).toContain(orders.confirmed.orderNo)
    expect(confirmedCard.getAttribute('aria-controls')).toBe('order-detail-panel')
    expect(confirmedCard.getAttribute('aria-expanded')).toBe('true')
    expect(pendingCard.getAttribute('aria-expanded')).toBe('false')
  })

  it('clears active filters and restores the full order list', async () => {
    mockOrdersPage([orders.pending, orders.confirmed, orders.closed])
    const root = await mountOrderCenter()

    clickButtonByText(root, 'Pending')
    await setSearchValue(root, 'not-present')

    expect(findButtonByText(root, 'Pending').getAttribute('aria-pressed')).toBe('true')
    expect(root.textContent).toContain('No matching orders')
    expect(root.textContent).not.toContain(orders.pending.productSummary)

    clickButtonByText(root, 'Clear filters')
    await settleVue()

    const search = root.querySelector<HTMLInputElement>('input[type="search"]')
    expect(findButtonByText(root, 'All').getAttribute('aria-pressed')).toBe('true')
    expect(search?.value).toBe('')
    expect(root.textContent).toContain(orders.pending.productSummary)
    expect(root.textContent).toContain(orders.confirmed.productSummary)
    expect(root.textContent).toContain(orders.closed.productSummary)
  })

  it('marks the next-page button busy and disabled while appending another page', async () => {
    let resolveNextPage: ((value: ReturnType<typeof pageResponse>) => void) | undefined
    apiGet.mockImplementation((url: string, config?: { params?: { page?: number } }) => {
      if (url !== '/orders/my') return Promise.reject(new Error(`Unexpected GET ${url}`))

      const page = Number(config?.params?.page ?? 0)
      if (page === 0) {
        return Promise.resolve(pageResponse([orders.pending], 0, 2))
      }

      return new Promise(resolve => {
        resolveNextPage = resolve
      })
    })

    const root = await mountOrderCenter()
    const nextPageButton = clickButtonByText(root, 'Next page')
    await nextTick()

    expect(nextPageButton.disabled).toBe(true)
    expect(nextPageButton.getAttribute('aria-busy')).toBe('true')

    resolveNextPage?.(pageResponse([orders.confirmed], 1, 2))
    await settleVue()

    expect(root.textContent).toContain(orders.confirmed.productSummary)
    expect(nextPageButton.getAttribute('aria-busy')).toBe('false')
  })

  it('keeps the newest refreshed list when an older refresh completes later', async () => {
    const staleRefresh = deferred<ReturnType<typeof pageResponse>>()
    const currentRefresh = deferred<ReturnType<typeof pageResponse>>()
    let listRequests = 0
    apiGet.mockImplementation((url: string) => {
      if (url === '/orders/my') {
        listRequests += 1
        if (listRequests === 1) return Promise.resolve(pageResponse([orders.pending]))
        if (listRequests === 2) return staleRefresh.promise
        if (listRequests === 3) return currentRefresh.promise
      }

      const detailMatch = String(url).match(/^\/orders\/(\d+)$/)
      if (detailMatch) {
        const order = Object.values(orders).find(candidate => candidate.id === Number(detailMatch[1]))
        return order
          ? Promise.resolve({ data: order })
          : Promise.reject(new Error(`Unknown order ${detailMatch[1]}`))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountOrderCenter()
    getOrderCard(orders.pending.productSummary).dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue()

    expect(document.getElementById('order-detail-panel')?.textContent).toContain(orders.pending.orderNo)

    clickButtonByText(root, 'Refresh')
    await nextTick()
    expect(findButtonByText(root, 'Refresh').disabled).toBe(true)

    clickButtonByText(root, 'Refresh')
    await nextTick()
    expect(listRequests).toBe(3)

    currentRefresh.resolve(pageResponse([orders.confirmed]))
    await settleVue()

    expect(root.textContent).toContain(orders.confirmed.productSummary)
    expect(root.textContent).not.toContain(orders.pending.productSummary)
    expect(document.getElementById('order-detail-panel')).toBeNull()
    expect(findButtonByText(root, 'Refresh').disabled).toBe(false)

    staleRefresh.resolve(pageResponse([orders.pending]))
    await settleVue()

    expect(root.textContent).toContain(orders.confirmed.productSummary)
    expect(root.textContent).not.toContain(orders.pending.productSummary)
    expect(document.getElementById('order-detail-panel')).toBeNull()
    expect(root.textContent).not.toContain('Load failed')
  })

  it('does not restore a stale preferred order after the user selects another card during refresh', async () => {
    const refreshLoad = deferred<ReturnType<typeof pageResponse>>()
    let listRequests = 0
    apiGet.mockImplementation((url: string) => {
      if (url === '/orders/my') {
        listRequests += 1
        if (listRequests === 1) return Promise.resolve(pageResponse([orders.pending, orders.confirmed]))
        return refreshLoad.promise
      }

      const detailMatch = String(url).match(/^\/orders\/(\d+)$/)
      if (detailMatch) {
        const order = Object.values(orders).find(candidate => candidate.id === Number(detailMatch[1]))
        return order
          ? Promise.resolve({ data: order })
          : Promise.reject(new Error(`Unknown order ${detailMatch[1]}`))
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const root = await mountOrderCenter()
    getOrderCard(orders.pending.productSummary).dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue()

    expect(document.getElementById('order-detail-panel')?.textContent).toContain(orders.pending.orderNo)

    clickButtonByText(root, 'Refresh')
    await settleVue(2)
    getOrderCard(orders.confirmed.productSummary).dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await settleVue()

    expect(document.getElementById('order-detail-panel')?.textContent).toContain(orders.confirmed.orderNo)

    refreshLoad.resolve(pageResponse([orders.pending, orders.confirmed]))
    await settleVue()

    expect(document.getElementById('order-detail-panel')?.textContent).toContain(orders.confirmed.orderNo)
    expect(getOrderCard(orders.pending.productSummary).getAttribute('aria-expanded')).toBe('false')
    expect(getOrderCard(orders.confirmed.productSummary).getAttribute('aria-expanded')).toBe('true')
  })

  it('ignores stale append responses after a full order refresh supersedes them', async () => {
    const appendLoad = deferred<ReturnType<typeof pageResponse>>()
    const refreshLoad = deferred<ReturnType<typeof pageResponse>>()
    const refreshedOrder = createOrder({
      id: 104,
      orderNo: 'CT-ORDER-104',
      productSummary: 'Fresh Kailash consultation'
    })
    const staleAppendOrder = createOrder({
      id: 105,
      orderNo: 'CT-ORDER-105',
      productSummary: 'Stale append consultation'
    })

    apiGet.mockImplementation((url: string, config?: { params?: { page?: number } }) => {
      if (url !== '/orders/my') return Promise.reject(new Error(`Unexpected GET ${url}`))

      const page = Number(config?.params?.page ?? 0)
      if (page === 0 && apiGet.mock.calls.filter(call => call[0] === '/orders/my').length === 1) {
        return Promise.resolve(pageResponse([orders.pending], 0, 2))
      }
      if (page === 1) {
        return appendLoad.promise
      }

      return refreshLoad.promise
    })

    const root = await mountOrderCenter()
    clickButtonByText(root, 'Next page')
    await settleVue(2)

    clickButtonByText(root, 'Refresh')
    await settleVue(2)

    refreshLoad.resolve(pageResponse([refreshedOrder], 0, 2))
    await settleVue()

    appendLoad.resolve(pageResponse([staleAppendOrder], 1, 2))
    await settleVue()

    expect(root.textContent).toContain(refreshedOrder.productSummary)
    expect(root.textContent).not.toContain(staleAppendOrder.productSummary)
    expect(root.textContent).toContain('1 / 2')
    expect(root.textContent).not.toContain('2 / 2')
    expect(root.querySelector('[aria-busy="true"]')).toBeNull()

    const currentNextPageButton = findButtonByText(root, 'Next page')
    expect(currentNextPageButton.disabled).toBe(false)
    expect(currentNextPageButton.getAttribute('aria-busy')).toBe('false')
  })
})

function createOrder(overrides: Partial<OrderFixture>): OrderFixture {
  const id = overrides.id ?? 1

  return {
    id,
    orderNo: overrides.orderNo ?? `CT-ORDER-${id}`,
    status: overrides.status ?? 'CONFIRMED',
    paymentStatus: overrides.paymentStatus ?? 'PAID',
    productSummary: overrides.productSummary ?? `Order ${id}`,
    payableAmount: overrides.payableAmount ?? 1200,
    currency: overrides.currency ?? 'CNY',
    createdAt: overrides.createdAt ?? '2026-06-08T08:30:00Z',
    items: overrides.items ?? [{
      id: id * 10,
      productType: 'ROUTE_PACKAGE',
      productName: `${overrides.productSummary ?? `Order ${id}`} item`,
      skuName: 'Standard',
      serviceStartDate: '2026-06-20',
      quantity: 1,
      subtotal: overrides.payableAmount ?? 1200
    }],
    paymentTransactions: overrides.paymentTransactions ?? [{
      id: id * 100,
      provider: 'Offline partner',
      amount: overrides.payableAmount ?? 1200,
      status: 'SUCCESS'
    }],
    vouchers: overrides.vouchers ?? [{
      id: id * 1000,
      voucherCode: `VOUCHER-${id}`,
      status: 'ISSUED',
      validFrom: '2026-06-20',
      validUntil: '2026-06-25'
    }]
  }
}
