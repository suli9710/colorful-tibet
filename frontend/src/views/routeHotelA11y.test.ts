import { describe, expect, it } from 'vitest'
import hotelBookingSource from './HotelBooking.vue?raw'
import hotelDetailSource from './HotelDetail.vue?raw'
import hotelOrdersSource from './HotelOrders.vue?raw'
import routeDetailSource from './RouteDetail.vue?raw'
import routePlannerSource from './RoutePlanner.vue?raw'

describe('route planner and hotel flow UX accessibility guardrails', () => {
  it('keeps route planner generation states accessible and actionable', () => {
    expect(routePlannerSource).toContain(':role="routeStatusRole"')
    expect(routePlannerSource).toContain(':aria-live="routeStatusLive"')
    expect(routePlannerSource).toContain('aria-atomic="true"')
    expect(routePlannerSource).toContain('id="route-planner-status-message"')
    expect(routePlannerSource).toContain('@click="generateRoute"')
    expect(routePlannerSource).toContain('@click="pauseRouteGeneration"')
    expect(routePlannerSource).toContain('@click="resumePausedRouteJob"')
    expect(routePlannerSource).toContain('PAUSED_ROUTE_JOB_STORAGE_KEY')
    expect(routePlannerSource).toContain('const pauseRouteGeneration')
    expect(routePlannerSource).toContain('const resumePausedRouteJob')
    expect(routePlannerSource).toContain('const safeRouteFailureMessage')
    expect(routePlannerSource).not.toContain('error.message?.')
    expect(routePlannerSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
  })

  it('keeps route planner and shared route actions guarded against duplicate requests', () => {
    expect(routePlannerSource).toContain('if (loading.value || streaming.value || activeRouteJobId.value) return')
    expect(routePlannerSource).toContain('if (!result.value || sharing.value) return')
    expect(routeDetailSource).toContain('const liking = ref(false)')
    expect(routeDetailSource).toContain('if (liking.value || !routeData.value) return')
    expect(routeDetailSource).toContain('if (!content || submitting.value) return')
    expect(routeDetailSource).toContain(':aria-label="t(\'routeDetail.commentPlaceholder\')"')
  })

  it('keeps official shared route authors labelled as official', () => {
    expect(routeDetailSource).toContain("if (route?.sourceType === 'OFFICIAL')")
    expect(routeDetailSource).toContain("return t('community.officialRoute')")
  })

  it('keeps route planner form controls keyboard and screen-reader friendly', () => {
    expect(routePlannerSource).toContain('type="radio"')
    expect(routePlannerSource).toContain('name="route-budget"')
    expect(routePlannerSource).toContain('name="route-preference"')
    expect(routePlannerSource).toContain('aria-controls="route-planner-days-value"')
    expect(routePlannerSource).toContain('role="progressbar"')
    expect(routePlannerSource).toContain(':aria-valuenow="routeGenerationProgressPercent"')
    expect(routePlannerSource).toContain(':aria-expanded="resultExpanded"')
    expect(routePlannerSource).toContain(':aria-label="getExternalBookingAriaLabel(item)"')
  })

  it('keeps hotel booking form validation described without raw backend details', () => {
    expect(hotelBookingSource).toContain('@submit.prevent="submitBooking"')
    expect(hotelBookingSource).toContain('autocomplete="name"')
    expect(hotelBookingSource).toContain('autocomplete="tel"')
    expect(hotelBookingSource).toContain(':aria-invalid="dateInvalid"')
    expect(hotelBookingSource).toContain(':aria-invalid="guestNameInvalid"')
    expect(hotelBookingSource).toContain(':aria-invalid="phoneInvalid"')
    expect(hotelBookingSource).toContain('role="alert"')
    expect(hotelBookingSource).toContain('bookingValidationMessages')
    expect(hotelBookingSource).toContain(':min="minCheckInDate"')
    expect(hotelBookingSource).toContain('MAX_BOOKING_NIGHTS')
    expect(hotelBookingSource).toContain('bookingUnavailable')
    expect(hotelBookingSource).toContain('bookingAssuranceItems')
    expect(hotelBookingSource).toContain('今日应付')
    expect(hotelBookingSource).toContain('phoneDigits')
    expect(hotelBookingSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
  })

  it('keeps external hotel navigation isolated from opener and referrer leakage', () => {
    expect(hotelDetailSource).toContain("'noopener,noreferrer'")
  })

  it('keeps hotel orders loading, empty, retry, and compact list states stable', () => {
    expect(hotelOrdersSource).toContain('role="status"')
    expect(hotelOrdersSource).toContain('aria-busy="true"')
    expect(hotelOrdersSource).toContain('role="list"')
    expect(hotelOrdersSource).toContain('role="listitem"')
    expect(hotelOrdersSource).toContain('const maskPhone')
    expect(hotelOrdersSource).toContain('const orderStatusLabel')
    expect(hotelOrdersSource).toContain('line-clamp-2 break-words')
    expect(hotelOrdersSource).toContain('min-h-11')
    expect(hotelOrdersSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
  })
})
