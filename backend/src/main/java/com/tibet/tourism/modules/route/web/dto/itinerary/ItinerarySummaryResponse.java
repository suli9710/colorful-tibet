package com.tibet.tourism.modules.route.web.dto.itinerary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItinerarySummaryResponse(
        Long id,
        Long parentItineraryId,
        String title,
        Integer days,
        LocalDate startDate,
        String budget,
        String preference,
        String versionType,
        String versionLabel,
        BigDecimal totalEstimatedCost,
        String status,
        LocalDateTime createdAt
) {
}
