ALTER TABLE users
  ADD COLUMN allowed_login_fingerprint_hash VARCHAR(64) NULL;
