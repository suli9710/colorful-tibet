import { applyThirdPartyScriptSecurity } from './scriptSecurity'

let loadPromise: Promise<void> | null = null
let loadedScriptKey = ''
const ENABLED_VALUES = new Set(['1', 'true', 'yes', 'on'])
type RecaptchaMode = 'v2' | 'v3'

export class RecaptchaError extends Error {
  constructor(message = 'reCAPTCHA verification failed') {
    super(message)
    this.name = 'RecaptchaError'
  }
}

export function isRecaptchaError(error: unknown): error is RecaptchaError {
  return error instanceof RecaptchaError
    || Boolean(error && typeof error === 'object' && (error as { name?: string }).name === 'RecaptchaError')
}

function recaptchaSiteKey(): string {
  return String(import.meta.env.VITE_RECAPTCHA_SITE_KEY || '').trim()
}

function recaptchaMode(): RecaptchaMode {
  return String(import.meta.env.VITE_RECAPTCHA_MODE || 'v3').trim().toLowerCase() === 'v2'
    ? 'v2'
    : 'v3'
}

function recaptchaEnabled(): boolean {
  const rawValue = import.meta.env.VITE_RECAPTCHA_ENABLED
  if (rawValue === undefined || rawValue === '') {
    return Boolean(recaptchaSiteKey())
  }
  return ENABLED_VALUES.has(String(rawValue).trim().toLowerCase())
}

function waitForReady(mode: RecaptchaMode, timeoutMs = 8000): Promise<void> {
  return new Promise((resolve, reject) => {
    const startedAt = Date.now()
    const timeout = window.setTimeout(() => {
      reject(new Error('Timed out waiting for reCAPTCHA'))
    }, timeoutMs)

    const checkReady = () => {
      const recaptcha = window.grecaptcha
      const hasRequiredApi = mode === 'v2'
        ? typeof recaptcha?.render === 'function'
          && typeof recaptcha?.getResponse === 'function'
          && typeof recaptcha?.reset === 'function'
        : typeof recaptcha?.ready === 'function'
          && typeof recaptcha?.execute === 'function'

      if (hasRequiredApi && recaptcha) {
        window.clearTimeout(timeout)
        if (mode === 'v3') {
          recaptcha.ready(() => resolve())
        } else {
          resolve()
        }
        return
      }

      if (Date.now() - startedAt < timeoutMs) {
        window.setTimeout(checkReady, 50)
      }
    }

    checkReady()
  })
}

function loadScript(mode: RecaptchaMode, siteKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>('script[src*="recaptcha/api.js"]')
    if (existingScript) {
      applyThirdPartyScriptSecurity(existingScript, 'recaptcha')
      waitForReady(mode).then(resolve, reject)
      return
    }

    const script = document.createElement('script')
    const renderParam = mode === 'v2' ? 'explicit' : encodeURIComponent(siteKey)
    script.src = `https://www.recaptcha.net/recaptcha/api.js?render=${renderParam}`
    script.async = true
    script.defer = true
    applyThirdPartyScriptSecurity(script, 'recaptcha')
    script.onload = () => waitForReady(mode).then(resolve, reject)
    script.onerror = () => reject(new Error('Failed to load reCAPTCHA'))
    document.head.appendChild(script)
  })
}

export function loadRecaptcha(siteKey: string): Promise<void> {
  const mode = recaptchaMode()
  const scriptKey = `${mode}:${siteKey}`
  if (!loadPromise || loadedScriptKey !== scriptKey) {
    loadedScriptKey = scriptKey
    loadPromise = loadScript(mode, siteKey).catch(error => {
      loadPromise = null
      loadedScriptKey = ''
      throw error
    })
  }
  return loadPromise
}

export function hasRecaptchaSiteKey(): boolean {
  return recaptchaEnabled() && Boolean(recaptchaSiteKey())
}

export function isRecaptchaV2Enabled(): boolean {
  return recaptchaEnabled() && recaptchaMode() === 'v2' && Boolean(recaptchaSiteKey())
}

export function isRecaptchaV3Enabled(): boolean {
  return recaptchaEnabled() && recaptchaMode() === 'v3' && Boolean(recaptchaSiteKey())
}

export async function getRecaptchaToken(action: string): Promise<string> {
  const siteKey = recaptchaSiteKey()
  if (!isRecaptchaV3Enabled() || !siteKey) return ''

  try {
    await loadRecaptcha(siteKey)
    const recaptcha = window.grecaptcha
    if (!recaptcha?.execute) {
      throw new RecaptchaError('reCAPTCHA execute API is not available')
    }
    const token = await recaptcha.execute(siteKey, { action })
    if (!token) {
      throw new RecaptchaError('reCAPTCHA returned an empty token')
    }
    return token
  } catch (error) {
    if (isRecaptchaError(error)) throw error
    throw new RecaptchaError()
  }
}

export async function renderRecaptchaCheckbox(
  container: HTMLElement,
  callbacks: {
    onVerify?: (token: string) => void
    onExpired?: () => void
    onError?: () => void
  } = {}
): Promise<number | null> {
  const siteKey = recaptchaSiteKey()
  if (!isRecaptchaV2Enabled() || !siteKey) return null

  await loadRecaptcha(siteKey)
  const recaptcha = window.grecaptcha
  if (!recaptcha?.render) {
    throw new Error('reCAPTCHA render API is not available')
  }
  container.innerHTML = ''
  return recaptcha.render(container, {
    sitekey: siteKey,
    theme: 'light',
    callback: token => callbacks.onVerify?.(token),
    'expired-callback': () => callbacks.onExpired?.(),
    'error-callback': () => callbacks.onError?.()
  })
}

export function getRecaptchaWidgetResponse(widgetId: number | null): string {
  const recaptcha = window.grecaptcha
  if (widgetId === null || widgetId === undefined || !recaptcha?.getResponse) return ''
  return recaptcha.getResponse(widgetId)
}

export function resetRecaptchaWidget(widgetId: number | null): void {
  const recaptcha = window.grecaptcha
  if (widgetId === null || widgetId === undefined || !recaptcha?.reset) return
  recaptcha.reset(widgetId)
}
