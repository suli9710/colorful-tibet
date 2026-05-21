package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ItineraryDayResponse(
        Long id,
        Integer dayNumber,
        LocalDate travelDate,
        String title,
        String region,
        String summary,
        BigDecimal estimatedCost,
        String altitudeRisk,
        List<ItineraryItemResponse> items
) {
}
