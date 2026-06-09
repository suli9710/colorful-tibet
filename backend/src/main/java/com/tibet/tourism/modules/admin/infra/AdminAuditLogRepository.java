package com.tibet.tourism.modules.admin.infra;

import com.tibet.tourism.modules.admin.domain.AdminAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<AdminAuditLog> findByTargetIdOrderByCreatedAtDesc(Long targetId);

    List<AdminAuditLog> findByActionOrderByCreatedAtDesc(String action);
}
