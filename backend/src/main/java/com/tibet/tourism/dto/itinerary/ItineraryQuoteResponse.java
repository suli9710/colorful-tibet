package com.tibet.tourism.dto.itinerary;

import java.math.BigDecimal;
import java.util.List;

public record ItineraryQuoteResponse(
        Long itineraryId,
        String title,
        BigDecimal totalEstimatedCost,
        BigDecimal bookableTotal,
        BigDecimal informationalTotal,
        String currency,
        List<ItineraryQuoteItemResponse> items
) {
}
