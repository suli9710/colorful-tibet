package com.tibet.tourism.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class PiiLegacyCiphertextStartupGuard implements ApplicationRunner {

    private static final String LEGACY_V1_PATTERN = "enc:v1:%";
    private static final List<PiiColumn> PII_COLUMNS = List.of(
            new PiiColumn("users", "phone"),
            new PiiColumn("hotel_bookings", "phone"),
            new PiiColumn("hotel_bookings", "guest_name"),
            new PiiColumn("hotel_bookings", "note"),
            new PiiColumn("orders", "customer_phone"),
            new PiiColumn("orders", "customer_name"),
            new PiiColumn("orders", "customer_note"),
            new PiiColumn("orders", "support_note"),
            new PiiColumn("invoices", "invoice_title"),
            new PiiColumn("invoices", "tax_no"));

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final PiiCryptoConverter converter = new PiiCryptoConverter();

    public PiiLegacyCiphertextStartupGuard(
            JdbcTemplate jdbcTemplate,
            @Value("${app.security.pii-legacy-key-guard-enabled:${PII_LEGACY_KEY_GUARD_ENABLED:true}}")
            boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        List<String> legacyColumns = new ArrayList<>();
        List<PiiColumn> legacyPiiColumns = new ArrayList<>();
        for (PiiColumn piiColumn : PII_COLUMNS) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + piiColumn.table()
                            + " WHERE " + piiColumn.column() + " LIKE ?",
                    Long.class,
                    LEGACY_V1_PATTERN);
            if (count != null && count > 0) {
                legacyColumns.add(piiColumn.table() + "." + piiColumn.column() + "=" + count);
                legacyPiiColumns.add(piiColumn);
            }
        }

        if (legacyColumns.isEmpty()) {
            return;
        }

        if (!PiiCryptoConverter.hasLegacyV1Key()) {
            throw new IllegalStateException("PII_ENCRYPTION_KEY is required while legacy enc:v1 rows remain: "
                    + String.join(", ", legacyColumns));
        }

        List<String> unreadableLegacyColumns = new ArrayList<>();
        for (PiiColumn piiColumn : legacyPiiColumns) {
            String sample = jdbcTemplate.queryForObject(
                    "SELECT " + piiColumn.column() + " FROM " + piiColumn.table()
                            + " WHERE " + piiColumn.column() + " LIKE ? LIMIT 1",
                    String.class,
                    LEGACY_V1_PATTERN);
            if (sample == null) {
                continue;
            }
            PiiCryptoConverter.BackfillValue backfillValue = converter.valueForBackfill(sample);
            if (!backfillValue.encryptable()) {
                unreadableLegacyColumns.add(piiColumn.table() + "." + piiColumn.column()
                        + "(" + backfillValue.failureReason() + ")");
            }
        }

        if (!unreadableLegacyColumns.isEmpty()) {
            throw new IllegalStateException("PII_ENCRYPTION_KEY cannot decrypt legacy enc:v1 rows: "
                    + String.join(", ", unreadableLegacyColumns));
        }
    }

    private record PiiColumn(String table, String column) {
    }
}
