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
  buildUrl: (keyword: string) => string
}

const encode = (value: string) => encodeURIComponent(value.trim())
const UTM_SOURCE = 'colorful_tibet'

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
    buildUrl: keyword => `https://you.ctrip.com/SearchSite/Default/Destination?query=${encode(keyword)}`
  },
  {
    id: 'fliggy',
    name: '飞猪',
    buildUrl: keyword => `https://travelsearch.fliggy.com/index.htm?keyword=${encode(keyword)}`
  },
  {
    id: 'meituan',
    name: '美团',
    buildUrl: keyword => `https://www.meituan.com/s/${encode(keyword)}/`
  },
  {
    id: 'qunar',
    name: '去哪儿',
    buildUrl: keyword => `https://travel.qunar.com/search/all/${encode(keyword)}`
  }
]

function withUtmSource(rawUrl: string) {
  try {
    const url = new URL(rawUrl)
    if (!['http:', 'https:'].includes(url.protocol)) return ''
    if (!url.searchParams.has('utm_source')) {
      url.searchParams.set('utm_source', UTM_SOURCE)
    }
    return url.toString()
  } catch {
    return ''
  }
}

function directBookingUrl(target: ExternalBookingTarget, providerId: string) {
  const providerUrl = target.providerUrls?.[providerId]
  return withUtmSource(providerUrl || target.externalUrl || target.officialUrl || '')
}

export function getExternalBookingUrl(target: ExternalBookingTarget, providerId = 'ctrip') {
  const provider = externalBookingProviders.find(item => item.id === providerId) || externalBookingProviders[0]
  return directBookingUrl(target, provider.id) || withUtmSource(provider.buildUrl(providerKeyword(target)))
}

export function openExternalBooking(target: ExternalBookingTarget, providerId = 'ctrip') {
  const url = getExternalBookingUrl(target, providerId)
  return window.open(url, '_blank', 'noopener,noreferrer')
}
