// @vitest-environment jsdom
import { readFileSync } from 'node:fs'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, type App } from 'vue'
import { createI18n } from 'vue-i18n'

const testState = vi.hoisted(() => ({
  apiDelete: vi.fn(),
  apiGet: vi.fn(),
  showToast: vi.fn()
}))

vi.mock('../api', () => ({
  default: {
    delete: testState.apiDelete,
    get: testState.apiGet
  },
  endpoints: {
    favorites: {
      list: '/favorites',
      remove: (routeId: number) => `/favorites/${routeId}`
    }
  }
}))

vi.mock('../composables/useToast', () => ({
  showToast: testState.showToast
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
            inViewOptions,
            initial,
            transition,
            whileHover,
            whileInView,
            whilePress,
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
    motion: new Proxy({}, {
      get: (_target, tag) => passthrough(String(tag))
    })
  }
})

import Favorites from './Favorites.vue'

interface FavoriteFixture {
  id: number
  route: {
    id: number
    name: string
    description: string
    days: number
    price: number
    difficulty: string
    temperature?: string
  }
}

const mountedApps: App[] = []
const favoriteLocaleKeys = [
  'empty',
  'goExplore',
  'loadErrorTitle',
  'loadFailed',
  'loading',
  'operationFailed',
  'paginationLabel',
  'remove',
  'retryLoad',
  'subtitle',
  'title',
  'unknownRoute'
] as const

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

const createTestI18n = () => createI18n({
  fallbackWarn: false,
  legacy: false,
  locale: 'en',
  missingWarn: false,
  messages: {
    en: {
      admin: {
        daysValue: '{count} days'
      },
      common: {
        loading: 'Loading'
      },
      favorites: {
        empty: 'No favorites yet',
        goExplore: 'Plan a route',
        loadErrorTitle: 'Unable to load favorites',
        loadFailed: 'Favorites load failed. Please try again.',
        loading: 'Loading favorite routes',
        operationFailed: 'Action failed',
        paginationLabel: 'Favorite route pages',
        remove: 'Remove favorite',
        retryLoad: 'Reload',
        subtitle: 'Saved Tibet routes',
        title: 'My favorites',
        unknownRoute: 'Untitled route'
      }
    }
  }
})

const favorite = (id: number, name: string): FavoriteFixture => ({
  id,
  route: {
    id: id * 10,
    name,
    description: `${name} description`,
    days: 5,
    difficulty: 'EASY',
    price: 1800,
    temperature: '12C'
  }
})

