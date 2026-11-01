import { describe, expect, it } from 'vitest'
import planner from './RoutePlanner.vue?raw'
import booking from './HotelBooking.vue?raw'
import detail from './RouteDetail.vue?raw'
import admin from './AdminDashboard.vue?raw'

describe('PR15 business integration preserves PR16 flow', () => {
  it('keeps planner review, retry and stale-request safeguards without replacing its layout', () => {
    for (const marker of ['planner-steps', 'planner-preferences', 'planner-review', 'planner-check', 'isCurrentItineraryRequest', 'confirmingRegenerate', 'const parameters = { ...generatedForm.value }', 'parameters.days > 15']) expect(planner).toContain(marker)
    expect(planner).not.toContain('<TripWorkbench')
  })
  it('exports a public-only offline projection, renders readiness and does not invent the party size', () => {
    expect(planner).toContain('buildOfflineTrip(bookableItinerary.value, new Set())')
    expect(planner).not.toContain('JSON.stringify(tibetTravelKit.value')
    expect(planner).not.toContain('travelers: 2')
    expect(planner).toContain('travelers: null')
    expect(planner).toContain('bookableItinerary.readiness.issues')
    expect(planner).toContain("['BOOKED', 'INQUIRY_SUBMITTED']")
  })
  it('adds explicit inventory/capacity gating without dropping room-switch request protection', () => {
    expect(booking).toContain('!canRequestRoom(selectedRoomSource.value)')
    expect(booking).not.toContain('DEFAULT_ROOM_CAPACITY')
    expect(booking).toContain('const requestedRoom = selectedRoom.value')
    expect(booking).toContain('requestedHotelId !== hotelId.value || requestedRoomId !== roomId.value')
    expect(admin).toContain('inventoryUnits: null as number | null')
  })
  it('labels community history as self-reported rather than verified', () => {
    expect(detail).toContain('作者自述')
    expect(detail).toContain('内容未经平台实地核验')
  })
})
