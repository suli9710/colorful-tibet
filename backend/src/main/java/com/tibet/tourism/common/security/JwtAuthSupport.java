package com.tibet.tourism.common.security;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@Component
public class JwtAuthSupport {

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
        if (!jwtUtils.validateJwtToken(token)) {
            throw new IllegalStateException("Invalid or expired JWT token");
        }
        if (tokenRevocationService.isRevoked(token)) {
            throw new IllegalStateException("Revoked JWT token");
        }
        return userSessionVersionService.resolveCurrentUser(token)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or session is stale"));
    }

    public Long resolveCurrentUserId(HttpServletRequest request) {
        return resolveCurrentUser(request).getId();
    }

    public Optional<User> resolveOptionalCurrentUser(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        if (!jwtUtils.validateJwtToken(token)) {
            return Optional.empty();
        }
        if (tokenRevocationService.isRevoked(token)) {
            return Optional.empty();
        }
        return userSessionVersionService.resolveCurrentUser(token);
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
