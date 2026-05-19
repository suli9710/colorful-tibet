package com.tibet.tourism.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CsrfTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int NONCE_BYTES = 32;
    private static final int MIN_SECRET_LENGTH = 64;
    private static final byte[] HKDF_SALT = "colorful-tibet-csrf-salt-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_INFO = "csrf-cookie-signing-key".getBytes(StandardCharsets.UTF_8);

    private final byte[] signingKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CsrfTokenService(@Value("${app.security.csrf-signing-secret}") String signingSecret) {
        if (!StringUtils.hasText(signingSecret)) {
            throw new IllegalStateException("CSRF signing secret must be configured");
        }
        String normalizedSecret = signingSecret.trim();
        if (normalizedSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("CSRF signing secret must be at least "
                    + MIN_SECRET_LENGTH + " characters");
        }
        this.signingKey = hkdfSha256(normalizedSecret.getBytes(StandardCharsets.UTF_8), 32);
    }

    public String generateToken(String sessionToken) {
        if (!StringUtils.hasText(sessionToken)) {
            throw new IllegalArgumentException("Session token is required for CSRF token generation");
        }

        byte[] nonceBytes = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonceBytes);
        String nonce = base64Url(nonceBytes);
        String signature = sign(nonce, sessionToken);
        return nonce + "." + signature;
    }

    public boolean isValid(String signedToken, String sessionToken) {
        if (!StringUtils.hasText(signedToken) || !StringUtils.hasText(sessionToken)) {
            return false;
        }

        int separatorIndex = signedToken.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == signedToken.length() - 1) {
            return false;
        }

        String nonce = signedToken.substring(0, separatorIndex);
        String providedSignature = signedToken.substring(separatorIndex + 1);
        String expectedSignature = sign(nonce, sessionToken);
        return constantTimeEquals(providedSignature, expectedSignature);
    }

    public boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String nonce, String sessionToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            mac.update(nonce.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            mac.update(sha256(sessionToken).getBytes(StandardCharsets.UTF_8));
            return base64Url(mac.doFinal());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign CSRF token", e);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return base64Url(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash session token", e);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] hkdfSha256(byte[] inputKeyMaterial, int length) {
        try {
            Mac extractor = Mac.getInstance(HMAC_ALGORITHM);
            extractor.init(new SecretKeySpec(HKDF_SALT, HMAC_ALGORITHM));
            byte[] pseudoRandomKey = extractor.doFinal(inputKeyMaterial);

            Mac expander = Mac.getInstance(HMAC_ALGORITHM);
            expander.init(new SecretKeySpec(pseudoRandomKey, HMAC_ALGORITHM));
            expander.update(HKDF_INFO);
            expander.update((byte) 1);
            byte[] output = expander.doFinal();
            byte[] derived = new byte[length];
            System.arraycopy(output, 0, derived, 0, length);
            return derived;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive CSRF signing key", e);
        }
    }
}
