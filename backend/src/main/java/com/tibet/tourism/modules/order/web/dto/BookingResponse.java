package com.tibet.tourism.modules.order.web.dto;

import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        SpotSummary spot,
        Long spotId,
        LocalDate visitDate,
        Integer ticketCount,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt
) {
    public record SpotSummary(
            Long id,
            String name,
            String location,
            String imageUrl
    ) {
    }

    public static BookingResponse fromEntity(Booking booking) {
        ScenicSpot spot = booking.getSpot();
        return new BookingResponse(
                booking.getId(),
                spot == null ? null : new SpotSummary(spot.getId(), spot.getName(), spot.getLocation(), spot.getImageUrl()),
                spot == null ? null : spot.getId(),
                booking.getVisitDate(),
                booking.getTicketCount(),
                booking.getTotalPrice(),
                booking.getStatus() == null ? null : booking.getStatus().name(),
                booking.getCreatedAt()
        );
    }
}
