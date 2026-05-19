package com.tibet.tourism.dto.order;

import jakarta.validation.constraints.Size;

public class CancelOrderRequest {
    @Size(max = 500)
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
