package com.tibet.tourism.modules.hotel.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HotelBookingResponse(
        Long id,
        HotelSummary hotel,
        Long hotelId,
        String roomName,
        Long roomTypeId,
        BigDecimal roomPrice,
        Integer nights,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guests,
        String guestName,
        String phone,
        String note,
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal discount,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt
) {
    public record HotelSummary(
            Long id,
            String name,
            String location,
            String imageUrl
    ) {
    }
}
