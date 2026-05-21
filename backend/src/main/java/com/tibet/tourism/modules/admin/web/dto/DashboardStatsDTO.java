package com.tibet.tourism.modules.admin.web.dto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStatsDTO {
    private long userCount;
    private long bookingCount;
    private BigDecimal totalRevenue;
    private List<Map<String, Object>> recentBookings;
    private List<Map<String, Object>> popularSpots;

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

    public List<Map<String, Object>> getRecentBookings() {
        return recentBookings;
    }

    public void setRecentBookings(List<Map<String, Object>> recentBookings) {
        this.recentBookings = recentBookings;
    }

    public List<Map<String, Object>> getPopularSpots() {
        return popularSpots;
    }

    public void setPopularSpots(List<Map<String, Object>> popularSpots) {
        this.popularSpots = popularSpots;
    }
}
