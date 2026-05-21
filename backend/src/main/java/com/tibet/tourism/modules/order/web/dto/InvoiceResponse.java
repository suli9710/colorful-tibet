package com.tibet.tourism.modules.order.web.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long id,
        String invoiceNo,
        String invoiceTitle,
        String taxNo,
        BigDecimal amount,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime issuedAt
) {
}
