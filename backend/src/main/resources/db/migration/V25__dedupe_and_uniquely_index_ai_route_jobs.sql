DELETE older
FROM ai_route_records older
JOIN ai_route_records newer
  ON newer.user_id = older.user_id
 AND newer.job_id = older.job_id
 AND newer.id > older.id
WHERE older.job_id IS NOT NULL;

DELIMITER //

CREATE PROCEDURE add_ai_route_user_job_unique_key_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'ai_route_records'
          AND index_name = 'uk_ai_route_records_user_job'
          AND non_unique = 0
    ) THEN
        ALTER TABLE ai_route_records
            ADD CONSTRAINT uk_ai_route_records_user_job UNIQUE (user_id, job_id);
    END IF;
END //

DELIMITER ;

CALL add_ai_route_user_job_unique_key_if_missing();
DROP PROCEDURE add_ai_route_user_job_unique_key_if_missing;
