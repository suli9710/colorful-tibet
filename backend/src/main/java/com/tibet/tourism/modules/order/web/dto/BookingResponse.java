package com.tibet.tourism.modules.order.web.dto;

import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        UserSummary user,
        SpotSummary spot,
        Long spotId,
        LocalDate visitDate,
        Integer ticketCount,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt
) {
    public record UserSummary(
            Long id,
            String username,
            String nickname
    ) {
    }

    public record SpotSummary(
            Long id,
            String name,
            String location,
            String imageUrl
    ) {
    }

    public static BookingResponse fromEntity(Booking booking) {
        User user = booking.getUser();
        ScenicSpot spot = booking.getSpot();
        return new BookingResponse(
                booking.getId(),
                user == null ? null : new UserSummary(user.getId(), user.getUsername(), user.getNickname()),
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
