package com.tibet.tourism.modules.spot.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class PriceInfo {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private BigDecimal basePrice;
    private BigDecimal peakSeasonPrice;
    private BigDecimal offSeasonPrice;
    private String source;
    private LocalDateTime fetchTime;
    private Double confidence;
    private String rawData;
    private boolean referenceOnly;

    public PriceInfo() {
        this.fetchTime = LocalDateTime.now(BEIJING_ZONE);
    }

    public PriceInfo(BigDecimal basePrice, String source) {
        this();
        this.basePrice = basePrice;
        this.source = source;
        this.confidence = 0.8;
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

    public LocalDateTime getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(LocalDateTime fetchTime) {
        this.fetchTime = fetchTime;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    @JsonIgnore
    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public boolean isReferenceOnly() {
        return referenceOnly;
    }

    public void setReferenceOnly(boolean referenceOnly) {
        this.referenceOnly = referenceOnly;
    }
}
