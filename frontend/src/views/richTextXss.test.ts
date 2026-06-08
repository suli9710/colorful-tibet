// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, type App, type Component } from 'vue'
import { createI18n } from 'vue-i18n'
import { renderMarkdownToSafeHtml, sanitizeHtml } from '../utils/sanitize'
import privacySource from './PrivacyPolicy.vue?raw'
import routeDetailSource from './RouteDetail.vue?raw'
import routePlannerSource from './RoutePlanner.vue?raw'
import termsSource from './TermsOfService.vue?raw'

const fixtures = vi.hoisted(() => ({
  attackMarkdown: `
# 安全边界

<script>alert('xss')</script>
<iframe src="https://evil.example/embed"></iframe>
<object data="https://evil.example/app.swf"></object>
<img src="/images/safe.jpg" onerror="alert('image')" alt="safe image">
<img src="https://tracker.example/pixel.png" alt="remote tracker">
<img src="/uploads/routes/day-1.jpg" alt="local upload image">
<a href="javascript:alert('bad')" onclick="alert('click')">bad link</a>

[encoded bad](java&#x0d;script:alert('bad'))
[safe external](https://safe.example/docs)
`
}))

const routeMocks = vi.hoisted(() => ({
  apiDelete: vi.fn(),
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  ensureSession: vi.fn(),
  refreshSession: vi.fn(),
  routerBack: vi.fn(),
  routerPush: vi.fn(),
  showConfirm: vi.fn(),
  showToast: vi.fn()
}))

vi.mock('../content/legal', () => ({
  getLegalMarkdown: vi.fn(() => fixtures.attackMarkdown)
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    fullPath: '/routes/shared/42',
    params: { id: '42' }
  }),
  useRouter: () => ({
    back: routeMocks.routerBack,
    push: routeMocks.routerPush
  })
}))

vi.mock('../api', () => ({
  default: {
    delete: routeMocks.apiDelete,
    get: routeMocks.apiGet,
    post: routeMocks.apiPost
  },
  endpoints: {
    routes: {
      sharedDetail: (id: number) => `/routes/shared/${id}`,
      sharedLike: (id: number) => `/routes/shared/${id}/like`,
      sharedLikeStatus: (id: number) => `/routes/shared/${id}/like-status`,
      sharedComments: (id: number) => `/routes/shared/${id}/comments`,
      deleteSharedComment: (routeId: number, commentId: number) => `/routes/shared/${routeId}/comments/${commentId}`
    }
  }
}))

vi.mock('../stores/auth', () => ({
  useAuthStore: () => ({
    ensureSession: routeMocks.ensureSession,
    refreshSession: routeMocks.refreshSession,
    user: null
  })
}))

vi.mock('../composables/useToast', () => ({
  showToast: routeMocks.showToast
}))

vi.mock('../composables/useConfirm', () => ({
  showConfirm: routeMocks.showConfirm
}))

vi.mock('../motion/presets', () => ({
  revealInitial: {},
  revealInView: {},
  revealTransition: {}
}))

vi.mock('motion-v', async () => {
  const vue = await import('vue')
  const Passthrough = vue.defineComponent({
    name: 'MotionPassthrough',
    setup(_, { slots }) {
      return () => vue.h('div', slots.default?.())
    }
  })

  return {
    motion: {
      button: Passthrough,
      div: Passthrough
    }
  }
})

import PrivacyPolicy from './PrivacyPolicy.vue'
import RouteDetail from './RouteDetail.vue'
import TermsOfService from './TermsOfService.vue'

const mountedApps: App[] = []

const RouterLinkStub = defineComponent({
  name: 'RouterLinkStub',
  props: {
    to: {
      required: true,
      type: [String, Object]
    }
  },
  setup(_, { slots }) {
    return () => h('a', { href: '/' }, slots.default?.())
  }
})

