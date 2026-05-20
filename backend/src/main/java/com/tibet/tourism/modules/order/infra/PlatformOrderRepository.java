package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformOrderRepository extends JpaRepository<PlatformOrder, Long> {

    Optional<PlatformOrder> findByIdAndUserId(Long id, Long userId);

    Optional<PlatformOrder> findByOrderNo(String orderNo);

    Optional<PlatformOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<PlatformOrder> findBySourceTypeAndSourceReferenceId(String sourceType, Long sourceReferenceId);

    List<PlatformOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PlatformOrder> findByStatusAndExpiresAtBefore(PlatformOrder.Status status, LocalDateTime expiresAt);
}
