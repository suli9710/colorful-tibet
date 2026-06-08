import { afterEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { clearAllRoutePlannerDrafts, useRoutePlannerDraft } from './useRoutePlannerDraft'

class MemoryStorage implements Storage {
  private values = new Map<string, string>()

  get length() {
    return this.values.size
  }

  clear() {
    this.values.clear()
  }

  getItem(key: string) {
    return this.values.get(key) ?? null
  }

  key(index: number) {
    return Array.from(this.values.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.values.delete(key)
  }

  setItem(key: string, value: string) {
    this.values.set(key, String(value))
  }
}

const defaultForm = {
  days: 5,
  budget: 'comfort',
  preference: 'natural'
}

const validJobId = '123e4567-e89b-12d3-a456-426614174000'
const storageKey = 'colorful-tibet:route-planner:draft:session'

function installBrowserStorage() {
  const sessionStorage = new MemoryStorage()
  const localStorage = new MemoryStorage()
  vi.stubGlobal('sessionStorage', sessionStorage)
  vi.stubGlobal('localStorage', localStorage)
  vi.stubGlobal('window', {
    sessionStorage,
    localStorage
  })
  return { sessionStorage, localStorage }
}

function createDraftState(overrides: {
  days?: number
  budget?: string
  preference?: string
  result?: string
  jobId?: string
  statusMessage?: string
  errorMessage?: string
  loading?: boolean
  streaming?: boolean
} = {}) {
  return {
    form: ref({
      days: overrides.days ?? defaultForm.days,
      budget: overrides.budget ?? defaultForm.budget,
      preference: overrides.preference ?? defaultForm.preference
    }),
    result: ref(overrides.result ?? ''),
    jobId: ref(overrides.jobId ?? ''),
    statusMessage: ref(overrides.statusMessage ?? ''),
    errorMessage: ref(overrides.errorMessage ?? ''),
    loading: ref(overrides.loading ?? false),
    streaming: ref(overrides.streaming ?? false)
  }
}

describe('route planner draft storage safety', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('persists only minimal non-sensitive metadata in session storage', () => {
    const { sessionStorage, localStorage } = installBrowserStorage()
    localStorage.setItem(storageKey, 'legacy local draft')
    const state = createDraftState({
      days: 45,
      budget: '19532458802',
      preference: '11010519491231002X',
      result: 'route result contains phone 19532458802 and id 11010519491231002X',
      jobId: validJobId,
      statusMessage: 'free text 19532458802',
      errorMessage: 'error detail 11010519491231002X'
    })

    const { persistRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey,
      version: 2
    })

    persistRouteDraft()

    const stored = JSON.parse(sessionStorage.getItem(storageKey) || '{}')
    expect(stored).toMatchObject({
      version: 2,
      form: {
        days: 30,
        budget: defaultForm.budget,
        preference: defaultForm.preference
      },
      result: '',
      jobId: '',
      statusMessage: '',
      errorMessage: '',
      completed: false
    })
    expect(localStorage.getItem(storageKey)).toBeNull()
    expect(JSON.stringify(stored)).not.toContain('19532458802')
    expect(JSON.stringify(stored)).not.toContain('11010519491231002X')
    expect(JSON.stringify(stored)).not.toContain('free text')
    expect(JSON.stringify(stored)).not.toContain(validJobId)
  })

  it('sanitizes legacy session drafts before restoring state', () => {
    const { sessionStorage } = installBrowserStorage()
    const onRestoreResult = vi.fn()
    sessionStorage.setItem(storageKey, JSON.stringify({
      version: 2,
      form: { days: 6, budget: 'economy', preference: 'photography' },
      result: 'legacy route result 19532458802',
      jobId: validJobId,
      statusMessage: 'legacy status text 19532458802',
      errorMessage: 'legacy error 11010519491231002X',
      completed: true,
      updatedAt: Date.now()
    }))
    const state = createDraftState()

    const { restoreRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey,
      version: 2,
      onRestoreResult
    })

    const restored = restoreRouteDraft()
    const cleaned = JSON.parse(sessionStorage.getItem(storageKey) || '{}')

    expect(restored?.form).toEqual({ days: 6, budget: 'economy', preference: 'photography' })
    expect(state.form.value).toEqual({ days: 6, budget: 'economy', preference: 'photography' })
    expect(state.jobId.value).toBe('')
    expect(state.result.value).toBe('')
    expect(state.statusMessage.value).toBe('')
    expect(state.errorMessage.value).toBe('')
    expect(onRestoreResult).not.toHaveBeenCalled()
    expect(cleaned.result).toBe('')
    expect(cleaned.jobId).toBe('')
    expect(cleaned.statusMessage).toBe('')
    expect(cleaned.errorMessage).toBe('')
    expect(JSON.stringify(cleaned)).not.toContain('19532458802')
    expect(JSON.stringify(cleaned)).not.toContain('11010519491231002X')
    expect(JSON.stringify(cleaned)).not.toContain(validJobId)
  })

  it('does not persist email or token-like form values even when defaults are unsafe', () => {
    const { sessionStorage } = installBrowserStorage()
    const unsafeDefaultForm = {
      days: 7,
      budget: 'owner@example.com',
      preference: 'token_abcdefghijklmnopqrstuvwxyz'
    }
    const state = createDraftState({
      budget: 'traveler@example.com',
      preference: 'token_1234567890123456789012345'
    })

    const { persistRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm: unsafeDefaultForm,
      storageKey,
      version: 2
    })

    persistRouteDraft()

    const stored = JSON.parse(sessionStorage.getItem(storageKey) || '{}')
    expect(stored.form).toEqual({
      days: defaultForm.days,
      budget: '',
      preference: ''
    })
    expect(JSON.stringify(stored)).not.toContain('owner@example.com')
    expect(JSON.stringify(stored)).not.toContain('traveler@example.com')
    expect(JSON.stringify(stored)).not.toContain('token_')
  })

  it('cleans email and token-like form values from legacy session drafts', () => {
    const { sessionStorage } = installBrowserStorage()
    sessionStorage.setItem(storageKey, JSON.stringify({
      version: 2,
      form: {
        days: 6,
        budget: 'traveler@example.com',
        preference: 'token_1234567890123456789012345'
      },
      result: '',
      jobId: '',
      statusMessage: '',
      errorMessage: '',
      completed: false,
      updatedAt: Date.now()
    }))
    const state = createDraftState()

    const { restoreRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey,
      version: 2
    })

    const restored = restoreRouteDraft()
    const cleaned = JSON.parse(sessionStorage.getItem(storageKey) || '{}')

    expect(restored?.form).toEqual({
      days: 6,
      budget: defaultForm.budget,
      preference: defaultForm.preference
    })
    expect(state.form.value).toEqual({
      days: 6,
      budget: defaultForm.budget,
      preference: defaultForm.preference
    })
    expect(JSON.stringify(cleaned)).not.toContain('traveler@example.com')
    expect(JSON.stringify(cleaned)).not.toContain('token_')
  })

  it('drops stale drafts instead of restoring them', () => {
    const { sessionStorage } = installBrowserStorage()
    sessionStorage.setItem(storageKey, JSON.stringify({
      version: 2,
      form: defaultForm,
      result: 'stale sensitive text 19532458802',
      jobId: validJobId,
      updatedAt: 1
    }))
    const state = createDraftState()

    const { restoreRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey,
      version: 2
    })

    expect(restoreRouteDraft()).toBeUndefined()
    expect(sessionStorage.getItem(storageKey)).toBeNull()
    expect(state.result.value).toBe('')
  })

  it('drops malformed or oversized drafts instead of restoring them', () => {
    const { sessionStorage } = installBrowserStorage()
    const state = createDraftState()

    sessionStorage.setItem(storageKey, '{bad-json')

    const { restoreRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey,
      version: 2
    })

    expect(restoreRouteDraft()).toBeUndefined()
    expect(sessionStorage.getItem(storageKey)).toBeNull()

    sessionStorage.setItem(storageKey, 'x'.repeat(3000))
    expect(restoreRouteDraft()).toBeUndefined()
    expect(sessionStorage.getItem(storageKey)).toBeNull()
  })

  it('falls back to the anonymous draft key when storage scope contains sensitive text', () => {
    const { sessionStorage } = installBrowserStorage()
    const state = createDraftState({
      days: 4,
      budget: 'comfort',
      preference: 'natural'
    })

    const { persistRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey: 'colorful-tibet:route-planner:draft:19532458802',
      version: 2
    })

    persistRouteDraft()

    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft:19532458802')).toBeNull()
    expect(JSON.parse(sessionStorage.getItem('colorful-tibet:route-planner:draft') || '{}')).toMatchObject({
      version: 2,
      form: {
        days: 4,
        budget: 'comfort',
        preference: 'natural'
      }
    })
  })

  it('falls back to the default key when storage scope is not approved', () => {
    const { sessionStorage } = installBrowserStorage()
    const state = createDraftState({ days: 3, budget: 'economy', preference: 'culture' })

    useRoutePlannerDraft(state, {
      defaultForm,
      storageKey: 'colorful-tibet:route-planner:draft:42',
      version: 2
    }).persistRouteDraft()

    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft:42')).toBeNull()
    expect(JSON.parse(sessionStorage.getItem('colorful-tibet:route-planner:draft') || '{}')).toMatchObject({
      version: 2,
      form: {
        days: 3,
        budget: 'economy',
        preference: 'culture'
      }
    })
  })

  it('clears drafts written before the storage scope changes', () => {
    const { sessionStorage, localStorage } = installBrowserStorage()
    const storageScope = ref<'anonymous' | 'session'>('anonymous')
    const state = createDraftState({ days: 8, budget: 'comfort', preference: 'culture' })

    const { persistRouteDraft, clearRouteDraft } = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey: () => `colorful-tibet:route-planner:draft:${storageScope.value}`,
      version: 2
    })

    persistRouteDraft()
    localStorage.setItem('colorful-tibet:route-planner:draft:anonymous', 'legacy anonymous')
    storageScope.value = 'session'
    persistRouteDraft()

    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft:anonymous')).not.toBeNull()
    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft:session')).not.toBeNull()

    clearRouteDraft()

    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft:anonymous')).toBeNull()
    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft:session')).toBeNull()
    expect(localStorage.getItem('colorful-tibet:route-planner:draft:anonymous')).toBeNull()
  })

  it('clears both session drafts and legacy localStorage drafts', () => {
    const { sessionStorage, localStorage } = installBrowserStorage()
    sessionStorage.setItem('colorful-tibet:route-planner:draft', 'default session')
    sessionStorage.setItem('colorful-tibet:route-planner:draft:42', 'scoped session')
    localStorage.setItem('colorful-tibet:route-planner:draft', 'default local')
    localStorage.setItem('colorful-tibet:route-planner:draft:42', 'scoped local')

    clearAllRoutePlannerDrafts()

    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft')).toBeNull()
    expect(sessionStorage.getItem('colorful-tibet:route-planner:draft:42')).toBeNull()
    expect(localStorage.getItem('colorful-tibet:route-planner:draft')).toBeNull()
    expect(localStorage.getItem('colorful-tibet:route-planner:draft:42')).toBeNull()
  })

  it('treats blocked browser storage as unavailable', () => {
    const state = createDraftState()
    const blockedWindow = {}
    Object.defineProperty(blockedWindow, 'sessionStorage', {
      get() {
        throw new Error('session storage blocked')
      }
    })
    Object.defineProperty(blockedWindow, 'localStorage', {
      get() {
        throw new Error('local storage blocked')
      }
    })
    vi.stubGlobal('window', blockedWindow)

    const draftStorage = useRoutePlannerDraft(state, {
      defaultForm,
      storageKey,
      version: 2
    })

    expect(() => draftStorage.persistRouteDraft()).not.toThrow()
    expect(() => draftStorage.restoreRouteDraft()).not.toThrow()
    expect(() => draftStorage.clearRouteDraft()).not.toThrow()
    expect(() => clearAllRoutePlannerDrafts()).not.toThrow()
  })

  it('clears legacy local drafts even when session storage is unavailable', () => {
    const localStorage = new MemoryStorage()
    localStorage.setItem(storageKey, 'legacy scoped draft')
    localStorage.setItem('colorful-tibet:route-planner:draft', 'legacy default draft')
    const blockedWindow = {}
    Object.defineProperty(blockedWindow, 'sessionStorage', {
      get() {
        throw new Error('session storage blocked')
      }
    })
    Object.defineProperty(blockedWindow, 'localStorage', {
      get() {
        return localStorage
      }
    })
    vi.stubGlobal('window', blockedWindow)

    const draftStorage = useRoutePlannerDraft(createDraftState(), {
      defaultForm,
      storageKey,
      version: 2
    })

    draftStorage.clearRouteDraft()
    expect(localStorage.getItem(storageKey)).toBeNull()

    clearAllRoutePlannerDrafts()
    expect(localStorage.getItem('colorful-tibet:route-planner:draft')).toBeNull()
  })
})
