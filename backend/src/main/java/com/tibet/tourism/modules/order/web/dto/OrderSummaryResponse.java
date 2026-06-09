package com.tibet.tourism.modules.order.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        String orderNo,
        String status,
        String paymentStatus,
        String currency,
        String productSummary,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount,
        String customerName,
        String customerPhone,
        String sourceType,
        Long sourceReferenceId,
        LocalDateTime lockedUntil,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt
) {
}
