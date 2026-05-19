package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_transactions_order", columnList = "order_id, created_at"),
        @Index(name = "idx_payment_transactions_no", columnList = "transaction_no", unique = true)
})
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PlatformOrder order;

    @Column(name = "transaction_no", nullable = false, length = 64, unique = true)
    private String transactionNo;

    @Column(length = 32)
    private String provider;

    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING;

    @Column(name = "signature_valid")
    private Boolean signatureValid = false;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "callback_payload", columnDefinition = "TEXT")
    private String callbackPayload;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlatformOrder getOrder() { return order; }
    public void setOrder(PlatformOrder order) { this.order = order; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Boolean getSignatureValid() { return signatureValid; }
    public void setSignatureValid(Boolean signatureValid) { this.signatureValid = signatureValid; }

    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }

    public String getCallbackPayload() { return callbackPayload; }
    public void setCallbackPayload(String callbackPayload) { this.callbackPayload = callbackPayload; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum Status {
        PENDING, SUCCESS, FAILED, REFUNDED
    }
}
