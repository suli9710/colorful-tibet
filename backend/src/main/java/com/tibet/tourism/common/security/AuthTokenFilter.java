package com.tibet.tourism.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;
    private final UserSessionVersionService userSessionVersionService;

    public AuthTokenFilter(
            JwtUtils jwtUtils,
            UserDetailsService userDetailsService,
            TokenRevocationService tokenRevocationService,
            UserSessionVersionService userSessionVersionService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.tokenRevocationService = tokenRevocationService;
        this.userSessionVersionService = userSessionVersionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        try {
            String jwt = parseJwt(request);

            if (jwt == null) {
                if (path.startsWith("/api/admin/")) {
                    logger.warn("Admin request without JWT: {}", path);
                }
            } else if (jwtUtils == null) {
                SecurityContextHolder.clearContext();
                logger.error("JwtUtils is null; injection failure");
            } else if (tokenRevocationService != null && tokenRevocationService.isRevoked(jwt)) {
                rejectJwt(path, "revoked");
            } else if (!jwtUtils.validateJwtToken(jwt)) {
                rejectJwt(path, "invalid");
            } else if (userSessionVersionService != null && !userSessionVersionService.tokenMatchesCurrentSession(jwt)) {
                rejectJwt(path, "stale-session");
            } else {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                if (userDetailsService != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    if (path.startsWith("/api/admin/")) {
                        logger.debug("Admin auth OK: user=user#{}, authorities={}",
                                PiiMasker.shortHash(username), userDetails.getAuthorities());
                    }
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            logger.error("Auth filter error for {}: {}", path, SensitiveLogSanitizer.exceptionSummary(e));
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return ApiSecurityPaths.isPublicRequest(request.getMethod(), request.getServletPath());
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        var authCookie = WebUtils.getCookie(request, CookieAuthConstants.AUTH_COOKIE_NAME);
        return authCookie == null ? null : authCookie.getValue();
    }

    private void rejectJwt(String path, String reason) {
        SecurityContextHolder.clearContext();
        logger.warn("Rejected JWT for path: {}, reason={}", path, reason);
    }
}
