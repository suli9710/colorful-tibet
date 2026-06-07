package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.InventoryLock;
import com.tibet.tourism.modules.order.domain.OrderItem;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLockRepository extends JpaRepository<InventoryLock, Long> {
    List<InventoryLock> findByOrder(PlatformOrder order);
    boolean existsByActiveLockKey(String activeLockKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryLock> findByActiveLockKey(String activeLockKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM InventoryLock l
            WHERE l.productType = :productType
              AND l.productId = :productId
              AND l.serviceDate = :serviceDate
              AND l.status IN :statuses
            """)
    List<InventoryLock> findActiveProductDateLocksForUpdate(
            @Param("productType") OrderItem.ProductType productType,
            @Param("productId") Long productId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("statuses") Collection<InventoryLock.Status> statuses);

    @Query("""
            SELECT COALESCE(SUM(l.quantity), 0)
            FROM InventoryLock l
            WHERE l.productType = :productType
              AND l.productId = :productId
              AND ((:skuId IS NULL AND l.skuId IS NULL) OR l.skuId = :skuId)
              AND l.serviceDate = :serviceDate
              AND l.status IN :statuses
            """)
    Long sumActiveQuantity(
            @Param("productType") OrderItem.ProductType productType,
            @Param("productId") Long productId,
            @Param("skuId") Long skuId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("statuses") Collection<InventoryLock.Status> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM InventoryLock l
            WHERE l.productType = :productType
              AND l.productId = :productId
              AND ((:skuId IS NULL AND l.skuId IS NULL) OR l.skuId = :skuId)
              AND l.serviceDate = :serviceDate
              AND l.status = :status
              AND l.expiresAt < :now
            """)
    List<InventoryLock> findExpiredProductLocksForUpdate(
            @Param("productType") OrderItem.ProductType productType,
            @Param("productId") Long productId,
            @Param("skuId") Long skuId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("status") InventoryLock.Status status,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT l FROM InventoryLock l
            WHERE l.status = :status
              AND l.expiresAt < :expiresAt
            """)
    List<InventoryLock> findExpiredLocks(
            @Param("status") InventoryLock.Status status,
            @Param("expiresAt") LocalDateTime expiresAt);
}
