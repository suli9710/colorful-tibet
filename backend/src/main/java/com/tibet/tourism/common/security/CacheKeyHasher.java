package com.tibet.tourism.common.security;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CacheKeyHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int LABEL_HASH_BYTES = 16;

    private final byte[] hmacKey;

    public CacheKeyHasher(@Value("${app.security.cache-key-hmac-secret:${CACHE_KEY_HMAC_SECRET:}}")
                          String hmacSecret) {
        this.hmacKey = normalizeHmacSecret(hmacSecret).getBytes(StandardCharsets.UTF_8);
    }

    public String cacheKey(String namespace, Object value) {
        String safeNamespace = StringUtils.hasText(namespace) ? namespace.trim() : "cache";
        if (value == null || !StringUtils.hasText(value.toString())) {
            return safeNamespace + "#empty";
        }

        String normalizedValue = value.toString().trim();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacKey, HMAC_ALGORITHM));
            byte[] hashed = mac.doFinal((safeNamespace + ":" + normalizedValue).getBytes(StandardCharsets.UTF_8));
            return safeNamespace + "#" + HexFormat.of().formatHex(hashed, 0, LABEL_HASH_BYTES);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create cache key label", ex);
        }
    }

    private String normalizeHmacSecret(String hmacSecret) {
        if (StringUtils.hasText(hmacSecret)) {
            return hmacSecret.trim();
        }
        return "local-cache-key-hmac-secret";
    }
}
