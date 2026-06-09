import { getHotelById, hotels } from './hotels'

export const defaultHotelImage = '/images/hotels/hotel-lhasa-interior.jpg'

type HotelBookingImageRecord = Record<string, unknown>

const isRecord = (value: unknown): value is HotelBookingImageRecord =>
  typeof value === 'object' && value !== null

const readString = (record: HotelBookingImageRecord | null, key: string) => {
  const value = record?.[key]
  return typeof value === 'string' ? value.trim() : undefined
}

const readPositiveId = (record: HotelBookingImageRecord | null, key: string) => {
  const value = record?.[key]
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

export const resolveHotelCoverImage = (coverImage?: string | null): string => {
  const image = coverImage?.trim()
  return image || defaultHotelImage
}

export const applyHotelImageFallback = (event: Event): void => {
  if (typeof HTMLImageElement === 'undefined' || !(event.target instanceof HTMLImageElement)) return
  const image = event.target
  if (image.src.endsWith(defaultHotelImage)) return
  image.src = defaultHotelImage
}

export const resolveHotelBookingImage = (booking: unknown): string => {
  const bookingRecord = isRecord(booking) ? booking : null
  const hotel = isRecord(bookingRecord?.hotel) ? bookingRecord.hotel : null
  const hotelId = readPositiveId(bookingRecord, 'hotelId') ?? readPositiveId(hotel, 'id')
  const hotelName = readString(bookingRecord, 'hotelName') || readString(hotel, 'name')
  const staticHotel = (hotelId ? getHotelById(hotelId) : undefined) ||
    (hotelName ? hotels.find(item => item.name === hotelName) : undefined)

  return resolveHotelCoverImage(
    staticHotel?.coverImage ||
    readString(hotel, 'coverImage') ||
    readString(hotel, 'imageUrl') ||
    readString(bookingRecord, 'coverImage') ||
    readString(bookingRecord, 'imageUrl')
  )
}
