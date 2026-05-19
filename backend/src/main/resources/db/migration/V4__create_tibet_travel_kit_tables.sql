CREATE TABLE IF NOT EXISTS tibet_travel_kits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    itinerary_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    generated_at DATETIME NULL,
    valid_until DATETIME NULL,
    risk_score INT NULL,
    risk_level VARCHAR(32) NULL,
    risk_label VARCHAR(32) NULL,
    max_altitude_meters INT NULL,
    high_altitude_days INT NULL,
    highland_summary TEXT NULL,
    included_sections TEXT NULL,
    adaptation_checklist TEXT NULL,
    warning_signs TEXT NULL,
    go_slow_rules TEXT NULL,
    offline_checklist TEXT NULL,
    voucher_hints TEXT NULL,
    backup_notes TEXT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_travel_kits_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tibet_travel_kits_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (id)
);

CREATE TABLE IF NOT EXISTS tibet_travel_kit_day_advice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    travel_kit_id BIGINT NOT NULL,
    day_number INT NULL,
    title VARCHAR(160) NULL,
    max_altitude_meters INT NULL,
    risk_level VARCHAR(32) NULL,
    pace_advice TEXT NULL,
    hydration_advice TEXT NULL,
    activity_limit TEXT NULL,
    warning TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_day_advice_kit FOREIGN KEY (travel_kit_id) REFERENCES tibet_travel_kits (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tibet_travel_kit_culture_tips (
    id BIGINT NOT NULL AUTO_INCREMENT,
    travel_kit_id BIGINT NOT NULL,
    scene VARCHAR(48) NULL,
    title VARCHAR(160) NOT NULL,
    context TEXT NULL,
    do_tips TEXT NULL,
    avoid_tips TEXT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_culture_tips_kit FOREIGN KEY (travel_kit_id) REFERENCES tibet_travel_kits (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tibet_travel_kit_phrases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    travel_kit_id BIGINT NOT NULL,
    category VARCHAR(48) NULL,
    chinese VARCHAR(160) NOT NULL,
    tibetan VARCHAR(220) NOT NULL,
    english VARCHAR(160) NULL,
    pronunciation VARCHAR(160) NULL,
    usage_text TEXT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_phrases_kit FOREIGN KEY (travel_kit_id) REFERENCES tibet_travel_kits (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tibet_travel_kit_emergency_contacts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    travel_kit_id BIGINT NOT NULL,
    name VARCHAR(96) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    description TEXT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_contacts_kit FOREIGN KEY (travel_kit_id) REFERENCES tibet_travel_kits (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tibet_travel_kit_map_pins (
    id BIGINT NOT NULL AUTO_INCREMENT,
    travel_kit_id BIGINT NOT NULL,
    pin_type VARCHAR(48) NULL,
    name VARCHAR(160) NOT NULL,
    latitude DECIMAL(19, 6) NULL,
    longitude DECIMAL(19, 6) NULL,
    altitude_meters INT NULL,
    note TEXT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_map_pins_kit FOREIGN KEY (travel_kit_id) REFERENCES tibet_travel_kits (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tibet_travel_kit_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    travel_kit_id BIGINT NOT NULL,
    level VARCHAR(32) NULL,
    alert_type VARCHAR(48) NULL,
    title VARCHAR(160) NOT NULL,
    message TEXT NULL,
    action TEXT NULL,
    related_day INT NULL,
    expires_at DATETIME NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_alerts_kit FOREIGN KEY (travel_kit_id) REFERENCES tibet_travel_kits (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tibet_travel_kit_sustainable_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    travel_kit_id BIGINT NOT NULL,
    category VARCHAR(48) NULL,
    title VARCHAR(160) NOT NULL,
    impact TEXT NULL,
    actions TEXT NULL,
    local_benefit TEXT NULL,
    carbon_hint TEXT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tibet_sustainable_kit FOREIGN KEY (travel_kit_id) REFERENCES tibet_travel_kits (id) ON DELETE CASCADE
);

DELIMITER //

CREATE PROCEDURE add_tibet_travel_kit_index_if_missing(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64),
    IN create_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = target_index
    ) THEN
        SET @ddl = create_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kits', 'idx_tibet_travel_kits_user_generated',
    'CREATE INDEX idx_tibet_travel_kits_user_generated ON tibet_travel_kits (user_id, generated_at)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kits', 'idx_tibet_travel_kits_itinerary_valid',
    'CREATE INDEX idx_tibet_travel_kits_itinerary_valid ON tibet_travel_kits (itinerary_id, valid_until)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_day_advice', 'idx_tibet_kit_day_advice_kit_day',
    'CREATE INDEX idx_tibet_kit_day_advice_kit_day ON tibet_travel_kit_day_advice (travel_kit_id, day_number)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_culture_tips', 'idx_tibet_kit_culture_kit_sort',
    'CREATE INDEX idx_tibet_kit_culture_kit_sort ON tibet_travel_kit_culture_tips (travel_kit_id, sort_order)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_phrases', 'idx_tibet_kit_phrases_kit_sort',
    'CREATE INDEX idx_tibet_kit_phrases_kit_sort ON tibet_travel_kit_phrases (travel_kit_id, sort_order)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_emergency_contacts', 'idx_tibet_kit_contacts_kit_sort',
    'CREATE INDEX idx_tibet_kit_contacts_kit_sort ON tibet_travel_kit_emergency_contacts (travel_kit_id, sort_order)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_map_pins', 'idx_tibet_kit_map_pins_kit_sort',
    'CREATE INDEX idx_tibet_kit_map_pins_kit_sort ON tibet_travel_kit_map_pins (travel_kit_id, sort_order)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_alerts', 'idx_tibet_kit_alerts_kit_sort',
    'CREATE INDEX idx_tibet_kit_alerts_kit_sort ON tibet_travel_kit_alerts (travel_kit_id, sort_order)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_alerts', 'idx_tibet_kit_alerts_expires',
    'CREATE INDEX idx_tibet_kit_alerts_expires ON tibet_travel_kit_alerts (expires_at)');
CALL add_tibet_travel_kit_index_if_missing('tibet_travel_kit_sustainable_options', 'idx_tibet_kit_sustainable_kit_sort',
    'CREATE INDEX idx_tibet_kit_sustainable_kit_sort ON tibet_travel_kit_sustainable_options (travel_kit_id, sort_order)');

DROP PROCEDURE add_tibet_travel_kit_index_if_missing;
