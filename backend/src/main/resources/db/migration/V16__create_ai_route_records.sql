CREATE TABLE IF NOT EXISTS ai_route_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    job_id VARCHAR(72) NULL,
    title VARCHAR(200) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    days INT NOT NULL,
    budget VARCHAR(32) NULL,
    preference VARCHAR(64) NULL,
    locale VARCHAR(16) NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'RUNNING',
    manually_saved BOOLEAN NOT NULL DEFAULT FALSE,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_route_records_user FOREIGN KEY (user_id) REFERENCES users (id)
);

DELIMITER //

CREATE PROCEDURE add_ai_route_record_index_if_missing(
    IN target_index VARCHAR(64),
    IN create_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'ai_route_records'
          AND index_name = target_index
    ) THEN
        SET @ddl = create_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL add_ai_route_record_index_if_missing('idx_ai_route_records_user_updated',
    'CREATE INDEX idx_ai_route_records_user_updated ON ai_route_records (user_id, updated_at)');
CALL add_ai_route_record_index_if_missing('idx_ai_route_records_user_saved_updated',
    'CREATE INDEX idx_ai_route_records_user_saved_updated ON ai_route_records (user_id, manually_saved, updated_at)');
CALL add_ai_route_record_index_if_missing('idx_ai_route_records_job',
    'CREATE INDEX idx_ai_route_records_job ON ai_route_records (job_id)');

DROP PROCEDURE add_ai_route_record_index_if_missing;
