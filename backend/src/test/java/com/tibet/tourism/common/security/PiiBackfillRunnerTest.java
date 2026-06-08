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
        private final String value;
        private final List<String> updatedValues = new ArrayList<>();

        private CapturingJdbcTemplate(String value) {
            this.value = value;
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
            try {
                ResultSet resultSet = mock(ResultSet.class);
                when(resultSet.getLong("id")).thenReturn(42L);
                when(resultSet.getString(columnFrom(sql))).thenReturn(value);
                return List.of(rowMapper.mapRow(resultSet, 0));
            } catch (SQLException e) {
                throw new DataRetrievalFailureException("Failed to map fake PII backfill row", e);
            }
        }

        @Override
        public int update(String sql, Object... args) throws DataAccessException {
            updatedValues.add((String) args[0]);
            return 1;
        }

        private List<String> updatedValues() {
            return updatedValues;
        }

        private String columnFrom(String sql) {
            int start = SELECT_PREFIX.length();
            int end = sql.indexOf(" FROM ", start);
            return sql.substring(start, end);
        }
    }
}
