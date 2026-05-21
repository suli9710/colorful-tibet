package com.tibet.tourism.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;

public final class ApiSecurityPaths {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD");

    private static final String[] PUBLIC_READ_PATHS = {
            "/api/spots",
            "/api/spots/search",
            "/api/spots/heatmap",
            "/api/spots/{id:\\d+}",
            "/api/spots/{id:\\d+}/similar",
            "/api/news",
            "/api/heritage",
            "/api/heritage/{id:\\d+}",
            "/api/heritage/{id:\\d+}/comments",
            "/api/heritage/{id:\\d+}/inheritors",
            "/api/heritage/{id:\\d+}/events",
            "/api/heritage/events/upcoming",
            "/api/tibet-specialty/culture-tips",
            "/api/tibet-specialty/phrasebook",
            "/api/tibet-specialty/sustainable-options",
            "/api/routes/shared",
            "/api/routes/shared/{id:\\d+}",
            "/api/routes/shared/{id:\\d+}/comments",
            "/api/carousels",
            "/api/hotel-bookings/hotels",
            "/api/hotel-bookings/hotels/{id:\\d+}",
            "/api/hotel-bookings/room-types/{hotelId:\\d+}",
            "/api/comments/spot/{spotId:\\d+}",
            "/api/community/questions",
            "/api/community/questions/{id:\\d+}",
            "/api/community/questions/{id:\\d+}/answers",
            "/api/community/questions/{id:\\d+}/like-status",
            "/images/**",
            "/uploads/**"
    };

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/logout",
            "/api/payments/callbacks/mock");

    private static final String[] ADMIN_API_PATHS = {
            "/api/admin/**",
            "/api/spots/admin/**"
    };

    private ApiSecurityPaths() {
    }

    public static RequestMatcher publicRequests() {
        return request -> isPublicRequest(request.getMethod(), servletPath(request));
    }

    public static RequestMatcher adminApiRequests() {
        return request -> matchesAny(servletPath(request), ADMIN_API_PATHS);
    }

    public static boolean isPublicRequest(String method, String path) {
        if (READ_METHODS.contains(method)) {
            return matchesAny(path, PUBLIC_READ_PATHS);
        }
        return "POST".equals(method) && PUBLIC_POST_PATHS.contains(path);
    }

    private static String servletPath(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || path.isBlank() ? request.getRequestURI() : path;
    }

    private static boolean matchesAny(String path, String[] patterns) {
        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
