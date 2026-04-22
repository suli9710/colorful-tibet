package com.tibet.tourism.repository;

import com.tibet.tourism.entity.HotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelBookingRepository extends JpaRepository<HotelBooking, Long> {
    List<HotelBooking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<HotelBooking> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(hb) FROM HotelBooking hb")
    long countAll();

    @Query("SELECT COALESCE(SUM(hb.totalPrice), 0) FROM HotelBooking hb WHERE hb.status = 'CONFIRMED'")
    java.math.BigDecimal sumTotalRevenue();

    List<HotelBooking> findTop5ByStatusOrderByCreatedAtDesc(HotelBooking.Status status);

    // 根据用户ID删除所有酒店预订
    void deleteByUserId(Long userId);
}
