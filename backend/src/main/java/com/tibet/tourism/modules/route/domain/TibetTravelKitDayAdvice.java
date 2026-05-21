package com.tibet.tourism.modules.route.domain;
import jakarta.persistence.*;

@Entity
@Table(name = "tibet_travel_kit_day_advice", indexes = {
        @Index(name = "idx_tibet_kit_day_advice_kit_day", columnList = "travel_kit_id, day_number")
})
public class TibetTravelKitDayAdvice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_kit_id", nullable = false)
    private TibetTravelKit travelKit;

    @Column(name = "day_number")
    private Integer dayNumber;

    @Column(length = 160)
    private String title;

    @Column(name = "max_altitude_meters")
    private Integer maxAltitudeMeters;

    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    @Column(name = "pace_advice", columnDefinition = "TEXT")
    private String paceAdvice;

    @Column(name = "hydration_advice", columnDefinition = "TEXT")
    private String hydrationAdvice;

    @Column(name = "activity_limit", columnDefinition = "TEXT")
    private String activityLimit;

    @Column(columnDefinition = "TEXT")
    private String warning;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TibetTravelKit getTravelKit() { return travelKit; }
    public void setTravelKit(TibetTravelKit travelKit) { this.travelKit = travelKit; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getMaxAltitudeMeters() { return maxAltitudeMeters; }
    public void setMaxAltitudeMeters(Integer maxAltitudeMeters) { this.maxAltitudeMeters = maxAltitudeMeters; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getPaceAdvice() { return paceAdvice; }
    public void setPaceAdvice(String paceAdvice) { this.paceAdvice = paceAdvice; }

    public String getHydrationAdvice() { return hydrationAdvice; }
    public void setHydrationAdvice(String hydrationAdvice) { this.hydrationAdvice = hydrationAdvice; }

    public String getActivityLimit() { return activityLimit; }
    public void setActivityLimit(String activityLimit) { this.activityLimit = activityLimit; }

    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
}
