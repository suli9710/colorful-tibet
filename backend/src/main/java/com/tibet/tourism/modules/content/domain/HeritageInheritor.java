package com.tibet.tourism.modules.content.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "heritage_inheritors", indexes = {
    @Index(name = "idx_heritage_inheritors_item", columnList = "heritage_item_id")
})
public class HeritageInheritor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_tibetan")
    private String nameTibetan;

    private String avatarUrl;

    @Column(name = "inheritor_level")
    private String level; // 国家级、省级、市级

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "bio_tibetan", columnDefinition = "TEXT")
    private String bioTibetan;

    @Column(columnDefinition = "TEXT")
    private String story;

    private String region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "heritage_item_id", nullable = false)
    @JsonIgnore
    private HeritageItem heritageItem;

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

    public String getNameTibetan() {
        return nameTibetan;
    }

    public void setNameTibetan(String nameTibetan) {
        this.nameTibetan = nameTibetan;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBioTibetan() {
        return bioTibetan;
    }

    public void setBioTibetan(String bioTibetan) {
        this.bioTibetan = bioTibetan;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public HeritageItem getHeritageItem() {
        return heritageItem;
    }

    public void setHeritageItem(HeritageItem heritageItem) {
        this.heritageItem = heritageItem;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
