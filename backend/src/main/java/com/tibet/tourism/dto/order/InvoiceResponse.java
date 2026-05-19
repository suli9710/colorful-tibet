package com.tibet.tourism.dto.order;

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
