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
})
