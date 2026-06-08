package com.tibet.tourism.modules.order.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefundReviewRequest(
        @NotNull Action action,
        @Size(max = 500) String note,
        @Size(max = 64) String providerTransactionNo
) {
    public enum Action {
        APPROVE,
        REJECT,
        COMPLETE
    }
}
