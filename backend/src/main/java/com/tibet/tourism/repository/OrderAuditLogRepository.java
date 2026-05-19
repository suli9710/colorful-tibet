package com.tibet.tourism.repository;

import com.tibet.tourism.entity.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {
}
