package com.tibet.tourism.dto.itinerary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ItineraryResponse(
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
        String sourceContent,
        LocalDateTime createdAt,
        List<ItineraryDayResponse> itineraryDays
) {
}
