package com.tibet.tourism.dto.itinerary;

import java.math.BigDecimal;

public record BookItineraryItemResponse(
        Long itemId,
        String bookingType,
        Long bookingId,
        BigDecimal totalPrice,
        String message
) {
}
