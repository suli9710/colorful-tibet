ALTER TABLE behavior_logs
  ADD CONSTRAINT fk_behavior_logs_user
  FOREIGN KEY (user_id) REFERENCES users (id)
  ON DELETE CASCADE;
