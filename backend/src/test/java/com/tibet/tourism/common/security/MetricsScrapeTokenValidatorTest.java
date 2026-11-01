package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class MetricsScrapeTokenValidatorTest {

    @Test
    void productionRejectsMissingWeakAndPlaceholderTokensWhenMetricsArePrivate() {
        MockEnvironment production = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        production.setActiveProfiles("prod");

        assertThatThrownBy(() -> new MetricsScrapeTokenValidator(production, false, "").validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new MetricsScrapeTokenValidator(production, false, "short").validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new MetricsScrapeTokenValidator(
                production,
                false,
                "replace-with-random-metrics-scrape-token-" + "x".repeat(64)).validate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productionAcceptsStrongDedicatedTokenAndNonProductionRemainsOptional() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        MockEnvironment local = new MockEnvironment();

        assertThatCode(() -> new MetricsScrapeTokenValidator(
                production,
                false,
                "m".repeat(64)).validate()).doesNotThrowAnyException();
        assertThatCode(() -> new MetricsScrapeTokenValidator(local, false, "").validate())
                .doesNotThrowAnyException();
    }
}
