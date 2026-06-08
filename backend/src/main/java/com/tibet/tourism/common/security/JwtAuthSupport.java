package com.tibet.tourism.common.security;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@Component
public class JwtAuthSupport {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthSupport.class);
    private static final String INVALID_TOKEN_ERROR = "Invalid or expired JWT token";

    private final JwtUtils jwtUtils;
    private final TokenRevocationService tokenRevocationService;
    private final UserSessionVersionService userSessionVersionService;

    public JwtAuthSupport(
            JwtUtils jwtUtils,
            TokenRevocationService tokenRevocationService,
            UserSessionVersionService userSessionVersionService) {
        this.jwtUtils = jwtUtils;
        this.tokenRevocationService = tokenRevocationService;
        this.userSessionVersionService = userSessionVersionService;
    }

    public User resolveCurrentUser(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("Missing JWT token");
        }
        if (!isUsableToken(token)) {
            throw new IllegalStateException(INVALID_TOKEN_ERROR);
        }
        try {
            return userSessionVersionService.resolveCurrentUser(token)
                    .orElseThrow(() -> new IllegalStateException(INVALID_TOKEN_ERROR));
        } catch (RuntimeException exception) {
            logger.warn("Current user session lookup failed: {}",
                    SensitiveLogSanitizer.exceptionSummary(exception));
            throw new IllegalStateException(INVALID_TOKEN_ERROR);
        }
    }

    public Long resolveCurrentUserId(HttpServletRequest request) {
        return resolveCurrentUser(request).getId();
    }

    public Optional<User> resolveOptionalCurrentUser(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        if (!isUsableToken(token)) {
            return Optional.empty();
        }
        try {
            return userSessionVersionService.resolveCurrentUser(token);
        } catch (RuntimeException exception) {
            logger.warn("Optional current user session lookup failed: {}",
                    SensitiveLogSanitizer.exceptionSummary(exception));
            return Optional.empty();
        }
    }

    private boolean isUsableToken(String token) {
        try {
            return jwtUtils.validateJwtToken(token) && !tokenRevocationService.isRevoked(token);
        } catch (RuntimeException exception) {
            logger.warn("JWT usability check failed: {}", SensitiveLogSanitizer.exceptionSummary(exception));
            return false;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        var authCookie = WebUtils.getCookie(request, CookieAuthConstants.AUTH_COOKIE_NAME);
        return authCookie == null ? null : authCookie.getValue();
    }
}
