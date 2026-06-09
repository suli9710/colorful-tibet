// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, nextTick, type App } from 'vue'
import { createI18n } from 'vue-i18n'
import AdminHeritagePanel from './AdminHeritagePanel.vue'

const { apiDelete, apiGet, apiPost, apiPut } = vi.hoisted(() => ({
  apiDelete: vi.fn(),
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn()
}))

vi.mock('../api', () => ({
  default: {
    delete: apiDelete,
    get: apiGet,
    post: apiPost,
    put: apiPut
  },
  endpoints: {
    admin: {
      uploadImage: '/admin/upload-image'
    },
    adminHeritage: {
      create: '/admin/heritage',
      delete: (id: number) => `/admin/heritage/${id}`,
      deleteEvent: (id: number) => `/admin/heritage/events/${id}`,
      deleteInheritor: (id: number) => `/admin/heritage/inheritors/${id}`,
      events: (itemId: number) => `/admin/heritage/${itemId}/events`,
      inheritors: (itemId: number) => `/admin/heritage/${itemId}/inheritors`,
      list: '/admin/heritage',
      update: (id: number) => `/admin/heritage/${id}`,
      updateEvent: (id: number) => `/admin/heritage/events/${id}`,
      updateInheritor: (id: number) => `/admin/heritage/inheritors/${id}`
    }
  }
}))

vi.mock('../composables/useConfirm', () => ({
  useConfirm: () => ({ showConfirm: vi.fn().mockResolvedValue(true) })
}))

vi.mock('../composables/useToast', () => ({
  useToast: () => ({ showToast: vi.fn() })
}))

vi.mock('./ImageUploadField.vue', () => ({
  default: { template: '<input aria-label="image upload" />' }
}))

vi.mock('./motion/MotionModal.vue', () => ({
  default: {
    props: ['show'],
    template: '<div v-if="show"><slot /></div>'
  }
}))

type ApiResponse = { data: unknown }
type Deferred<T> = {
  promise: Promise<T>
  reject: (reason?: unknown) => void
  resolve: (value: T) => void
}

const mountedApps: App[] = []
let responseQueues: Map<string, Array<Deferred<ApiResponse>>>

const heritageItems = [
  {
    id: 1,
    name: 'Yak butter sculpture',
    category: 'Craft',
    description: 'Sculpted offerings',
    region: 'Lhasa'
  },
  {
    id: 2,
    name: 'Thangka painting',
    category: 'Art',
    description: 'Painted scrolls',
    region: 'Shigatse'
  }
]

const createDeferred = <T,>(): Deferred<T> => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, reject, resolve }
}

const enqueueResponse = (url: string) => {
  const deferred = createDeferred<ApiResponse>()
  const queue = responseQueues.get(url) ?? []
  queue.push(deferred)
  responseQueues.set(url, queue)

  return deferred
}

const createTestI18n = () => createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      admin: {
        addNew: 'Add',
        avatarUrl: 'Avatar URL',
        baikeUrl: 'Baike URL',
        bio: 'Bio',
        category: 'Category',
        confirmDelete: 'Delete?',
        contactInfo: 'Contact',
        content: 'Content',
        createHeritage: 'Create heritage',
        deleteFailed: 'Delete failed',
        editHeritage: 'Edit heritage',
        heritageManagement: 'Heritage management',
        heritageItemsLoadFailed: 'Heritage items failed to load',
        image: 'Image',
        imageUrl: 'Image URL',
        inheritorStory: 'Story',
        level: 'Level',
        loadingHeritage: 'Loading heritage',
        location: 'Location',
        manageRelations: 'Relations',
        name: 'Name',
        noEvents: 'No events',
        noHeritage: 'No heritage',
        noInheritors: 'No inheritors',
        protectionLevel: 'Protection level',
        region: 'Region',
        retryHeritageItems: 'Retry heritage items',
        saveFailed: 'Save failed',
        saveSuccess: 'Saved',
        saving: 'Saving',
        searchHeritage: 'Search heritage',
        tibetanDescription: 'Tibetan description',
        titleLabel: 'Title',
        videoUrl: 'Video URL',
        views: 'Views'
      },
      common: {
        cancel: 'Cancel',
        collapse: 'Collapse',
        delete: 'Delete',
        edit: 'Edit',
        items: ' items',
        loading: 'Loading',
        refresh: 'Refresh',
        save: 'Save'
      },
      community: {
        likeCount: 'Likes'
      },
      heritage: {
        commentsTitle: 'Comments',
        culturalValue: 'Cultural value',
        inheritors: 'Inheritors',
        originStory: 'Origin story',
        relatedEvents: 'Events'
      }
    }
  }
})

