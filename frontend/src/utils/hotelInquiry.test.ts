import { describe, expect, it } from 'vitest'
import { canRequestRoom, explicitRoomCapacity } from './hotelInquiry'
const room = { capacity: 2, inventoryUnits: 3, inventoryStatus: 'CONFIGURED', priceStatus: 'REFERENCE' }
describe('conservative hotel inquiry eligibility', () => {
  it('accepts an explicit configured room without claiming supplier confirmation', () => expect(canRequestRoom(room)).toBe(true))
  it.each([undefined, null, 0, -1, 1.5, NaN, Infinity, '2'])('does not invent capacity from %s', capacity => {
    expect(explicitRoomCapacity({ capacity })).toBeNull()
    expect(canRequestRoom({ ...room, capacity })).toBe(false)
  })
  it.each([undefined, null, 0, -1, 0.5, NaN, Infinity, '3'])('rejects unknown or invalid stock %s', inventoryUnits => expect(canRequestRoom({ ...room, inventoryUnits })).toBe(false))
  it('rejects missing inventory or unavailable pricing even with positive quantities', () => {
    expect(canRequestRoom({ ...room, inventoryStatus: 'UNKNOWN' })).toBe(false)
    expect(canRequestRoom({ ...room, priceStatus: 'UNAVAILABLE' })).toBe(false)
    expect(canRequestRoom(null)).toBe(false)
  })
})
