package com.tibet.tourism.security;

import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtAuthSupport {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public JwtAuthSupport(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    public User resolveCurrentUser(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("Missing JWT token");
        }
        if (!jwtUtils.validateJwtToken(token)) {
            throw new IllegalStateException("Invalid or expired JWT token");
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
        return null;
    }
}
