package com.tibet.tourism.common.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionSafetyValidator {

    private static final List<String> PRODUCTION_ENVIRONMENT_VARIABLES =
            List.of("APP_ENV", "ENVIRONMENT", "RAILWAY_ENVIRONMENT");
    private static final List<String> CLOUD_ENVIRONMENT_SIGNALS = List.of(
            "K_SERVICE",
            "RENDER_SERVICE_ID",
            "FLY_APP_NAME",
            "WEBSITE_HOSTNAME",
            "KUBERNETES_SERVICE_HOST");
    private static final int MIN_CACHE_KEY_HMAC_SECRET_LENGTH = 64;

    private final Environment environment;
    private final boolean cookieSecure;
    private final boolean requireStrongSecrets;
    private final boolean mockCallbackEnabled;
    private final String scraplingApiKey;
    private final boolean recaptchaEnabled;
    private final boolean registrationRecaptchaRequired;
    private final String recaptchaSiteKey;
    private final String recaptchaSecretKey;
    private final String piiKeys;
    private final String piiActiveKid;
    private final String superAdminTotpSecret;
    private final String cacheKeyHmacSecret;
    private final boolean rateLimitEnabled;
    private final boolean rateLimitRedisEnabled;
    private final boolean rateLimitRedisFailClosed;
    private final boolean bruteForceEnabled;
    private final boolean bruteForceRedisEnabled;
    private final boolean bruteForceRedisFailClosed;
    private final boolean totpReplayFailClosed;

    @Autowired
    public ProductionSafetyValidator(
            Environment environment,
            @Value("${app.security.cookie-secure:true}") boolean cookieSecure,
            @Value("${app.security.require-strong-secrets:false}") boolean requireStrongSecrets,
            @Value("${app.payments.mock-callback-enabled:false}") boolean mockCallbackEnabled,
            @Value("${scrapling.service.api-key:}") String scraplingApiKey,
            @Value("${app.security.antibot.recaptcha.enabled:false}") boolean recaptchaEnabled,
            @Value("${app.security.registration-recaptcha-required:false}") boolean registrationRecaptchaRequired,
            @Value("${app.security.antibot.recaptcha.site-key:}") String recaptchaSiteKey,
            @Value("${app.security.antibot.recaptcha.secret-key:}") String recaptchaSecretKey,
            @Value("${app.security.pii-keys:${PII_KEYS:}}") String piiKeys,
            @Value("${app.security.pii-active-kid:${PII_ACTIVE_KID:}}") String piiActiveKid,
            @Value("${app.security.super-admin-totp-secret:}") String superAdminTotpSecret,
            @Value("${app.security.cache-key-hmac-secret:${CACHE_KEY_HMAC_SECRET:}}") String cacheKeyHmacSecret,
            @Value("${app.security.rate-limit.enabled:true}") boolean rateLimitEnabled,
            @Value("${app.security.rate-limit.redis-enabled:true}") boolean rateLimitRedisEnabled,
            @Value("${app.security.rate-limit.redis-fail-closed:false}") boolean rateLimitRedisFailClosed,
            @Value("${app.security.brute-force.enabled:true}") boolean bruteForceEnabled,
            @Value("${app.security.brute-force.redis-enabled:true}") boolean bruteForceRedisEnabled,
            @Value("${app.security.brute-force.redis-fail-closed:false}") boolean bruteForceRedisFailClosed,
            @Value("${app.security.totp-replay-fail-closed:false}") boolean totpReplayFailClosed) {
        this.environment = environment;
        this.cookieSecure = cookieSecure;
        this.requireStrongSecrets = requireStrongSecrets;
        this.mockCallbackEnabled = mockCallbackEnabled;
        this.scraplingApiKey = scraplingApiKey;
        this.recaptchaEnabled = recaptchaEnabled;
        this.registrationRecaptchaRequired = registrationRecaptchaRequired;
        this.recaptchaSiteKey = recaptchaSiteKey;
        this.recaptchaSecretKey = recaptchaSecretKey;
        this.piiKeys = piiKeys;
        this.piiActiveKid = piiActiveKid;
        this.superAdminTotpSecret = superAdminTotpSecret;
        this.cacheKeyHmacSecret = cacheKeyHmacSecret;
        this.rateLimitEnabled = rateLimitEnabled;
        this.rateLimitRedisEnabled = rateLimitRedisEnabled;
        this.rateLimitRedisFailClosed = rateLimitRedisFailClosed;
        this.bruteForceEnabled = bruteForceEnabled;
        this.bruteForceRedisEnabled = bruteForceRedisEnabled;
        this.bruteForceRedisFailClosed = bruteForceRedisFailClosed;
        this.totpReplayFailClosed = totpReplayFailClosed;
    }

    /** Retains source compatibility for existing direct unit-test construction. */
    ProductionSafetyValidator(
            Environment environment,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey,
            boolean recaptchaEnabled,
            boolean registrationRecaptchaRequired,
            String recaptchaSiteKey,
            String recaptchaSecretKey,
            String piiKeys,
            String piiActiveKid,
            String superAdminTotpSecret,
            String cacheKeyHmacSecret,
            boolean rateLimitEnabled,
            boolean rateLimitRedisEnabled,
            boolean rateLimitRedisFailClosed,
            boolean bruteForceEnabled,
            boolean bruteForceRedisEnabled,
            boolean bruteForceRedisFailClosed) {
        this(
                environment,
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey,
                recaptchaEnabled,
                registrationRecaptchaRequired,
                recaptchaSiteKey,
                recaptchaSecretKey,
                piiKeys,
                piiActiveKid,
                superAdminTotpSecret,
                cacheKeyHmacSecret,
                rateLimitEnabled,
                rateLimitRedisEnabled,
                rateLimitRedisFailClosed,
                bruteForceEnabled,
                bruteForceRedisEnabled,
                bruteForceRedisFailClosed,
                true);
    }

    @PostConstruct
    public void validateProductionSafety() {
        validate(
                isProductionSafetyRequired(environment),
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey,
                recaptchaEnabled,
                registrationRecaptchaRequired,
                recaptchaSiteKey,
                recaptchaSecretKey,
                piiKeys,
                piiActiveKid,
                superAdminTotpSecret,
                cacheKeyHmacSecret,
                rateLimitEnabled,
                rateLimitRedisEnabled,
                rateLimitRedisFailClosed,
                bruteForceEnabled,
                bruteForceRedisEnabled,
                bruteForceRedisFailClosed,
                totpReplayFailClosed);
    }

    static void validate(
            boolean productionSafetyRequired,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey,
            boolean recaptchaEnabled,
            boolean registrationRecaptchaRequired,
            String recaptchaSiteKey,
            String recaptchaSecretKey,
            String piiKeys,
            String piiActiveKid,
            String superAdminTotpSecret,
            String cacheKeyHmacSecret,
            boolean rateLimitEnabled,
            boolean rateLimitRedisEnabled,
            boolean rateLimitRedisFailClosed,
            boolean bruteForceEnabled,
            boolean bruteForceRedisEnabled,
            boolean bruteForceRedisFailClosed,
            boolean totpReplayFailClosed) {
        if (productionSafetyRequired) {
            if (!cookieSecure) {
                throw new IllegalStateException("Production deployment requires app.security.cookie-secure=true");
            }
            if (!requireStrongSecrets) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.require-strong-secrets=true");
            }
            if (mockCallbackEnabled) {
                throw new IllegalStateException(
                        "Production deployment requires app.payments.mock-callback-enabled=false");
            }
            if (isBlankOrPlaceholder(scraplingApiKey)) {
                throw new IllegalStateException("Production deployment requires scrapling.service.api-key");
            }
            if (!recaptchaEnabled) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.antibot.recaptcha.enabled=true");
            }
            if (!registrationRecaptchaRequired) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.registration-recaptcha-required=true");
            }
            if (isBlankOrPlaceholder(recaptchaSiteKey)) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.antibot.recaptcha.site-key");
            }
            if (isBlankOrPlaceholder(recaptchaSecretKey)) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.antibot.recaptcha.secret-key");
            }
            if (isBlankOrPlaceholder(piiKeys)) {
                throw new IllegalStateException("Production deployment requires app.security.pii-keys");
            }
            if (isBlankOrPlaceholder(piiActiveKid)) {
                throw new IllegalStateException("Production deployment requires app.security.pii-active-kid");
            }
            if (!piiKeysContainActiveKid(piiKeys, piiActiveKid)) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.pii-active-kid to match app.security.pii-keys");
            }
            if (isBlankOrPlaceholder(superAdminTotpSecret)) {
                throw new IllegalStateException("Production deployment requires app.security.super-admin-totp-secret");
            }
            if (isBlankOrPlaceholder(cacheKeyHmacSecret)) {
                throw new IllegalStateException("Production deployment requires app.security.cache-key-hmac-secret");
            }
            if (cacheKeyHmacSecret.trim().length() < MIN_CACHE_KEY_HMAC_SECRET_LENGTH) {
                throw new IllegalStateException("Production cache key HMAC secret must be at least "
                        + MIN_CACHE_KEY_HMAC_SECRET_LENGTH + " characters");
            }
            if (!rateLimitEnabled) {
                throw new IllegalStateException("Production deployment requires app.security.rate-limit.enabled=true");
            }
            if (!rateLimitRedisEnabled) {
                throw new IllegalStateException("Production deployment requires app.security.rate-limit.redis-enabled=true");
            }
            if (!rateLimitRedisFailClosed) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.rate-limit.redis-fail-closed=true");
            }
            if (!bruteForceEnabled) {
                throw new IllegalStateException("Production deployment requires app.security.brute-force.enabled=true");
            }
            if (!bruteForceRedisEnabled) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.brute-force.redis-enabled=true");
            }
            if (!bruteForceRedisFailClosed) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.brute-force.redis-fail-closed=true");
            }
            if (!totpReplayFailClosed) {
                throw new IllegalStateException(
                        "Production deployment requires app.security.totp-replay-fail-closed=true");
            }
        }
    }

    private static boolean isBlankOrPlaceholder(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return normalizedValue.contains("replace-with")
                || normalizedValue.contains("placeholder")
                || normalizedValue.startsWith("changeme")
                || normalizedValue.startsWith("change-me");
    }

    private static boolean piiKeysContainActiveKid(String piiKeys, String piiActiveKid) {
        String normalizedActiveKid = piiActiveKid == null ? "" : piiActiveKid.trim();
        if (!StringUtils.hasText(normalizedActiveKid)) {
            return false;
        }
        return Arrays.stream(piiKeys.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(entry -> entry.indexOf(':') > 0 ? entry.substring(0, entry.indexOf(':')).trim() : "")
                .anyMatch(normalizedActiveKid::equals);
    }

    public static boolean isProductionSafetyRequired(Environment environment) {
        return environment != null
                && (isProdProfileActive(environment)
                        || hasProductionEnvironmentValue(environment)
                        || hasCloudEnvironmentSignal(environment));
    }

    private static boolean isProdProfileActive(Environment environment) {
        return environment != null
                && Arrays.stream(environment.getActiveProfiles())
                        .anyMatch(ProductionSafetyValidator::isProductionValue);
    }

    private static boolean hasProductionEnvironmentValue(Environment environment) {
        return PRODUCTION_ENVIRONMENT_VARIABLES.stream()
                .map(variableName -> getEnvironmentProperty(environment, variableName))
                .anyMatch(ProductionSafetyValidator::isProductionValue);
    }

    private static boolean hasCloudEnvironmentSignal(Environment environment) {
        return CLOUD_ENVIRONMENT_SIGNALS.stream()
                .map(variableName -> getEnvironmentProperty(environment, variableName))
                .anyMatch(StringUtils::hasText);
    }

    private static boolean isProductionValue(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalizedValue = value.trim();
        return "production".equalsIgnoreCase(normalizedValue) || "prod".equalsIgnoreCase(normalizedValue);
    }

    private static String getEnvironmentProperty(Environment environment, String variableName) {
        String value = environment.getProperty(variableName);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return environment.getProperty(variableName.toLowerCase(Locale.ROOT).replace('_', '.'));
    }
}
