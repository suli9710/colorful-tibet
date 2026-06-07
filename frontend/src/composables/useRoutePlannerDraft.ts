import { unref, type Ref } from 'vue'

const DEFAULT_STORAGE_KEY = 'colorful-tibet:route-planner:draft'
const DEFAULT_DRAFT_VERSION = 1

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

const hasBrowserStorage = () => typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'

const normalizeDraftForm = (
  draftForm: Partial<RoutePlannerFormState> | undefined,
  defaultForm: RoutePlannerFormState
): RoutePlannerFormState => {
  const days = Number(draftForm?.days)
  return {
    days: Number.isFinite(days) ? Math.min(30, Math.max(1, Math.round(days))) : defaultForm.days,
    budget: draftForm?.budget || defaultForm.budget,
    preference: draftForm?.preference || defaultForm.preference
  }
}

export function useRoutePlannerDraft(
  state: RoutePlannerDraftState,
  options: RoutePlannerDraftOptions
) {
  const resolveStorageKey = () => {
    if (typeof options.storageKey === 'function') {
      return options.storageKey() || DEFAULT_STORAGE_KEY
    }
    return unref(options.storageKey) || DEFAULT_STORAGE_KEY
  }
  const version = options.version || DEFAULT_DRAFT_VERSION

  const persistRouteDraft = (overrides: Partial<RoutePlannerDraft> = {}) => {
    if (!hasBrowserStorage()) return
    const storageKey = resolveStorageKey()
    routeDraftKeys.add(storageKey)

    const payload: RoutePlannerDraft = {
      version,
      form: { ...state.form.value },
      result: state.result.value,
      jobId: state.jobId?.value || '',
      statusMessage: state.statusMessage.value,
      errorMessage: state.errorMessage.value,
      completed: !state.loading.value && !state.streaming.value && !!state.result.value && !state.errorMessage.value,
      updatedAt: Date.now(),
      ...overrides
    }

    try {
      localStorage.setItem(storageKey, JSON.stringify(payload))
    } catch (error) {
      options.onPersistError?.(error)
    }
  }

  const restoreRouteDraft = () => {
    if (!hasBrowserStorage()) return

    const storageKey = resolveStorageKey()
    const rawDraft = localStorage.getItem(storageKey)
    if (!rawDraft) return

    try {
      const draft = JSON.parse(rawDraft) as Partial<RoutePlannerDraft>
      if (draft.version !== version) return

      const restoredDraft: RoutePlannerDraft = {
        version,
        form: normalizeDraftForm(draft.form, options.defaultForm),
        result: typeof draft.result === 'string' ? draft.result : '',
        jobId: typeof draft.jobId === 'string' ? draft.jobId : '',
        statusMessage: typeof draft.statusMessage === 'string' ? draft.statusMessage : '',
        errorMessage: typeof draft.errorMessage === 'string' ? draft.errorMessage : '',
        completed: Boolean(draft.completed),
        updatedAt: Number(draft.updatedAt) || 0
      }

      state.form.value = restoredDraft.form
      if (state.jobId) {
        state.jobId.value = restoredDraft.jobId
      }

      if (!restoredDraft.result) return restoredDraft

      state.result.value = restoredDraft.result
      state.errorMessage.value = restoredDraft.errorMessage
      state.statusMessage.value = restoredDraft.statusMessage || options.getRestoredStatusMessage?.(restoredDraft) || ''
      options.onRestoreResult?.(restoredDraft.result, restoredDraft)

      return restoredDraft
    } catch (error) {
      options.onRestoreError?.(error)
      localStorage.removeItem(storageKey)
    }
  }

  const clearRouteDraft = () => {
    if (!hasBrowserStorage()) return
    const storageKey = resolveStorageKey()
    routeDraftKeys.delete(storageKey)
    localStorage.removeItem(storageKey)
  }

  return {
    persistRouteDraft,
    restoreRouteDraft,
    clearRouteDraft
  }
}

export function clearAllRoutePlannerDrafts() {
  if (!hasBrowserStorage()) return
  for (const key of routeDraftKeys) {
    localStorage.removeItem(key)
  }
  routeDraftKeys.clear()
  for (let index = localStorage.length - 1; index >= 0; index -= 1) {
    const key = localStorage.key(index)
    if (key?.startsWith('colorful-tibet:route-planner:draft:')) {
      localStorage.removeItem(key)
    }
  }
}
