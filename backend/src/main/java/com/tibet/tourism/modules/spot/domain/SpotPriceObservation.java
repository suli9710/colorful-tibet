package com.tibet.tourism.modules.spot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spot_price_observations", indexes = {
        @Index(name = "idx_spot_price_observations_spot", columnList = "spot_id"),
        @Index(name = "idx_spot_price_observations_status", columnList = "status"),
        @Index(name = "idx_spot_price_observations_observed_at", columnList = "observed_at")
})
public class SpotPriceObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private ScenicSpot spot;

    @Column(name = "spot_name", nullable = false)
    private String spotName;

    @Column(name = "base_price", precision = 19, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "peak_season_price", precision = 19, scale = 2)
    private BigDecimal peakSeasonPrice;

    @Column(name = "off_season_price", precision = 19, scale = 2)
    private BigDecimal offSeasonPrice;

    @Column(length = 128)
    private String source;

    private Double confidence;

    @Column(name = "reference_only", nullable = false)
    private boolean referenceOnly;

    @Column(nullable = false)
    private boolean publishable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "review_reason", length = 255)
    private String reviewReason;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @PrePersist
    protected void onCreate() {
        if (observedAt == null) {
            observedAt = LocalDateTime.now();
        }
    }

    public static SpotPriceObservation from(
            ScenicSpot spot,
            com.tibet.tourism.modules.spot.web.dto.PriceInfo priceInfo,
            boolean publishable,
            Status status,
            String reviewReason) {
        SpotPriceObservation observation = new SpotPriceObservation();
        observation.setSpot(spot);
        observation.setSpotName(spot.getName());
        observation.setBasePrice(priceInfo.getBasePrice());
        observation.setPeakSeasonPrice(priceInfo.getPeakSeasonPrice());
        observation.setOffSeasonPrice(priceInfo.getOffSeasonPrice());
        observation.setSource(priceInfo.getSource());
        observation.setConfidence(priceInfo.getConfidence());
        observation.setReferenceOnly(priceInfo.isReferenceOnly());
        observation.setPublishable(publishable);
        observation.setStatus(status);
        observation.setReviewReason(reviewReason);
        return observation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScenicSpot getSpot() {
        return spot;
    }

    public void setSpot(ScenicSpot spot) {
        this.spot = spot;
    }

    public String getSpotName() {
        return spotName;
    }

    public void setSpotName(String spotName) {
        this.spotName = spotName;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getPeakSeasonPrice() {
        return peakSeasonPrice;
    }

    public void setPeakSeasonPrice(BigDecimal peakSeasonPrice) {
        this.peakSeasonPrice = peakSeasonPrice;
    }

    public BigDecimal getOffSeasonPrice() {
        return offSeasonPrice;
    }

    public void setOffSeasonPrice(BigDecimal offSeasonPrice) {
        this.offSeasonPrice = offSeasonPrice;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public boolean isReferenceOnly() {
        return referenceOnly;
    }

    public void setReferenceOnly(boolean referenceOnly) {
        this.referenceOnly = referenceOnly;
    }

    public boolean isPublishable() {
        return publishable;
    }

    public void setPublishable(boolean publishable) {
        this.publishable = publishable;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public enum Status {
        REVIEW_REQUIRED,
        PUBLISHED
    }
}
