package com.tibet.tourism.common.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import java.security.SecureRandom;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThat;
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
    void rejectsPublishedDevelopmentSecretInAllModes() {
        JwtUtils jwtUtils = jwtUtils(
                "dev-only-jwt-secret-change-me-before-any-shared-deployment-2026",
                86_400_000,
                false);

        assertThatThrownBy(jwtUtils::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published development placeholder");
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
        JwtUtils jwtUtils = jwtUtils(strongBase64Secret(), 86_400_000, true);

        assertThatCode(jwtUtils::validateJwtConfiguration).doesNotThrowAnyException();
    }

    @Test
    void rejectsLowEntropyProductionSecret() {
        JwtUtils jwtUtils = jwtUtils("a".repeat(64), 86_400_000, true);

        assertThatThrownBy(jwtUtils::validateJwtConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("high-entropy random material");
    }

    @Test
    void generatedTokenCarriesSessionVersionClaim() {
        JwtUtils jwtUtils = jwtUtils("b".repeat(64), 86_400_000, false);
        jwtUtils.validateJwtConfiguration();
        var principal = org.springframework.security.core.userdetails.User
                .withUsername("traveler")
                .password("encoded")
                .roles("USER")
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = jwtUtils.generateJwtToken(authentication, 7L);

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(jwtUtils.getSessionVersionFromJwtToken(token)).isEqualTo(7L);
    }

    @Test
    void generatedTokenRequiresConfiguredIssuerAndAudience() {
        JwtUtils jwtUtils = jwtUtils("c".repeat(64), 86_400_000, false);
        jwtUtils.validateJwtConfiguration();
        var principal = org.springframework.security.core.userdetails.User
                .withUsername("traveler")
                .password("encoded")
                .roles("USER")
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = jwtUtils.generateJwtToken(authentication, 1L);
        JwtUtils differentAudience = jwtUtils("c".repeat(64), 86_400_000, false);
        ReflectionTestUtils.setField(differentAudience, "jwtAudience", "other-audience");
        differentAudience.validateJwtConfiguration();

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(differentAudience.validateJwtToken(token)).isFalse();
    }

    private JwtUtils jwtUtils(String secret, int expirationMs, boolean requireStrongSecrets, String... activeProfiles) {
        JwtUtils jwtUtils = new JwtUtils();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);

        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
        ReflectionTestUtils.setField(jwtUtils, "jwtIssuer", "colorful-tibet-test");
        ReflectionTestUtils.setField(jwtUtils, "jwtAudience", "colorful-tibet-web");
        ReflectionTestUtils.setField(jwtUtils, "requireStrongSecrets", requireStrongSecrets);
        ReflectionTestUtils.setField(jwtUtils, "environment", environment);
        return jwtUtils;
    }

    private String strongBase64Secret() {
        byte[] bytes = new byte[64];
        new SecureRandom(new byte[]{1, 2, 3, 4}).nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
