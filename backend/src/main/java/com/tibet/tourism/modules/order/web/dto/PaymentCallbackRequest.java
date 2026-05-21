package com.tibet.tourism.modules.order.web.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PaymentCallbackRequest(
        @NotBlank String orderNo,
        @NotBlank String transactionNo,
        @NotBlank String provider,
        @NotNull BigDecimal amount,
        @NotBlank @Pattern(regexp = "^(SUCCESS|FAILED)$") String status,
        @NotBlank String signature
) {
}
