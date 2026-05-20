package com.tibet.tourism.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_bookings_spot_visit", columnList = "spot_id, visit_date"),
    @Index(name = "idx_bookings_status_created", columnList = "status, created_at")
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private ScenicSpot spot;

    private LocalDate visitDate;
    private Integer ticketCount;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Version
    @Column(name = "version")
    private Long version = 0L;

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

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public Integer getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Integer ticketCount) {
        this.ticketCount = ticketCount;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public enum Status {
        PENDING, CONFIRMED, CANCELLED
    }
}
