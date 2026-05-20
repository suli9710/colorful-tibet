package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_orders_status_created", columnList = "status, created_at"),
        @Index(name = "idx_orders_order_no", columnList = "order_no", unique = true),
        @Index(name = "idx_orders_source", columnList = "source_type, source_reference_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_user_idempotency", columnNames = {"user_id", "idempotency_key"})
})
public class PlatformOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 40, unique = true)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", length = 96)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(nullable = false, length = 8)
    private String currency = "CNY";

    @Column(name = "product_summary", length = 220)
    private String productSummary;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "payable_amount")
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Column(name = "customer_name", length = 64)
    private String customerName;

    @Column(name = "customer_phone", length = 32)
    private String customerPhone;

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "support_note", columnDefinition = "TEXT")
    private String supportNote;

    @Column(name = "source_type", length = 48)
    private String sourceType;

    @Column(name = "source_reference_id")
    private Long sourceReferenceId;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("requestedAt ASC")
    private List<RefundOrder> refunds = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("issuedAt ASC")
    private List<Voucher> vouchers = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("requestedAt ASC")
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderAuditLog> auditLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addPaymentTransaction(PaymentTransaction transaction) {
        paymentTransactions.add(transaction);
        transaction.setOrder(this);
    }

    public void addRefund(RefundOrder refund) {
        refunds.add(refund);
        refund.setOrder(this);
    }

    public void addVoucher(Voucher voucher) {
        vouchers.add(voucher);
        voucher.setOrder(this);
    }

    public void addInvoice(Invoice invoice) {
        invoices.add(invoice);
        invoice.setOrder(this);
    }

    public void addAuditLog(OrderAuditLog auditLog) {
        auditLogs.add(auditLog);
        auditLog.setOrder(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getProductSummary() { return productSummary; }
    public void setProductSummary(String productSummary) { this.productSummary = productSummary; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }

    public String getSupportNote() { return supportNote; }
    public void setSupportNote(String supportNote) { this.supportNote = supportNote; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSourceReferenceId() { return sourceReferenceId; }
    public void setSourceReferenceId(Long sourceReferenceId) { this.sourceReferenceId = sourceReferenceId; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public List<PaymentTransaction> getPaymentTransactions() { return paymentTransactions; }
    public void setPaymentTransactions(List<PaymentTransaction> paymentTransactions) { this.paymentTransactions = paymentTransactions; }

    public List<RefundOrder> getRefunds() { return refunds; }
    public void setRefunds(List<RefundOrder> refunds) { this.refunds = refunds; }

    public List<Voucher> getVouchers() { return vouchers; }
    public void setVouchers(List<Voucher> vouchers) { this.vouchers = vouchers; }

    public List<Invoice> getInvoices() { return invoices; }
    public void setInvoices(List<Invoice> invoices) { this.invoices = invoices; }

    public List<OrderAuditLog> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<OrderAuditLog> auditLogs) { this.auditLogs = auditLogs; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public enum Status {
        PENDING_PAYMENT, PAID, CONFIRMED, CANCELLED, REFUND_PENDING, REFUNDED, EXPIRED
    }

    public enum PaymentStatus {
        UNPAID, PAID, PARTIALLY_REFUNDED, REFUNDED, FAILED
    }
}
