package com.tibet.tourism.modules.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction_reservations")
public class PaymentTransactionReservation {

    @Id
    @Column(name = "transaction_no", nullable = false, length = 64)
    private String transactionNo;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "reference_no", length = 64)
    private String referenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 16)
    private UsageType usageType = UsageType.PAYMENT;

    @Column(length = 32)
    private String provider;

    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentTransaction.Status status = PaymentTransaction.Status.PENDING;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public UsageType getUsageType() { return usageType; }
    public void setUsageType(UsageType usageType) { this.usageType = usageType; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentTransaction.Status getStatus() { return status; }
    public void setStatus(PaymentTransaction.Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum UsageType {
        PAYMENT, REFUND
    }
}
