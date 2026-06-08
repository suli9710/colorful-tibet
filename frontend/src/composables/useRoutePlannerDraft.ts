import { unref, type Ref } from 'vue'

const ROUTE_DRAFT_KEY_PREFIX = 'colorful-tibet:route-planner:draft'
const DEFAULT_STORAGE_KEY = ROUTE_DRAFT_KEY_PREFIX
const DEFAULT_DRAFT_VERSION = 1
const DEFAULT_DRAFT_TTL_MS = 6 * 60 * 60 * 1000
const DRAFT_STORAGE_MAX_CHARS = 2048
const SAFE_FORM_KEY_PATTERN = /^[a-z][a-z0-9_-]{0,31}$/i
const SAFE_STORAGE_KEY_PATTERN = /^colorful-tibet:route-planner:draft(?::(?:anonymous|session))?$/i
const SENSITIVE_TEXT_PATTERNS = [
  /(?:^|[^\d])1[3-9]\d{9}(?!\d)/,
  /(?:^|[^\d])\d{15}(?!\d)/,
  /(?:^|[^\d])\d{17}[\dXx](?![\dXx])/,
  /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i,
  /\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b/,
  /\b(?:bearer|jwt|token|secret|api[_-]?key|access[_-]?token|refresh[_-]?token|session)[_:= -]?[A-Za-z0-9._~+/=-]{16,}\b/i,
  /\b[A-Fa-f0-9]{32,}\b/,
  /\b[A-Za-z0-9_-]{48,}\b/
] as const

export interface RoutePlannerFormState {
  days: number
  budget: string
  preference: string
}

export interface RoutePlannerDraft {
  version: number
  form: RoutePlannerFormState
  result: string
  jobId: string
  statusMessage: string
  errorMessage: string
  completed: boolean
  updatedAt: number
}

const routeDraftKeys = new Set<string>()

interface RoutePlannerDraftState {
  form: Ref<RoutePlannerFormState>
  result: Ref<string>
  jobId?: Ref<string>
  statusMessage: Ref<string>
  errorMessage: Ref<string>
  loading: Ref<boolean>
  streaming: Ref<boolean>
}

interface RoutePlannerDraftOptions {
  defaultForm: RoutePlannerFormState
  storageKey?: string | Ref<string> | (() => string)
  version?: number
  getRestoredStatusMessage?: (draft: RoutePlannerDraft) => string
  onRestoreResult?: (result: string, draft: RoutePlannerDraft) => void
  onPersistError?: (error: unknown) => void
  onRestoreError?: (error: unknown) => void
}

const getBrowserStorage = (kind: 'sessionStorage' | 'localStorage'): Storage | null => {
  if (typeof window === 'undefined') return null
  try {
    return window[kind] || null
  } catch {
    return null
  }
}

const getSessionStorage = () => getBrowserStorage('sessionStorage')
const getLocalStorage = () => getBrowserStorage('localStorage')
const hasBrowserStorage = () => Boolean(getSessionStorage())
const removeSessionDraft = (key: string) => {
  try {
    getSessionStorage()?.removeItem(key)
  } catch {
    // Ignore cleanup failures in private browsing or locked-down contexts.
  }
}

const readSessionDraft = (key: string) => {
  try {
    return getSessionStorage()?.getItem(key) || ''
  } catch {
    return ''
  }
}

const writeSessionDraft = (key: string, value: string) => {
  const storage = getSessionStorage()
  if (!storage) return
  storage.setItem(key, value)
}

const clearLegacyLocalDraft = (key: string) => {
  try {
    getLocalStorage()?.removeItem(key)
  } catch {
    // Ignore legacy cleanup failures.
  }
}

const isSensitiveStorageText = (value: string) =>
  SENSITIVE_TEXT_PATTERNS.some(pattern => pattern.test(value))

const normalizeSafeFormValue = (value: unknown) => {
  if (typeof value !== 'string') return null
  const normalized = value.trim()
  if (!SAFE_FORM_KEY_PATTERN.test(normalized) || isSensitiveStorageText(normalized)) return null
  return normalized
}

const normalizeFormKey = (value: unknown, fallback: string) => {
  return normalizeSafeFormValue(value) ?? normalizeSafeFormValue(fallback) ?? ''
}

const normalizeStorageKey = (value: unknown) => {
  if (typeof value !== 'string') return DEFAULT_STORAGE_KEY
  const normalized = value.trim()
  if (!SAFE_STORAGE_KEY_PATTERN.test(normalized) || isSensitiveStorageText(normalized)) {
    return DEFAULT_STORAGE_KEY
  }
  return normalized
}

const normalizeDraftForm = (
  draftForm: Partial<RoutePlannerFormState> | undefined,
  defaultForm: RoutePlannerFormState
): RoutePlannerFormState => {
  const days = Number(draftForm?.days)
  return {
    days: Number.isFinite(days) ? Math.min(30, Math.max(1, Math.round(days))) : defaultForm.days,
    budget: normalizeFormKey(draftForm?.budget, defaultForm.budget),
    preference: normalizeFormKey(draftForm?.preference, defaultForm.preference)
  }
}

const normalizeJobId = (_jobId: unknown) => {
  return ''
}

const normalizeUpdatedAt = (updatedAt: unknown) => {
  const timestamp = Number(updatedAt)
  return Number.isFinite(timestamp) && timestamp > 0 ? timestamp : 0
}

const isExpiredDraft = (updatedAt: number) =>
  !updatedAt || Date.now() - updatedAt > DEFAULT_DRAFT_TTL_MS

