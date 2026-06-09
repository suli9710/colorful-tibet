package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.env.MockEnvironment;

class PiiLegacyCiphertextStartupGuardTest {

    @AfterEach
    void resetKeys() {
        PiiCryptoConverter.configure("", "", "");
        PiiCryptoConverter.configureMetrics(null);
    }

    @Test
    void failsStartupWhenLegacyRowsExistWithoutLegacyKey() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("enc:v2:%"), eq("enc:v1:%")))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("enc:v1:%")))
                .thenReturn(0L, 2L);

        PiiLegacyCiphertextStartupGuard guard =
                new PiiLegacyCiphertextStartupGuard(jdbcTemplate, prodEnvironment(), true);

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PII_ENCRYPTION_KEY is required")
                .hasMessageContaining("hotel_bookings.phone=2");
    }

    @Test
    void failsStartupWhenPlaintextPiiRowsRemainInProduction() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("enc:v2:%"), eq("enc:v1:%")))
                .thenReturn(0L, 0L, 0L, 0L, 0L, 0L, 3L, 0L, 0L, 2L);

        PiiLegacyCiphertextStartupGuard guard =
                new PiiLegacyCiphertextStartupGuard(jdbcTemplate, prodEnvironment(), true);

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PII v2 backfill is required")
                .hasMessageContaining("orders.customer_note=3")
                .hasMessageContaining("order_audit_logs.note=2");
    }

    @Test
    void rejectsPlaceholderLegacyKeyAtStartupConfiguration() {
        assertThatThrownBy(() -> PiiCryptoConverter.configure("", "", "replace-with-at-least-64-random-characters"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy PII encryption key")
                .hasMessageContaining("placeholder");
    }

    @Test
    void failsStartupWhenLegacyRowsCannotBeDecryptedByConfiguredLegacyKey() throws Exception {
        String legacyCiphertext = encryptLegacyV1("alice@example.test", "real-legacy-key");
        JdbcTemplate jdbcTemplate = legacyRowsForFirstColumn(List.of(new FakeRow(1L, legacyCiphertext)));
        PiiCryptoConverter.configure("", "", "wrong-legacy-key");

        PiiLegacyCiphertextStartupGuard guard =
                new PiiLegacyCiphertextStartupGuard(jdbcTemplate, prodEnvironment(), true);

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot decrypt legacy enc:v1 rows")
                .hasMessageContaining("users.phone");
    }

    @Test
    void scansEveryLegacyRowBeforeAllowingProductionStartup() throws Exception {
        String legacyKey = "real-legacy-key";
        String validLegacyCiphertext = encryptLegacyV1("alice@example.test", legacyKey);
        String unreadableLegacyCiphertext = encryptLegacyV1("mallory@example.test", "other-legacy-key");
        List<FakeRow> legacyRows = new ArrayList<>();
        for (long id = 1; id <= 500; id++) {
            legacyRows.add(new FakeRow(id, validLegacyCiphertext));
        }
        legacyRows.add(new FakeRow(501L, unreadableLegacyCiphertext));
        JdbcTemplate jdbcTemplate = legacyRowsForFirstColumn(legacyRows);
        PiiCryptoConverter.configure("", "", legacyKey);

        PiiLegacyCiphertextStartupGuard guard =
                new PiiLegacyCiphertextStartupGuard(jdbcTemplate, prodEnvironment(), true);

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot decrypt legacy enc:v1 rows")
                .hasMessageContaining("users.phone=1");
    }

    @Test
    void acceptsLegacyRowsWhenConfiguredLegacyKeyCanDecryptSamples() throws Exception {
        String legacyKey = "real-legacy-key";
        String legacyCiphertext = encryptLegacyV1("alice@example.test", legacyKey);
        JdbcTemplate jdbcTemplate = legacyRowsForFirstColumn(List.of(new FakeRow(1L, legacyCiphertext)));
        PiiCryptoConverter.configure("", "", legacyKey);

        new PiiLegacyCiphertextStartupGuard(jdbcTemplate, prodEnvironment(), true).run(null);
    }

    @Test
    void runsWhenProductionIntentComesFromEnvironmentWithoutProdProfile() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("enc:v2:%"), eq("enc:v1:%")))
                .thenReturn(1L);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        environment.setProperty("APP_ENV", "production");

        PiiLegacyCiphertextStartupGuard guard =
                new PiiLegacyCiphertextStartupGuard(jdbcTemplate, environment, true);

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PII v2 backfill is required")
                .hasMessageContaining("users.phone=1");
    }

    @Test
    void skipsGuardOutsideProductionIntent() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        new PiiLegacyCiphertextStartupGuard(jdbcTemplate, environment, true).run(null);
    }

    @Test
    void backfillRunsBeforeLegacyStartupGuard() {
        PiiBackfillRunner backfillRunner = new PiiBackfillRunner(mock(JdbcTemplate.class), false);
        PiiLegacyCiphertextStartupGuard startupGuard =
                new PiiLegacyCiphertextStartupGuard(mock(JdbcTemplate.class), prodEnvironment(), true);

        assertThat(backfillRunner.getOrder()).isLessThan(startupGuard.getOrder());
    }

    private static MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private static String encryptLegacyV1(String value, String rawKey) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        return "enc:v1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    }

    private static JdbcTemplate legacyRowsForFirstColumn(List<FakeRow> legacyRows) {
        return new LegacyRowsJdbcTemplate(legacyRows);
    }

    private static final class LegacyRowsJdbcTemplate extends JdbcTemplate {
        private final List<FakeRow> legacyRows;
        private int unbackfilledCountQueries;
        private int legacyCountQueries;

        private LegacyRowsJdbcTemplate(List<FakeRow> legacyRows) {
            this.legacyRows = legacyRows;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
            if (args.length == 2 && "enc:v2:%".equals(args[0]) && "enc:v1:%".equals(args[1])) {
                unbackfilledCountQueries++;
                return requiredType.cast(0L);
            }

            if (args.length == 1 && "enc:v1:%".equals(args[0])) {
                long count = legacyCountQueries == 0 ? legacyRows.size() : 0L;
                legacyCountQueries++;
                return requiredType.cast(count);
            }

            throw new UnsupportedOperationException("Unexpected queryForObject call: " + sql);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
            long lastSeenId = ((Number) args[0]).longValue();
            int limit = ((Number) args[2]).intValue();
            List<FakeRow> batch = legacyRows.stream()
                    .filter(row -> row.id() > lastSeenId)
                    .sorted(Comparator.comparingLong(FakeRow::id))
                    .limit(limit)
                    .toList();

            List<T> mappedRows = new ArrayList<>();
            try {
                for (int index = 0; index < batch.size(); index++) {
                    FakeRow row = batch.get(index);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getLong("id")).thenReturn(row.id());
                    when(resultSet.getString("phone")).thenReturn(row.value());
                    mappedRows.add(rowMapper.mapRow(resultSet, index));
                }
            } catch (SQLException exception) {
                throw new DataRetrievalFailureException("Failed to map fake legacy PII row", exception);
            }
            return mappedRows;
        }
    }

    private record FakeRow(long id, String value) {
    }
}
