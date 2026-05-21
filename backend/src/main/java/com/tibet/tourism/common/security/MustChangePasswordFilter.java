package com.tibet.tourism.common.security;

import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(MustChangePasswordFilter.class);
    private static final Set<String> ALLOWED_METHOD_PATHS = Set.of(
            "GET /api/auth/me",
            "POST /api/auth/me/change-password",
            "POST /api/auth/logout");

    private final UserRepository userRepository;

    public MustChangePasswordFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = authentication.getName();
        boolean mustChangePassword = userRepository.findByUsername(username)
                .map(user -> Boolean.TRUE.equals(user.getMustChangePassword()))
                .orElse(false);

        if (!mustChangePassword || isAllowedPasswordChangePath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.warn("Blocked request from user requiring password change: username={}, path={}",
                username,
                request.getServletPath());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Password change required\",\"code\":\"PASSWORD_CHANGE_REQUIRED\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null
                || !path.startsWith("/api/")
                || ApiSecurityPaths.isPublicRequest(request.getMethod(), path);
    }

    private boolean isAllowedPasswordChangePath(HttpServletRequest request) {
        return ALLOWED_METHOD_PATHS.contains(request.getMethod() + " " + request.getServletPath());
    }
}
