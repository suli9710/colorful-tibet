package com.tibet.tourism.common.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrfTokenServiceTest {

    private static final String SECRET = "test-csrf-secret-that-is-long-enough-for-hmac-signing-2026-abcdef-abcdef";
    private static final String SESSION_TOKEN = "session.jwt.value";

    private final CsrfTokenService csrfTokenService = new CsrfTokenService(SECRET);

    @Test
    void generatedTokenValidatesAgainstOriginalSessionToken() {
        String token = csrfTokenService.generateToken(SESSION_TOKEN);

        assertTrue(csrfTokenService.isValid(token, SESSION_TOKEN));
    }

    @Test
    void generatedTokensUseDifferentNonces() {
        String first = csrfTokenService.generateToken(SESSION_TOKEN);
        String second = csrfTokenService.generateToken(SESSION_TOKEN);

        assertNotEquals(first, second);
        assertTrue(csrfTokenService.isValid(first, SESSION_TOKEN));
        assertTrue(csrfTokenService.isValid(second, SESSION_TOKEN));
    }

    @Test
    void tokenDoesNotValidateAgainstDifferentSessionToken() {
        String token = csrfTokenService.generateToken(SESSION_TOKEN);

        assertFalse(csrfTokenService.isValid(token, "another.session.token"));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = csrfTokenService.generateToken(SESSION_TOKEN);

        assertFalse(csrfTokenService.isValid(token + "x", SESSION_TOKEN));
    }

    @Test
    void publishedDevelopmentPlaceholderIsRejected() {
        assertThrows(IllegalStateException.class, () -> new CsrfTokenService(
                "dev-only-csrf-signing-secret-change-me-before-shared-deployment-2026-05-colorful-tibet"));
    }

    @Test
    void copiedExamplePlaceholderIsRejected() {
        assertThrows(IllegalStateException.class, () -> new CsrfTokenService(
                "replace-with-different-at-least-64-random-characters-for-colorful-tibet"));
    }
}
