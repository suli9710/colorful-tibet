// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import {
  cleanupMountedViews,
  clickButtonByText,
  createDeferred,
  createTestI18n,
  mountWithPlugins
} from './domBehaviorTestHelpers'

const testState = vi.hoisted(() => ({
  apiDelete: vi.fn(),
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  hasValidSession: vi.fn(),
  refreshSession: vi.fn(),
  requireAuth: vi.fn(),
  routeParams: { id: '99' } as Record<string, string>,
  routerBack: vi.fn(),
  routerPush: vi.fn(),
  showConfirm: vi.fn(),
  showToast: vi.fn()
}))

const endpointsMock = vi.hoisted(() => ({
  community: {
    acceptQuestionAnswer: (questionId: number | string, answerId: number | string) =>
      `/community/questions/${questionId}/answers/${answerId}/accept`,
    createQuestionAnswer: (id: number | string) => `/community/questions/${id}/answers`,
    deleteQuestion: (id: number | string) => `/community/questions/${id}`,
    questionAnswers: (id: number | string) => `/community/questions/${id}/answers`,
    questionDetail: (id: number | string) => `/community/questions/${id}`,
    questionLike: (id: number | string) => `/community/questions/${id}/like`,
    questionLikeStatus: (id: number | string) => `/community/questions/${id}/like-status`
  }
}))

vi.mock('../api', () => ({
  default: {
    delete: testState.apiDelete,
    get: testState.apiGet,
    post: testState.apiPost
  },
  endpoints: endpointsMock
}))

vi.mock('vue-router', async () => {
  const vue = await import('vue')
  const routeParams = vue.reactive({ ...testState.routeParams })
  testState.routeParams = routeParams as Record<string, string>
  const route = vue.reactive({
    get fullPath() {
      return `/community/questions/${routeParams.id}`
    },
    params: routeParams
  })

  return {
    useRoute: () => route,
    useRouter: () => ({
      back: testState.routerBack,
      push: testState.routerPush
    })
  }
})

vi.mock('../stores/auth', () => ({
  useAuthStore: () => ({
    hasValidSession: testState.hasValidSession,
    refreshSession: testState.refreshSession
  })
}))

vi.mock('../composables/useAuthGuard', () => ({
  useAuthGuard: () => ({ requireAuth: testState.requireAuth })
}))

vi.mock('../composables/useConfirm', () => ({
  showConfirm: testState.showConfirm
}))

vi.mock('../composables/useToast', () => ({
  showToast: testState.showToast
}))

vi.mock('lucide-vue-next', () => ({
  Heart: defineComponent({
    name: 'HeartIconStub',
    setup() {
      return () => h('svg', { 'aria-hidden': 'true' })
    }
  })
}))

vi.mock('motion-v', async () => {
  const vue = await import('vue')

  return {
    motion: new Proxy({}, {
      get: () => vue.defineComponent({
        inheritAttrs: false,
        setup(_, { attrs, slots }) {
          const { animate, initial, transition, ...domAttrs } = attrs
          return () => vue.h('div', domAttrs, slots.default?.())
        }
      })
    })
  }
})

import QuestionDetail from './QuestionDetail.vue'

const messages = {
  en: {
    common: {
      cancel: 'Cancel',
      delete: 'Delete',
      loading: 'Loading'
    },
    community: {
      acceptAnswer: 'Accept answer',
      accepted: 'Accepted',
      answers: 'Answers',
      noAnswers: 'No answers yet',
      nextPage: 'Next page',
      solved: 'Solved',
      submitAnswer: 'Submit answer',
      submittingAnswer: 'Submitting',
      unsolved: 'Unsolved',
      writeAnswer: 'Write answer',
      yourAnswer: 'Your answer'
    },
    questionDetail: {
      acceptFailed: 'Accept failed',
      anonymousUser: 'Anonymous user',
      answerFailed: 'Answer failed',
      answersLoadFailed: 'Answers failed to load',
      backToCommunity: 'Back to community',
      confirmDelete: 'Delete this question?',
      deleteFailed: 'Delete failed',
      deleteQuestion: 'Delete question',
      likedState: 'currently liked',
      likeQuestionAction: 'Like question',
      likeQuestionAria: '{action}, {state}, {count} likes',
      loadingAnswers: 'Loading answers',
      loadingQuestion: 'Loading question',
      loadFailed: 'Question failed to load',
      notLikedState: 'not liked',
      questionNotFound: 'Question not found',
      retryAnswers: 'Retry answers',
      retryLoad: 'Retry question',
      unlikeQuestionAction: 'Unlike question',
      viewCount: '{count} views'
    }
  }
}

const questionResponse = (
  id = 99,
  title = 'How should I acclimate?',
  content = 'Spend the first night in Lhasa and keep the first day slow.'
) => ({
  data: {
    author: { nickname: 'Guide' },
    content,
    createdAt: '2026-06-09T08:00:00Z',
    id,
    isResolved: false,
    likeCount: 3,
    tags: '',
    title,
    viewCount: 42
  }
})

