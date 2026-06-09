package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PiiBackfillRunnerTest {

    private static final String KEY_32_BYTES_BASE64 =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @AfterEach
    void resetKeys() {
        PiiCryptoConverter.configure("", "", "");
        PiiCryptoConverter.configureMetrics(null);
    }

    @Test
    void skipsLegacyV1CiphertextWhenLegacyKeyIsMissing() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        PiiCryptoConverter.configureMetrics(meterRegistry);
        String legacyCiphertext = encryptLegacyV1("alice@example.test", "legacy-strong-enough-test-key");
        CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate(legacyCiphertext);

        new PiiBackfillRunner(jdbcTemplate, true).run(null);

        assertThat(jdbcTemplate.updatedValues()).isEmpty();
        assertThat(meterRegistry.find("pii_decrypt_failure_total")
                .tag("version", "v1")
                .tag("reason", "missing_key")
                .counter()).isNotNull();
        assertThat(meterRegistry.find("pii_decrypt_failure_total")
                .tag("version", "v1")
                .tag("reason", "missing_key")
                .counter()
                .count()).isGreaterThan(0.0);
    }

    @Test
    void backfillStillEncryptsOrdinaryPlaintext() throws Exception {
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate("13900000000");

        new PiiBackfillRunner(jdbcTemplate, true).run(null);

        PiiCryptoConverter converter = new PiiCryptoConverter();
        assertThat(jdbcTemplate.updatedValues()).isNotEmpty();
        assertThat(jdbcTemplate.updatedValues()).allSatisfy(value -> {
            assertThat(value).startsWith("enc:v2:kid1:");
                assertThat(converter.convertToEntityAttribute(value)).isEqualTo("13900000000");
        });
    }

    @Test
    void backfillContinuesFetchingNextBatchForLargeColumns() {
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        List<FakeRow> rows = new ArrayList<>();
        for (long id = 1; id <= 501; id++) {
            rows.add(new FakeRow(id, "13900000000"));
        }
        CapturingJdbcTemplate jdbcTemplate = CapturingJdbcTemplate.withRows("users.phone", rows);

        new PiiBackfillRunner(jdbcTemplate, true).run(null);

        assertThat(jdbcTemplate.queryCount("users.phone")).isGreaterThanOrEqualTo(2);
        assertThat(jdbcTemplate.updatedValues()).hasSize(501);
        assertThat(jdbcTemplate.updatedIds()).contains(1L, 500L, 501L);
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

    private static final class CapturingJdbcTemplate extends JdbcTemplate {
        private static final String SELECT_PREFIX = "SELECT id, ";
        private final List<FakeRow> defaultRows;
        private final Map<String, List<FakeRow>> rowsByColumn;
        private final Map<String, Integer> queryCounts = new LinkedHashMap<>();
        private final List<String> updatedValues = new ArrayList<>();
        private final List<Long> updatedIds = new ArrayList<>();

        private CapturingJdbcTemplate(String value) {
            this.defaultRows = List.of(new FakeRow(42L, value));
            this.rowsByColumn = Map.of();
        }

        private CapturingJdbcTemplate(Map<String, List<FakeRow>> rowsByColumn) {
            this.defaultRows = List.of();
            this.rowsByColumn = rowsByColumn;
        }

        private static CapturingJdbcTemplate withRows(String tableColumn, List<FakeRow> rows) {
            return new CapturingJdbcTemplate(Map.of(tableColumn, List.copyOf(rows)));
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
            try {
                String column = columnFrom(sql);
                String key = tableFrom(sql) + "." + column;
                queryCounts.merge(key, 1, Integer::sum);
                long lastSeenId = ((Number) args[0]).longValue();
                int limit = ((Number) args[2]).intValue();
                List<FakeRow> sourceRows = rowsByColumn.getOrDefault(key, defaultRows);
                List<T> mappedRows = new ArrayList<>();
                List<FakeRow> batch = sourceRows.stream()
                        .filter(row -> row.id() > lastSeenId)
                        .sorted(Comparator.comparingLong(FakeRow::id))
                        .limit(limit)
                        .toList();
                for (int index = 0; index < batch.size(); index++) {
                    FakeRow row = batch.get(index);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getLong("id")).thenReturn(row.id());
                    when(resultSet.getString(column)).thenReturn(row.value());
                    mappedRows.add(rowMapper.mapRow(resultSet, index));
                }
                return mappedRows;
            } catch (SQLException e) {
                throw new DataRetrievalFailureException("Failed to map fake PII backfill row", e);
            }
        }

        @Override
        public int update(String sql, Object... args) throws DataAccessException {
            updatedValues.add((String) args[0]);
            updatedIds.add(((Number) args[1]).longValue());
            return 1;
        }

        private List<String> updatedValues() {
            return updatedValues;
        }

        private List<Long> updatedIds() {
            return updatedIds;
        }

        private int queryCount(String tableColumn) {
            return queryCounts.getOrDefault(tableColumn, 0);
        }

        private String columnFrom(String sql) {
            int start = SELECT_PREFIX.length();
            int end = sql.indexOf(" FROM ", start);
            return sql.substring(start, end);
        }

        private String tableFrom(String sql) {
            int start = sql.indexOf(" FROM ") + " FROM ".length();
            int end = sql.indexOf(" WHERE ", start);
            return sql.substring(start, end);
        }
    }

    private record FakeRow(long id, String value) {
    }
}
