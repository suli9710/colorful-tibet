package com.tibet.tourism.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (!path.startsWith("/api/") || SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!hasAuthCookie(request) || hasValidCsrfToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Invalid CSRF token\"}");
    }

    private boolean hasAuthCookie(HttpServletRequest request) {
        return WebUtils.getCookie(request, CookieAuthConstants.AUTH_COOKIE_NAME) != null;
    }

    private boolean hasValidCsrfToken(HttpServletRequest request) {
        String csrfCookie = readCookie(request, CookieAuthConstants.CSRF_COOKIE_NAME);
        String csrfHeader = request.getHeader(CookieAuthConstants.CSRF_HEADER_NAME);
        return StringUtils.hasText(csrfCookie)
                && StringUtils.hasText(csrfHeader)
                && csrfCookie.equals(csrfHeader);
    }

    private String readCookie(HttpServletRequest request, String name) {
        var cookie = WebUtils.getCookie(request, name);
        return cookie == null ? null : cookie.getValue();
    }
}
