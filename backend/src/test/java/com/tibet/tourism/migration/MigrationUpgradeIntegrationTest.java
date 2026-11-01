package com.tibet.tourism.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real MySQL upgrade on an explicitly named, pre-created EMPTY test schema only.
 * Never creates/drops databases, cleans a schema, repairs history or contacts an external host.
 * The populated schema is retained for inspection; provision a new empty one for another run.
 */
@EnabledIfEnvironmentVariable(named = "RUN_MIGRATION_UPGRADE_TESTS", matches = "true")
class MigrationUpgradeIntegrationTest {
    @Test
    void main29UpgradesAdditivelyWithoutInventingStockOrProvenance() throws Exception {
        String database = required("MYSQL_UPGRADE_TEST_DATABASE");
        assertTrue(database.matches("(?:test_[a-zA-Z0-9_]+|[a-zA-Z0-9_]+_test)"), "Explicit test-only database name required");
        int port = Integer.parseInt(required("MYSQL_UPGRADE_TEST_PORT"));
        assertTrue(port >= 1024 && port <= 65535, "Explicit local dedicated MySQL port required");
        String url = "jdbc:mysql://127.0.0.1:" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = required("MYSQL_UPGRADE_TEST_USER");
        String password = required("MYSQL_UPGRADE_TEST_PASSWORD");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(0L, scalar(connection, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()"),
                    "Refusing nonempty schema: this test never cleans or repairs an existing database");
            Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration")
                    .cleanDisabled(true).target("29").load().migrate();
            seedLegacyData(connection);
            Map<String, List<List<String>>> before = snapshots(connection);
            List<List<String>> history = rows(connection,
                    "SELECT version,script,checksum,success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank");
            assertEquals(29, history.size());
            Flyway latest = Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration")
                    .cleanDisabled(true).load();
            assertEquals(3, latest.migrate().migrationsExecuted);
            assertEquals(before, snapshots(connection), "Every legacy row/column must be preserved");
            assertEquals(history, rows(connection,
                    "SELECT version,script,checksum,success FROM flyway_schema_history WHERE CAST(version AS UNSIGNED)<=29 ORDER BY installed_rank"),
                    "Already-applied V1-V29 migration history/checksums must not change");
            assertEquals(1L, scalar(connection, "SELECT COUNT(*) FROM room_types WHERE id=900001 AND inventory_units IS NULL"));
            assertEquals(1L, scalar(connection, "SELECT COUNT(*) FROM itineraries WHERE id=900001 AND travelers IS NULL AND generation_rule_version IS NULL"));
            assertEquals(1L, scalar(connection, "SELECT COUNT(*) FROM shared_routes WHERE id=900001 AND travel_status IS NULL AND travel_month IS NULL"));
            SQLException negative = assertThrows(SQLException.class,
                    () -> execute(connection, "UPDATE room_types SET inventory_units=-1 WHERE id=900001"));
            SQLException excessive = assertThrows(SQLException.class,
                    () -> execute(connection, "UPDATE room_types SET inventory_units=10001 WHERE id=900001"));
            assertEquals(3819, negative.getErrorCode(), "Must fail the MySQL CHECK, not SQL syntax or permissions");
            assertEquals(3819, excessive.getErrorCode(), "Must fail the MySQL CHECK, not SQL syntax or permissions");
            assertEquals(1L, scalar(connection, "SELECT COUNT(*) FROM room_types WHERE id=900001 AND inventory_units IS NULL"));
            assertEquals(0, latest.migrate().migrationsExecuted, "Repeated upgrade must be a no-op");
            latest.validate();
            assertEquals(before, snapshots(connection));
        }
    }

    private static void seedLegacyData(Connection c) throws SQLException {
        execute(c, "INSERT INTO users(id,username,password) VALUES(900001,'migration-only-fixture','not-a-login')");
        execute(c, "INSERT INTO hotels(id,name) VALUES(900001,'Legacy hotel')");
        execute(c, "INSERT INTO room_types(id,hotel_id,name,price,capacity) VALUES(900001,900001,'Legacy twin',500,2)");
        execute(c, "INSERT INTO hotel_bookings(id,user_id,hotel_id,room_type_id,room_name,room_price,nights,check_in_date,check_out_date,total_price,status,created_at) VALUES"
                + "(900001,900001,900001,900001,'Legacy twin',500,1,'2030-01-01','2030-01-02',500,'PENDING',NOW()),"
                + "(900002,900001,900001,900001,'Legacy twin',500,1,'2030-01-02','2030-01-03',500,'CONFIRMED',NOW())");
        execute(c, "INSERT INTO orders(id,order_no,user_id,status,payment_status,total_amount,payable_amount,source_type,source_reference_id) VALUES"
                + "(900001,'UPGRADE-LEGACY',900001,'CONFIRMED','PAID',500,500,'LEGACY_HOTEL_BOOKING',900002)");
        execute(c, "INSERT INTO order_items(id,order_id,product_type,product_id,sku_id,product_name,quantity,subtotal,status) VALUES"
                + "(900001,900001,'HOTEL_ROOM',900001,900001,'Legacy hotel',1,500,'CONFIRMED')");
        execute(c, "INSERT INTO inventory_locks(id,order_id,order_item_id,product_type,product_id,sku_id,service_date,quantity,status,active_lock_key) VALUES"
                + "(900001,900001,900001,'HOTEL_ROOM',900001,900001,'2030-01-02',1,'CONFIRMED','HOTEL_ROOM:900001:900001:2030-01-02')");
        execute(c, "INSERT INTO itineraries(id,user_id,title,days,source_content) VALUES(900001,900001,'Legacy itinerary',1,'Keep original plan')");
        execute(c, "INSERT INTO shared_routes(id,author_id,title,content,days,created_at,updated_at) VALUES(900001,900001,'Legacy route','Keep original story',1,NOW(),NOW())");
    }

    private static Map<String, List<List<String>>> snapshots(Connection c) throws SQLException {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        for (String table : List.of("users", "hotels", "room_types", "hotel_bookings", "orders", "order_items", "inventory_locks", "itineraries", "shared_routes")) {
            // Compare all pre-existing columns. Only the explicitly additive new columns are excluded.
            List<String> columns = new ArrayList<>();
            try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, null)) {
                while (r.next()) {
                    String column = r.getString("COLUMN_NAME");
                    if (!List.of("inventory_units", "travelers", "generation_rule_version", "travel_status", "travel_month").contains(column)) columns.add("`" + column + "`");
                }
            }
            assertFalse(columns.isEmpty());
            result.put(table, rows(c, "SELECT " + String.join(",", columns) + " FROM " + table + " ORDER BY id"));
        }
        return result;
    }

    private static List<List<String>> rows(Connection c, String sql) throws SQLException {
        List<List<String>> result = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) row.add(r.getString(i));
                result.add(row);
            }
        }
        return result;
    }
    private static long scalar(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) { assertTrue(r.next()); return r.getLong(1); }
    }
    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.executeUpdate(sql); }
    }
    private static String required(String name) {
        String value = System.getenv(name);
        assertNotNull(value, name + " must be explicitly configured");
        assertFalse(value.isBlank(), name + " must not be blank");
        return value;
    }
}
