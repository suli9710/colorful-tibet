package com.tibet.tourism.modules.spot.web.dto;

import com.tibet.tourism.common.util.LocaleHelper;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ScenicSpotResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        String altitude,
        String location,
        ScenicSpot.Category category,
        BigDecimal ticketPrice,
        BigDecimal peakSeasonPrice,
        BigDecimal offSeasonPrice,
        LocalDate peakStartDate,
        LocalDate peakEndDate,
        LocalDate freeStartDate,
        LocalDate freeEndDate,
        BigDecimal rating,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer visitCount
) {
    public static ScenicSpotResponse fromEntity(ScenicSpot spot) {
        return fromEntity(spot, "zh");
    }

    public static ScenicSpotResponse fromEntity(ScenicSpot spot, String locale) {
        if (spot == null) {
            return null;
        }
        return new ScenicSpotResponse(
                spot.getId(),
                LocaleHelper.resolveByLocale(locale, spot.getName(), spot.getNameTibetan()),
                LocaleHelper.resolveByLocale(locale, spot.getDescription(), spot.getDescriptionTibetan()),
                spot.getImageUrl(),
                spot.getAltitude(),
                spot.getLocation(),
                spot.getCategory(),
                spot.getTicketPrice(),
                spot.getPeakSeasonPrice(),
                spot.getOffSeasonPrice(),
                spot.getPeakStartDate(),
                spot.getPeakEndDate(),
                spot.getFreeStartDate(),
                spot.getFreeEndDate(),
                spot.getRating() != null ? spot.getRating() : BigDecimal.ZERO,
                spot.getLatitude(),
                spot.getLongitude(),
                spot.getVisitCount() != null ? spot.getVisitCount() : 0);
    }
}
