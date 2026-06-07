CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NULL,
    avatar VARCHAR(255) NULL,
    phone VARCHAR(255) NULL,
    city VARCHAR(255) NULL,
    ip_address VARCHAR(255) NULL,
    last_login_at DATETIME NULL,
    role VARCHAR(255) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_nickname UNIQUE (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scenic_spots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    name_tibetan VARCHAR(255) NULL,
    description TEXT NULL,
    description_tibetan TEXT NULL,
    image_url VARCHAR(255) NULL,
    altitude VARCHAR(255) NULL,
    location VARCHAR(255) NULL,
    category VARCHAR(255) NULL,
    ticket_price DECIMAL(19, 2) NULL,
    peak_season_price DECIMAL(19, 2) NULL,
    off_season_price DECIMAL(19, 2) NULL,
    peak_start_date DATE NULL,
    peak_end_date DATE NULL,
    free_start_date DATE NULL,
    free_end_date DATE NULL,
    rating DECIMAL(19, 2) NULL DEFAULT 0,
    latitude DECIMAL(19, 6) NULL,
    longitude DECIMAL(19, 6) NULL,
    visit_count INT NULL DEFAULT 0,
    num INT NULL,
    open_info VARCHAR(255) NULL,
    entry_time VARCHAR(255) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_scenic_spots_category ON scenic_spots (category);
CREATE INDEX idx_scenic_spots_visit_count ON scenic_spots (visit_count);
CREATE INDEX idx_scenic_spots_rating ON scenic_spots (rating);

CREATE TABLE IF NOT EXISTS spot_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    spot_id BIGINT NULL,
    tag VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_spot_tags_spot FOREIGN KEY (spot_id) REFERENCES scenic_spots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hotels (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NULL,
    phone VARCHAR(255) NULL,
    price_range VARCHAR(255) NULL,
    rating DECIMAL(19, 2) NULL,
    image_url VARCHAR(255) NULL,
    facilities TEXT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS room_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(19, 2) NULL,
    capacity INT NULL,
    image_url VARCHAR(255) NULL,
    amenities VARCHAR(255) NULL,
    sort_order INT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_room_types_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hotel_bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    hotel_id BIGINT NULL,
    room_name VARCHAR(255) NULL,
    room_price DECIMAL(19, 2) NULL,
    nights INT NULL,
    check_in_date DATE NULL,
    check_out_date DATE NULL,
    guests INT NULL,
    guest_name VARCHAR(255) NULL,
    phone VARCHAR(255) NULL,
    note VARCHAR(255) NULL,
    subtotal DECIMAL(19, 2) NULL,
    service_fee DECIMAL(19, 2) NULL,
    discount DECIMAL(19, 2) NULL,
    total_price DECIMAL(19, 2) NULL,
    status VARCHAR(255) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_hotel_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_hotel_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    spot_id BIGINT NULL,
    visit_date DATE NULL,
    ticket_count INT NULL,
    total_price DECIMAL(19, 2) NULL,
    status VARCHAR(255) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_spot FOREIGN KEY (spot_id) REFERENCES scenic_spots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_visit_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    spot_id BIGINT NULL,
    rating INT NULL,
    click_count INT NULL DEFAULT 0,
    dwell_seconds INT NULL DEFAULT 0,
    visit_date DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_visit_history_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_visit_history_spot FOREIGN KEY (spot_id) REFERENCES scenic_spots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_uvh_user_id ON user_visit_history (user_id);
CREATE INDEX idx_uvh_spot_id ON user_visit_history (spot_id);

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content TEXT NULL,
    rating INT NULL,
    image_url VARCHAR(255) NULL,
    like_count INT NULL DEFAULT 0,
    user_id BIGINT NULL,
    spot_id BIGINT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comments_spot FOREIGN KEY (spot_id) REFERENCES scenic_spots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comments_spot_created ON comments (spot_id, created_at);

CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_comment_likes_user_comment UNIQUE (user_id, comment_id),
    CONSTRAINT fk_comment_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS travel_routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    name_tibetan VARCHAR(255) NULL,
    description TEXT NULL,
    description_tibetan TEXT NULL,
    days INT NULL,
    price DECIMAL(19, 2) NULL,
    difficulty VARCHAR(255) NULL,
    spots_json TEXT NULL,
    temperature VARCHAR(255) NULL,
    geography VARCHAR(255) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_favorites_user_route UNIQUE (user_id, route_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_favorites_route FOREIGN KEY (route_id) REFERENCES travel_routes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shared_routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    days INT NOT NULL,
    budget VARCHAR(50) NULL,
    preference VARCHAR(50) NULL,
    source_type VARCHAR(20) NULL DEFAULT 'USER',
    source_route_id BIGINT NULL,
    price DECIMAL(19, 2) NULL,
    difficulty VARCHAR(20) NULL,
    temperature VARCHAR(100) NULL,
    geography VARCHAR(100) NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_shared_routes_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS route_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_route_comments_route FOREIGN KEY (route_id) REFERENCES shared_routes (id),
    CONSTRAINT fk_route_comments_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS route_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_route_likes_route_user UNIQUE (route_id, user_id),
    CONSTRAINT fk_route_likes_route FOREIGN KEY (route_id) REFERENCES shared_routes (id),
    CONSTRAINT fk_route_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS travel_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(500) NULL,
    view_count INT NOT NULL DEFAULT 0,
    answer_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_travel_questions_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS travel_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    is_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_travel_answers_question FOREIGN KEY (question_id) REFERENCES travel_questions (id),
    CONSTRAINT fk_travel_answers_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_question_likes_question_user UNIQUE (question_id, user_id),
    CONSTRAINT fk_question_likes_question FOREIGN KEY (question_id) REFERENCES travel_questions (id),
    CONSTRAINT fk_question_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS carousels (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255) NULL,
    tag VARCHAR(255) NULL,
    image_url VARCHAR(255) NULL,
    link_url VARCHAR(255) NULL,
    sort_order INT NULL DEFAULT 0,
    active BOOLEAN NULL DEFAULT TRUE,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS heritage_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    name_tibetan VARCHAR(255) NULL,
    description TEXT NULL,
    description_tibetan TEXT NULL,
    category VARCHAR(255) NULL,
    image_url VARCHAR(255) NULL,
    video_url VARCHAR(255) NULL,
    origin_story TEXT NULL,
    significance TEXT NULL,
    baike_url VARCHAR(512) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS news (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    title_tibetan VARCHAR(255) NULL,
    content TEXT NULL,
    content_tibetan TEXT NULL,
    category VARCHAR(255) NULL,
    image_url VARCHAR(255) NULL,
    view_count INT NULL DEFAULT 0,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_news_created_at ON news (created_at);

CREATE TABLE IF NOT EXISTS tibetan_dictionary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chinese_text VARCHAR(500) NOT NULL,
    tibetan_text TEXT NOT NULL,
    type VARCHAR(255) NOT NULL,
    usage_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tibetan_dictionary_text_type UNIQUE (chinese_text, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS behavior_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    endpoint VARCHAR(128) NULL,
    fingerprint VARCHAR(64) NULL,
    mouse_speed_mean DOUBLE NULL,
    mouse_speed_std_dev DOUBLE NULL,
    straightness_ratio DOUBLE NULL,
    mouse_interval_std_dev DOUBLE NULL,
    key_interval_std_dev DOUBLE NULL,
    recaptcha_score DOUBLE NULL,
    final_risk_score DOUBLE NULL,
    decision VARCHAR(16) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bl_user_id ON behavior_logs (user_id);
CREATE INDEX idx_bl_created_at ON behavior_logs (created_at);
CREATE INDEX idx_bl_decision ON behavior_logs (decision);
