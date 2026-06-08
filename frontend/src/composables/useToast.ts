import { readonly, ref } from 'vue'
import { redactForErrorReport } from '../utils/errorMonitoring'

export type ToastType = 'info' | 'success' | 'warning' | 'error'

export interface ToastMessage {
  id: number
  message: string
  type: ToastType
}

const toasts = ref<ToastMessage[]>([])
const toastTimers = new Map<number, ReturnType<typeof setTimeout>>()
let nextToastId = 1

const TOAST_MESSAGE_LIMIT = 240
const STACK_LINE_PATTERN = /^\s*(?:at\s+\S+|\.\.\. \d+ more|Caused by:|Traceback \(most recent call last\):)/i
const SERVER_INTERNAL_PATTERN = /(?:exception|stack trace|org\.springframework|java\.|javax\.|jakarta\.|\bsql\w*|syntaxerror|referenceerror|typeerror|\/var\/|\/srv\/|[A-Z]:\\)/i
const FALLBACK_MESSAGES: Record<ToastType, string> = {
  info: '操作已收到，请继续查看页面提示',
  success: '操作成功',
  warning: '操作需要检查，请稍后重试',
  error: '操作失败，请稍后重试'
}

export const normalizeToastMessage = (message: unknown, type: ToastType = 'info') => {
  const redacted = redactForErrorReport(String(message || '')) || ''
  const visibleLines = redacted
    .split(/\r?\n/)
    .filter(line => !STACK_LINE_PATTERN.test(line))
  const normalized = visibleLines.join(' ').replace(/\s+/g, ' ').trim()

  if (!normalized) return ''
  if (SERVER_INTERNAL_PATTERN.test(normalized)) return FALLBACK_MESSAGES[type]

  return normalized.length > TOAST_MESSAGE_LIMIT
    ? `${normalized.slice(0, TOAST_MESSAGE_LIMIT - 1)}…`
    : normalized
}

export const dismissToast = (id: number) => {
  const timer = toastTimers.get(id)
  if (timer) {
    clearTimeout(timer)
    toastTimers.delete(id)
  }
  toasts.value = toasts.value.filter(toast => toast.id !== id)
}

export const clearToasts = () => {
  toastTimers.forEach(timer => clearTimeout(timer))
  toastTimers.clear()
  toasts.value = []
}

export const showToast = (message: string, type: ToastType = 'info', duration = 3600) => {
  const normalizedMessage = normalizeToastMessage(message, type)
  if (!normalizedMessage) return undefined

  const id = nextToastId++
  toasts.value = [...toasts.value, { id, message: normalizedMessage, type }]

  if (duration > 0) {
    const timer = setTimeout(() => dismissToast(id), duration)
    toastTimers.set(id, timer)
  }

  return id
}

export const useToast = () => ({
  toasts: readonly(toasts),
  showToast,
  dismissToast,
  clearToasts
})
