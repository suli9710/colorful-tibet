// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, nextTick, type App } from 'vue'
import { createI18n } from 'vue-i18n'

const testState = vi.hoisted(() => ({
  apiGet: vi.fn(),
  routerPush: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: testState.routerPush
  })
}))

vi.mock('../api', () => ({
  default: {
    get: testState.apiGet
  },
  endpoints: {
    spots: {
      list: '/spots',
      search: '/spots/search'
    }
  }
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
            drag,
            dragConstraints,
            exit,
            inViewOptions,
            initial,
            layout,
            layoutId,
            transition,
            whileHover,
            whileInView,
            whilePress,
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

  return {
    AnimatePresence: vue.defineComponent({
      inheritAttrs: false,
      setup(_, { slots }) {
        return () => vue.h(vue.Fragment, slots.default?.())
      }
    }),
    motion: new Proxy({}, {
      get: (_target, tag) => passthrough(String(tag))
    })
  }
})

import ScenicSpots from './ScenicSpots.vue'

const mountedApps: App[] = []
const spotsPageSize = 20

const scenicSpotFixtures = [
  {
    id: 1,
    name: 'Yamdrok Lake',
    description: 'Turquoise lake near Nangartse',
    category: 'NATURAL',
    ticketPrice: 0,
    altitude: 4441,
    location: 'Shannan',
    rating: 4.8,
    tags: [{ id: 'lake', tag: 'lake' }],
    visitCount: 1200
  },
  {
    id: 2,
    name: 'Jokhang Temple',
    description: 'Historic temple in Lhasa',
    category: 'CULTURAL',
    ticketPrice: 85,
    altitude: 3650,
    location: 'Lhasa',
    rating: 4.6,
    tags: [{ id: 'temple', tag: 'temple' }],
    visitCount: 980
  }
]

const createTestI18n = () => createI18n({
  fallbackLocale: 'en',
  fallbackWarn: false,
  legacy: false,
  locale: 'en',
  missingWarn: false,
  messages: {
    en: {
      common: {
        freeTicket: 'Free',
        loading: 'Loading',
        pendingConfirm: 'Pending',
        priceCny: 'CNY {price}',
        search: 'Search'
      },
      community: {
        nextPage: 'Next page'
      },
      spotDetail: {
        noDescription: 'No description'
      },
      spots: {
        altitude: 'Altitude',
        cardAria: 'View {name} details',
        category: {
          all: 'All spots',
          cultural: 'Cultural heritage',
          natural: 'Natural scenery'
        },
        clearSearch: 'Clear scenic spot search',
        filterByCategory: 'Filter by {category}',
        loadErrorTitle: 'Unable to load scenic spots',
        loadingLabel: 'Loading scenic spots',
        location: 'Location',
        noCategoryMessage: 'No scenic spots match this category.',
        noCategoryTitle: 'No spots found',
        noDataMessage: 'No scenic spots are available.',
        noDataTitle: 'No scenic spot data',
        noSearchMessage: 'No scenic spots match this search.',
        paginationLabel: 'Scenic spot pagination',
        rating: 'Rating',
        ratingValue: '{rating} rating',
        reload: 'Reload',
        resultSummary: '{category}: {count} spots',
        resultSummaryWithKeyword: '{base} for "{keyword}"',
        retryLoad: 'Retry scenic spots',
        searchHelp: 'Search by name, location, or tag. Press Escape to clear.',
        searchPlaceholder: 'Search scenic spots, areas, or tags',
        showAll: 'Show all scenic spots',
        subtitle: 'Explore Tibet scenic spots',
        ticketFrom: 'Tickets from',
        title: 'Scenic spots',
        viewDetails: 'View details',
        visitCount: 'Visits',
        visitCountValue: '{count} visits'
      },
      toast: {
        pageLoadFailed: 'Page failed to load'
      }
    }
  }
})

const paginatedSpots = (
  content: unknown[],
  page = 0,
  totalElements = content.length,
  totalPages = totalElements > 0 ? Math.ceil(totalElements / spotsPageSize) : 0
) => ({
  data: {
    content,
    page,
    size: spotsPageSize,
    totalElements,
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

const settleVue = async (turns = 10) => {
  for (let index = 0; index < turns; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const mountScenicSpots = () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(ScenicSpots)
  app.use(createTestI18n())
  mountedApps.push(app)
  app.mount(root)

  return root
}

const mountLoadedScenicSpots = async () => {
  testState.apiGet.mockImplementation((url: string) => {
    if (url === '/spots' || url === '/spots/search') {
      return Promise.resolve(paginatedSpots(scenicSpotFixtures))
    }

    return Promise.reject(new Error(`Unexpected GET ${url}`))
  })

  const root = mountScenicSpots()
  await settleVue()

  return root
}

const spotCards = (root: ParentNode) =>
  Array.from(root.querySelectorAll<HTMLElement>('[role="link"]'))

const spotCardLabels = (root: ParentNode) =>
  spotCards(root).map(card => card.getAttribute('aria-label') ?? '')

const statusRegions = (root: ParentNode) =>
  Array.from(root.querySelectorAll<HTMLElement>('[role="status"], [aria-live]'))

const findStatusByText = (root: ParentNode, text: string) =>
  statusRegions(root).find(region => region.textContent?.includes(text)) ?? null

const findButtonByText = (root: ParentNode, text: string) =>
  Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find(button =>
    button.textContent?.includes(text)
  ) ?? null

const setSearchValue = async (root: ParentNode, value: string) => {
  const input = root.querySelector<HTMLInputElement>('#spot-search')

  expect(input).toBeTruthy()
  input!.value = value
  input!.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }))
  await settleVue()

  return input!
}

const expectOnlyCards = (root: ParentNode, names: string[]) => {
  const labels = spotCardLabels(root)

  expect(labels).toHaveLength(names.length)
  names.forEach(name => {
    expect(labels).toContain(`View ${name} details`)
  })
}

beforeEach(() => {
  testState.apiGet.mockReset()
  testState.routerPush.mockReset()
  testState.routerPush.mockResolvedValue(undefined)
})

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

describe('ScenicSpots runtime DOM behavior', () => {
  it('exposes a loading status region and then renders API scenic spot cards with a result summary', async () => {
    const initialRequest = deferred<ReturnType<typeof paginatedSpots>>()
    testState.apiGet.mockReturnValueOnce(initialRequest.promise)

    const root = mountScenicSpots()
    const loadingStatus = statusRegions(root).find(region =>
      region.getAttribute('aria-busy') === 'true'
      && region.getAttribute('aria-label') === 'Loading scenic spots'
    )

    expect(loadingStatus).toBeTruthy()
    expect(loadingStatus?.getAttribute('role')).toBe('status')
    expect(loadingStatus?.textContent).toContain('Loading scenic spots')

    initialRequest.resolve(paginatedSpots(scenicSpotFixtures))
    await settleVue()

    expectOnlyCards(root, ['Yamdrok Lake', 'Jokhang Temple'])
    const summary = findStatusByText(root, 'All spots: 2 spots')

    expect(summary).toBeTruthy()
    expect(summary?.id).toBe('spots-result-summary')
    expect(summary?.getAttribute('aria-live')).toBe('polite')
    expect(root.querySelector('#spot-search')?.getAttribute('aria-describedby')).toContain('spots-result-summary')
  })

  it('filters visible cards from the search input and clears the query on Escape', async () => {
    const root = await mountLoadedScenicSpots()

    expectOnlyCards(root, ['Yamdrok Lake', 'Jokhang Temple'])

    const input = await setSearchValue(root, 'temple')

    expect(testState.apiGet).toHaveBeenCalledWith('/spots/search', {
      params: {
        keyword: 'temple',
        page: 0,
        size: spotsPageSize
      }
    })
    expectOnlyCards(root, ['Jokhang Temple'])
    expect(findStatusByText(root, 'All spots: 1 spots for "temple"')).toBeTruthy()

    input.dispatchEvent(new KeyboardEvent('keydown', {
      bubbles: true,
      cancelable: true,
      key: 'Escape'
    }))
    await settleVue()

    expect(input.value).toBe('')
    expectOnlyCards(root, ['Yamdrok Lake', 'Jokhang Temple'])
    expect(testState.apiGet).toHaveBeenCalledWith('/spots', {
      params: {
        page: 0,
        size: spotsPageSize
      }
    })
  })

  it('uses category buttons as DOM controls for filtering cards', async () => {
    const root = await mountLoadedScenicSpots()
    const naturalButton = findButtonByText(root, 'Natural scenery')

    expect(naturalButton).toBeTruthy()
    naturalButton!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await settleVue()

    expect(naturalButton?.getAttribute('aria-pressed')).toBe('true')
    expect(testState.apiGet).toHaveBeenCalledWith('/spots', {
      params: {
        category: 'NATURAL',
        page: 0,
        size: spotsPageSize
      }
    })
    expectOnlyCards(root, ['Yamdrok Lake'])

    const allButton = findButtonByText(root, 'All spots')

    expect(allButton).toBeTruthy()
    allButton!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await settleVue()

    expect(allButton?.getAttribute('aria-pressed')).toBe('true')
    expectOnlyCards(root, ['Yamdrok Lake', 'Jokhang Temple'])
  })
})
