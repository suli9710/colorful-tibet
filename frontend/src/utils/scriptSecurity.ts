type ThirdPartyScriptProvider = 'amap' | 'recaptcha'
type ScriptCrossOrigin = 'anonymous' | 'use-credentials'

const PROVIDER_ENV_PREFIX: Record<ThirdPartyScriptProvider, string> = {
  amap: 'AMAP',
  recaptcha: 'RECAPTCHA'
}

export interface ThirdPartyScriptSecurityConfig {
  nonce: string
  integrity: string
  crossOrigin?: ScriptCrossOrigin
}

function envValue(name: string): string {
  const env = import.meta.env as unknown as Record<string, string | undefined>
  return String(env[name] || '').trim()
}

function metaContent(names: string[]): string {
  if (typeof document === 'undefined') return ''

  for (const name of names) {
    const element = document.querySelector<HTMLMetaElement>(`meta[name="${name}"], meta[property="${name}"]`)
    const content = element?.content.trim()
    if (content) return content
  }

  return ''
}

function windowNonce(): string {
  if (typeof window === 'undefined') return ''

  const nonce = (window as Window & { __CSP_NONCE__?: unknown }).__CSP_NONCE__
  return typeof nonce === 'string' ? nonce.trim() : ''
}

function normalizeCrossOrigin(value: string): ScriptCrossOrigin | undefined {
  const normalized = value.trim().toLowerCase()
  if (normalized === 'anonymous' || normalized === 'use-credentials') return normalized
  return undefined
}

export function thirdPartyScriptSecurityConfig(
  provider: ThirdPartyScriptProvider
): ThirdPartyScriptSecurityConfig {
  const prefix = PROVIDER_ENV_PREFIX[provider]
  const nonce = metaContent(['csp-nonce'])
    || windowNonce()
    || envValue('VITE_CSP_NONCE')
  const integrity = metaContent([
    `${provider}-script-integrity`,
    `third-party-${provider}-script-integrity`
  ]) || envValue(`VITE_${prefix}_SCRIPT_INTEGRITY`)
  const configuredCrossOrigin = metaContent([
    `${provider}-script-crossorigin`,
    `third-party-${provider}-script-crossorigin`
  ]) || envValue(`VITE_${prefix}_SCRIPT_CROSSORIGIN`)
  const crossOrigin = normalizeCrossOrigin(configuredCrossOrigin)
    || (integrity ? 'anonymous' : undefined)

  return {
    nonce,
    integrity,
    crossOrigin
  }
}

export function applyThirdPartyScriptSecurity(
  script: HTMLScriptElement,
  provider: ThirdPartyScriptProvider
): ThirdPartyScriptSecurityConfig {
  const config = thirdPartyScriptSecurityConfig(provider)

  if (config.nonce) {
    script.nonce = config.nonce
    script.setAttribute('nonce', config.nonce)
  }

  if (config.integrity) {
    script.integrity = config.integrity
  }

  if (config.crossOrigin) {
    script.crossOrigin = config.crossOrigin
  }

  return config
}
