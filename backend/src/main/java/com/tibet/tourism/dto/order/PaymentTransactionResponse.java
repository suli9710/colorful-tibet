package com.tibet.tourism.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        Long id,
        String transactionNo,
        String provider,
        BigDecimal amount,
        String status,
        Boolean signatureValid,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