const answersResponse = (content = [
  {
    content: 'Hydrate and avoid heavy exertion on arrival.',
    createdAt: '2026-06-09T09:00:00Z',
    id: 501,
    isAccepted: false,
    likeCount: 1,
    user: { nickname: 'Traveler' }
  }
]) => ({
  data: {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0
  }
})

const errorWithStatus = (status: number) => ({ response: { status } })

const mountQuestionDetail = () =>
  mountWithPlugins(QuestionDetail, {
    i18n: createTestI18n(messages, 'en')
  })

beforeEach(() => {
  testState.apiDelete.mockReset()
  testState.apiGet.mockReset()
  testState.apiPost.mockReset()
  testState.hasValidSession.mockReset()
  testState.refreshSession.mockReset()
  testState.requireAuth.mockReset()
  testState.routerBack.mockReset()
  testState.routerPush.mockReset()
  testState.showConfirm.mockReset()
  testState.showToast.mockReset()
  testState.routeParams.id = '99'
  testState.hasValidSession.mockReturnValue(false)
  testState.refreshSession.mockResolvedValue(false)
  testState.requireAuth.mockResolvedValue(false)
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  cleanupMountedViews()
  vi.restoreAllMocks()
})

describe('QuestionDetail runtime DOM behavior', () => {
  it('announces loading and keeps transient detail failures retryable instead of showing not found', async () => {
    const initialDetail = createDeferred<ReturnType<typeof questionResponse>>()
    let detailCalls = 0

    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/community/questions/99') {
        detailCalls += 1
        return detailCalls === 1 ? initialDetail.promise : Promise.resolve(questionResponse())
      }
      if (url === '/community/questions/99/answers') {
        return Promise.resolve(answersResponse())
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const { root, flush } = await mountQuestionDetail()

    const loadingStatus = root.querySelector<HTMLElement>('[role="status"][aria-busy="true"]')
    expect(loadingStatus?.getAttribute('aria-label')).toBe('Loading question')

    initialDetail.reject(new Error('temporary outage'))
    await flush()

    const alert = root.querySelector<HTMLElement>('[role="alert"]')
    expect(alert?.textContent).toContain('Question failed to load')
    expect(root.textContent).not.toContain('Question not found')

    await clickButtonByText(root, 'Retry question')

    expect(root.textContent).toContain('How should I acclimate?')
    expect(root.textContent).toContain('Hydrate and avoid heavy exertion on arrival.')
    expect(detailCalls).toBe(2)
  })

  it('keeps a loaded question visible when the answer list fails and retries only answers', async () => {
    let answersCalls = 0

    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/community/questions/99') {
        return Promise.resolve(questionResponse())
      }
      if (url === '/community/questions/99/answers') {
        answersCalls += 1
        return answersCalls === 1
          ? Promise.reject(new Error('answers timeout'))
          : Promise.resolve(answersResponse())
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const { root } = await mountQuestionDetail()

    expect(root.textContent).toContain('How should I acclimate?')
    expect(root.textContent).toContain('Answers failed to load')
    expect(root.textContent).not.toContain('Question not found')

    await clickButtonByText(root, 'Retry answers')

    expect(root.textContent).toContain('Hydrate and avoid heavy exertion on arrival.')
    expect(answersCalls).toBe(2)
  })

  it('still treats 404 question responses as a true not-found state', async () => {
    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/community/questions/99') {
        return Promise.reject(errorWithStatus(404))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const { root } = await mountQuestionDetail()

    expect(root.textContent).toContain('Question not found')
    expect(root.querySelector('[role="alert"]')).toBeNull()
    expect(testState.apiGet).not.toHaveBeenCalledWith('/community/questions/99/answers', expect.anything())
  })

  it('reloads on question id changes and ignores stale older detail responses', async () => {
    const oldDetail = createDeferred<ReturnType<typeof questionResponse>>()
    const newDetail = createDeferred<ReturnType<typeof questionResponse>>()

    testState.apiGet.mockImplementation((url: string) => {
      if (url === '/community/questions/99') {
        return oldDetail.promise
      }
      if (url === '/community/questions/100') {
        return newDetail.promise
      }
      if (url === '/community/questions/100/answers') {
        return Promise.resolve(answersResponse([
          {
            content: 'Book a slower second-day transfer.',
            createdAt: '2026-06-09T10:00:00Z',
            id: 601,
            isAccepted: false,
            likeCount: 0,
            user: { nickname: 'Guide' }
          }
        ]))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    const { root, flush } = await mountQuestionDetail()

    testState.routeParams.id = '100'
    await flush(2)
    newDetail.resolve(questionResponse(100, 'Which transfer should I book?', 'Use a route with fewer long climbs.'))
    await flush()

    expect(root.textContent).toContain('Which transfer should I book?')
    expect(root.textContent).toContain('Book a slower second-day transfer.')

    oldDetail.resolve(questionResponse(99, 'Old acclimation question', 'Old detail should not return.'))
    await flush()

    expect(root.textContent).toContain('Which transfer should I book?')
    expect(root.textContent).not.toContain('Old acclimation question')
    expect(root.textContent).not.toContain('Old detail should not return.')
  })
})
