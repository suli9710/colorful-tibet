package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_order", columnList = "order_id, requested_at"),
        @Index(name = "idx_invoices_no", columnList = "invoice_no", unique = true)
})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PlatformOrder order;

    @Column(name = "invoice_no", nullable = false, length = 64, unique = true)
    private String invoiceNo;

    @Column(name = "invoice_title", nullable = false, length = 160)
    private String invoiceTitle;

    @Column(name = "tax_no", length = 64)
    private String taxNo;

    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.REQUESTED;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlatformOrder getOrder() { return order; }
    public void setOrder(PlatformOrder order) { this.order = order; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getInvoiceTitle() { return invoiceTitle; }
    public void setInvoiceTitle(String invoiceTitle) { this.invoiceTitle = invoiceTitle; }

    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String taxNo) { this.taxNo = taxNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public enum Status {
        REQUESTED, ISSUED, REJECTED
    }
}
