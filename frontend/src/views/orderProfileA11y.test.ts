import { describe, expect, it } from 'vitest'
import orderCenterSource from './OrderCenter.vue?raw'
import userProfileSource from './UserProfile.vue?raw'

describe('order and profile keyboard accessibility guardrails', () => {
  it('keeps OrderCenter order cards keyboard reachable and actionable', () => {
    expect(orderCenterSource).toContain('role="button"')
    expect(orderCenterSource).toContain('tabindex="0"')
    expect(orderCenterSource).toContain(':aria-label="orderCardActionLabel(order)"')
    expect(orderCenterSource).toContain(':aria-expanded="selectedOrderId === order.id"')
    expect(orderCenterSource).toContain('aria-controls="order-detail-panel"')
    expect(orderCenterSource).toContain('id="order-detail-panel"')
    expect(orderCenterSource).toContain('@keydown.enter.prevent="selectOrder(order.id)"')
    expect(orderCenterSource).toContain('@keydown.space.prevent="selectOrder(order.id)"')
    expect(orderCenterSource).toContain('const orderCardActionLabel = (order: Order) =>')
  })

  it('uses a router-link instead of a mouse-only h3 for profile route cards', () => {
    expect(userProfileSource).toContain('<router-link')
    expect(userProfileSource).toContain(':to="`/community/${route.id}`"')
    expect(userProfileSource).toContain('focus-visible:ring-2 focus-visible:ring-tibet-gold/60')
    expect(userProfileSource).not.toContain('@click="router.push(`/community/${route.id}`)"')
  })

  it('keeps account order and profile lists paginated beyond the first page', () => {
    expect(orderCenterSource).toContain('readPaginatedResponse<Order>')
    expect(orderCenterSource).toContain('ordersPageInfo')
    expect(orderCenterSource).toContain('loadNextOrdersPage')
    expect(orderCenterSource).toContain('params: { page, size: ordersPageSize')

    expect(userProfileSource).toContain('hotelBookingsPageInfo')
    expect(userProfileSource).toContain('spotCommentsPageInfo')
    expect(userProfileSource).toContain('routeCommentsPageInfo')
    expect(userProfileSource).toContain('body.spotCommentsPage')
    expect(userProfileSource).toContain('body.routeCommentsPage')
    expect(userProfileSource).toContain('loadNextHotelBookingsPage')
    expect(userProfileSource).toContain('loadNextCommentsPage')
  })

  it('uses a roving tabindex tablist pattern in UserProfile', () => {
    expect(userProfileSource).toContain(':tabindex="activeTab === tab.id ? 0 : -1"')
    expect(userProfileSource).toContain(':aria-controls="profileTabPanelId(tab.id)"')
    expect(userProfileSource).toContain('@keydown="handleProfileTabKeydown($event, index)"')
    expect(userProfileSource).toContain('role="tabpanel"')
    expect(userProfileSource).toContain(':aria-labelledby="profileTabId(\'routes\')"')
    expect(userProfileSource).toContain("event.key === 'ArrowRight'")
    expect(userProfileSource).toContain("event.key === 'Home'")
    expect(userProfileSource).toContain("event.key === 'End'")
  })
})
