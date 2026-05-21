DELIMITER //

CREATE PROCEDURE add_index_if_missing(
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

CALL add_index_if_missing('bookings', 'idx_bookings_user_created',
    'CREATE INDEX idx_bookings_user_created ON bookings (user_id, created_at)');
CALL add_index_if_missing('bookings', 'idx_bookings_spot_visit',
    'CREATE INDEX idx_bookings_spot_visit ON bookings (spot_id, visit_date)');
CALL add_index_if_missing('bookings', 'idx_bookings_status_created',
    'CREATE INDEX idx_bookings_status_created ON bookings (status, created_at)');

CALL add_index_if_missing('hotel_bookings', 'idx_hb_user_created',
    'CREATE INDEX idx_hb_user_created ON hotel_bookings (user_id, created_at)');
CALL add_index_if_missing('hotel_bookings', 'idx_hb_hotel_created',
    'CREATE INDEX idx_hb_hotel_created ON hotel_bookings (hotel_id, created_at)');
CALL add_index_if_missing('hotel_bookings', 'idx_hb_status_created',
    'CREATE INDEX idx_hb_status_created ON hotel_bookings (status, created_at)');

CALL add_index_if_missing('shared_routes', 'idx_shared_routes_author_created',
    'CREATE INDEX idx_shared_routes_author_created ON shared_routes (author_id, created_at)');
CALL add_index_if_missing('shared_routes', 'idx_shared_routes_source',
    'CREATE INDEX idx_shared_routes_source ON shared_routes (source_type, source_route_id)');

CALL add_index_if_missing('travel_questions', 'idx_travel_questions_author_created',
    'CREATE INDEX idx_travel_questions_author_created ON travel_questions (author_id, created_at)');
CALL add_index_if_missing('travel_questions', 'idx_travel_questions_resolved_created',
    'CREATE INDEX idx_travel_questions_resolved_created ON travel_questions (is_resolved, created_at)');

CALL add_index_if_missing('route_comments', 'idx_route_comments_route_created',
    'CREATE INDEX idx_route_comments_route_created ON route_comments (route_id, created_at)');
CALL add_index_if_missing('route_comments', 'idx_route_comments_user_created',
    'CREATE INDEX idx_route_comments_user_created ON route_comments (user_id, created_at)');

CALL add_index_if_missing('comment_likes', 'idx_comment_likes_comment',
    'CREATE INDEX idx_comment_likes_comment ON comment_likes (comment_id)');
CALL add_index_if_missing('comment_likes', 'idx_comment_likes_user',
    'CREATE INDEX idx_comment_likes_user ON comment_likes (user_id)');

DROP PROCEDURE add_index_if_missing;
