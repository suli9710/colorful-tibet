package com.tibet.tourism.modules.hotel.web.dto;

import com.tibet.tourism.modules.hotel.domain.RoomType;
import java.math.BigDecimal;

public record PublicRoomTypeResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer capacity,
        String imageUrl,
        String amenities,
        Integer inventoryUnits,
        String inventoryStatus,
        String priceStatus,
        String fulfillmentStatus
) {
    public PublicRoomTypeResponse(Long id, String name, BigDecimal price, Integer capacity, String imageUrl, String amenities) {
        this(id, name, price, capacity, imageUrl, amenities, null, "UNKNOWN",
                price == null || price.signum() <= 0 ? "UNAVAILABLE" : "REFERENCE", "SUPPLIER_CONFIRMATION_REQUIRED");
    }

    public static PublicRoomTypeResponse from(RoomType roomType) {
        return new PublicRoomTypeResponse(
                roomType.getId(),
                roomType.getName(),
                roomType.getPrice(),
                roomType.getCapacity(),
                roomType.getImageUrl(),
                roomType.getAmenities(),
                roomType.getInventoryUnits(),
                roomType.getInventoryUnits() == null ? "UNKNOWN" : roomType.getInventoryUnits() == 0 ? "SOLD_OUT" : "CONFIGURED",
                roomType.getPrice() == null || roomType.getPrice().signum() <= 0 ? "UNAVAILABLE" : "REFERENCE",
                "SUPPLIER_CONFIRMATION_REQUIRED");
    }
}
