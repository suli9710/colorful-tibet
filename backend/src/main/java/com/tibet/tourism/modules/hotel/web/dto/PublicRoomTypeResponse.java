package com.tibet.tourism.modules.hotel.web.dto;

import com.tibet.tourism.modules.hotel.domain.RoomType;
import java.math.BigDecimal;

public record PublicRoomTypeResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer capacity,
        String imageUrl,
        String amenities
) {
    public static PublicRoomTypeResponse from(RoomType roomType) {
        return new PublicRoomTypeResponse(
                roomType.getId(),
                roomType.getName(),
                roomType.getPrice(),
                roomType.getCapacity(),
                roomType.getImageUrl(),
                roomType.getAmenities());
    }
}
