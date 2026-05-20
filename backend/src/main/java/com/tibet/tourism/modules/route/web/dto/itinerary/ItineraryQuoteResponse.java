package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
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
