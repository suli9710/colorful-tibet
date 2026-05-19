package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tibet_travel_kit_alerts", indexes = {
        @Index(name = "idx_tibet_kit_alerts_kit_sort", columnList = "travel_kit_id, sort_order"),
        @Index(name = "idx_tibet_kit_alerts_expires", columnList = "expires_at")
})
public class TibetTravelKitAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_kit_id", nullable = false)
    private TibetTravelKit travelKit;

    @Column(length = 32)
    private String level;

    @Column(name = "alert_type", length = 48)
    private String type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String action;

    @Column(name = "related_day")
    private Integer relatedDay;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TibetTravelKit getTravelKit() { return travelKit; }
    public void setTravelKit(TibetTravelKit travelKit) { this.travelKit = travelKit; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Integer getRelatedDay() { return relatedDay; }
    public void setRelatedDay(Integer relatedDay) { this.relatedDay = relatedDay; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
