import { describe, expect, it } from 'vitest'
import favoritesSource from './Favorites.vue?raw'
import hotelBookingSource from './HotelBooking.vue?raw'
import hotelOrdersSource from './HotelOrders.vue?raw'
import orderCenterSource from './OrderCenter.vue?raw'
import scenicSpotsSource from './ScenicSpots.vue?raw'

describe('feedback view state safeguards', () => {
  it('keeps favorites load failures visible, safe, and actionable', () => {
    expect(favoritesSource).toContain('safeClientErrorMessage')
    expect(favoritesSource).toContain('summarizeClientError')
    expect(favoritesSource).toContain('errorMessage')
    expect(favoritesSource).toContain('收藏列表加载失败，请稍后重试')
    expect(favoritesSource).toContain('重新加载')
    expect(favoritesSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
    expect(favoritesSource).not.toMatch(/\b\w+\.message\b/)
  })

  it('keeps order center failures and empty filters actionable without raw backend details', () => {
    expect(orderCenterSource).toContain('safeClientErrorMessage')
    expect(orderCenterSource).toContain('role="alert"')
    expect(orderCenterSource).toContain('重新加载')
    expect(orderCenterSource).toContain('const resetFilters')
    expect(orderCenterSource).toContain('清除筛选')
    expect(orderCenterSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
    expect(orderCenterSource).not.toMatch(/\b\w+\.message\b/)
  })

  it('cleans hotel order cache on booking/list failures and shows safe retry UI', () => {
    expect(hotelBookingSource).toContain('const clearHotelOrderCache')
    expect(hotelBookingSource).toContain('clearHotelOrderClientStorage')
    expect(hotelBookingSource).toContain('clearHotelOrderCache()')
    expect(hotelBookingSource).toContain('safeClientErrorMessage')
    expect(hotelBookingSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
    expect(hotelBookingSource).not.toMatch(/(?:localStorage|sessionStorage)\.(?:getItem|setItem)\('hotel-orders'/)

    expect(hotelOrdersSource).toContain('const clearHotelOrderCache')
    expect(hotelOrdersSource).toContain('clearHotelOrderClientStorage')
    expect(hotelOrdersSource).toContain('safeClientErrorMessage')
    expect(hotelOrdersSource).toContain('role="alert"')
    expect(hotelOrdersSource).toContain("t('common.reload')")
    expect(hotelOrdersSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
    expect(hotelOrdersSource).not.toMatch(/(?:localStorage|sessionStorage)\.(?:getItem|setItem)\('hotel-orders'/)
  })

  it('keeps scenic spot list failures generic and retry, loading, empty states announced', () => {
    expect(scenicSpotsSource).toContain('safeClientErrorMessage')
    expect(scenicSpotsSource).toContain('summarizeClientError')
    expect(scenicSpotsSource).toContain('errorMessage.value = safeClientErrorMessage')
    expect(scenicSpotsSource).toContain('role="alert"')
    expect(scenicSpotsSource).toContain('aria-live="assertive"')
    expect(scenicSpotsSource).toContain('role="status"')
    expect(scenicSpotsSource).toContain('aria-busy="true"')
    expect(scenicSpotsSource).toContain("t('spots.retryLoad')")
    expect(scenicSpotsSource).toContain("t('spots.reload')")
    expect(scenicSpotsSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
    expect(scenicSpotsSource).not.toMatch(/\b\w+\.response\??\.\s*status\b/)
    expect(scenicSpotsSource).not.toMatch(/\b\w+\.message\b/)
  })

  it('keeps scenic spot cards as one keyboard link surface without nested buttons', () => {
    const spotsGridSource = getSourceSection(scenicSpotsSource, '<!-- Spots Grid -->', '</AnimatePresence>')

    expect(spotsGridSource).toContain('role="link"')
    expect(spotsGridSource).toContain('tabindex="0"')
    expect(spotsGridSource).toContain('@click="goToSpot(spot)"')
    expect(spotsGridSource).toContain('@keydown.enter.prevent="goToSpot(spot)"')
    expect(spotsGridSource).toContain('@keydown.space.prevent="goToSpot(spot)"')
    expect(spotsGridSource).toContain("t('spots.viewDetails')")
    expect(spotsGridSource).toContain('aria-hidden="true"')
    expect(spotsGridSource).not.toMatch(/<button\b/)
  })
})

const getSourceSection = (source: string, startMarker: string, endMarker: string) => {
  const startIndex = source.indexOf(startMarker)
  const endIndex = source.indexOf(endMarker, startIndex + startMarker.length)

  if (startIndex === -1 || endIndex === -1) {
    throw new Error(`Missing source section between "${startMarker}" and "${endMarker}"`)
  }

  return source.slice(startIndex, endIndex)
}
