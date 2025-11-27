package com.tibet.tourism.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_visit_history")
public class UserVisitHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "spot_id")
    @JsonIgnore
    private ScenicSpot spot;

    private Integer rating; // 1-5

    @Column(name = "click_count")
    private Integer clickCount;

    @Column(name = "dwell_seconds")
    private Integer dwellSeconds;

    @Column(name = "visit_date")
    private LocalDateTime visitDate;

    @PrePersist
    protected void onCreate() {
        if (visitDate == null) {
            visitDate = LocalDateTime.now();
        }
        if (clickCount == null) {
            clickCount = 0;
        }
        if (dwellSeconds == null) {
            dwellSeconds = 0;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ScenicSpot getSpot() {
        return spot;
    }

    public void setSpot(ScenicSpot spot) {
        this.spot = spot;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Integer getDwellSeconds() {
        return dwellSeconds;
    }

    public void setDwellSeconds(Integer dwellSeconds) {
        this.dwellSeconds = dwellSeconds;
    }

    public LocalDateTime getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
    }
}
