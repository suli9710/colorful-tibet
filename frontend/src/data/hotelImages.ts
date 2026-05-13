import { getHotelById, hotels } from './hotels'

export const defaultHotelImage = '/images/hotels/hotel-lhasa-interior.jpg'

export const resolveHotelCoverImage = (coverImage?: string | null): string => {
  const image = coverImage?.trim()
  return image || defaultHotelImage
}

export const applyHotelImageFallback = (event: Event): void => {
  const image = event.target as HTMLImageElement
  if (image.src.endsWith(defaultHotelImage)) return
  image.src = defaultHotelImage
}

export const resolveHotelBookingImage = (booking: any): string => {
  const hotel = booking?.hotel || {}
  const hotelId = Number(booking?.hotelId || hotel.id)
  const staticHotel = getHotelById(hotelId) || hotels.find(item => item.name === (booking?.hotelName || hotel.name))

  return resolveHotelCoverImage(
    staticHotel?.coverImage ||
    hotel.coverImage ||
    hotel.imageUrl ||
    booking?.coverImage ||
    booking?.imageUrl
  )
}
