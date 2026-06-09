package com.tibet.tourism.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PiiLegacyCiphertextStartupGuard implements ApplicationRunner, Ordered {

    private static final String LEGACY_V1_PATTERN = "enc:v1:%";
    private static final String V2_PATTERN = "enc:v2:%";
    private static final int ORDER = Ordered.LOWEST_PRECEDENCE - 90;
    private static final int BATCH_SIZE = 500;
    private static final List<PiiColumn> PII_COLUMNS = List.of(
            new PiiColumn("users", "phone"),
            new PiiColumn("hotel_bookings", "phone"),
            new PiiColumn("hotel_bookings", "guest_name"),
            new PiiColumn("hotel_bookings", "note"),
            new PiiColumn("orders", "customer_phone"),
            new PiiColumn("orders", "customer_name"),
            new PiiColumn("orders", "customer_note"),
            new PiiColumn("orders", "support_note"),
            new PiiColumn("refunds", "reason"),
            new PiiColumn("order_audit_logs", "note"),
            new PiiColumn("invoices", "invoice_title"),
            new PiiColumn("invoices", "tax_no"));

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final boolean enabled;
    private final PiiCryptoConverter converter = new PiiCryptoConverter();

    public PiiLegacyCiphertextStartupGuard(
            JdbcTemplate jdbcTemplate,
            Environment environment,
            @Value("${app.security.pii-legacy-key-guard-enabled:${PII_LEGACY_KEY_GUARD_ENABLED:true}}")
            boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        this.enabled = enabled;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || !ProductionSafetyValidator.isProductionSafetyRequired(environment)) {
            return;
        }

        List<String> unbackfilledColumns = new ArrayList<>();
        for (PiiColumn piiColumn : PII_COLUMNS) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + piiColumn.table()
                            + " WHERE " + piiColumn.column() + " IS NOT NULL"
                            + " AND " + piiColumn.column() + " <> ''"
                            + " AND " + piiColumn.column() + " NOT LIKE ?"
                            + " AND " + piiColumn.column() + " NOT LIKE ?",
                    Long.class,
                    V2_PATTERN,
                    LEGACY_V1_PATTERN);
            if (count != null && count > 0) {
                unbackfilledColumns.add(piiColumn.table() + "." + piiColumn.column() + "=" + count);
            }
        }

        if (!unbackfilledColumns.isEmpty()) {
            throw new IllegalStateException("PII v2 backfill is required before production startup; "
                    + "plain or unknown-version PII rows remain: " + String.join(", ", unbackfilledColumns));
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
            LegacyScanFailure failure = scanLegacyRows(piiColumn);
            if (failure.unreadableRows() > 0) {
                unreadableLegacyColumns.add(piiColumn.table() + "." + piiColumn.column()
                        + "=" + failure.unreadableRows() + "(" + failure.firstFailureReason() + ")");
            }
        }

        if (!unreadableLegacyColumns.isEmpty()) {
            throw new IllegalStateException("PII_ENCRYPTION_KEY cannot decrypt legacy enc:v1 rows: "
                    + String.join(", ", unreadableLegacyColumns));
        }
    }

    private LegacyScanFailure scanLegacyRows(PiiColumn piiColumn) {
        int unreadableRows = 0;
        String firstFailureReason = null;
        long lastSeenId = 0L;
        while (true) {
            List<RowValue> legacyRows = fetchLegacyBatch(piiColumn, lastSeenId);
            if (legacyRows.isEmpty()) {
                return new LegacyScanFailure(unreadableRows, firstFailureReason);
            }

            for (RowValue legacyRow : legacyRows) {
                lastSeenId = legacyRow.id();
                PiiCryptoConverter.BackfillValue backfillValue = converter.valueForBackfill(legacyRow.value());
                if (!backfillValue.encryptable()) {
                    unreadableRows++;
                    if (firstFailureReason == null) {
                        firstFailureReason = backfillValue.failureReason();
                    }
                }
            }

            if (legacyRows.size() < BATCH_SIZE) {
                return new LegacyScanFailure(unreadableRows, firstFailureReason);
            }
        }
    }

    private List<RowValue> fetchLegacyBatch(PiiColumn piiColumn, long lastSeenId) {
        return jdbcTemplate.query(
                "SELECT id, " + piiColumn.column() + " FROM " + piiColumn.table()
                        + " WHERE id > ?"
                        + " AND " + piiColumn.column() + " LIKE ?"
                        + " ORDER BY id ASC"
                        + " LIMIT ?",
                (rs, rowNum) -> new RowValue(rs.getLong("id"), rs.getString(piiColumn.column())),
                lastSeenId,
                LEGACY_V1_PATTERN,
                BATCH_SIZE);
    }

    private record PiiColumn(String table, String column) {
    }

    private record RowValue(long id, String value) {
    }

    private record LegacyScanFailure(int unreadableRows, String firstFailureReason) {
    }
}
