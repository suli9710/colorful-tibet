package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {

    @Modifying
    @Query("UPDATE OrderAuditLog log SET log.actorUser = null WHERE log.actorUser.id = :userId")
    void clearActorUserByUserId(@Param("userId") Long userId);
}
