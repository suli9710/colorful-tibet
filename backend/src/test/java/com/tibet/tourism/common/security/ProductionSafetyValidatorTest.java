package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyValidatorTest {

    private static final String PII_KEYS =
            "kid-prod:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String PII_ACTIVE_KID = "kid-prod";
    private static final String CACHE_KEY_HMAC_SECRET =
            "cache-key-hmac-secret-that-is-long-enough-for-prod-2026-abcdefghi";

    @Test
    void prodProfileRejectsInsecureCookies() {
        ProductionSafetyValidator validator = validator(true, false, true, false, "scrapling-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.cookie-secure=true");
    }

    @Test
    void prodProfileRejectsWeakSecretMode() {
        ProductionSafetyValidator validator = validator(true, true, false, false, "scrapling-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.require-strong-secrets=true");
    }

    @Test
    void prodProfileRejectsMockPaymentCallbacks() {
        ProductionSafetyValidator validator = validator(true, true, true, true, "scrapling-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.payments.mock-callback-enabled=false");
    }

    @Test
    void prodProfileRejectsMissingScraplingApiKey() {
        ProductionSafetyValidator validator = validator(true, true, true, false, " ");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scrapling.service.api-key");
    }

    @Test
    void prodProfileRejectsPlaceholderScraplingApiKey() {
        ProductionSafetyValidator validator = validator(
                true,
                true,
                true,
                false,
                "replace-with-at-least-32-random-characters");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scrapling.service.api-key");
    }

    @Test
    void localProfileAllowsMissingScraplingApiKey() {
        ProductionSafetyValidator validator = validator(false, false, false, false, "", false, false, "", "");

        assertThatCode(validator::validateProductionSafety).doesNotThrowAnyException();
    }

    @Test
    void prodProfileRejectsDisabledRecaptcha() {
        ProductionSafetyValidator validator =
                validator(true, true, true, false, "scrapling-key", false, true, "site-key", "secret-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.antibot.recaptcha.enabled=true");
    }

    @Test
    void prodProfileRejectsRegistrationWithoutRecaptchaRequirement() {
        ProductionSafetyValidator validator =
                validator(true, true, true, false, "scrapling-key", true, false, "site-key", "secret-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.registration-recaptcha-required=true");
    }

    @Test
    void prodProfileRejectsPlaceholderRecaptchaKeys() {
        ProductionSafetyValidator validator = validator(
                true,
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "replace-with-recaptcha-site-key",
                "replace-with-recaptcha-secret-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.antibot.recaptcha.site-key");
    }

    @Test
    void prodProfileRejectsBlankRecaptchaSecretKey() {
        ProductionSafetyValidator validator = validator(
                true,
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                " ");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.antibot.recaptcha.secret-key");
    }

    @Test
    void prodProfileRejectsPlaceholderSuperAdminTotpSecret() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                PII_KEYS,
                PII_ACTIVE_KID,
                "replace-with-base32-totp-secret",
                CACHE_KEY_HMAC_SECRET,
                true,
                true,
                true,
                true,
                true,
                true);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.super-admin-totp-secret");
    }

    @Test
    void prodProfileRejectsPlaceholderCacheKeyHmacSecret() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                PII_KEYS,
                PII_ACTIVE_KID,
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                "replace-with-cache-key-hmac-secret",
                true,
                true,
                true,
                true,
                true,
                true);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.cache-key-hmac-secret");
    }

    @Test
    void prodProfileRejectsShortCacheKeyHmacSecret() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                PII_KEYS,
                PII_ACTIVE_KID,
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                "short-cache-key-secret",
                true,
                true,
                true,
                true,
                true,
                true);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production cache key HMAC secret must be at least 64 characters");
    }

    @Test
    void prodProfileRejectsDisabledRateLimitRedis() {
        ProductionSafetyValidator validator = validator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                true,
                false,
                true,
                true,
                true,
                true);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.rate-limit.redis-enabled=true");
    }

    @Test
    void prodProfileRejectsRateLimitRedisFallback() {
        ProductionSafetyValidator validator = validator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                true,
                true,
                false,
                true,
                true,
                true);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.rate-limit.redis-fail-closed=true");
    }

    @Test
    void prodProfileRejectsBruteForceRedisFallback() {
        ProductionSafetyValidator validator = validator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                true,
                true,
                true,
                true,
                true,
                false);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.brute-force.redis-fail-closed=true");
    }

    @Test
    void prodProfileRejectsMissingPiiKeys() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                "",
                PII_ACTIVE_KID,
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                CACHE_KEY_HMAC_SECRET,
                true,
                true,
                true,
                true,
                true,
                true);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.pii-keys");
    }

    @Test
    void prodProfileRejectsPiiActiveKidOutsideConfiguredKeys() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                productionEnvironment(),
                true,
                true,
                false,
                "scrapling-key",
                true,
                true,
                "site-key",
                "secret-key",
                PII_KEYS,
                "kid-missing",
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                CACHE_KEY_HMAC_SECRET,
                true,
                true,
                true,
                true,
                true,
                true);

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.pii-active-kid to match app.security.pii-keys");
    }

    @Test
    void productionEnvironmentVariablesTriggerProductionSafety() {
        for (String variableName : List.of("APP_ENV", "ENVIRONMENT", "RAILWAY_ENVIRONMENT")) {
            MockEnvironment environment = localEnvironment();
            environment.setProperty(variableName, "production");
            ProductionSafetyValidator validator = validator(environment, true, true, false, "");

            assertThatThrownBy(validator::validateProductionSafety)
                    .as(variableName)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("scrapling.service.api-key");
        }
    }

    @Test
    void productionProfileNameTriggersProductionSafety() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        ProductionSafetyValidator validator = validator(environment, true, false, false, "scrapling-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.require-strong-secrets=true");
    }

    @Test
    void dottedProductionEnvironmentPropertiesTriggerProductionSafety() {
        for (String propertyName : List.of("app.env", "railway.environment")) {
            MockEnvironment environment = localEnvironment();
            environment.setProperty(propertyName, "production");
            ProductionSafetyValidator validator = validator(environment, true, true, false, "");

            assertThatThrownBy(validator::validateProductionSafety)
                    .as(propertyName)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("scrapling.service.api-key");
        }
    }

    @Test
    void prodEnvironmentValueTriggersProductionSafety() {
        MockEnvironment environment = localEnvironment();
        environment.setProperty("APP_ENV", " prod ");
        ProductionSafetyValidator validator = validator(environment, true, false, false, "scrapling-key");

        assertThatThrownBy(validator::validateProductionSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.require-strong-secrets=true");
    }

    @Test
    void cloudEnvironmentSignalsTriggerProductionSafety() {
        List<String> cloudSignals = List.of(
                "K_SERVICE",
                "RENDER_SERVICE_ID",
                "FLY_APP_NAME",
                "WEBSITE_HOSTNAME",
                "KUBERNETES_SERVICE_HOST");

        for (String variableName : cloudSignals) {
            MockEnvironment environment = localEnvironment();
            environment.setProperty(variableName, "present");
            ProductionSafetyValidator validator = validator(environment, false, true, false, "scrapling-key");

            assertThatThrownBy(validator::validateProductionSafety)
                    .as(variableName)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("app.security.cookie-secure=true");
        }
    }

    @Test
    void prodProfileAcceptsSafeConfiguration() {
        ProductionSafetyValidator validator = validator(true, true, true, false, "scrapling-key");

        assertThatCode(validator::validateProductionSafety).doesNotThrowAnyException();
    }

    private ProductionSafetyValidator validator(
            boolean prodProfile,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(prodProfile ? "prod" : "local");
        return validator(environment, cookieSecure, requireStrongSecrets, mockCallbackEnabled, scraplingApiKey);
    }

    private ProductionSafetyValidator validator(
            boolean prodProfile,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey,
            boolean recaptchaEnabled,
            boolean registrationRecaptchaRequired,
            String recaptchaSiteKey,
            String recaptchaSecretKey) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(prodProfile ? "prod" : "local");
        return validator(
                environment,
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey,
                recaptchaEnabled,
                registrationRecaptchaRequired,
                recaptchaSiteKey,
                recaptchaSecretKey);
    }

    private MockEnvironment localEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        return environment;
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private ProductionSafetyValidator validator(
            MockEnvironment environment,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey) {
        return new ProductionSafetyValidator(
                environment,
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey,
                true,
                true,
                "site-key",
                "secret-key",
                PII_KEYS,
                PII_ACTIVE_KID,
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                CACHE_KEY_HMAC_SECRET,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    private ProductionSafetyValidator validator(
            MockEnvironment environment,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey,
            boolean recaptchaEnabled,
            boolean registrationRecaptchaRequired,
            String recaptchaSiteKey,
            String recaptchaSecretKey) {
        return new ProductionSafetyValidator(
                environment,
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey,
                recaptchaEnabled,
                registrationRecaptchaRequired,
                recaptchaSiteKey,
                recaptchaSecretKey,
                PII_KEYS,
                PII_ACTIVE_KID,
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                CACHE_KEY_HMAC_SECRET,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    private ProductionSafetyValidator validator(
            MockEnvironment environment,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey,
            boolean recaptchaEnabled,
            boolean registrationRecaptchaRequired,
            String recaptchaSiteKey,
            String recaptchaSecretKey,
            boolean rateLimitEnabled,
            boolean rateLimitRedisEnabled,
            boolean rateLimitRedisFailClosed,
            boolean bruteForceEnabled,
            boolean bruteForceRedisEnabled,
            boolean bruteForceRedisFailClosed) {
        return new ProductionSafetyValidator(
                environment,
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey,
                recaptchaEnabled,
                registrationRecaptchaRequired,
                recaptchaSiteKey,
                recaptchaSecretKey,
                PII_KEYS,
                PII_ACTIVE_KID,
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                CACHE_KEY_HMAC_SECRET,
                rateLimitEnabled,
                rateLimitRedisEnabled,
                rateLimitRedisFailClosed,
                bruteForceEnabled,
                bruteForceRedisEnabled,
                bruteForceRedisFailClosed);
    }
}
