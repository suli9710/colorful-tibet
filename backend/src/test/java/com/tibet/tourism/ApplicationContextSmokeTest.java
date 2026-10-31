package com.tibet.tourism;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Boots the full Spring/JPA context (Flyway migrations against MySQL, bean wiring,
 * startup safety validators) so CI catches context-load failures that the mock-based
 * unit tests cannot. Requires a reachable MySQL test database, so it is gated behind the
 * {@code RUN_CONTEXT_TESTS=true} environment variable and only runs in the dedicated CI
 * job (keeping the default {@code mvn test} fast and database-free).
 */
@Import(ApplicationContextSmokeTest.ProbeConfiguration.class)
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.public-docs-enabled=true",
        "app.seed.content.enabled=false",
        "app.seed.demo-users.enabled=false",
        "recommendation.item-similarity.preload-on-startup=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_CONTEXT_TESTS", matches = "true")
class ApplicationContextSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionCount()).isPositive();
        assertThat(applicationContext.getBean(org.springframework.cache.CacheManager.class)).isNotNull();
        assertThat(applicationContext.getBean(com.tibet.tourism.common.config.CacheConfig.class)).isNotNull();
        assertThat(applicationContext.getBeansOfType(org.springframework.cache.interceptor.CacheInterceptor.class)).isNotEmpty();
    }

    @Test
    void readinessProbeReportsUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful())
                .withFailMessage("Unexpected HTTP status %s; body: %s", response.getStatusCode(), response.getBody())
                .isTrue();
        assertThat(response.getBody()).contains("UP");
    }
    @Test
    void httpJsonPreservesDateDecimalAndIgnoredPropertyContract() throws Exception {
        // Exact numeric test-only mapping follows the existing public read allowlist;
        // arbitrary /api/spots/* paths correctly require authentication.
        assertThat(com.tibet.tourism.common.security.ApiSecurityPaths.isPublicRequest("GET", "/api/spots/0")).isTrue();
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/spots/0", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .withFailMessage("Unexpected HTTP status %s; body: %s", response.getStatusCode(), response.getBody())
                .isTrue();
        var json = applicationContext.getBean(ObjectMapper.class).readTree(response.getBody());
        assertThat(json.path("departAt").asText()).isEqualTo("2030-02-03T04:05:06");
        assertThat(json.path("price").decimalValue()).isEqualByComparingTo("123.45");
        assertThat(json.has("secret")).isFalse();
    }

    @Test
    void openApiEndpointGeneratesValidPathsWithUpgradedSpringdoc() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .withFailMessage("Unexpected HTTP status %s; body: %s", response.getStatusCode(), response.getBody())
                .isTrue();
        var json = applicationContext.getBean(ObjectMapper.class).readTree(response.getBody());
        assertThat(json.path("openapi").asText()).startsWith("3.");
        assertThat(json.path("paths").isObject()).isTrue();
        assertThat(json.path("paths").has("/api/hotel-bookings/hotels"))
                .withFailMessage("Hotel list route missing from OpenAPI paths: %s", json.path("paths"))
                .isTrue();
        assertThat(json.path("paths").has("/api/hotel-bookings/room-types/{hotelId}"))
                .withFailMessage("Room type route missing from OpenAPI paths: %s", json.path("paths"))
                .isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean JsonContractProbe jsonContractProbe() { return new JsonContractProbe(); }
    }

    @RestController
    static class JsonContractProbe {
        @GetMapping("/api/spots/0")
        JsonContract contract() {
            return new JsonContract(LocalDateTime.of(2030, 2, 3, 4, 5, 6), new BigDecimal("123.45"), "must-not-leak");
        }
    }

    record JsonContract(LocalDateTime departAt, BigDecimal price, @JsonIgnore String secret) {}

}
