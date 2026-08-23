package com.tibet.tourism.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAccessDeniedHandler.class);
    private static final String ERROR = "Forbidden";
    private static final String MESSAGE = "Access denied";
    private final AdminAccessDeniedAuditPublisher auditPublisher;

    public SecurityAccessDeniedHandler() {
        this(null);
    }

    public SecurityAccessDeniedHandler(AdminAccessDeniedAuditPublisher auditPublisher) {
        this.auditPublisher = auditPublisher;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        publishDeniedAudit(request);
        SecurityErrorResponseWriter.writeJson(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                ERROR,
                MESSAGE);
    }

    private void publishDeniedAudit(HttpServletRequest request) {
        if (auditPublisher == null) {
            return;
        }
        try {
            auditPublisher.publish(request, AdminAccessDeniedAuditEvent.Source.SECURITY_FILTER);
        } catch (RuntimeException exception) {
            // A failed audit must not turn an already-denied request into a 500 response.
            logger.warn("Administrator access-denial audit handler failed: source=security_filter, error={}",
                    SensitiveLogSanitizer.exceptionSummary(exception));
        }
    }
}
