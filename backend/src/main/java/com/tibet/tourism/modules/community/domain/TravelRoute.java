package com.tibet.tourism.modules.community.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_routes")
public class TravelRoute {
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
    private Integer days;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String spotsJson; // Storing IDs as JSON string for simplicity

    private String temperature; // 温度描述，如"15°C - 25°C"
    private String geography; // 地理特征，如"高原山地、河谷地带"

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

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getSpotsJson() {
        return spotsJson;
    }

    public void setSpotsJson(String spotsJson) {
        this.spotsJson = spotsJson;
    }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public String getGeography() { return geography; }
    public void setGeography(String geography) { this.geography = geography; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
}
