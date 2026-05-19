package com.tibet.tourism.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "behavior_logs", indexes = {
        @Index(name = "idx_bl_user_id", columnList = "userId"),
        @Index(name = "idx_bl_created_at", columnList = "createdAt"),
        @Index(name = "idx_bl_decision", columnList = "decision")
})
public class BehaviorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 128)
    private String endpoint;

    @Column(length = 64)
    private String fingerprint;

    private Double mouseSpeedMean;
    private Double mouseSpeedStdDev;
    private Double straightnessRatio;
    private Double mouseIntervalStdDev;
    private Double keyIntervalStdDev;
    private Double recaptchaScore;
    private Double finalRiskScore;

    @Column(length = 16)
    private String decision;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public Double getMouseSpeedMean() { return mouseSpeedMean; }
    public void setMouseSpeedMean(Double mouseSpeedMean) { this.mouseSpeedMean = mouseSpeedMean; }
    public Double getMouseSpeedStdDev() { return mouseSpeedStdDev; }
    public void setMouseSpeedStdDev(Double mouseSpeedStdDev) { this.mouseSpeedStdDev = mouseSpeedStdDev; }
    public Double getStraightnessRatio() { return straightnessRatio; }
    public void setStraightnessRatio(Double straightnessRatio) { this.straightnessRatio = straightnessRatio; }
    public Double getMouseIntervalStdDev() { return mouseIntervalStdDev; }
    public void setMouseIntervalStdDev(Double mouseIntervalStdDev) { this.mouseIntervalStdDev = mouseIntervalStdDev; }
    public Double getKeyIntervalStdDev() { return keyIntervalStdDev; }
    public void setKeyIntervalStdDev(Double keyIntervalStdDev) { this.keyIntervalStdDev = keyIntervalStdDev; }
    public Double getRecaptchaScore() { return recaptchaScore; }
    public void setRecaptchaScore(Double recaptchaScore) { this.recaptchaScore = recaptchaScore; }
    public Double getFinalRiskScore() { return finalRiskScore; }
    public void setFinalRiskScore(Double finalRiskScore) { this.finalRiskScore = finalRiskScore; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
