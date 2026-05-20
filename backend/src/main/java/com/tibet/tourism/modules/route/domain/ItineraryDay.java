package com.tibet.tourism.modules.route.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itinerary_days", indexes = {
        @Index(name = "idx_itinerary_days_itinerary_day", columnList = "itinerary_id, day_number")
})
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 96)
    private String region;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(length = 32)
    private String altitudeRisk;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ItineraryItem> items = new ArrayList<>();

    public void addItem(ItineraryItem item) {
        items.add(item);
        item.setDay(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Itinerary getItinerary() { return itinerary; }
    public void setItinerary(Itinerary itinerary) { this.itinerary = itinerary; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getAltitudeRisk() { return altitudeRisk; }
    public void setAltitudeRisk(String altitudeRisk) { this.altitudeRisk = altitudeRisk; }

    public List<ItineraryItem> getItems() { return items; }
    public void setItems(List<ItineraryItem> items) { this.items = items; }
}
