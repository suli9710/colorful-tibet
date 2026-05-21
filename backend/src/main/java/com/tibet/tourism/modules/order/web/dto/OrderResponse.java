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
}
