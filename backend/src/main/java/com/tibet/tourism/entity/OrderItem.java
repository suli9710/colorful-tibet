package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order_sort", columnList = "order_id, sort_order"),
        @Index(name = "idx_order_items_product", columnList = "product_type, product_id")
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PlatformOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    private ProductType productType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sku_id")
    private Long skuId;

    @Column(name = "product_name", nullable = false, length = 160)
    private String productName;

    @Column(name = "sku_name", length = 160)
    private String skuName;

    @Column(name = "service_start_date")
    private LocalDate serviceStartDate;

    @Column(name = "service_end_date")
    private LocalDate serviceEndDate;

    private Integer quantity = 1;

    @Column(name = "unit_price")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    private BigDecimal subtotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.LOCKED;

    @Column(name = "cancellation_policy_id")
    private Long cancellationPolicyId;

    @Column(name = "legacy_reference_type", length = 48)
    private String legacyReferenceType;

    @Column(name = "legacy_reference_id")
    private Long legacyReferenceId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlatformOrder getOrder() { return order; }
    public void setOrder(PlatformOrder order) { this.order = order; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }

    public LocalDate getServiceStartDate() { return serviceStartDate; }
    public void setServiceStartDate(LocalDate serviceStartDate) { this.serviceStartDate = serviceStartDate; }

    public LocalDate getServiceEndDate() { return serviceEndDate; }
    public void setServiceEndDate(LocalDate serviceEndDate) { this.serviceEndDate = serviceEndDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Long getCancellationPolicyId() { return cancellationPolicyId; }
    public void setCancellationPolicyId(Long cancellationPolicyId) { this.cancellationPolicyId = cancellationPolicyId; }

    public String getLegacyReferenceType() { return legacyReferenceType; }
    public void setLegacyReferenceType(String legacyReferenceType) { this.legacyReferenceType = legacyReferenceType; }

    public Long getLegacyReferenceId() { return legacyReferenceId; }
    public void setLegacyReferenceId(Long legacyReferenceId) { this.legacyReferenceId = legacyReferenceId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public enum ProductType {
        SCENIC_SPOT, HOTEL_ROOM, EXPERIENCE, CAR, GUIDE, ITINERARY
    }

    public enum Status {
        LOCKED, CONFIRMED, CANCELLED, REFUND_PENDING, REFUNDED, EXPIRED
    }
}
