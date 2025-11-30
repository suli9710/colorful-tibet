package com.tibet.tourism.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "scenic_spots")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "tags"})
public class ScenicSpot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_tibetan")
    private String nameTibetan;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_tibetan", columnDefinition = "TEXT")
    private String descriptionTibetan;

    private String imageUrl;
    private String altitude;
    private String location;

    @Enumerated(EnumType.STRING)
    private Category category;

    // 基础票价（默认价格）
    private BigDecimal ticketPrice;
    // 旺季票价
    private BigDecimal peakSeasonPrice;
    // 淡季票价
    private BigDecimal offSeasonPrice;
    // 旺季时间范围
    private LocalDate peakStartDate;
    private LocalDate peakEndDate;
    // 特殊免费时间段（例如冬季旅游季免费）
    private LocalDate freeStartDate;
    private LocalDate freeEndDate;
    private BigDecimal rating = BigDecimal.ZERO;
    
    // Coordinates for Heatmap
    private BigDecimal latitude;
    private BigDecimal longitude;

    private Integer visitCount = 0;

    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // 防止序列化时的懒加载问题
    private List<SpotTag> tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

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

    public String getNameTibetan() {
        return nameTibetan;
    }

    public void setNameTibetan(String nameTibetan) {
        this.nameTibetan = nameTibetan;
    }

    public String getDescriptionTibetan() {
        return descriptionTibetan;
    }

    public void setDescriptionTibetan(String descriptionTibetan) {
        this.descriptionTibetan = descriptionTibetan;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
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

    public LocalDate getPeakStartDate() {
        return peakStartDate;
    }

    public void setPeakStartDate(LocalDate peakStartDate) {
        this.peakStartDate = peakStartDate;
    }

    public LocalDate getPeakEndDate() {
        return peakEndDate;
    }

    public void setPeakEndDate(LocalDate peakEndDate) {
        this.peakEndDate = peakEndDate;
    }

    public LocalDate getFreeStartDate() {
        return freeStartDate;
    }

    public void setFreeStartDate(LocalDate freeStartDate) {
        this.freeStartDate = freeStartDate;
    }

    public LocalDate getFreeEndDate() {
        return freeEndDate;
    }

    public void setFreeEndDate(LocalDate freeEndDate) {
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

    public List<SpotTag> getTags() {
        return tags;
    }

    public void setTags(List<SpotTag> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public enum Category {
        NATURAL, CULTURAL, RELIGIOUS, HISTORICAL
    }
}
