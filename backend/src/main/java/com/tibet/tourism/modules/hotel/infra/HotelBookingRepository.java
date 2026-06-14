package com.tibet.tourism.modules.hotel.infra;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelBookingRepository extends JpaRepository<HotelBooking, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "hotel"})
    @Query("SELECT hb FROM HotelBooking hb WHERE hb.id = :id AND hb.deletedAt IS NULL")
    Optional<HotelBooking> findById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "hotel"})
    @Query("SELECT hb FROM HotelBooking hb WHERE hb.id = :id AND hb.user.id = :userId AND hb.deletedAt IS NULL")
    Optional<HotelBooking> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

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

    @Query("""
            SELECT hb FROM HotelBooking hb
            WHERE hb.status = :status
              AND hb.deletedAt IS NULL
              AND hb.createdAt < :createdAt
            ORDER BY hb.createdAt ASC, hb.id ASC
            """)
    List<HotelBooking> findStalePendingBookings(@Param("status") HotelBooking.Status status,
                                                 @Param("createdAt") LocalDateTime createdAt,
                                                 Pageable pageable);

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

    // 统计某酒店下的所有预订（含软删除行），用于删除酒店前的外键安全检查
    @Query("SELECT COUNT(hb) FROM HotelBooking hb WHERE hb.hotel.id = :hotelId")
    long countByHotelId(@Param("hotelId") Long hotelId);
}
