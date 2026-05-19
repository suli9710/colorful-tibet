package com.tibet.tourism.repository;

import com.tibet.tourism.entity.HotelBooking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HotelBookingRepository extends JpaRepository<HotelBooking, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "hotel"})
    @Query("SELECT hb FROM HotelBooking hb WHERE hb.id = :id AND hb.deletedAt IS NULL")
    Optional<HotelBooking> findById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "hotel"})
    @Query("SELECT hb FROM HotelBooking hb WHERE hb.user.id = :userId AND hb.deletedAt IS NULL ORDER BY hb.createdAt DESC")
    List<HotelBooking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "hotel"})
    @Query("SELECT hb FROM HotelBooking hb WHERE hb.user.id = :userId AND hb.deletedAt IS NULL ORDER BY hb.createdAt DESC")
    Page<HotelBooking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel"})
    @Query("SELECT hb FROM HotelBooking hb WHERE hb.deletedAt IS NULL ORDER BY hb.createdAt DESC")
    List<HotelBooking> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "hotel"})
    @Query("SELECT hb FROM HotelBooking hb WHERE hb.deletedAt IS NULL ORDER BY hb.createdAt DESC")
    Page<HotelBooking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(hb) FROM HotelBooking hb WHERE hb.deletedAt IS NULL")
    long countAll();

    @Query("SELECT COUNT(hb) FROM HotelBooking hb WHERE hb.status = :status AND hb.deletedAt IS NULL")
    long countByStatus(@Param("status") HotelBooking.Status status);

    @Query("SELECT COALESCE(SUM(hb.totalPrice), 0) FROM HotelBooking hb WHERE hb.status = 'CONFIRMED' AND hb.deletedAt IS NULL")
    java.math.BigDecimal sumTotalRevenue();

    @EntityGraph(attributePaths = {"user", "hotel"})
    List<HotelBooking> findTop5ByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(HotelBooking.Status status);

    List<HotelBooking> findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(HotelBooking.Status status);

    List<HotelBooking> findByStatusAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtAsc(HotelBooking.Status status, LocalDateTime createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT hb FROM HotelBooking hb
            WHERE hb.roomTypeId = :roomTypeId
              AND hb.deletedAt IS NULL
              AND hb.status IN :statuses
              AND hb.checkInDate < :checkOut
              AND hb.checkOutDate > :checkIn
            """)
    List<HotelBooking> findOverlappingActiveBookingsForUpdate(@Param("roomTypeId") Long roomTypeId,
                                                              @Param("statuses") Collection<HotelBooking.Status> statuses,
                                                              @Param("checkIn") LocalDate checkIn,
                                                              @Param("checkOut") LocalDate checkOut);

    // 根据用户ID删除所有酒店预订
    void deleteByUserId(Long userId);
}
