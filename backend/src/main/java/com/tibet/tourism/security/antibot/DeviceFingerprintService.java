package com.tibet.tourism.security.antibot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceFingerprintService {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintService.class);
    private static final String FP_KEY_PREFIX = "antibot:device-fp:";
    private static final String USER_KEY_PREFIX = "antibot:user-fp:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final AntibotProperties properties;

    public DeviceFingerprintService(ObjectProvider<RedisTemplate<String, Object>> redisProvider,
                                    AntibotProperties properties) {
        this.redisTemplate = redisProvider.getIfAvailable();
        this.properties = properties;
    }

    public int assess(String fingerprint, Long userId) {
        AntibotProperties.Fingerprint cfg = properties.getFingerprint();
        if (!properties.isEnabled() || !cfg.isEnabled()) {
            return 0;
        }
        if (!StringUtils.hasText(fingerprint)) {
            return 30;
        }
        if (redisTemplate == null || userId == null) {
            return 0;
        }

        try {
            String fpKey = FP_KEY_PREFIX + fingerprint;
            String userKey = USER_KEY_PREFIX + userId;
            long ttl = cfg.getTtlSeconds();

            redisTemplate.opsForSet().add(fpKey, userId.toString());
            redisTemplate.expire(fpKey, ttl, TimeUnit.SECONDS);

            redisTemplate.opsForSet().add(userKey, fingerprint);
            redisTemplate.expire(userKey, ttl, TimeUnit.SECONDS);

            Set<Object> usersOnFp = redisTemplate.opsForSet().members(fpKey);
            Set<Object> fpsOnUser = redisTemplate.opsForSet().members(userKey);

            int userCount = usersOnFp != null ? usersOnFp.size() : 0;
            int fpCount = fpsOnUser != null ? fpsOnUser.size() : 0;

            int risk = 0;
            if (userCount > cfg.getMaxUsersPerFingerprint()) {
                risk = Math.max(risk, 80);
                log.info("Fingerprint {} shared by {} users (threshold {})",
                        fingerprint, userCount, cfg.getMaxUsersPerFingerprint());
            }
            if (fpCount > cfg.getMaxFingerprintsPerUser()) {
                risk = Math.max(risk, 70);
                log.info("User {} has {} fingerprints (threshold {})",
                        userId, fpCount, cfg.getMaxFingerprintsPerUser());
            }
            return risk;
        } catch (Exception e) {
            log.warn("Fingerprint assessment failed: {}", e.getMessage());
            return 0;
        }
    }
}
