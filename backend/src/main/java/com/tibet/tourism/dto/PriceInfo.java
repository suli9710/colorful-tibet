package com.tibet.tourism.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格信息DTO
 */
public class PriceInfo {
    private BigDecimal basePrice;           // 基础票价
    private BigDecimal peakSeasonPrice;     // 旺季价格
    private BigDecimal offSeasonPrice;      // 淡季价格
    private String source;                  // 价格来源（如：携程、去哪儿、AI提取等）
    private LocalDateTime fetchTime;        // 获取时间
    private Double confidence;              // 置信度（0-1）
    private String rawData;                 // 原始数据（用于调试）

    public PriceInfo() {
        this.fetchTime = LocalDateTime.now();
    }

    public PriceInfo(BigDecimal basePrice, String source) {
        this();
        this.basePrice = basePrice;
        this.source = source;
        this.confidence = 0.8; // 默认置信度
    }

    // Getters and Setters
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

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }
}
















