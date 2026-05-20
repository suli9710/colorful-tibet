package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_locks", indexes = {
        @Index(name = "idx_inventory_locks_product_date", columnList = "product_type, product_id, service_date"),
        @Index(name = "idx_inventory_locks_expires", columnList = "expires_at, status")
})
public class InventoryLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PlatformOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    private OrderItem.ProductType productType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sku_id")
    private Long skuId;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.LOCKED;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlatformOrder getOrder() { return order; }
    public void setOrder(PlatformOrder order) { this.order = order; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public OrderItem.ProductType getProductType() { return productType; }
    public void setProductType(OrderItem.ProductType productType) { this.productType = productType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public enum Status {
        LOCKED, CONFIRMED, RELEASED, EXPIRED
    }
}
