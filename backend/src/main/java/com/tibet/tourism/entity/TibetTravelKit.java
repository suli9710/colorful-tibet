package com.tibet.tourism.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tibet_travel_kits", indexes = {
        @Index(name = "idx_tibet_travel_kits_user_generated", columnList = "user_id, generated_at"),
        @Index(name = "idx_tibet_travel_kits_itinerary_valid", columnList = "itinerary_id, valid_until")
})
public class TibetTravelKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    @Column(name = "risk_label", length = 32)
    private String riskLabel;

    @Column(name = "max_altitude_meters")
    private Integer maxAltitudeMeters;

    @Column(name = "high_altitude_days")
    private Integer highAltitudeDays;

    @Column(name = "highland_summary", columnDefinition = "TEXT")
    private String highlandSummary;

    @Column(name = "included_sections", columnDefinition = "TEXT")
    private String includedSections;

    @Column(name = "adaptation_checklist", columnDefinition = "TEXT")
    private String adaptationChecklist;

    @Column(name = "warning_signs", columnDefinition = "TEXT")
    private String warningSigns;

    @Column(name = "go_slow_rules", columnDefinition = "TEXT")
    private String goSlowRules;

    @Column(name = "offline_checklist", columnDefinition = "TEXT")
    private String offlineChecklist;

    @Column(name = "voucher_hints", columnDefinition = "TEXT")
    private String voucherHints;

    @Column(name = "backup_notes", columnDefinition = "TEXT")
    private String backupNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "travelKit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<TibetTravelKitDayAdvice> dayAdvices = new ArrayList<>();

    @OneToMany(mappedBy = "travelKit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TibetTravelKitCultureTip> cultureTips = new ArrayList<>();

    @OneToMany(mappedBy = "travelKit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TibetTravelKitPhrase> phrases = new ArrayList<>();

    @OneToMany(mappedBy = "travelKit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TibetTravelKitEmergencyContact> emergencyContacts = new ArrayList<>();

    @OneToMany(mappedBy = "travelKit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TibetTravelKitMapPin> mapPins = new ArrayList<>();

    @OneToMany(mappedBy = "travelKit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TibetTravelKitAlert> alerts = new ArrayList<>();

    @OneToMany(mappedBy = "travelKit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TibetTravelKitSustainableOption> sustainableOptions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (generatedAt == null) {
            generatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addDayAdvice(TibetTravelKitDayAdvice advice) {
        dayAdvices.add(advice);
        advice.setTravelKit(this);
    }

    public void addCultureTip(TibetTravelKitCultureTip tip) {
        cultureTips.add(tip);
        tip.setTravelKit(this);
    }

    public void addPhrase(TibetTravelKitPhrase phrase) {
        phrases.add(phrase);
        phrase.setTravelKit(this);
    }

    public void addEmergencyContact(TibetTravelKitEmergencyContact contact) {
        emergencyContacts.add(contact);
        contact.setTravelKit(this);
    }

    public void addMapPin(TibetTravelKitMapPin pin) {
        mapPins.add(pin);
        pin.setTravelKit(this);
    }

    public void addAlert(TibetTravelKitAlert alert) {
        alerts.add(alert);
        alert.setTravelKit(this);
    }

    public void addSustainableOption(TibetTravelKitSustainableOption option) {
        sustainableOptions.add(option);
        option.setTravelKit(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Itinerary getItinerary() { return itinerary; }
    public void setItinerary(Itinerary itinerary) { this.itinerary = itinerary; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRiskLabel() { return riskLabel; }
    public void setRiskLabel(String riskLabel) { this.riskLabel = riskLabel; }

    public Integer getMaxAltitudeMeters() { return maxAltitudeMeters; }
    public void setMaxAltitudeMeters(Integer maxAltitudeMeters) { this.maxAltitudeMeters = maxAltitudeMeters; }

    public Integer getHighAltitudeDays() { return highAltitudeDays; }
    public void setHighAltitudeDays(Integer highAltitudeDays) { this.highAltitudeDays = highAltitudeDays; }

    public String getHighlandSummary() { return highlandSummary; }
    public void setHighlandSummary(String highlandSummary) { this.highlandSummary = highlandSummary; }

    public String getIncludedSections() { return includedSections; }
    public void setIncludedSections(String includedSections) { this.includedSections = includedSections; }

    public String getAdaptationChecklist() { return adaptationChecklist; }
    public void setAdaptationChecklist(String adaptationChecklist) { this.adaptationChecklist = adaptationChecklist; }

    public String getWarningSigns() { return warningSigns; }
    public void setWarningSigns(String warningSigns) { this.warningSigns = warningSigns; }

    public String getGoSlowRules() { return goSlowRules; }
    public void setGoSlowRules(String goSlowRules) { this.goSlowRules = goSlowRules; }

    public String getOfflineChecklist() { return offlineChecklist; }
    public void setOfflineChecklist(String offlineChecklist) { this.offlineChecklist = offlineChecklist; }

    public String getVoucherHints() { return voucherHints; }
    public void setVoucherHints(String voucherHints) { this.voucherHints = voucherHints; }

    public String getBackupNotes() { return backupNotes; }
    public void setBackupNotes(String backupNotes) { this.backupNotes = backupNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<TibetTravelKitDayAdvice> getDayAdvices() { return dayAdvices; }
    public void setDayAdvices(List<TibetTravelKitDayAdvice> dayAdvices) { this.dayAdvices = dayAdvices; }

    public List<TibetTravelKitCultureTip> getCultureTips() { return cultureTips; }
    public void setCultureTips(List<TibetTravelKitCultureTip> cultureTips) { this.cultureTips = cultureTips; }

    public List<TibetTravelKitPhrase> getPhrases() { return phrases; }
    public void setPhrases(List<TibetTravelKitPhrase> phrases) { this.phrases = phrases; }

    public List<TibetTravelKitEmergencyContact> getEmergencyContacts() { return emergencyContacts; }
    public void setEmergencyContacts(List<TibetTravelKitEmergencyContact> emergencyContacts) { this.emergencyContacts = emergencyContacts; }

    public List<TibetTravelKitMapPin> getMapPins() { return mapPins; }
    public void setMapPins(List<TibetTravelKitMapPin> mapPins) { this.mapPins = mapPins; }

    public List<TibetTravelKitAlert> getAlerts() { return alerts; }
    public void setAlerts(List<TibetTravelKitAlert> alerts) { this.alerts = alerts; }

    public List<TibetTravelKitSustainableOption> getSustainableOptions() { return sustainableOptions; }
    public void setSustainableOptions(List<TibetTravelKitSustainableOption> sustainableOptions) { this.sustainableOptions = sustainableOptions; }
}
