CREATE TABLE IF NOT EXISTS spot_price_observations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spot_id BIGINT NOT NULL,
    spot_name VARCHAR(255) NOT NULL,
    base_price DECIMAL(19, 2) NULL,
    peak_season_price DECIMAL(19, 2) NULL,
    off_season_price DECIMAL(19, 2) NULL,
    source VARCHAR(128) NULL,
    confidence DOUBLE NULL,
    reference_only BOOLEAN NOT NULL DEFAULT FALSE,
    publishable BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    review_reason VARCHAR(255) NULL,
    observed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_spot_price_observations_spot
        FOREIGN KEY (spot_id) REFERENCES scenic_spots (id) ON DELETE CASCADE
);

CREATE INDEX idx_spot_price_observations_spot ON spot_price_observations (spot_id);
CREATE INDEX idx_spot_price_observations_status ON spot_price_observations (status);
CREATE INDEX idx_spot_price_observations_observed_at ON spot_price_observations (observed_at);
