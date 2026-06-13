package com.tibet.tourism.modules.route.domain;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itineraries", indexes = {
        @Index(name = "idx_itineraries_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_itineraries_parent", columnList = "parent_itinerary_id")
})
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_itinerary_id")
    private Itinerary parentItinerary;

    @Column(nullable = false, length = 160)
    private String title;

    private Integer days;
    private LocalDate startDate;

    @Column(length = 32)
    private String budget;

    @Column(length = 64)
    private String preference;

    @Column(length = 48)
    private String versionType;

    @Column(length = 64)
    private String versionLabel;

    @Column(columnDefinition = "TEXT")
    private String sourceContent;

    private BigDecimal totalEstimatedCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @BatchSize(size = 50)
    private List<ItineraryDay> itineraryDays = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addDay(ItineraryDay day) {
        itineraryDays.add(day);
        day.setItinerary(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Itinerary getParentItinerary() { return parentItinerary; }
    public void setParentItinerary(Itinerary parentItinerary) { this.parentItinerary = parentItinerary; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }

    public String getVersionType() { return versionType; }
    public void setVersionType(String versionType) { this.versionType = versionType; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public String getSourceContent() { return sourceContent; }
    public void setSourceContent(String sourceContent) { this.sourceContent = sourceContent; }

    public BigDecimal getTotalEstimatedCost() { return totalEstimatedCost; }
    public void setTotalEstimatedCost(BigDecimal totalEstimatedCost) { this.totalEstimatedCost = totalEstimatedCost; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public List<ItineraryDay> getItineraryDays() { return itineraryDays; }
    public void setItineraryDays(List<ItineraryDay> itineraryDays) { this.itineraryDays = itineraryDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum Status {
        DRAFT, QUOTED, BOOKED, ARCHIVED
    }
}
