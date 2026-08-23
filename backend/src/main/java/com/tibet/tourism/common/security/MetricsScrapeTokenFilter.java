package com.tibet.tourism.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates the Prometheus service without issuing a long-lived administrator JWT. */
@Component
public class MetricsScrapeTokenFilter extends OncePerRequestFilter {

    static final String METRICS_PATH = "/actuator/prometheus";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MIN_TOKEN_LENGTH = 32;

    private final String configuredToken;

    public MetricsScrapeTokenFilter(
            @Value("${app.security.metrics-scrape-token:${METRICS_SCRAPE_TOKEN:}}") String configuredToken) {
        this.configuredToken = configuredToken == null ? "" : configuredToken.trim();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (configuredToken.length() >= MIN_TOKEN_LENGTH
                && StringUtils.hasText(authorization)
                && authorization.startsWith(BEARER_PREFIX)
                && constantTimeEquals(configuredToken, authorization.substring(BEARER_PREFIX.length()))) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "prometheus-scraper",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isMetricsRequest(request);
    }

    static boolean isMetricsRequest(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!StringUtils.hasText(path)) {
            path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
        }
        return METRICS_PATH.equals(path);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
