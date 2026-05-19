package com.tibet.tourism.dto.order;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderItemResponse(
        Long id,
        String productType,
        Long productId,
        Long skuId,
        String productName,
        String skuName,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String status,
        String legacyReferenceType,
        Long legacyReferenceId
) {
}
