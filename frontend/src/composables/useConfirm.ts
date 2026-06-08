import { computed, ref } from 'vue'

export type ConfirmTone = 'default' | 'danger'

export interface ConfirmOptions {
  title?: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  tone?: ConfirmTone
}

export interface ConfirmRequest extends Required<Pick<ConfirmOptions, 'message' | 'tone'>> {
  id: number
  title?: string
  confirmLabel?: string
  cancelLabel?: string
  resolve: (confirmed: boolean) => void
}

const queue = ref<ConfirmRequest[]>([])
let nextConfirmId = 1

export const activeConfirm = computed(() => queue.value[0] ?? null)

export const showConfirm = (options: ConfirmOptions | string): Promise<boolean> => {
  const normalizedOptions = typeof options === 'string' ? { message: options } : options
  const message = String(normalizedOptions.message || '').trim()
  if (!message) return Promise.resolve(false)

  return new Promise(resolve => {
    queue.value = [
      ...queue.value,
      {
        id: nextConfirmId++,
        message,
        title: normalizedOptions.title?.trim(),
        confirmLabel: normalizedOptions.confirmLabel?.trim(),
        cancelLabel: normalizedOptions.cancelLabel?.trim(),
        tone: normalizedOptions.tone || 'default',
        resolve
      }
    ]
  })
}

export const resolveConfirm = (confirmed: boolean) => {
  const current = queue.value[0]
  if (!current) return

  current.resolve(confirmed)
  queue.value = queue.value.slice(1)
}

export const clearConfirms = (confirmed = false) => {
  const pending = queue.value
  queue.value = []
  pending.forEach(request => request.resolve(confirmed))
}

export const useConfirm = () => ({
  activeConfirm,
  showConfirm,
  resolveConfirm,
  clearConfirms
})
