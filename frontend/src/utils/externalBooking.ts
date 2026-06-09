export type ExternalBookingKind = 'spot' | 'hotel' | 'route'

export interface ExternalBookingTarget {
  kind: ExternalBookingKind
  name?: string | null
  location?: string | null
  date?: string | null
  officialUrl?: string | null
  externalUrl?: string | null
  providerUrls?: Partial<Record<string, string | null>>
}

export interface ExternalBookingProvider {
  id: string
  name: string
  allowedHostSuffixes: string[]
  buildUrl: (keyword: string) => string
}

const encode = (value: string) => encodeURIComponent(value.trim())
const UTM_SOURCE = 'colorful_tibet'
const EXTRA_ALLOWED_HOSTS_ENV = 'VITE_EXTERNAL_BOOKING_ALLOWED_HOSTS'

const providerKeyword = (target: ExternalBookingTarget) => {
  const name = target.name?.trim() || '西藏旅游'
  const location = target.location?.trim()
  const suffix = target.kind === 'hotel'
    ? '酒店 预订'
    : target.kind === 'spot'
      ? '门票 预订'
      : '旅游 线路 预订'
  return [location, name, suffix].filter(Boolean).join(' ')
}

export const externalBookingProviders: ExternalBookingProvider[] = [
  {
    id: 'ctrip',
    name: '携程',
    allowedHostSuffixes: ['ctrip.com'],
    buildUrl: keyword => `https://you.ctrip.com/SearchSite/Default/Destination?query=${encode(keyword)}`
  },
  {
    id: 'fliggy',
    name: '飞猪',
    allowedHostSuffixes: ['fliggy.com'],
    buildUrl: keyword => `https://travelsearch.fliggy.com/index.htm?keyword=${encode(keyword)}`
  },
  {
    id: 'meituan',
    name: '美团',
    allowedHostSuffixes: ['meituan.com'],
    buildUrl: keyword => `https://www.meituan.com/s/${encode(keyword)}/`
  },
  {
    id: 'qunar',
    name: '去哪儿',
    allowedHostSuffixes: ['qunar.com'],
    buildUrl: keyword => `https://travel.qunar.com/search/all/${encode(keyword)}`
  }
]

function envValue(name: string): string {
  const env = import.meta.env as unknown as Record<string, string | undefined>
  return String(env[name] || '').trim()
}

function normalizeHostSuffix(value: string) {
  const trimmedValue = value.trim().toLowerCase()
  if (!trimmedValue) return ''

  try {
    return new URL(trimmedValue).hostname.replace(/\.$/, '')
  } catch {
    return trimmedValue.replace(/^\.+/, '').replace(/\.$/, '')
  }
}

function configuredAllowedHostSuffixes() {
  return envValue(EXTRA_ALLOWED_HOSTS_ENV)
    .split(',')
    .map(normalizeHostSuffix)
    .filter(Boolean)
}

function allAllowedHostSuffixes() {
  return [
    ...externalBookingProviders.flatMap(provider => provider.allowedHostSuffixes),
    ...configuredAllowedHostSuffixes()
  ]
}

function hostnameMatchesSuffix(hostname: string, suffix: string) {
  const normalizedHostname = normalizeHostSuffix(hostname)
  const normalizedSuffix = normalizeHostSuffix(suffix)
  return Boolean(normalizedHostname && normalizedSuffix)
    && (normalizedHostname === normalizedSuffix || normalizedHostname.endsWith(`.${normalizedSuffix}`))
}

function isAllowedBookingUrl(url: URL, allowedHostSuffixes: string[]) {
  return url.protocol === 'https:'
    && !url.username
    && !url.password
    && allowedHostSuffixes.some(suffix => hostnameMatchesSuffix(url.hostname, suffix))
}

function withUtmSource(rawUrl: string, allowedHostSuffixes: string[]) {
  try {
    const url = new URL(rawUrl)
    if (!isAllowedBookingUrl(url, allowedHostSuffixes)) return ''
    if (!url.searchParams.has('utm_source')) {
      url.searchParams.set('utm_source', UTM_SOURCE)
    }
    return url.toString()
  } catch {
    return ''
  }
}

function directBookingUrl(target: ExternalBookingTarget, provider: ExternalBookingProvider) {
  const providerUrl = target.providerUrls?.[provider.id]
  const trustedProviderUrl = withUtmSource(providerUrl || '', provider.allowedHostSuffixes)
  if (trustedProviderUrl) return trustedProviderUrl

  for (const candidate of [target.externalUrl, target.officialUrl]) {
    const safeUrl = withUtmSource(candidate || '', allAllowedHostSuffixes())
    if (safeUrl) return safeUrl
  }

  return ''
}

export function getExternalBookingUrl(target: ExternalBookingTarget, providerId = 'ctrip') {
  const provider = externalBookingProviders.find(item => item.id === providerId) || externalBookingProviders[0]
  return directBookingUrl(target, provider) || withUtmSource(provider.buildUrl(providerKeyword(target)), provider.allowedHostSuffixes)
}

export function openExternalBooking(target: ExternalBookingTarget, providerId = 'ctrip') {
  const url = getExternalBookingUrl(target, providerId)
  return window.open(url, '_blank', 'noopener,noreferrer')
}
