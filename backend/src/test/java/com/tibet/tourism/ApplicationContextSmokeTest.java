package com.tibet.tourism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.tibet.tourism.modules.order.application.PaymentTransactionReservationService;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
        "app.security.public-docs-enabled=true",
        "springdoc.api-docs.enabled=true",
        "recommendation.item-similarity.preload-on-startup=false",
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout=3"
})
@EnabledIfEnvironmentVariable(named = "RUN_CONTEXT_TESTS", matches = "true")
class ApplicationContextSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PaymentTransactionReservationService reservationService;

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

    @Test
    void openApiDocumentIsGeneratedByCompatibleSpringdocRuntime() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/v3/api-docs", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"openapi\"").contains("\"paths\"");
    }

    @Test
    void behaviorLogsEnforceUserErasureAtDatabaseBoundary() {
        String deleteRule = jdbcTemplate.queryForObject("""
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = 'behavior_logs'
                  AND constraint_name = 'fk_behavior_logs_user'
                """, String.class);
        Integer constrainedColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.key_column_usage
                WHERE constraint_schema = DATABASE()
                  AND table_name = 'behavior_logs'
                  AND constraint_name = 'fk_behavior_logs_user'
                  AND column_name = 'user_id'
                  AND referenced_table_name = 'users'
                  AND referenced_column_name = 'id'
                """, Integer.class);

        assertThat(deleteRule).isEqualToIgnoringCase("CASCADE");
        assertThat(constrainedColumnCount).isOne();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = "behavior-privacy-" + suffix;
        jdbcTemplate.update("""
                INSERT INTO users (username, password, role, created_at)
                VALUES (?, ?, 'ROLE_USER', CURRENT_TIMESTAMP)
                """, username, "integration-test-password");
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username);

        try {
            jdbcTemplate.update("""
                    INSERT INTO behavior_logs (user_id, endpoint, decision, created_at)
                    VALUES (?, 'integration-test', 'ALLOW', CURRENT_TIMESTAMP)
                    """, userId);
            Long behaviorLogId = jdbcTemplate.queryForObject(
                    "SELECT id FROM behavior_logs WHERE user_id = ?",
                    Long.class,
                    userId);

            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);

            Integer survivingLogCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM behavior_logs WHERE id = ?",
                    Integer.class,
                    behaviorLogId);
            assertThat(survivingLogCount)
                    .as("deleting a user must erase asynchronously persisted behavior identifiers")
                    .isZero();

            assertThatThrownBy(() -> jdbcTemplate.update("""
                    INSERT INTO behavior_logs (user_id, endpoint, decision, created_at)
                    VALUES (?, 'integration-test', 'ALLOW', CURRENT_TIMESTAMP)
                    """, userId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM behavior_logs WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void requiresNewPaymentReservationDoesNotWaitForOuterOrderLock() {
        Integer foreignKeyCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = 'payment_transaction_reservations'
                  AND constraint_name = 'fk_payment_transaction_reservations_order_no'
                  AND constraint_type = 'FOREIGN KEY'
                """, Integer.class);
        assertThat(foreignKeyCount)
                .as("V26 must remove the FK that made the inner reservation transaction wait")
                .isZero();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String orderNo = "LOCK-" + suffix;
        String transactionNo = "TX-" + suffix;
        jdbcTemplate.update("""
                INSERT INTO orders (order_no, user_id, created_at, updated_at)
                VALUES (?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, orderNo);

        try {
            TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);
            assertTimeout(Duration.ofSeconds(8), () -> {
                Boolean reserved = outerTransaction.execute(status -> {
                    Long lockedOrderId = jdbcTemplate.queryForObject(
                            "SELECT id FROM orders WHERE order_no = ? FOR UPDATE",
                            Long.class,
                            orderNo);
                    assertThat(lockedOrderId).isNotNull();

                    return reservationService.reservePaymentTransaction(
                            transactionNo,
                            orderNo,
                            "INTEGRATION_TEST",
                            BigDecimal.ONE,
                            PaymentTransaction.Status.SUCCESS);
                });
                assertThat(reserved).isTrue();
            });

            Integer reservationCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM payment_transaction_reservations WHERE transaction_no = ?",
                    Integer.class,
                    transactionNo);
            assertThat(reservationCount).isOne();
        } finally {
            jdbcTemplate.update(
                    "DELETE FROM payment_transaction_reservations WHERE transaction_no = ?",
                    transactionNo);
            jdbcTemplate.update("DELETE FROM orders WHERE order_no = ?", orderNo);
        }
    }
}
