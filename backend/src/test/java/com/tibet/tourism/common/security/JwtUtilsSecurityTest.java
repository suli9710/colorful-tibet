package com.tibet.tourism.common.security;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
    void acceptsHexEncodedProductionSecretFromOpensslRand() {
        // `openssl rand -hex 32` is the documented way to mint these. Its 64 hex characters are all in
        // the Base64 alphabet, so the Base64 branch decoded them to 48 bytes and refused the secret
        // outright - despite it carrying 256 bits of entropy - without ever reaching the entropy check.
        String hexSecret = "3f8a1c9d0e7b6452af31c8d95e0b7a24fd6c83915ea27b40c9d15e83a7620fb4";

        JwtUtils jwtUtils = jwtUtils(hexSecret, 86_400_000, true);

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
        assertThat(jwtUtils.isMfaVerified(token)).isFalse();

        String mfaBinding = "a1b2c3d4e5f60718293a4b5c6d7e8f90";
        String mfaToken = jwtUtils.generateJwtToken(authentication, 7L, true, mfaBinding);
        assertThat(jwtUtils.validateJwtToken(mfaToken)).isTrue();
        assertThat(jwtUtils.isMfaVerified(mfaToken)).isTrue();
        assertThat(jwtUtils.getMfaBindingFromJwtToken(mfaToken)).isEqualTo(mfaBinding);
        assertThat(jwtUtils.getMfaBindingFromJwtToken(token)).isEmpty();
    }

    @Test
    void refusesToMintMfaVerifiedTokenWithoutOpaqueCredentialBinding() {
        JwtUtils jwtUtils = jwtUtils("b".repeat(64), 86_400_000, false);
        jwtUtils.validateJwtConfiguration();
        var principal = org.springframework.security.core.userdetails.User
                .withUsername("administrator")
                .password("encoded")
                .roles("ADMIN")
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        assertThatThrownBy(() -> jwtUtils.generateJwtToken(authentication, 1L, true, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opaque credential binding");
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

    @Test
    void invalidJwtLogDoesNotExposeTokenValue() {
        JwtUtils jwtUtils = jwtUtils("d".repeat(64), 86_400_000, false);
        jwtUtils.validateJwtConfiguration();
        String invalidToken = "header.payload.signature-with-secret-value";
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            assertThat(jwtUtils.validateJwtToken(invalidToken)).isFalse();
        } finally {
            detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getFormattedMessage()).contains("messageHash=");
            assertThat(event.getFormattedMessage()).contains("type=");
        });
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains(invalidToken)
                        || message.contains("signature-with-secret-value"));
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

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(JwtUtils.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(JwtUtils.class);
        logger.detachAppender(appender);
        appender.stop();
    }
}
