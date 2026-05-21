package com.tibet.tourism.modules.content.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "heritage_events", indexes = {
    @Index(name = "idx_heritage_events_item", columnList = "heritage_item_id"),
    @Index(name = "idx_heritage_events_date", columnList = "event_date")
})
public class HeritageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "title_tibetan")
    private String titleTibetan;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_tibetan", columnDefinition = "TEXT")
    private String descriptionTibetan;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String location;

    private String imageUrl;

    @Column(name = "contact_info")
    private String contactInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "heritage_item_id")
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleTibetan() {
        return titleTibetan;
    }

    public void setTitleTibetan(String titleTibetan) {
        this.titleTibetan = titleTibetan;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionTibetan() {
        return descriptionTibetan;
    }

    public void setDescriptionTibetan(String descriptionTibetan) {
        this.descriptionTibetan = descriptionTibetan;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
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
