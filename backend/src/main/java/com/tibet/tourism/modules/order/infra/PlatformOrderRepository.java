package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformOrderRepository extends JpaRepository<PlatformOrder, Long> {

    Optional<PlatformOrder> findByIdAndUserId(Long id, Long userId);

    Optional<PlatformOrder> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PlatformOrder o WHERE o.orderNo = :orderNo")
    Optional<PlatformOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PlatformOrder o WHERE o.id = :id AND o.user.id = :userId")
    Optional<PlatformOrder> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    Optional<PlatformOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<PlatformOrder> findBySourceTypeAndSourceReferenceId(String sourceType, Long sourceReferenceId);

    List<PlatformOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PlatformOrder> findByStatusAndExpiresAtBefore(PlatformOrder.Status status, LocalDateTime expiresAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PlatformOrder o WHERE o.status = :status AND o.expiresAt < :expiresAt")
    List<PlatformOrder> findByStatusAndExpiresAtBeforeForUpdate(@Param("status") PlatformOrder.Status status,
                                                                @Param("expiresAt") LocalDateTime expiresAt);

    void deleteByUserId(Long userId);
}
