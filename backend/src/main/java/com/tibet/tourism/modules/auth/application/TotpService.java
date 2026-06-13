package com.tibet.tourism.modules.auth.application;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int ACCEPTED_WINDOW_STEPS = 1;
    private static final int MIN_SECRET_BYTES = 16;
    private static final int OTP_MODULO = 1_000_000;

    // Tracks the latest time step already consumed per account so a captured code
    // cannot be replayed within its ~90s validity window.
    private final Map<String, Long> lastConsumedStep = new ConcurrentHashMap<>();

    public void validateSecret(String base32Secret) {
        rejectPlaceholderSecret(base32Secret);
        byte[] decoded = decodeBase32(base32Secret);
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("TOTP secret must contain at least 128 bits of entropy");
        }
    }

    public boolean isValidCode(String base32Secret, String providedCode) {
        return matchStep(base32Secret, providedCode).isPresent();
    }

    /**
     * Validates a code and, on success, atomically marks its time step as consumed for the
     * given account so the same (or an older) step cannot be reused. Returns false when the
     * code is invalid or has already been consumed (replay).
     */
    public boolean consumeCode(String accountKey, String base32Secret, String providedCode) {
        OptionalLong matched = matchStep(base32Secret, providedCode);
        if (matched.isEmpty()) {
            return false;
        }
        long matchedStep = matched.getAsLong();
        boolean[] accepted = {false};
        lastConsumedStep.compute(accountKey, (key, previous) -> {
            if (previous != null && previous >= matchedStep) {
                return previous;
            }
            accepted[0] = true;
            return matchedStep;
        });
        return accepted[0];
    }

    private OptionalLong matchStep(String base32Secret, String providedCode) {
        if (!StringUtils.hasText(providedCode) || !providedCode.matches("\\d{" + DIGITS + "}")) {
            return OptionalLong.empty();
        }

        byte[] secret = decodeBase32(base32Secret);
        long currentStep = Instant.now().getEpochSecond() / PERIOD_SECONDS;
        for (int offset = -ACCEPTED_WINDOW_STEPS; offset <= ACCEPTED_WINDOW_STEPS; offset++) {
            long step = currentStep + offset;
            String expected = generateCode(secret, step);
            if (MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    providedCode.getBytes(StandardCharsets.UTF_8))) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    String generateCodeForTime(String base32Secret, Instant instant) {
        return generateCode(decodeBase32(base32Secret), instant.getEpochSecond() / PERIOD_SECONDS);
    }

    private void rejectPlaceholderSecret(String base32Secret) {
        if (!StringUtils.hasText(base32Secret)) {
            throw new IllegalArgumentException("TOTP secret must be configured");
        }

        String normalized = base32Secret.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("replace-with")
                || normalized.contains("placeholder")
                || normalized.contains("changeme")
                || normalized.contains("change-me")) {
            throw new IllegalArgumentException("TOTP secret must not use a development placeholder");
        }
    }

    private String generateCode(byte[] secret, long timeStep) {
        byte[] counter = new byte[8];
        long value = timeStep;
        for (int i = counter.length - 1; i >= 0; i--) {
            counter[i] = (byte) (value & 0xff);
            value >>= 8;
        }

        byte[] hash = hmacSha1(secret, counter);
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % OTP_MODULO;
        return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
    }

    private byte[] hmacSha1(byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to calculate TOTP code", exception);
        }
    }

    private byte[] decodeBase32(String base32Secret) {
        if (!StringUtils.hasText(base32Secret)) {
            throw new IllegalArgumentException("TOTP secret must be configured");
        }

        String normalized = base32Secret.replace(" ", "")
                .replace("-", "")
                .replace("=", "")
                .toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            int value = BASE32_ALPHABET.indexOf(ch);
            if (value < 0) {
                throw new IllegalArgumentException("TOTP secret must be valid Base32");
            }

            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
                buffer &= (1 << bitsLeft) - 1;
            }
        }

        return output.toByteArray();
    }
}
