package com.tibet.tourism;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots the full Spring/JPA context (Flyway migrations against MySQL, bean wiring,
 * startup safety validators) so CI catches context-load failures that the mock-based
 * unit tests cannot. Requires a reachable MySQL test database, so it is gated behind the
 * {@code RUN_CONTEXT_TESTS=true} environment variable and only runs in the dedicated CI
 * job (keeping the default {@code mvn test} fast and database-free).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
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
    }

    @Test
    void readinessProbeReportsUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("UP");
    }
}
