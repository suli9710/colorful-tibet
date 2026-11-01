package com.tibet.tourism.modules.order.web.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        Long id,
        Long orderItemId,
        String refundNo,
        BigDecimal amount,
        String status,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
) {
    @com.fasterxml.jackson.annotation.JsonProperty("settlementStatus")
    public String settlementStatus() {
        return "COMPLETED".equals(status) ? "MANUAL_RECORD_ONLY" : "NOT_SETTLED";
    }
}
