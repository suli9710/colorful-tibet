package com.tibet.tourism.repository;

import com.tibet.tourism.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CONFIRMED'")
    long countConfirmed();

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = 'CONFIRMED'")
    java.math.BigDecimal sumConfirmedRevenue();

    List<Booking> findTop5ByStatusOrderByCreatedAtDesc(Booking.Status status);

    // 根据用户ID删除所有订单
    void deleteByUserId(Long userId);
}
