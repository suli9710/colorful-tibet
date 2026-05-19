package com.tibet.tourism.dto.itinerary;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItineraryQuoteItemResponse(
        Long itemId,
        Integer dayNumber,
        LocalDate travelDate,
        String itemType,
        String title,
        BigDecimal estimatedCost,
        Boolean bookable,
        String bookingAction,
        String bookingStatus,
        String priceType
) {
}
