package com.tibet.tourism.modules.order.web.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        String status,
        String paymentStatus,
        String currency,
        String productSummary,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount,
        String customerName,
        String customerPhone,
        String customerNote,
        String sourceType,
        Long sourceReferenceId,
        LocalDateTime lockedUntil,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        List<PaymentTransactionResponse> paymentTransactions,
        List<RefundResponse> refunds,
        List<VoucherResponse> vouchers,
        List<InvoiceResponse> invoices
) {
    @com.fasterxml.jackson.annotation.JsonProperty("fulfillmentStatus")
    public String fulfillmentStatus() {
        return "SUPPLIER_CONFIRMATION_REQUIRED";
    }

    @com.fasterxml.jackson.annotation.JsonProperty("paymentMode")
    public String paymentMode() {
        if (paymentTransactions == null || paymentTransactions.isEmpty()) return "NOT_CONNECTED";
        if (paymentTransactions.stream().allMatch(payment -> "MOCK".equalsIgnoreCase(payment.provider()))) return "SIMULATION";
        if (paymentTransactions.stream().allMatch(payment -> payment.provider() != null
                && (payment.provider().startsWith("MANUAL") || payment.provider().startsWith("LEGACY")))) return "MANUAL_RECORD";
        return "RECORDED_UNVERIFIED";
    }
}
