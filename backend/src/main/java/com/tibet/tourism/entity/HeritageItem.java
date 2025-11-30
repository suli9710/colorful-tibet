package com.tibet.tourism.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "heritage_items")
public class HeritageItem {
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
    private String category;
    private String imageUrl;
    private String videoUrl;

    @Column(columnDefinition = "TEXT")
    private String originStory;

    @Column(columnDefinition = "TEXT")
    private String significance;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getOriginStory() {
        return originStory;
    }

    public void setOriginStory(String originStory) {
        this.originStory = originStory;
    }

    public String getSignificance() {
        return significance;
    }

    public void setSignificance(String significance) {
        this.significance = significance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
