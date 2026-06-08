package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyValidatorTest {

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
                "secret-key");
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
                recaptchaSecretKey);
    }
}
