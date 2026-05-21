package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {
}
