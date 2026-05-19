ALTER TABLE users
  MODIFY phone VARCHAR(512) NULL,
  MODIFY ip_address VARCHAR(64) NULL,
  ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE hotel_bookings
  ADD COLUMN room_type_id BIGINT NULL,
  ADD COLUMN deleted_at DATETIME NULL;

CREATE INDEX idx_hb_room_dates
  ON hotel_bookings (room_type_id, check_in_date, check_out_date, status, deleted_at);
