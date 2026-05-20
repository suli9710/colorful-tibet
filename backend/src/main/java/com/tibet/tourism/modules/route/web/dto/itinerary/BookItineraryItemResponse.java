package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
import java.math.BigDecimal;

public record BookItineraryItemResponse(
        Long itemId,
        String bookingType,
        Long bookingId,
        BigDecimal totalPrice,
        String message
) {
}
