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

function truncate(value: string | undefined, limit: number): string | undefined {
  if (!value) {
    return undefined
  }

  return value.length > limit ? `${value.slice(0, limit)}...` : value
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
      message: truncate(error.message, 1000) || 'Unknown error',
      stack: truncate(error.stack, 4000),
    }
  }

  return {
    name: 'NonErrorRejection',
    message: truncate(stringifyUnknown(error), 1000) || 'Unknown error',
  }
}

function sendPayload(payload: ErrorPayload): void {
  if (!ERROR_REPORT_URL) {
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
    info: truncate(info, 500),
    path: router.currentRoute.value.fullPath,
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
