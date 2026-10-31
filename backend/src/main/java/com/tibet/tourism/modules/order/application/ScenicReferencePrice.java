package com.tibet.tourism.modules.order.application;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;

/** Stored reference tariff only; never infers a recurring regional free-ticket policy. */
public final class ScenicReferencePrice {
    private ScenicReferencePrice() {}

    public static BigDecimal resolve(ScenicSpot spot, LocalDate visitDate) {
        if (spot == null || visitDate == null) {
            throw new IllegalArgumentException("Scenic spot and visit date are required");
        }
        MonthDay day = MonthDay.from(visitDate);
        boolean peak = !day.isBefore(MonthDay.of(5, 1)) && !day.isAfter(MonthDay.of(10, 31));
        BigDecimal seasonal = peak ? spot.getPeakSeasonPrice() : spot.getOffSeasonPrice();
        BigDecimal price = seasonal != null ? seasonal : spot.getTicketPrice();
        if (price == null || price.signum() < 0) {
            throw new IllegalStateException("Scenic reference price is unavailable; supplier confirmation is required");
        }
        // Explicitly configured zero is valid; an absent price is not a free ticket.
        return price;
    }
}