const createStorableDraft = (
  draft: Partial<RoutePlannerDraft>,
  defaultForm: RoutePlannerFormState,
  version: number
): RoutePlannerDraft => ({
  version,
  form: normalizeDraftForm(draft.form, defaultForm),
  result: '',
  jobId: normalizeJobId(draft.jobId),
  statusMessage: '',
  errorMessage: '',
  completed: false,
  updatedAt: normalizeUpdatedAt(draft.updatedAt) || Date.now()
})

export function useRoutePlannerDraft(
  state: RoutePlannerDraftState,
  options: RoutePlannerDraftOptions
) {
  const resolveStorageKey = () => {
    if (typeof options.storageKey === 'function') {
      return normalizeStorageKey(options.storageKey() || DEFAULT_STORAGE_KEY)
    }
    return normalizeStorageKey(unref(options.storageKey) || DEFAULT_STORAGE_KEY)
  }
  const instanceDraftKeys = new Set<string>()
  const rememberStorageKey = (key: string) => {
    routeDraftKeys.add(key)
    instanceDraftKeys.add(key)
    return key
  }
  const version = options.version || DEFAULT_DRAFT_VERSION

  const persistRouteDraft = (overrides: Partial<RoutePlannerDraft> = {}) => {
    if (!hasBrowserStorage()) return
    const storageKey = rememberStorageKey(resolveStorageKey())

    const payload = createStorableDraft({
      version,
      form: { ...state.form.value },
      jobId: state.jobId?.value || '',
      updatedAt: Date.now(),
      ...overrides
    }, options.defaultForm, version)

    try {
      const serialized = JSON.stringify(payload)
      if (serialized.length > DRAFT_STORAGE_MAX_CHARS) {
        removeSessionDraft(storageKey)
        options.onPersistError?.(new Error('Route planner draft is too large to store safely'))
        return
      }
      writeSessionDraft(storageKey, serialized)
      clearLegacyLocalDraft(storageKey)
    } catch (error) {
      removeSessionDraft(storageKey)
      options.onPersistError?.(error)
    }
  }

  const restoreRouteDraft = () => {
    if (!hasBrowserStorage()) return

    const storageKey = rememberStorageKey(resolveStorageKey())
    const rawDraft = readSessionDraft(storageKey)
    if (!rawDraft) return
    clearLegacyLocalDraft(storageKey)
    if (rawDraft.length > DRAFT_STORAGE_MAX_CHARS) {
      removeSessionDraft(storageKey)
      return
    }

    try {
      const draft = JSON.parse(rawDraft) as Partial<RoutePlannerDraft>
      if (draft.version !== version) {
        removeSessionDraft(storageKey)
        return
      }
      const updatedAt = normalizeUpdatedAt(draft.updatedAt)
      if (isExpiredDraft(updatedAt)) {
        removeSessionDraft(storageKey)
        return
      }

      const restoredDraft = createStorableDraft({ ...draft, updatedAt }, options.defaultForm, version)
      const serialized = JSON.stringify(restoredDraft)
      if (serialized.length > DRAFT_STORAGE_MAX_CHARS) {
        removeSessionDraft(storageKey)
        return
      }
      writeSessionDraft(storageKey, serialized)

      state.form.value = restoredDraft.form
      if (state.jobId) {
        state.jobId.value = ''
      }

      if (!restoredDraft.result) return restoredDraft

      state.result.value = restoredDraft.result
      state.errorMessage.value = restoredDraft.errorMessage
      state.statusMessage.value = restoredDraft.statusMessage || options.getRestoredStatusMessage?.(restoredDraft) || ''
      options.onRestoreResult?.(restoredDraft.result, restoredDraft)

      return restoredDraft
    } catch (error) {
      options.onRestoreError?.(error)
      removeSessionDraft(storageKey)
    }
  }

  const clearRouteDraft = () => {
    const keysToClear = new Set([...instanceDraftKeys, resolveStorageKey()])
    for (const storageKey of keysToClear) {
      routeDraftKeys.delete(storageKey)
      removeSessionDraft(storageKey)
      clearLegacyLocalDraft(storageKey)
    }
    instanceDraftKeys.clear()
  }

  return {
    persistRouteDraft,
    restoreRouteDraft,
    clearRouteDraft
  }
}

export function clearAllRoutePlannerDrafts() {
  for (const key of routeDraftKeys) {
    removeSessionDraft(key)
    clearLegacyLocalDraft(key)
  }
  routeDraftKeys.clear()
  const sessionStorage = getSessionStorage()
  try {
    for (let index = (sessionStorage?.length || 0) - 1; index >= 0; index -= 1) {
      const key = sessionStorage?.key(index)
      if (key === DEFAULT_STORAGE_KEY || key?.startsWith(`${ROUTE_DRAFT_KEY_PREFIX}:`)) {
        removeSessionDraft(key)
      }
    }
  } catch {
    // Ignore storage enumeration failures.
  }
  const localStorage = getLocalStorage()
  try {
    localStorage?.removeItem(DEFAULT_STORAGE_KEY)
    for (let index = (localStorage?.length || 0) - 1; index >= 0; index -= 1) {
      const key = localStorage?.key(index)
      if (key?.startsWith(`${ROUTE_DRAFT_KEY_PREFIX}:`)) {
        localStorage?.removeItem(key)
      }
    }
  } catch {
    // Ignore legacy cleanup failures.
  }
}
