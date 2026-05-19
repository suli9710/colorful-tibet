package com.tibet.tourism.dto.itinerary;

import java.math.BigDecimal;

public record ItineraryItemResponse(
        Long id,
        String itemType,
        String title,
        String description,
        String startTime,
        Integer durationMinutes,
        BigDecimal estimatedCost,
        Integer altitudeMeters,
        String riskLevel,
        String alternatives,
        String bookingAction,
        String bookingStatus,
        String bookingReferenceType,
        Long bookingReferenceId,
        Long scenicSpotId,
        String scenicSpotName,
        Long hotelId,
        String hotelName,
        Long roomTypeId,
        String roomTypeName,
        Integer sortOrder
) {
}
