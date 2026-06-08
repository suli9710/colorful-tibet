package com.tibet.tourism.modules.hotel.web.dto;

import com.tibet.tourism.modules.hotel.domain.Hotel;
import java.math.BigDecimal;

public record PublicHotelResponse(
        Long id,
        String name,
        String location,
        String priceRange,
        BigDecimal rating,
        String imageUrl,
        String facilities
) {
    public static PublicHotelResponse from(Hotel hotel) {
        return new PublicHotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getPriceRange(),
                hotel.getRating(),
                hotel.getImageUrl(),
                hotel.getFacilities());
    }
}
