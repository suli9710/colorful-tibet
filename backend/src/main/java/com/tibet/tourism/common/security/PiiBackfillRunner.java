package com.tibet.tourism.common.security;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class PiiBackfillRunner implements ApplicationRunner, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(PiiBackfillRunner.class);
    private static final String V2_PREFIX = "enc:v2:%";
    private static final int ORDER = Ordered.LOWEST_PRECEDENCE - 100;
    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final PiiCryptoConverter converter = new PiiCryptoConverter();

    public PiiBackfillRunner(
            JdbcTemplate jdbcTemplate,
            @Value("${app.security.pii-migration-enabled:${PII_MIGRATION_ENABLED:false}}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        int users = migrateColumn("users", "phone");
        int hotelBookingPhones = migrateColumn("hotel_bookings", "phone");
        int hotelBookingGuests = migrateColumn("hotel_bookings", "guest_name");
        int hotelBookingNotes = migrateColumn("hotel_bookings", "note");
        int orderPhones = migrateColumn("orders", "customer_phone");
        int orderNames = migrateColumn("orders", "customer_name");
        int orderCustomerNotes = migrateColumn("orders", "customer_note");
        int orderSupportNotes = migrateColumn("orders", "support_note");
        int refundReasons = migrateColumn("refunds", "reason");
        int orderAuditNotes = migrateColumn("order_audit_logs", "note");
        int invoiceTitles = migrateColumn("invoices", "invoice_title");
        int invoiceTaxNos = migrateColumn("invoices", "tax_no");
        logger.info("PII v2 backfill completed: users={}, hotelBookingPhones={}, hotelBookingGuests={}, "
                        + "hotelBookingNotes={}, orderPhones={}, orderNames={}, orderCustomerNotes={}, "
                        + "orderSupportNotes={}, refundReasons={}, orderAuditNotes={}, "
                        + "invoiceTitles={}, invoiceTaxNos={}",
                users, hotelBookingPhones, hotelBookingGuests, hotelBookingNotes, orderPhones, orderNames,
                orderCustomerNotes, orderSupportNotes, refundReasons, orderAuditNotes, invoiceTitles, invoiceTaxNos);
    }

    private int migrateColumn(String table, String column) {
        int migrated = 0;
        long lastSeenId = 0L;
        while (true) {
            List<RowValue> rows = fetchBackfillBatch(table, column, lastSeenId);
            if (rows.isEmpty()) {
                return migrated;
            }

            for (RowValue row : rows) {
                lastSeenId = row.id();
                if (!StringUtils.hasText(row.value())) {
                    continue;
                }
                PiiCryptoConverter.BackfillValue backfillValue = converter.valueForBackfill(row.value());
                if (!backfillValue.encryptable()) {
                    logger.atWarn()
                            .addKeyValue("security_event", "pii_backfill_skip")
                            .addKeyValue("table", table)
                            .addKeyValue("column", column)
                            .addKeyValue("row_id", row.id())
                            .addKeyValue("version", backfillValue.encryptedVersion())
                            .addKeyValue("reason", backfillValue.failureReason())
                            .log("PII v2 backfill skipped encrypted value because it could not be decrypted");
                    continue;
                }
                String encrypted = converter.convertToDatabaseColumn(backfillValue.plaintext());
                jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?",
                        encrypted, row.id());
                migrated++;
            }

            if (rows.size() < BATCH_SIZE) {
                return migrated;
            }
        }
    }

    private List<RowValue> fetchBackfillBatch(String table, String column, long lastSeenId) {
        String selectSql = "SELECT id, " + column + " FROM " + table
                + " WHERE id > ?"
                + " AND " + column + " IS NOT NULL"
                + " AND " + column + " <> ''"
                + " AND " + column + " NOT LIKE ?"
                + " ORDER BY id ASC"
                + " LIMIT ?";
        return jdbcTemplate.query(
                selectSql,
                (rs, rowNum) -> new RowValue(rs.getLong("id"), rs.getString(column)),
                lastSeenId,
                V2_PREFIX,
                BATCH_SIZE);
    }

    private record RowValue(long id, String value) {
    }
}
