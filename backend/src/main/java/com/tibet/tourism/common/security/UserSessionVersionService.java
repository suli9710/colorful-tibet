package com.tibet.tourism.common.security;

import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserSessionVersionService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public UserSessionVersionService(UserRepository userRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    public boolean tokenMatchesCurrentSession(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String username = jwtUtils.getUserNameFromJwtToken(token);
        long tokenVersion = jwtUtils.getSessionVersionFromJwtToken(token);
        return userRepository.findByUsername(username)
                .map(user -> user.getSessionVersion() == tokenVersion)
                .orElse(false);
    }

    public Optional<User> resolveCurrentUser(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        String username = jwtUtils.getUserNameFromJwtToken(token);
        long tokenVersion = jwtUtils.getSessionVersionFromJwtToken(token);
        return userRepository.findByUsername(username)
                .filter(user -> user.getSessionVersion() == tokenVersion);
    }

    @Transactional
    public void invalidateTokenSubject(String token) {
        if (!StringUtils.hasText(token) || !jwtUtils.validateJwtToken(token)) {
            return;
        }
        String username = jwtUtils.getUserNameFromJwtToken(token);
        userRepository.findByUsername(username).ifPresent(this::invalidateUser);
    }

    @Transactional
    public void invalidateUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        user.incrementSessionVersion();
        userRepository.save(user);
    }

    @Transactional
    public void invalidateUserId(Long userId) {
        if (userId == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(this::invalidateUser);
    }
}
