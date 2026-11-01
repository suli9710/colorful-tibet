/** Internal stock is not a supplier confirmation. Unknowns must not become defaults. */
export interface InquiryRoom {
  capacity?: unknown
  inventoryUnits?: unknown
  inventoryStatus?: string
  priceStatus?: string
}
export function explicitRoomCapacity(room: InquiryRoom | null | undefined): number | null {
  const capacity = room?.capacity
  return typeof capacity === 'number' && Number.isInteger(capacity) && capacity > 0 ? capacity : null
}
export function canRequestRoom(room: InquiryRoom | null | undefined): boolean {
  return explicitRoomCapacity(room) !== null && room?.inventoryStatus === 'CONFIGURED'
    && typeof room.inventoryUnits === 'number' && Number.isInteger(room.inventoryUnits)
    && room.inventoryUnits > 0 && room.priceStatus === 'REFERENCE'
}
