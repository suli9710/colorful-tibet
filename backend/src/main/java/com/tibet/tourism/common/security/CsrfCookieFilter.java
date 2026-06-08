package com.tibet.tourism.common.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfCookieFilter.class);
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final CsrfTokenService csrfTokenService;
    private final Set<String> allowedOrigins;

    public CsrfCookieFilter(
            CsrfTokenService csrfTokenService,
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.csrfTokenService = csrfTokenService;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(origin -> !origin.contains("*"))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (!path.startsWith("/api/") || SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authToken = readCookie(request, CookieAuthConstants.AUTH_COOKIE_NAME);
        if (ApiSecurityPaths.isPublicPostRequest(request.getMethod(), path)) {
            if (StringUtils.hasText(authToken)
                    && ApiSecurityPaths.requiresCsrfWhenAuthenticatedPublicPost(request.getMethod(), path)) {
                enforceAuthenticatedCsrf(request, response, filterChain, authToken);
                return;
            }
            if (hasBrowserRequestMetadata(request) && !hasTrustedRequestMetadata(request)) {
                reject(response, "Untrusted public POST request metadata");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(authToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        enforceAuthenticatedCsrf(request, response, filterChain, authToken);
    }

    private void enforceAuthenticatedCsrf(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String authToken) throws IOException, ServletException {
        if (!hasTrustedRequestMetadata(request)) {
            reject(response, "Untrusted CSRF request metadata");
            return;
        }

        if (!hasValidCsrfToken(request, authToken)) {
            reject(response, "Invalid CSRF token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasValidCsrfToken(HttpServletRequest request, String authToken) {
        String csrfCookie = readCookie(request, CookieAuthConstants.CSRF_COOKIE_NAME);
        String csrfHeader = request.getHeader(CookieAuthConstants.CSRF_HEADER_NAME);
        return StringUtils.hasText(csrfCookie)
                && StringUtils.hasText(csrfHeader)
                && csrfTokenService.constantTimeEquals(csrfCookie, csrfHeader)
                && csrfTokenService.isValid(csrfCookie, authToken);
    }

    private boolean hasTrustedRequestMetadata(HttpServletRequest request) {
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if (StringUtils.hasText(fetchSite)
                && !Set.of("same-origin", "same-site").contains(fetchSite.toLowerCase())) {
            return false;
        }

        String origin = request.getHeader("Origin");
        if (!StringUtils.hasText(origin)) {
            return StringUtils.hasText(fetchSite) && "same-origin".equalsIgnoreCase(fetchSite);
        }

        return origin.equals(requestOrigin(request))
                || allowedOrigins.contains(origin);
    }

    private boolean hasBrowserRequestMetadata(HttpServletRequest request) {
        return StringUtils.hasText(request.getHeader("Origin"))
                || StringUtils.hasText(request.getHeader("Sec-Fetch-Site"));
    }

    private String requestOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }

    private void reject(HttpServletResponse response, String reason) throws IOException {
        logger.warn("Rejected state-changing request: {}", reason);
        SecurityErrorResponseWriter.writeJson(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Invalid CSRF token");
    }

    private String readCookie(HttpServletRequest request, String name) {
        var cookie = WebUtils.getCookie(request, name);
        return cookie == null ? null : cookie.getValue();
    }
}
