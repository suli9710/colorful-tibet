export interface RiskSignalRequest {
  method?: string
  url?: string
}

const RISK_ASSESSED_POST_PATHS = [
  /^\/(?:api\/)?orders\/?$/,
  /^\/(?:api\/)?bookings\/?$/,
  /^\/(?:api\/)?hotel-bookings\/?$/,
  /^\/(?:api\/)?itineraries\/\d+\/items\/\d+\/bookings\/?$/,
  /^\/(?:api\/)?payments\/callbacks\/mock\/?$/,
]

const requestPath = (url: string) => {
  try {
    return new URL(url, 'https://local.invalid').pathname
  } catch {
    return ''
  }
}

export const shouldAttachDeviceFingerprint = ({ method, url }: RiskSignalRequest) => {
  if (method?.toLowerCase() !== 'post' || !url) return false
  const path = requestPath(url)
  return RISK_ASSESSED_POST_PATHS.some(pattern => pattern.test(path))
}
