import type { App } from 'vue'
import type { Router } from 'vue-router'

type ErrorSource = 'vue' | 'window.error' | 'unhandledrejection'

interface ErrorPayload {
  source: ErrorSource
  name: string
  message: string
  stack?: string
  info?: string
  path: string
  release: string
  userAgent: string
  timestamp: string
}

const ERROR_REPORT_URL = import.meta.env.VITE_FRONTEND_ERROR_REPORT_URL as string | undefined
const RELEASE = (import.meta.env.VITE_APP_VERSION as string | undefined) || 'development'
const MAX_REPORTS_PER_MINUTE = 5
const REDACTION_PATTERN = /((?:token|secret|password|authorization|code|key)=)[^&\s]+/gi
let reportWindowStartedAt = 0
let reportsInWindow = 0

function truncate(value: string | undefined, limit: number): string | undefined {
  if (!value) {
    return undefined
  }

  return value.length > limit ? `${value.slice(0, limit)}...` : value
}

function redact(value: string | undefined): string | undefined {
  return truncate(value?.replace(REDACTION_PATTERN, '$1<redacted>'), value === undefined ? 0 : value.length)
}

function stringifyUnknown(value: unknown): string {
  if (typeof value === 'string') {
    return value
  }

  try {
    return JSON.stringify(value) ?? String(value)
  } catch {
    return String(value)
  }
}

function normalizeError(error: unknown): Pick<ErrorPayload, 'name' | 'message' | 'stack'> {
  if (error instanceof Error) {
    return {
      name: error.name || 'Error',
      message: truncate(redact(error.message), 1000) || 'Unknown error',
      stack: truncate(redact(error.stack), 4000),
    }
  }

  return {
    name: 'NonErrorRejection',
    message: truncate(redact(stringifyUnknown(error)), 1000) || 'Unknown error',
  }
}

function safeReportPath(router: Router): string {
  const route = router.currentRoute.value
  return route.path || '/'
}

function shouldSendReport(): boolean {
  const now = Date.now()
  if (now - reportWindowStartedAt > 60_000) {
    reportWindowStartedAt = now
    reportsInWindow = 0
  }
  reportsInWindow += 1
  return reportsInWindow <= MAX_REPORTS_PER_MINUTE
}

function isAllowedReportUrl(value: string): boolean {
  try {
    const url = new URL(value, window.location.origin)
    return url.origin === window.location.origin && url.pathname.startsWith('/api/')
  } catch {
    return false
  }
}

function sendPayload(payload: ErrorPayload): void {
  if (!ERROR_REPORT_URL || !shouldSendReport() || !isAllowedReportUrl(ERROR_REPORT_URL)) {
    return
  }

  const body = JSON.stringify(payload)

  if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
    const blob = new Blob([body], { type: 'application/json' })
    if (navigator.sendBeacon(ERROR_REPORT_URL, blob)) {
      return
    }
  }

  void fetch(ERROR_REPORT_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
    keepalive: true,
  }).catch(() => undefined)
}

function reportError(source: ErrorSource, error: unknown, router: Router, info?: string): void {
  const normalized = normalizeError(error)
  const payload: ErrorPayload = {
    source,
    ...normalized,
    info: truncate(redact(info), 500),
    path: safeReportPath(router),
    release: RELEASE,
    userAgent: navigator.userAgent,
    timestamp: new Date().toISOString(),
  }

  if (import.meta.env.DEV) {
    console.error('[frontend-error]', payload)
  }

  sendPayload(payload)
}

export function registerErrorMonitoring(app: App, router: Router): void {
  app.config.errorHandler = (error, _instance, info) => {
    reportError('vue', error, router, info)
  }

  if (typeof window === 'undefined') {
    return
  }

  window.addEventListener('error', (event) => {
    reportError('window.error', event.error ?? event.message, router)
  })

  window.addEventListener('unhandledrejection', (event) => {
    reportError('unhandledrejection', event.reason, router)
  })
}
