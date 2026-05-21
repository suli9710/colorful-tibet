import { readonly, ref } from 'vue'

export type ToastType = 'info' | 'success' | 'warning' | 'error'

export interface ToastMessage {
  id: number
  message: string
  type: ToastType
}

const toasts = ref<ToastMessage[]>([])
let nextToastId = 1

export const dismissToast = (id: number) => {
  toasts.value = toasts.value.filter(toast => toast.id !== id)
}

export const showToast = (message: string, type: ToastType = 'info', duration = 3600) => {
  const normalizedMessage = String(message || '').trim()
  if (!normalizedMessage) return

  const id = nextToastId++
  toasts.value = [...toasts.value, { id, message: normalizedMessage, type }]

  window.setTimeout(() => dismissToast(id), duration)
}

export const installAlertToastBridge = () => {
  if (typeof window === 'undefined') return

  const bridgeKey = '__colorfulTibetToastAlertBridgeInstalled'
  if ((window as any)[bridgeKey]) return

  ;(window as any)[bridgeKey] = true
  window.alert = (message?: any) => {
    showToast(String(message ?? ''), 'info')
  }
}

export const useToast = () => ({
  toasts: readonly(toasts),
  showToast,
  dismissToast
})
