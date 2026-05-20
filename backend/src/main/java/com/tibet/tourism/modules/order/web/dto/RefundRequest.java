package com.tibet.tourism.modules.order.web.dto;
import jakarta.validation.constraints.Size;

public class RefundRequest {
    private Long orderItemId;

    @Size(max = 500)
    private String reason;

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
