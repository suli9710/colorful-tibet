package com.tibet.tourism.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsSecurityTest {

    @Test
    void rejectsMissingSecret() {
        JwtUtils jwtUtils = jwtUtils("", 86_400_000, false);

        assertThatThrownBy(jwtUtils::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret must be configured");
    }

    @Test
    void rejectsShortSecretInAllModes() {
        JwtUtils jwtUtils = jwtUtils("short-secret", 86_400_000, false);

        assertThatThrownBy(jwtUtils::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 64 characters");
    }

    @Test
    void rejectsDevelopmentSecretInStrictMode() {
        JwtUtils jwtUtils = jwtUtils(
                "dev-only-jwt-secret-change-me-before-any-shared-deployment-2026-extra",
                86_400_000,
                true);

        assertThatThrownBy(jwtUtils::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development placeholder");
    }

    @Test
    void rejectsDevelopmentSecretWhenProdProfileIsActive() {
        JwtUtils jwtUtils = jwtUtils(
                "dev-only-jwt-secret-change-me-before-any-shared-deployment-2026-extra",
                86_400_000,
                false,
                "prod");

        assertThatThrownBy(jwtUtils::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development placeholder");
    }

    @Test
    void rejectsExpirationOutsideSafeBounds() {
        JwtUtils jwtUtils = jwtUtils("secure-test-jwt-secret-with-more-than-sixty-four-characters-1234567890", 60_000, false);

        assertThatThrownBy(jwtUtils::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("between 5 minutes and 7 days");
    }

    @Test
    void acceptsStrongProductionSecret() {
        JwtUtils jwtUtils = jwtUtils("a".repeat(64), 86_400_000, true);

        assertThatCode(jwtUtils::validateJwtConfiguration).doesNotThrowAnyException();
    }

    private JwtUtils jwtUtils(String secret, int expirationMs, boolean requireStrongSecrets, String... activeProfiles) {
        JwtUtils jwtUtils = new JwtUtils();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);

        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
        ReflectionTestUtils.setField(jwtUtils, "requireStrongSecrets", requireStrongSecrets);
        ReflectionTestUtils.setField(jwtUtils, "environment", environment);
        return jwtUtils;
    }
}
