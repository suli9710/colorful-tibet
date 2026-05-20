package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "spot"})
    Optional<Booking> findById(Long id);

    @EntityGraph(attributePaths = {"user", "spot"})
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CONFIRMED'")
    long countConfirmed();

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = 'CONFIRMED'")
    java.math.BigDecimal sumConfirmedRevenue();

    @EntityGraph(attributePaths = {"user", "spot"})
    List<Booking> findTop5ByStatusOrderByCreatedAtDesc(Booking.Status status);

    List<Booking> findByStatusOrderByCreatedAtAsc(Booking.Status status);

    List<Booking> findByStatusAndCreatedAtAfterOrderByCreatedAtAsc(Booking.Status status, LocalDateTime createdAt);

    @Modifying
    @Transactional
    @Query("DELETE FROM Booking b WHERE b.id = :id AND b.user.id = :userId AND b.status = :status")
    int deleteByIdAndUserIdAndStatus(@Param("id") Long id,
                                     @Param("userId") Long userId,
                                     @Param("status") Booking.Status status);

    // 根据用户ID删除所有订单
    void deleteByUserId(Long userId);
    void deleteBySpotId(Long spotId);
}
