package com.tibet.tourism.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BehaviorLogMigrationContractTest {

    @Test
    void separatesLegacyDataCleanupFromForeignKeySchemaChange() throws IOException {
        String cleanup = migration("V28__remove_orphaned_behavior_logs.sql");
        String schemaChange = migration("V29__cascade_behavior_logs_on_user_delete.sql");

        assertThat(cleanup)
                .contains("DELETE BEHAVIOR_LOG")
                .contains("LEFT JOIN USERS")
                .contains("USER_ACCOUNT.ID IS NULL")
                .doesNotContain("ALTER TABLE");
        assertThat(schemaChange)
                .contains("ALTER TABLE BEHAVIOR_LOGS")
                .contains("FOREIGN KEY (USER_ID) REFERENCES USERS (ID)")
                .contains("ON DELETE CASCADE")
                .doesNotContain("DELETE FROM", "DELETE BEHAVIOR_LOG");
    }

    private String migration(String filename) throws IOException {
        return new ClassPathResource("db/migration/" + filename)
                .getContentAsString(StandardCharsets.UTF_8)
                .toUpperCase(Locale.ROOT);
    }
}
