package com.tibet.tourism.dto;

import com.tibet.tourism.entity.ScenicSpot;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ScenicSpotDTO {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String altitude;
    private String location;
    private ScenicSpot.Category category;
    private BigDecimal ticketPrice;
    private BigDecimal peakSeasonPrice;
    private BigDecimal offSeasonPrice;
    private java.time.LocalDate peakStartDate;
    private java.time.LocalDate peakEndDate;
    private java.time.LocalDate freeStartDate;
    private java.time.LocalDate freeEndDate;
    private BigDecimal rating;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer visitCount;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAltitude() {
        return altitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ScenicSpot.Category getCategory() {
        return category;
    }

    public void setCategory(ScenicSpot.Category category) {
        this.category = category;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
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

    public java.time.LocalDate getPeakStartDate() {
        return peakStartDate;
    }

    public void setPeakStartDate(java.time.LocalDate peakStartDate) {
        this.peakStartDate = peakStartDate;
    }

    public java.time.LocalDate getPeakEndDate() {
        return peakEndDate;
    }

    public void setPeakEndDate(java.time.LocalDate peakEndDate) {
        this.peakEndDate = peakEndDate;
    }

    public java.time.LocalDate getFreeStartDate() {
        return freeStartDate;
    }

    public void setFreeStartDate(java.time.LocalDate freeStartDate) {
        this.freeStartDate = freeStartDate;
    }

    public java.time.LocalDate getFreeEndDate() {
        return freeEndDate;
    }

    public void setFreeEndDate(java.time.LocalDate freeEndDate) {
        this.freeEndDate = freeEndDate;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Integer getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(Integer visitCount) {
        this.visitCount = visitCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static ScenicSpotDTO fromEntity(ScenicSpot spot) {
        return fromEntity(spot, "zh");
    }

    public static ScenicSpotDTO fromEntity(ScenicSpot spot, String locale) {
        if (spot == null) {
            throw new IllegalArgumentException("ScenicSpot entity cannot be null");
        }
        ScenicSpotDTO dto = new ScenicSpotDTO();
        dto.setId(spot.getId());
        
        // 根据语言选择名称和描述
        if ("bo".equals(locale) && spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
            dto.setName(spot.getNameTibetan());
        } else {
            dto.setName(spot.getName());
        }
        
        if ("bo".equals(locale) && spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
            dto.setDescription(spot.getDescriptionTibetan());
        } else {
            dto.setDescription(spot.getDescription());
        }
        
        dto.setImageUrl(spot.getImageUrl());
        dto.setAltitude(spot.getAltitude());
        dto.setLocation(spot.getLocation());
        dto.setCategory(spot.getCategory());
        dto.setTicketPrice(spot.getTicketPrice());
        dto.setPeakSeasonPrice(spot.getPeakSeasonPrice());
        dto.setOffSeasonPrice(spot.getOffSeasonPrice());
        dto.setPeakStartDate(spot.getPeakStartDate());
        dto.setPeakEndDate(spot.getPeakEndDate());
        dto.setFreeStartDate(spot.getFreeStartDate());
        dto.setFreeEndDate(spot.getFreeEndDate());
        dto.setRating(spot.getRating() != null ? spot.getRating() : BigDecimal.ZERO);
        dto.setLatitude(spot.getLatitude());
        dto.setLongitude(spot.getLongitude());
        dto.setVisitCount(spot.getVisitCount() != null ? spot.getVisitCount() : 0);
        dto.setCreatedAt(spot.getCreatedAt());
        return dto;
    }
}

