package com.tibet.tourism.repository;

import com.tibet.tourism.entity.PlatformOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlatformOrderRepository extends JpaRepository<PlatformOrder, Long> {

    Optional<PlatformOrder> findByIdAndUserId(Long id, Long userId);

    Optional<PlatformOrder> findByOrderNo(String orderNo);

    Optional<PlatformOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<PlatformOrder> findBySourceTypeAndSourceReferenceId(String sourceType, Long sourceReferenceId);

    List<PlatformOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PlatformOrder> findByStatusAndExpiresAtBefore(PlatformOrder.Status status, LocalDateTime expiresAt);
}
