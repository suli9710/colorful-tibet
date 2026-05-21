ALTER TABLE inventory_locks
    ADD COLUMN active_lock_key VARCHAR(160) NULL;

CREATE UNIQUE INDEX uk_inventory_locks_active_lock_key
    ON inventory_locks (active_lock_key);
