package com.tibet.tourism.dto.order;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VoucherResponse(
        Long id,
        Long orderItemId,
        String voucherCode,
        String status,
        LocalDate validFrom,
        LocalDate validUntil,
        LocalDateTime issuedAt
) {
}
