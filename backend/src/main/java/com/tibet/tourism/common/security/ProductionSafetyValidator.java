package com.tibet.tourism.common.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    private final Environment environment;
    private final boolean cookieSecure;
    private final boolean requireStrongSecrets;
    private final boolean mockCallbackEnabled;
    private final String scraplingApiKey;
    private final boolean recaptchaEnabled;
    private final boolean registrationRecaptchaRequired;
    private final String recaptchaSiteKey;
    private final String recaptchaSecretKey;

    public ProductionSafetyValidator(
            Environment environment,
            @Value("${app.security.cookie-secure:true}") boolean cookieSecure,
            @Value("${app.security.require-strong-secrets:false}") boolean requireStrongSecrets,
            @Value("${app.payments.mock-callback-enabled:false}") boolean mockCallbackEnabled,
            @Value("${scrapling.service.api-key:}") String scraplingApiKey,
            @Value("${app.security.antibot.recaptcha.enabled:false}") boolean recaptchaEnabled,
            @Value("${app.security.registration-recaptcha-required:false}") boolean registrationRecaptchaRequired,
            @Value("${app.security.antibot.recaptcha.site-key:}") String recaptchaSiteKey,
            @Value("${app.security.antibot.recaptcha.secret-key:}") String recaptchaSecretKey) {
        this.environment = environment;
        this.cookieSecure = cookieSecure;
        this.requireStrongSecrets = requireStrongSecrets;
        this.mockCallbackEnabled = mockCallbackEnabled;
        this.scraplingApiKey = scraplingApiKey;
        this.recaptchaEnabled = recaptchaEnabled;
        this.registrationRecaptchaRequired = registrationRecaptchaRequired;
        this.recaptchaSiteKey = recaptchaSiteKey;
        this.recaptchaSecretKey = recaptchaSecretKey;
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
                recaptchaSecretKey);
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
            String recaptchaSecretKey) {
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
            if (!StringUtils.hasText(scraplingApiKey)) {
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

    private static boolean isProductionSafetyRequired(Environment environment) {
        return environment != null
                && (isProdProfileActive(environment)
                        || hasProductionEnvironmentValue(environment)
                        || hasCloudEnvironmentSignal(environment));
    }

    private static boolean isProdProfileActive(Environment environment) {
        return environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
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
