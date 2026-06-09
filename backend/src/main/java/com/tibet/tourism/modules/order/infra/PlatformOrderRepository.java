package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformOrderRepository extends JpaRepository<PlatformOrder, Long> {

    @Query("SELECT o FROM PlatformOrder o WHERE o.id = :id AND o.user.id = :userId AND o.userHiddenAt IS NULL")
    Optional<PlatformOrder> findVisibleByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Optional<PlatformOrder> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PlatformOrder o WHERE o.orderNo = :orderNo")
    Optional<PlatformOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PlatformOrder o WHERE o.id = :id AND o.user.id = :userId AND o.userHiddenAt IS NULL")
    Optional<PlatformOrder> findVisibleByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PlatformOrder o WHERE o.id = :id")
    Optional<PlatformOrder> findByIdForUpdate(@Param("id") Long id);

    Optional<PlatformOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<PlatformOrder> findBySourceTypeAndSourceReferenceId(String sourceType, Long sourceReferenceId);

    @Query("SELECT o FROM PlatformOrder o WHERE o.user.id = :userId AND o.userHiddenAt IS NULL")
    Page<PlatformOrder> findVisibleByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT o FROM PlatformOrder o
            WHERE o.user.id = :userId
              AND o.userHiddenAt IS NULL
            ORDER BY o.createdAt DESC
            """)
    List<PlatformOrder> findVisibleByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    List<PlatformOrder> findByStatusAndExpiresAtBefore(PlatformOrder.Status status, LocalDateTime expiresAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM PlatformOrder o
            WHERE o.status = :status
              AND o.expiresAt < :expiresAt
            ORDER BY o.expiresAt ASC, o.id ASC
            """)
    List<PlatformOrder> findByStatusAndExpiresAtBeforeForUpdate(@Param("status") PlatformOrder.Status status,
                                                                @Param("expiresAt") LocalDateTime expiresAt,
                                                                Pageable pageable);

    void deleteByUserId(Long userId);
}
