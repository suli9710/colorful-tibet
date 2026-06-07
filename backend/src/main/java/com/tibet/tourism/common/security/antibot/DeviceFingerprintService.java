package com.tibet.tourism.common.security.antibot;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeviceFingerprintService {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintService.class);
    private static final String FP_KEY_PREFIX = "antibot:device-fp:";
    private static final String USER_KEY_PREFIX = "antibot:user-fp:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int LABEL_HASH_BYTES = 8;

    private final StringRedisTemplate redisTemplate;
    private final AntibotProperties properties;
    private final byte[] hmacKey;

    public DeviceFingerprintService(ObjectProvider<StringRedisTemplate> redisProvider,
                                    AntibotProperties properties,
                                    @Value("${app.security.antibot.fingerprint.hmac-secret:${jwt.secret:}}")
                                    String hmacSecret) {
        this.redisTemplate = redisProvider.getIfAvailable();
        this.properties = properties;
        this.hmacKey = normalizeHmacSecret(hmacSecret).getBytes(StandardCharsets.UTF_8);
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
            String fingerprintLabel = fingerprintLabel(fingerprint);
            String userLabel = userLabel(userId);
            String fpKey = FP_KEY_PREFIX + fingerprintLabel;
            String userKey = USER_KEY_PREFIX + userLabel;
            long ttl = cfg.getTtlSeconds();

            redisTemplate.opsForSet().add(fpKey, userLabel);
            redisTemplate.expire(fpKey, ttl, TimeUnit.SECONDS);

            redisTemplate.opsForSet().add(userKey, fingerprintLabel);
            redisTemplate.expire(userKey, ttl, TimeUnit.SECONDS);

            Set<String> usersOnFp = redisTemplate.opsForSet().members(fpKey);
            Set<String> fpsOnUser = redisTemplate.opsForSet().members(userKey);

            int userCount = usersOnFp != null ? usersOnFp.size() : 0;
            int fpCount = fpsOnUser != null ? fpsOnUser.size() : 0;

            int risk = 0;
            if (userCount > cfg.getMaxUsersPerFingerprint()) {
                risk = Math.max(risk, 80);
                log.info("Fingerprint {} shared by {} users (threshold {})",
                        fingerprintLabel, userCount, cfg.getMaxUsersPerFingerprint());
            }
            if (fpCount > cfg.getMaxFingerprintsPerUser()) {
                risk = Math.max(risk, 70);
                log.info("User {} has {} fingerprints (threshold {})",
                        userLabel, fpCount, cfg.getMaxFingerprintsPerUser());
            }
            return risk;
        } catch (Exception e) {
            log.warn("Fingerprint assessment failed: {}", e.getMessage());
            return 0;
        }
    }

    public String fingerprintLabel(String fingerprint) {
        return hmacLabel("fp", fingerprint);
    }

    public String userLabel(Long userId) {
        return hmacLabel("user", userId == null ? null : userId.toString());
    }

    private String hmacLabel(String namespace, String value) {
        if (!StringUtils.hasText(value)) {
            return namespace + "#empty";
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacKey, HMAC_ALGORITHM));
            byte[] hashed = mac.doFinal((namespace + ":" + value.trim()).getBytes(StandardCharsets.UTF_8));
            return namespace + "#" + HexFormat.of().formatHex(hashed, 0, LABEL_HASH_BYTES);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create fingerprint label", e);
        }
    }

    private String normalizeHmacSecret(String hmacSecret) {
        if (StringUtils.hasText(hmacSecret)) {
            return hmacSecret.trim();
        }
        return "local-antibot-fingerprint-hmac-key";
    }
}