const settleVue = async () => {
  for (let index = 0; index < 8; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const mountPanel = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const app = createApp(AdminHeritagePanel)
  app.use(createTestI18n())
  mountedApps.push(app)
  app.mount(root)
  await settleVue()

  const header = root.querySelector<HTMLElement>('[aria-controls="admin-heritage-panel-content"]')
  expect(header).not.toBeNull()
  header?.click()
  await settleVue()

  return root
}

const buttonsWithText = (root: HTMLElement, text: string) => {
  return Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
    .filter(button => button.textContent?.trim().includes(text))
}

beforeEach(() => {
  responseQueues = new Map()
  apiGet.mockImplementation((url: string) => {
    const nextResponse = responseQueues.get(url)?.shift()
    if (nextResponse) return nextResponse.promise

    if (url === '/admin/heritage') {
      return Promise.resolve({ data: heritageItems })
    }

    return Promise.reject(new Error(`Unexpected GET ${url}`))
  })
})

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

describe('AdminHeritagePanel relation loading', () => {
  it('keeps existing heritage items visible and retryable when a refresh fails', async () => {
    const root = await mountPanel()

    expect(root.textContent).toContain('Yak butter sculpture')
    expect(root.textContent).toContain('Thangka painting')

    const failedRefresh = enqueueResponse('/admin/heritage')
    buttonsWithText(root, 'Refresh')[0].click()
    await settleVue()

    failedRefresh.reject(new Error('temporary heritage outage'))
    await settleVue()

    expect(root.textContent).toContain('Heritage items failed to load')
    expect(root.textContent).toContain('Retry heritage items')
    expect(root.textContent).toContain('Yak butter sculpture')
    expect(root.textContent).toContain('Thangka painting')
  })

  it('ignores stale heritage item refresh responses after a newer refresh succeeds', async () => {
    const root = await mountPanel()

    const staleRefresh = enqueueResponse('/admin/heritage')
    const latestRefresh = enqueueResponse('/admin/heritage')

    buttonsWithText(root, 'Refresh')[0].click()
    buttonsWithText(root, 'Refresh')[0].click()
    await settleVue()

    latestRefresh.resolve({
      data: [{
        id: 3,
        name: 'Fresh opera',
        category: 'Performance',
        description: 'Fresh response',
        region: 'Lhokha'
      }]
    })
    await settleVue()

    staleRefresh.resolve({
      data: [{
        id: 4,
        name: 'Stale dance',
        category: 'Performance',
        description: 'Old response',
        region: 'Ngari'
      }]
    })
    await settleVue()

    expect(root.textContent).toContain('Fresh opera')
    expect(root.textContent).not.toContain('Stale dance')
    expect(root.textContent).not.toContain('Heritage items failed to load')
  })

  it('keeps stale relation responses from the previously selected item out of the current item', async () => {
    const firstInheritors = enqueueResponse('/admin/heritage/1/inheritors')
    const firstEvents = enqueueResponse('/admin/heritage/1/events')
    const secondInheritors = enqueueResponse('/admin/heritage/2/inheritors')
    const secondEvents = enqueueResponse('/admin/heritage/2/events')

    const root = await mountPanel()
    buttonsWithText(root, 'Relations')[0].click()
    await settleVue()
    buttonsWithText(root, 'Relations')[0].click()
    await settleVue()

    firstInheritors.resolve({ data: [{ id: 101, name: 'Old inheritor' }] })
    firstEvents.resolve({ data: [{ id: 201, title: 'Old event' }] })
    await settleVue()

    expect(root.textContent).toContain('Loading')
    expect(root.textContent).not.toContain('Old inheritor')
    expect(root.textContent).not.toContain('Old event')

    secondInheritors.resolve({ data: [{ id: 102, name: 'Current inheritor' }] })
    secondEvents.resolve({ data: [{ id: 202, title: 'Current event' }] })
    await settleVue()

    expect(root.textContent).toContain('Current inheritor')
    expect(root.textContent).toContain('Current event')
    expect(root.textContent).not.toContain('Old inheritor')
    expect(root.textContent).not.toContain('Old event')
    expect(root.textContent).not.toContain('Loading')
  })

  it('lets only the latest request write relations when the same item is reopened', async () => {
    const firstInheritors = enqueueResponse('/admin/heritage/1/inheritors')
    const firstEvents = enqueueResponse('/admin/heritage/1/events')
    const secondInheritors = enqueueResponse('/admin/heritage/1/inheritors')
    const secondEvents = enqueueResponse('/admin/heritage/1/events')

    const root = await mountPanel()
    buttonsWithText(root, 'Relations')[0].click()
    await settleVue()
    buttonsWithText(root, 'Collapse')[0].click()
    await settleVue()
    buttonsWithText(root, 'Relations')[0].click()
    await settleVue()

    secondInheritors.resolve({ data: [{ id: 103, name: 'Latest inheritor' }] })
    secondEvents.resolve({ data: [{ id: 203, title: 'Latest event' }] })
    await settleVue()

    expect(root.textContent).toContain('Latest inheritor')
    expect(root.textContent).toContain('Latest event')

    firstInheritors.resolve({ data: [{ id: 104, name: 'Reopened stale inheritor' }] })
    firstEvents.resolve({ data: [{ id: 204, title: 'Reopened stale event' }] })
    await settleVue()

    expect(root.textContent).toContain('Latest inheritor')
    expect(root.textContent).toContain('Latest event')
    expect(root.textContent).not.toContain('Reopened stale inheritor')
    expect(root.textContent).not.toContain('Reopened stale event')
  })

  it('keeps stale relation failures from clearing the current item relations', async () => {
    const firstInheritors = enqueueResponse('/admin/heritage/1/inheritors')
    const firstEvents = enqueueResponse('/admin/heritage/1/events')
    const secondInheritors = enqueueResponse('/admin/heritage/2/inheritors')
    const secondEvents = enqueueResponse('/admin/heritage/2/events')

    const root = await mountPanel()
    buttonsWithText(root, 'Relations')[0].click()
    await settleVue()
    buttonsWithText(root, 'Relations')[0].click()
    await settleVue()

    secondInheritors.resolve({ data: [{ id: 105, name: 'Surviving inheritor' }] })
    secondEvents.resolve({ data: [{ id: 205, title: 'Surviving event' }] })
    await settleVue()

    expect(root.textContent).toContain('Surviving inheritor')
    expect(root.textContent).toContain('Surviving event')

    firstInheritors.reject(new Error('stale relation failure'))
    firstEvents.resolve({ data: [] })
    await settleVue()

    expect(root.textContent).toContain('Surviving inheritor')
    expect(root.textContent).toContain('Surviving event')
    expect(root.textContent).not.toContain('No inheritors')
    expect(root.textContent).not.toContain('No events')
  })
})
