package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
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
    public static final String UNKNOWN_PRICE_LABEL = "价格待确认 · 合计未包含此项目";

    @com.fasterxml.jackson.annotation.JsonProperty("priceStatus")
    public String priceStatus() { return UNKNOWN_PRICE_LABEL.equals(priceType) ? "UNKNOWN" : "ESTIMATE"; }
    @com.fasterxml.jackson.annotation.JsonProperty("availabilityStatus")
    public String availabilityStatus() { return Boolean.TRUE.equals(bookable) ? "CONSULTATION_ONLY" : "UNRESOLVED"; }
}
