package com.tibet.tourism.common.security;

import java.util.Objects;

/**
 * Sanitized security event for a denied administrator endpoint.
 *
 * <p>The event deliberately contains no request path, query string, body, header or raw user
 * identifier. Code-owned enums are converted to the stable labels persisted by the administrator
 * audit trail.</p>
 */
public record AdminAccessDeniedAuditEvent(Category category, Long targetId, Source source) {

    public AdminAccessDeniedAuditEvent {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(source, "source");
    }

    public enum Category {
        ADMIN_API("admin_api", "admin_api_access_denied"),
        PRICE_ADMIN_API("price", "price_admin_access_denied"),
        SCENIC_SPOT_ADMIN_API("scenic_spot", "scenic_spot_admin_access_denied"),
        RECOMMENDATION_DEBUG("scenic_spot_recommendation", "recommendation_debug_access_denied"),
        HOTEL_BOOKING_STATUS("hotel_booking", "hotel_booking_status_access_denied");

        private final String targetType;
        private final String action;

        Category(String targetType, String action) {
            this.targetType = targetType;
            this.action = action;
        }

        public String targetType() {
            return targetType;
        }

        public String action() {
            return action;
        }
    }

    public enum Source {
        SECURITY_FILTER("security_filter"),
        METHOD_SECURITY("method_security");

        private final String reason;

        Source(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }
}
