package com.tibet.tourism.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 将本地 H2 文件库中的历史数据迁移到当前 MySQL 数据库。
 *
 * 使用方式：
 * 1) 确保 spring.datasource 指向 MySQL
 * 2) 设置 app.migration.h2-to-mysql.enabled=true
 * 3) 启动应用，迁移完成后将 app.migration.h2-to-mysql.enabled 改回 false
 */
@Component
@ConditionalOnProperty(prefix = "app.migration.h2-to-mysql", name = "enabled", havingValue = "true")
public class H2ToMySqlMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(H2ToMySqlMigrationRunner.class);

    private final JdbcTemplate mysqlJdbcTemplate;

    @Value("${app.migration.h2-to-mysql.h2-url:jdbc:h2:file:./data/h2/tibet_tourism;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1}")
    private String h2Url;

    @Value("${app.migration.h2-to-mysql.h2-username:sa}")
    private String h2Username;

    @Value("${app.migration.h2-to-mysql.h2-password:}")
    private String h2Password;

    @Value("${app.migration.h2-to-mysql.skip-non-empty-table:true}")
    private boolean skipNonEmptyTable;

    public H2ToMySqlMigrationRunner(JdbcTemplate mysqlJdbcTemplate) {
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("[H2->MySQL] 开始执行数据迁移，H2 URL: {}", h2Url);

        try (Connection h2Connection = DriverManager.getConnection(h2Url, h2Username, h2Password)) {
            List<String> tables = listH2Tables(h2Connection);
            if (tables.isEmpty()) {
                log.warn("[H2->MySQL] H2 数据库未找到可迁移表，流程结束");
                return;
            }

            mysqlJdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
            try {
                int migratedTables = 0;
                int skippedTables = 0;

                for (String table : tables) {
                    try {
                        if (!mysqlTableExists(table)) {
                            log.warn("[H2->MySQL] 目标表不存在，跳过: {}", table);
                            skippedTables++;
                            continue;
                        }

                        long targetCount = countRows(table);
                        if (skipNonEmptyTable && targetCount > 0) {
                            log.info("[H2->MySQL] 目标表已有数据（{} 行），按配置跳过: {}", targetCount, table);
                            skippedTables++;
                            continue;
                        }

                        int inserted = migrateTable(h2Connection, table);
                        log.info("[H2->MySQL] 表迁移完成: {} -> {} 行", table, inserted);
                        migratedTables++;
                    } catch (Exception ex) {
                        skippedTables++;
                        log.error("[H2->MySQL] 表迁移失败，已跳过: {}，原因: {}", table, ex.getMessage(), ex);
                    }
                }

                log.info("[H2->MySQL] 迁移结束：成功表数={}，跳过表数={}，总表数={}", migratedTables, skippedTables, tables.size());
            } finally {
                mysqlJdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        } catch (SQLException e) {
            log.error("[H2->MySQL] 连接 H2 失败，迁移终止: {}", e.getMessage(), e);
        }
    }

    private List<String> listH2Tables(Connection h2Connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME";

        try (PreparedStatement statement = h2Connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (tableName != null && !tableName.isBlank()) {
                    tables.add(tableName);
                }
            }
        }
        return tables;
    }

    private boolean mysqlTableExists(String tableName) {
        String lower = tableName.toLowerCase(Locale.ROOT);
        String upper = tableName.toUpperCase(Locale.ROOT);

        Integer count = mysqlJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (?, ?, ?)",
                Integer.class,
                tableName,
                lower,
                upper
        );
        return count != null && count > 0;
    }

    private int migrateTable(Connection h2Connection, String tableName) throws SQLException {
        String selectSql = "SELECT * FROM \"" + tableName + "\"";

        try (PreparedStatement selectStatement = h2Connection.prepareStatement(selectSql);
             ResultSet resultSet = selectStatement.executeQuery()) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            if (columnCount == 0) {
                return 0;
            }

            String insertSql = buildInsertSql(tableName, metaData, columnCount);
            int inserted = 0;

            while (resultSet.next()) {
                Object[] values = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    values[i - 1] = resultSet.getObject(i);
                }
                mysqlJdbcTemplate.update(insertSql, values);
                inserted++;
            }
            return inserted;
        }
    }

    private String buildInsertSql(String tableName, ResultSetMetaData metaData, int columnCount) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                columns.append(", ");
                placeholders.append(", ");
            }
            String columnName = metaData.getColumnName(i);
            if (columnName == null || columnName.isBlank()) {
                columnName = "col_" + i;
            }
            columns.append('`').append(columnName).append('`');
            placeholders.append('?');
        }

        return "INSERT INTO `" + tableName + "` (" + columns + ") VALUES (" + placeholders + ")";
    }

    private long countRows(String tableName) {
        Long count = mysqlJdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Long.class);
        return count == null ? 0L : count;
    }
}
