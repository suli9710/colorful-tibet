package com.tibet.tourism.common.security;

import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Persists sanitized administrator access denials without changing the already-decided response. */
@Component
public class AdminAccessDeniedAuditListener {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccessDeniedAuditListener.class);

    private final AdminAuditLogService auditLogService;

    public AdminAccessDeniedAuditListener(AdminAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onAccessDenied(AdminAccessDeniedAuditEvent event) {
        try {
            AdminAuditLogService.Attempt attempt = auditLogService.begin(
                    event.category().targetType(),
                    event.targetId(),
                    event.category().action());
            auditLogService.complete(
                    attempt,
                    AdminAuditLogService.Result.DENIED,
                    event.source().reason());
        } catch (RuntimeException exception) {
            logger.warn("Administrator access-denial audit persistence failed: category={}, source={}, error={}",
                    event.category().name(),
                    event.source().name(),
                    SensitiveLogSanitizer.exceptionSummary(exception));
        }
    }
}
