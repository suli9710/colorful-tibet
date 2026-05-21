package com.tibet.tourism.common.security;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@Component
public class JwtAuthSupport {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthSupport(
            JwtUtils jwtUtils,
            UserRepository userRepository,
            TokenRevocationService tokenRevocationService) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.tokenRevocationService = tokenRevocationService;
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
        String username = jwtUtils.getUserNameFromJwtToken(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public Long resolveCurrentUserId(HttpServletRequest request) {
        return resolveCurrentUser(request).getId();
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
