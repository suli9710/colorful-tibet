package com.tibet.tourism.dto;

import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.ScenicSpot;
import java.math.BigDecimal;
import java.util.List;

public class DashboardStatsDTO {
    private long userCount;
    private long bookingCount;
    private BigDecimal totalRevenue;
    private List<Booking> recentBookings;
    private List<ScenicSpot> popularSpots;

    // Getters and Setters
    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public long getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(long bookingCount) {
        this.bookingCount = bookingCount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public List<Booking> getRecentBookings() {
        return recentBookings;
    }

    public void setRecentBookings(List<Booking> recentBookings) {
        this.recentBookings = recentBookings;
    }

    public List<ScenicSpot> getPopularSpots() {
        return popularSpots;
    }

    public void setPopularSpots(List<ScenicSpot> popularSpots) {
        this.popularSpots = popularSpots;
    }
}
