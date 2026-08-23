package com.tibet.tourism.common.security;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MetricsScrapeTokenValidator {

    private static final int MIN_PRODUCTION_TOKEN_LENGTH = 64;

    private final Environment environment;
    private final boolean publicMetricsEnabled;
    private final String metricsScrapeToken;

    public MetricsScrapeTokenValidator(
            Environment environment,
            @Value("${app.security.public-metrics-enabled:false}") boolean publicMetricsEnabled,
            @Value("${app.security.metrics-scrape-token:${METRICS_SCRAPE_TOKEN:}}") String metricsScrapeToken) {
        this.environment = environment;
        this.publicMetricsEnabled = publicMetricsEnabled;
        this.metricsScrapeToken = metricsScrapeToken == null ? "" : metricsScrapeToken.trim();
    }

    @PostConstruct
    public void validate() {
        if (!ProductionSafetyValidator.isProductionSafetyRequired(environment) || publicMetricsEnabled) {
            return;
        }
        if (!StringUtils.hasText(metricsScrapeToken)
                || metricsScrapeToken.length() < MIN_PRODUCTION_TOKEN_LENGTH
                || isPlaceholder(metricsScrapeToken)) {
            throw new IllegalStateException(
                    "Production deployment requires a non-placeholder metrics scrape token "
                            + "with at least " + MIN_PRODUCTION_TOKEN_LENGTH + " characters");
        }
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("replace-with")
                || normalized.contains("placeholder")
                || normalized.startsWith("changeme")
                || normalized.startsWith("change-me");
    }
}
