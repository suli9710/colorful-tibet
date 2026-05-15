package com.tibet.tourism.repository;

import com.tibet.tourism.entity.HotelBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HotelBookingRepository extends JpaRepository<HotelBooking, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "hotel"})
    Optional<HotelBooking> findById(Long id);

    @EntityGraph(attributePaths = {"user", "hotel"})
    List<HotelBooking> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "hotel"})
    Page<HotelBooking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel"})
    List<HotelBooking> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "hotel"})
    Page<HotelBooking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(hb) FROM HotelBooking hb")
    long countAll();

    long countByStatus(HotelBooking.Status status);

    @Query("SELECT COALESCE(SUM(hb.totalPrice), 0) FROM HotelBooking hb WHERE hb.status = 'CONFIRMED'")
    java.math.BigDecimal sumTotalRevenue();

    @EntityGraph(attributePaths = {"user", "hotel"})
    List<HotelBooking> findTop5ByStatusOrderByCreatedAtDesc(HotelBooking.Status status);

    List<HotelBooking> findByStatusOrderByCreatedAtAsc(HotelBooking.Status status);

    List<HotelBooking> findByStatusAndCreatedAtAfterOrderByCreatedAtAsc(HotelBooking.Status status, LocalDateTime createdAt);

    // 根据用户ID删除所有酒店预订
    void deleteByUserId(Long userId);
}