const favoritesPage = (content: FavoriteFixture[], page = 0, totalPages = 1) => ({
  data: {
    content,
    page,
    size: 20,
    totalElements: totalPages * 20,
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

const settleVue = async (turns = 8) => {
  for (let index = 0; index < turns; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const mountFavorites = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(Favorites)
  app.component('router-link', RouterLinkStub)
  app.use(createTestI18n())
  mountedApps.push(app)
  app.mount(root)
  await settleVue()

  return root
}

const findButtonByExactText = (root: ParentNode, text: string) => {
  const button = Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
    .find(candidate => candidate.textContent?.trim() === text)

  expect(button).toBeTruthy()
  return button!
}

beforeEach(() => {
  testState.apiDelete.mockReset()
  testState.apiGet.mockReset()
  testState.showToast.mockReset()
  vi.spyOn(console, 'error').mockImplementation(() => undefined)
})

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.restoreAllMocks()
})

describe('Favorites DOM behavior', () => {
  it('keeps favorite feedback copy covered in supported locales', () => {
    for (const localePath of ['../i18n/locales/zh.json', '../i18n/locales/bo.json']) {
      const messages = JSON.parse(readFileSync(new URL(localePath, import.meta.url), 'utf8')) as {
        favorites?: Record<string, string>
      }

      for (const key of favoriteLocaleKeys) {
        expect(messages.favorites?.[key], `${localePath} favorites.${key}`).toBeTruthy()
      }
    }
  })

  it('normalizes malformed favorite items before rendering visible route data', async () => {
    testState.apiGet.mockResolvedValue({
      data: {
        content: [
          null,
          'unexpected favorite',
          {
            id: '12',
            route: {
              id: '99',
              name: 42,
              description: null,
              days: 'many',
              price: 'free',
              difficulty: null,
              temperature: 12
            }
          }
        ],
        totalPages: 'many'
      }
    })

    const root = await mountFavorites()

    expect(root.textContent).toContain('Untitled route')
    expect(root.textContent).not.toContain('undefined')
    expect(root.textContent).not.toContain('NaN')
    expect(root.textContent).not.toContain('¥')
    expect(root.textContent).not.toContain('...')
    expect(root.querySelector('[role="navigation"]')).toBeNull()
  })

  it('keeps the newest favorites page when an older page response finishes later', async () => {
    const stalePage = deferred<ReturnType<typeof favoritesPage>>()
    const currentPage = deferred<ReturnType<typeof favoritesPage>>()

    testState.apiGet.mockImplementation((url: string, config?: { params?: { page?: number } }) => {
      if (url !== '/favorites') return Promise.reject(new Error(`Unexpected GET ${url}`))

      const page = Number(config?.params?.page ?? 0)
      if (testState.apiGet.mock.calls.length === 1) {
        return Promise.resolve(favoritesPage([favorite(1, 'Initial route')], 0, 3))
      }
      if (page === 1) return stalePage.promise
      if (page === 2) return currentPage.promise

      return Promise.reject(new Error(`Unexpected favorites page ${page}`))
    })

    const root = await mountFavorites()

    expect(root.textContent).toContain('Initial route')
    expect(findButtonByExactText(root, '1').getAttribute('aria-current')).toBe('page')

    const pageTwoButton = findButtonByExactText(root, '2')
    const pageThreeButton = findButtonByExactText(root, '3')

    pageTwoButton.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    pageThreeButton.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await settleVue(2)

    expect(root.querySelector('[role="status"]')?.getAttribute('aria-busy')).toBe('true')

    currentPage.resolve(favoritesPage([favorite(3, 'Newest route')], 2, 3))
    await settleVue()

    expect(root.textContent).toContain('Newest route')
    expect(root.textContent).not.toContain('Initial route')
    expect(findButtonByExactText(root, '3').getAttribute('aria-current')).toBe('page')

    stalePage.resolve(favoritesPage([favorite(2, 'Stale route')], 1, 3))
    await settleVue()

    expect(root.textContent).toContain('Newest route')
    expect(root.textContent).not.toContain('Stale route')
    expect(root.textContent).not.toContain('Favorites load failed')
    expect(findButtonByExactText(root, '3').getAttribute('aria-current')).toBe('page')
  })

  it('keeps failed page loads distinct from empty favorites and hides stale pagination controls', async () => {
    const failedPage = deferred<ReturnType<typeof favoritesPage>>()

    testState.apiGet.mockImplementation((url: string, config?: { params?: { page?: number } }) => {
      if (url !== '/favorites') return Promise.reject(new Error(`Unexpected GET ${url}`))

      const page = Number(config?.params?.page ?? 0)
      if (page === 0) return Promise.resolve(favoritesPage([favorite(1, 'Initial route')], 0, 3))
      if (page === 1) return failedPage.promise

      return Promise.reject(new Error(`Unexpected favorites page ${page}`))
    })

    const root = await mountFavorites()

    findButtonByExactText(root, '2').dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await settleVue(2)

    expect(root.querySelector('[role="navigation"]')).toBeNull()

    failedPage.reject(new Error('Network failed'))
    await settleVue()

    const alert = root.querySelector('[role="alert"]')
    expect(alert).toBeTruthy()
    expect(alert?.textContent).toContain('Unable to load favorites')
    expect(alert?.textContent).toContain('Favorites load failed. Please try again.')
    expect(alert?.textContent).toContain('Reload')
    expect(root.textContent).not.toContain('No favorites yet')
    expect(root.querySelector('[role="navigation"]')).toBeNull()
  })
})
