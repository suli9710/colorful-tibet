ALTER TABLE heritage_items
    ADD COLUMN region VARCHAR(255) NULL,
    ADD COLUMN protection_level VARCHAR(255) NULL,
    ADD COLUMN view_count INT NULL DEFAULT 0,
    ADD COLUMN like_count INT NULL DEFAULT 0,
    ADD COLUMN comment_count INT NULL DEFAULT 0;

CREATE TABLE heritage_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    heritage_item_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_heritage_likes_user_item UNIQUE (user_id, heritage_item_id),
    CONSTRAINT fk_heritage_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_heritage_likes_item FOREIGN KEY (heritage_item_id) REFERENCES heritage_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_heritage_likes_item ON heritage_likes (heritage_item_id);
CREATE INDEX idx_heritage_likes_user ON heritage_likes (user_id);

CREATE TABLE heritage_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content TEXT NOT NULL,
    image_url VARCHAR(255) NULL,
    rating INT NULL,
    user_id BIGINT NOT NULL,
    heritage_item_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_heritage_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_heritage_comments_item FOREIGN KEY (heritage_item_id) REFERENCES heritage_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_heritage_comments_item_created ON heritage_comments (heritage_item_id, created_at);

CREATE TABLE heritage_inheritors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    name_tibetan VARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    inheritor_level VARCHAR(255) NULL,
    bio TEXT NULL,
    bio_tibetan TEXT NULL,
    story TEXT NULL,
    region VARCHAR(255) NULL,
    heritage_item_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_heritage_inheritors_item FOREIGN KEY (heritage_item_id) REFERENCES heritage_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_heritage_inheritors_item ON heritage_inheritors (heritage_item_id);

CREATE TABLE heritage_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    title_tibetan VARCHAR(255) NULL,
    description TEXT NULL,
    description_tibetan TEXT NULL,
    event_date DATE NULL,
    end_date DATE NULL,
    location VARCHAR(255) NULL,
    image_url VARCHAR(255) NULL,
    contact_info VARCHAR(255) NULL,
    heritage_item_id BIGINT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_heritage_events_item FOREIGN KEY (heritage_item_id) REFERENCES heritage_items (id) ON DELETE SET NULL
);

CREATE INDEX idx_heritage_events_item ON heritage_events (heritage_item_id);
CREATE INDEX idx_heritage_events_date ON heritage_events (event_date);
