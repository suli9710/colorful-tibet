CREATE TABLE IF NOT EXISTS itineraries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    parent_itinerary_id BIGINT NULL,
    title VARCHAR(160) NOT NULL,
    days INT NULL,
    start_date DATE NULL,
    budget VARCHAR(32) NULL,
    preference VARCHAR(64) NULL,
    version_type VARCHAR(48) NULL,
    version_label VARCHAR(64) NULL,
    source_content TEXT NULL,
    total_estimated_cost DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(32) DEFAULT 'DRAFT',
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_itineraries_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_itineraries_parent FOREIGN KEY (parent_itinerary_id) REFERENCES itineraries (id)
);

CREATE TABLE IF NOT EXISTS itinerary_days (
    id BIGINT NOT NULL AUTO_INCREMENT,
    itinerary_id BIGINT NOT NULL,
    day_number INT NOT NULL,
    travel_date DATE NULL,
    title VARCHAR(160) NOT NULL,
    region VARCHAR(96) NULL,
    summary TEXT NULL,
    estimated_cost DECIMAL(19, 2) DEFAULT 0,
    altitude_risk VARCHAR(32) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_itinerary_days_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (id)
);

CREATE TABLE IF NOT EXISTS itinerary_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    day_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    start_time VARCHAR(16) NULL,
    duration_minutes INT NULL,
    estimated_cost DECIMAL(19, 2) DEFAULT 0,
    altitude_meters INT NULL,
    risk_level VARCHAR(32) NULL,
    alternatives TEXT NULL,
    booking_action VARCHAR(32) NULL,
    booking_status VARCHAR(32) NULL,
    booking_reference_type VARCHAR(32) NULL,
    booking_reference_id BIGINT NULL,
    scenic_spot_id BIGINT NULL,
    hotel_id BIGINT NULL,
    room_type_id BIGINT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_itinerary_items_day FOREIGN KEY (day_id) REFERENCES itinerary_days (id),
    CONSTRAINT fk_itinerary_items_spot FOREIGN KEY (scenic_spot_id) REFERENCES scenic_spots (id),
    CONSTRAINT fk_itinerary_items_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT fk_itinerary_items_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id)
);

DELIMITER //

CREATE PROCEDURE add_itinerary_index_if_missing(
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

CALL add_itinerary_index_if_missing('itineraries', 'idx_itineraries_user_created',
    'CREATE INDEX idx_itineraries_user_created ON itineraries (user_id, created_at)');
CALL add_itinerary_index_if_missing('itineraries', 'idx_itineraries_parent',
    'CREATE INDEX idx_itineraries_parent ON itineraries (parent_itinerary_id)');
CALL add_itinerary_index_if_missing('itinerary_days', 'idx_itinerary_days_itinerary_day',
    'CREATE INDEX idx_itinerary_days_itinerary_day ON itinerary_days (itinerary_id, day_number)');
CALL add_itinerary_index_if_missing('itinerary_items', 'idx_itinerary_items_day_sort',
    'CREATE INDEX idx_itinerary_items_day_sort ON itinerary_items (day_id, sort_order)');
CALL add_itinerary_index_if_missing('itinerary_items', 'idx_itinerary_items_spot',
    'CREATE INDEX idx_itinerary_items_spot ON itinerary_items (scenic_spot_id)');
CALL add_itinerary_index_if_missing('itinerary_items', 'idx_itinerary_items_hotel',
    'CREATE INDEX idx_itinerary_items_hotel ON itinerary_items (hotel_id)');

DROP PROCEDURE add_itinerary_index_if_missing;
