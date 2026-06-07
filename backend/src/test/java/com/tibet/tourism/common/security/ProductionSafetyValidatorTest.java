package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        ProductionSafetyValidator validator = validator(false, false, false, false, "");

        assertThatCode(validator::validateProductionSafety).doesNotThrowAnyException();
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
        return new ProductionSafetyValidator(
                environment,
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey);
    }
}
