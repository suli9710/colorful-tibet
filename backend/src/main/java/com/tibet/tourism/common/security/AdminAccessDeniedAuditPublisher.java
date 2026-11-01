package com.tibet.tourism.common.security;

import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent.Category;
import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent.Source;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Maps sensitive endpoint denials to bounded, code-owned audit categories. */
@Component
public class AdminAccessDeniedAuditPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccessDeniedAuditPublisher.class);
    private static final Pattern HOTEL_BOOKING_STATUS_PATH =
            Pattern.compile("^/api/hotel-bookings/(\\d+)/status/?$");

    private final ApplicationEventPublisher eventPublisher;

    public AdminAccessDeniedAuditPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes only a sanitized classification. Audit dispatch is fail-open because the protected
     * operation has already been denied and its 403 response must not be replaced by an audit error.
     */
    public void publish(HttpServletRequest request, Source source) {
        Optional<Classification> classification = classify(request);
        if (classification.isEmpty()) {
            return;
        }

        Classification denied = classification.get();
        AdminAccessDeniedAuditEvent event =
                new AdminAccessDeniedAuditEvent(denied.category(), denied.targetId(), source);
        try {
            eventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            logger.warn("Administrator access-denial audit dispatch failed: category={}, source={}, error={}",
                    denied.category().name(),
                    source.name(),
                    SensitiveLogSanitizer.exceptionSummary(exception));
        }
    }

    private Optional<Classification> classify(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        String path = servletPath(request);
        if (!StringUtils.hasText(path)) {
            return Optional.empty();
        }

        if (matchesBase(path, "/api/admin")) {
            return Optional.of(new Classification(Category.ADMIN_API, null));
        }
        if (matchesBase(path, "/api/prices")) {
            return Optional.of(new Classification(Category.PRICE_ADMIN_API, null));
        }
        if (matchesBase(path, "/api/spots/admin")) {
            return Optional.of(new Classification(Category.SCENIC_SPOT_ADMIN_API, null));
        }
        if ("/api/spots/recommendations/debug".equals(path)
                || "/api/spots/recommendations/debug/".equals(path)) {
            return Optional.of(new Classification(Category.RECOMMENDATION_DEBUG, null));
        }
        if ("PUT".equalsIgnoreCase(request.getMethod())) {
            Matcher matcher = HOTEL_BOOKING_STATUS_PATH.matcher(path);
            if (matcher.matches()) {
                try {
                    return Optional.of(new Classification(
                            Category.HOTEL_BOOKING_STATUS,
                            Long.parseLong(matcher.group(1))));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private static String servletPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (StringUtils.hasText(path)) {
            return path;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(requestUri)
                && StringUtils.hasText(contextPath)
                && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private static boolean matchesBase(String path, String base) {
        return base.equals(path) || path.startsWith(base + "/");
    }

    private record Classification(Category category, Long targetId) {
    }
}
