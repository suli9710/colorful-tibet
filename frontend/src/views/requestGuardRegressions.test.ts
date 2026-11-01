import { describe, expect, it } from 'vitest'
import adminDashboardSource from './AdminDashboard.vue?raw'
import loginSource from './Login.vue?raw'
import scenicSpotDetailSource from './ScenicSpotDetail.vue?raw'

describe('scenic spot detail stale-request guards', () => {
  it('never compares raw Number(route.params.id) values', () => {
    // Number('abc') is NaN and NaN !== NaN, so a non-numeric route id made every guard fail and
    // pinned loading/commentsLoading to true, rendering an infinite spinner with no error state.
    const rawComparisons = scenicSpotDetailSource.match(
      /requestedSpotId\s*[!=]==\s*Number\(route\.params\.id\)/g
    )
    expect(rawComparisons).toBeNull()
    expect(scenicSpotDetailSource).toContain('const routeSpotId = (): number | null =>')
    expect(scenicSpotDetailSource).toContain('Number.isFinite(parsed) ? parsed : null')
    expect(scenicSpotDetailSource).toContain('const isCurrentSpot = (id: number | null): boolean =>')
  })

  it('surfaces an error instead of spinning forever on a non-numeric route id', () => {
    expect(scenicSpotDetailSource).toContain('if (requestedSpotId === null) {')
    expect(scenicSpotDetailSource).toMatch(
      /if \(requestedSpotId === null\) \{[\s\S]*?detailError\.value = t\('spotDetail\.detailErrorMessage'\)[\s\S]*?loading\.value = false/
    )
  })

  it('gives comment pagination its own sequence counter', () => {
    // Sharing commentsRequestId meant a refresh invalidated an in-flight "next page" request and the
    // finally guard never cleared commentsLoadingMore, disabling the pagination button for good.
    expect(scenicSpotDetailSource).toContain('let commentsPageRequestId = 0')
    expect(scenicSpotDetailSource).toContain('const pageRequestId = ++commentsPageRequestId')
    expect(scenicSpotDetailSource).toMatch(
      /if \(pageRequestId === commentsPageRequestId\) \{\s*commentsLoadingMore\.value = false/
    )
  })
})

describe('login reCAPTCHA modes', () => {
  it('supports the v2 checkbox so step-up can never lock an account out', () => {
    // Login only implemented v3; built with the supported VITE_RECAPTCHA_MODE=v2 it sent no token at
    // all, so any account that tripped the backend step-up threshold could never authenticate again.
    expect(loginSource).toContain('isRecaptchaV2Enabled')
    expect(loginSource).toContain('getRecaptchaWidgetResponse')
    expect(loginSource).toContain('renderRecaptchaCheckbox')
    expect(loginSource).toContain('ref="recaptchaContainer"')
    expect(loginSource).toMatch(/if \(!widgetResponse\) throw new RecaptchaError\(\)/)
  })

  it('keeps a swallowed v3 failure actionable when the server then rejects the login', () => {
    expect(loginSource).toContain('recaptchaUnavailable = true')
    expect(loginSource).toMatch(
      /if \(recaptchaUnavailable && responseStatus\(error\) === 403\)[\s\S]*?t\('security\.recaptchaFailed'\)/
    )
  })
})

describe('admin dashboard polling load', () => {
  it('stops draining pages once a fetch is superseded', () => {
    expect(adminDashboardSource).toContain(
      'const fetchAllAdminPages = async <T>(url: string, isStale: () => boolean = () => false)'
    )
    expect(adminDashboardSource).toMatch(/for \(let page = 0; page < maxAdminListPages; page \+= 1\) \{\s*\/\/[\s\S]*?if \(isStale\(\)\) return collected/)
    expect(adminDashboardSource).toContain(
      'await fetchAllAdminPages<HotelOrder>(endpoints.hotelBookings.all, isStale)'
    )
  })

  it('does not walk every booking page on the fast operational tick', () => {
    expect(adminDashboardSource).toContain('const hotelOrderRefreshEveryTicks = 4')
    expect(adminDashboardSource).toContain('void pollOperationalData(operationalTicks % hotelOrderRefreshEveryTicks === 0)')
    expect(adminDashboardSource).toContain('includeHotelOrders && !loadingHotelOrders.value')
  })
})