const createTestI18n = () =>
  createI18n({
    legacy: false,
    locale: 'zh',
    messages: {
      zh: {
        common: {
          cancel: 'Cancel',
          delete: 'Delete'
        },
        community: {
          officialRoute: 'Official'
        },
        privacy: {
          lastUpdated: 'Updated today',
          title: 'Privacy'
        },
        routeDetail: {
          anonymous: 'Anonymous',
          author: 'Author',
          backToList: 'Back',
          commentFailed: 'Comment failed',
          commentPlaceholder: 'Comment',
          comments: 'Comments',
          commentSuccess: 'Comment saved',
          confirmDeleteComment: 'Delete comment?',
          days: ' days',
          deleteCommentFailed: 'Delete failed',
          deleteCommentSuccess: 'Deleted',
          loginRequiredComment: 'Login required',
          loginRequiredLike: 'Login required',
          noComments: 'No comments',
          notFound: 'Not found',
          operationFailed: 'Operation failed',
          postComment: 'Post',
          publishedAt: 'Published',
          submitting: 'Submitting'
        },
        terms: {
          backToHome: 'Home',
          lastUpdated: 'Updated today',
          title: 'Terms'
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

const settleVue = async () => {
  for (let index = 0; index < 8; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const renderIntoDom = (html: string) => {
  const root = document.createElement('article')
  root.innerHTML = html
  return root
}

const assertNoExecutableDom = (root: ParentNode) => {
  expect(root.querySelector('script, iframe, object, embed, svg, math, form, style')).toBeNull()

  root.querySelectorAll('*').forEach(element => {
    Array.from(element.attributes).forEach(attribute => {
      expect(attribute.name.toLowerCase()).not.toMatch(/^on/)
      expect(attribute.value).not.toMatch(/(?:javascript|vbscript|data:text\/html|data:image\/svg)/i)
    })
  })
}

const expectSafeExternalLink = (root: ParentNode) => {
  const safeLink = root.querySelector('a[href="https://safe.example/docs"]')

  expect(safeLink).not.toBeNull()
  expect(safeLink?.getAttribute('target')).toBe('_blank')
  expect(safeLink?.getAttribute('rel')).toBe('noopener noreferrer')
}

const expectNoRemoteTrackerImages = (root: ParentNode) => {
  expect(root.querySelector('img[src*="tracker.example"]')).toBeNull()
  expect((root as Element).innerHTML).not.toContain('tracker.example')
}

const expectLocalUploadImage = (root: ParentNode) => {
  const localImage = root.querySelector('img[src="/uploads/routes/day-1.jpg"]')

  expect(localImage).not.toBeNull()
  expect(localImage?.getAttribute('alt')).toBe('local upload image')
}

describe('rich text XSS safeguards', () => {
  beforeEach(() => {
    routeMocks.apiGet.mockImplementation((url: string) => {
      if (url.includes('/comments')) return Promise.resolve({ data: [] })
      if (url.includes('/like-status')) return Promise.resolve({ data: { liked: false } })

      return Promise.resolve({
        data: {
          author: { nickname: 'Guide' },
          budget: 'moderate',
          commentCount: 0,
          content: fixtures.attackMarkdown,
          createdAt: '2026-06-08T00:00:00Z',
          days: 3,
          id: 42,
          likeCount: 0,
          preference: 'culture',
          title: 'Shared Route',
          viewCount: 1
        }
      })
    })
    routeMocks.refreshSession.mockResolvedValue(undefined)
    routeMocks.ensureSession.mockResolvedValue(true)
    routeMocks.showConfirm.mockResolvedValue(false)
  })

  afterEach(() => {
    mountedApps.splice(0).forEach(app => app.unmount())
    document.body.innerHTML = ''
    vi.clearAllMocks()
  })

  it('renders legal Markdown through the shared sanitizer before v-html reaches the DOM', async () => {
    expect(privacySource).toContain('renderMarkdownToSafeHtml(markdown)')
    expect(termsSource).toContain('renderMarkdownToSafeHtml(markdown)')

    const privacyRoot = await mountView(PrivacyPolicy)
    const termsRoot = await mountView(TermsOfService)

    assertNoExecutableDom(privacyRoot.querySelector('.legal-markdown') ?? privacyRoot)
    assertNoExecutableDom(termsRoot.querySelector('.legal-markdown') ?? termsRoot)
    expectSafeExternalLink(privacyRoot)
    expectSafeExternalLink(termsRoot)
    expectNoRemoteTrackerImages(privacyRoot)
    expectNoRemoteTrackerImages(termsRoot)
    expectLocalUploadImage(privacyRoot)
    expectLocalUploadImage(termsRoot)
  })

  it('renders shared route detail Markdown through the shared sanitizer before v-html reaches the DOM', async () => {
    expect(routeDetailSource).toContain('renderMarkdownToSafeHtml(routeData.value.content)')
    expect(routeDetailSource).toContain('v-html="renderedContent"')

    const root = await mountView(RouteDetail)
    const markdownRoot = root.querySelector('.route-detail-markdown')

    expect(markdownRoot).not.toBeNull()
    assertNoExecutableDom(markdownRoot ?? root)
    expectSafeExternalLink(markdownRoot ?? root)
    expectNoRemoteTrackerImages(markdownRoot ?? root)
    expectLocalUploadImage(markdownRoot ?? root)
  })

  it('keeps route planner worker HTML sanitized before assigning it to v-html content', () => {
    expect(routePlannerSource).toContain('renderedResult.value = sanitizeHtml(html)')
    expect(routePlannerSource).toContain('renderMarkdownToSafeHtml(markdown)')
    expect(routePlannerSource.match(/v-html="renderedResult"/g)?.length).toBe(2)

    const workerHtml = `
      <p onclick="alert(1)">Generated route</p>
      <iframe src="https://evil.example/embed"></iframe>
      <img src="https://tracker.example/pixel.png" alt="remote tracker">
      <img src="javascript:alert(2)" onerror="alert(3)">
      <a href="javascript:alert(4)">bad worker link</a>
      <a href="https://safe.example/docs">safe external</a>
    `
    const root = renderIntoDom(sanitizeHtml(workerHtml))

    assertNoExecutableDom(root)
    expectSafeExternalLink(root)
    expectNoRemoteTrackerImages(root)
  })

  it('keeps sanitizer output safe when inserted into a DOM node directly', () => {
    const root = renderIntoDom(renderMarkdownToSafeHtml(fixtures.attackMarkdown))

    assertNoExecutableDom(root)
    expectSafeExternalLink(root)
    expectNoRemoteTrackerImages(root)
    expectLocalUploadImage(root)
  })
})
