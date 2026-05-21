package com.tibet.tourism.modules.spot.application;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Optional actuator health check for the Scrapling price service.
 */
@Component("scrapling")
@ConditionalOnProperty(name = "scrapling.service.health-enabled", havingValue = "true")
public class ScraplingHealthIndicator implements HealthIndicator {

    private final WebClient.Builder webClientBuilder;
    private final String serviceUrl;
    private final Duration timeout;

    public ScraplingHealthIndicator(
            WebClient.Builder webClientBuilder,
            @Value("${scrapling.service.url:}") String serviceUrl,
            @Value("${scrapling.service.timeout-seconds:30}") int timeoutSeconds) {
        this.webClientBuilder = webClientBuilder;
        this.serviceUrl = serviceUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public Health health() {
        if (!StringUtils.hasText(serviceUrl)) {
            return Health.unknown().withDetail("configured", false).build();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(endpoint("/health"))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .block();

            if (response == null) {
                return Health.down().withDetail("reason", "empty response").build();
            }

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("url", serviceUrl);
            details.put("mode", response.get("mode"));
            details.put("serviceVersion", response.get("serviceVersion"));
            details.put("scraplingVersion", response.get("scraplingVersion"));
            details.put("searchProviders", response.get("searchProviders"));
            details.put("maxSources", response.get("maxSources"));

            Object status = response.get("status");
            if ("ok".equals(status)) {
                return Health.up().withDetails(details).build();
            }

            return Health.down().withDetails(details).withDetail("status", status).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("url", serviceUrl).build();
        }
    }

    private String endpoint(String path) {
        String normalized = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
        return normalized + path;
    }
}
