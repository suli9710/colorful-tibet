package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_policies", indexes = {
        @Index(name = "idx_cancellation_policies_product", columnList = "product_type, active, priority")
})
public class CancellationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    private OrderItem.ProductType productType;

    @Column(name = "policy_name", nullable = false, length = 120)
    private String policyName;

    @Column(name = "free_cancel_before_hours")
    private Integer freeCancelBeforeHours = 24;

    @Column(name = "refund_rate")
    private BigDecimal refundRate = BigDecimal.ONE;

    @Column(columnDefinition = "TEXT")
    private String rules;

    private Boolean active = true;
    private Integer priority = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderItem.ProductType getProductType() { return productType; }
    public void setProductType(OrderItem.ProductType productType) { this.productType = productType; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public Integer getFreeCancelBeforeHours() { return freeCancelBeforeHours; }
    public void setFreeCancelBeforeHours(Integer freeCancelBeforeHours) { this.freeCancelBeforeHours = freeCancelBeforeHours; }

    public BigDecimal getRefundRate() { return refundRate; }
    public void setRefundRate(BigDecimal refundRate) { this.refundRate = refundRate; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
