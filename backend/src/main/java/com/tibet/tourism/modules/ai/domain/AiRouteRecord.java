package com.tibet.tourism.modules.ai.domain;

import com.tibet.tourism.modules.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_route_records", indexes = {
        @Index(name = "idx_ai_route_records_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_ai_route_records_user_saved_updated", columnList = "user_id, manually_saved, updated_at"),
        @Index(name = "idx_ai_route_records_job", columnList = "job_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_route_records_user_job", columnNames = {"user_id", "job_id"})
})
public class AiRouteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_id", length = 72)
    private String jobId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content = "";

    @Column(nullable = false)
    private Integer days;

    @Column(length = 32)
    private String budget;

    @Column(length = 64)
    private String preference;

    @Column(length = 16)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.RUNNING;

    @Column(name = "manually_saved", nullable = false)
    private Boolean manuallySaved = false;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (title == null || title.isBlank()) {
            title = defaultTitle(days);
        }
        if (content == null) {
            content = "";
        }
        if (status == null) {
            status = Status.RUNNING;
        }
        if (manuallySaved == null) {
            manuallySaved = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (content == null) {
            content = "";
        }
        if (manuallySaved == null) {
            manuallySaved = false;
        }
    }

    public static String defaultTitle(Integer days) {
        int safeDays = days == null ? 5 : days;
        return "AI Route " + safeDays + " Days";
    }

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

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Boolean getManuallySaved() {
        return manuallySaved;
    }

    public void setManuallySaved(Boolean manuallySaved) {
        this.manuallySaved = manuallySaved;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum Status {
        RUNNING,
        COMPLETED,
        FAILED
    }
}
