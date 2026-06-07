package com.tibet.tourism.common.security;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class PiiBackfillRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PiiBackfillRunner.class);
    private static final String V2_PREFIX = "enc:v2:%";

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
        int invoiceTitles = migrateColumn("invoices", "invoice_title");
        int invoiceTaxNos = migrateColumn("invoices", "tax_no");
        logger.info("PII v2 backfill completed: users={}, hotelBookingPhones={}, hotelBookingGuests={}, "
                        + "hotelBookingNotes={}, orderPhones={}, orderNames={}, orderCustomerNotes={}, "
                        + "orderSupportNotes={}, invoiceTitles={}, invoiceTaxNos={}",
                users, hotelBookingPhones, hotelBookingGuests, hotelBookingNotes, orderPhones, orderNames,
                orderCustomerNotes, orderSupportNotes, invoiceTitles, invoiceTaxNos);
    }

    private int migrateColumn(String table, String column) {
        String selectSql = "SELECT id, " + column + " FROM " + table
                + " WHERE " + column + " IS NOT NULL"
                + " AND " + column + " <> ''"
                + " AND " + column + " NOT LIKE ?";
        List<RowValue> rows = jdbcTemplate.query(
                selectSql,
                (rs, rowNum) -> new RowValue(rs.getLong("id"), rs.getString(column)),
                V2_PREFIX);

        int migrated = 0;
        for (RowValue row : rows) {
            if (!StringUtils.hasText(row.value())) {
                continue;
            }
            String plaintext = converter.convertToEntityAttribute(row.value());
            String encrypted = converter.convertToDatabaseColumn(plaintext);
            jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?",
                    encrypted, row.id());
            migrated++;
        }
        return migrated;
    }

    private record RowValue(long id, String value) {
    }
}
