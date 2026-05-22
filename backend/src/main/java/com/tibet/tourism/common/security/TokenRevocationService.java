package com.tibet.tourism.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TokenRevocationService {

    private static final Logger logger = LoggerFactory.getLogger(TokenRevocationService.class);
    private static final String REDIS_PREFIX = "jwt-revoked:";
    private static final int MAX_IN_MEMORY_REVOKED_TOKENS = 20_000;

    private final StringRedisTemplate redisTemplate;
    private final JwtUtils jwtUtils;
    private final Map<String, Long> revokedTokenExpirations = new ConcurrentHashMap<>();

    @Value("${app.security.jwt-revocation.redis-enabled:true}")
    private boolean redisEnabled;

    public TokenRevocationService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            JwtUtils jwtUtils) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.jwtUtils = jwtUtils;
    }

    public void revoke(String token) {
        if (!StringUtils.hasText(token) || !jwtUtils.validateJwtToken(token)) {
            return;
        }

        Date expiration = jwtUtils.getExpirationDateFromJwtToken(token);
        long expiresAt = expiration == null ? System.currentTimeMillis() : expiration.getTime();
        long ttlMillis = expiresAt - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }

        String tokenHash = hashToken(token);
        revokedTokenExpirations.put(tokenHash, expiresAt);
        evictExpiredInMemory();

        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(
                        REDIS_PREFIX + tokenHash,
                        "revoked",
                        ttlMillis,
                        TimeUnit.MILLISECONDS);
            } catch (Exception exception) {
                logger.debug("Redis unavailable for JWT revocation save: {}", exception.getMessage());
            }
        }
    }

    public boolean isRevoked(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String tokenHash = hashToken(token);
        Long localExpiresAt = revokedTokenExpirations.get(tokenHash);
        if (localExpiresAt != null) {
            if (localExpiresAt > System.currentTimeMillis()) {
                return true;
            }
            revokedTokenExpirations.remove(tokenHash, localExpiresAt);
        }

        if (redisEnabled && redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(REDIS_PREFIX + tokenHash);
                if (value != null) {
                    Long ttlSeconds = redisTemplate.getExpire(REDIS_PREFIX + tokenHash, TimeUnit.SECONDS);
                    if (ttlSeconds != null && ttlSeconds > 0) {
                        revokedTokenExpirations.put(
                                tokenHash,
                                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds));
                    }
                    return true;
                }
            } catch (Exception exception) {
                logger.debug("Redis unavailable for JWT revocation check; using in-memory state: {}",
                        exception.getMessage());
            }
        }

        return false;
    }

    private void evictExpiredInMemory() {
        if (revokedTokenExpirations.size() < MAX_IN_MEMORY_REVOKED_TOKENS) {
            return;
        }
        long now = System.currentTimeMillis();
        revokedTokenExpirations.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash JWT token", exception);
        }
    }
}
