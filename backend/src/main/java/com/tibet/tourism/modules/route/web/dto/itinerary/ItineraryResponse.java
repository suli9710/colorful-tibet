package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
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
        List<ItineraryDayResponse> itineraryDays,
        ItineraryReadinessResponse readiness,
        Integer travelers,
        String generationRuleVersion
) {
    public ItineraryResponse(Long id, Long parentItineraryId, String title, Integer days, LocalDate startDate,
            String budget, String preference, String versionType, String versionLabel, BigDecimal totalEstimatedCost,
            String status, String sourceContent, LocalDateTime createdAt, List<ItineraryDayResponse> itineraryDays) {
        this(id, parentItineraryId, title, days, startDate, budget, preference, versionType, versionLabel,
                totalEstimatedCost, status, sourceContent, createdAt, itineraryDays, null, null, null);
    }
}
