-- Unknown stock stays unknown; never infer room quantity from guest capacity.
-- Existing bookings are preserved. Operators must reconcile supply before enabling new reservations.
ALTER TABLE room_types ADD COLUMN inventory_units INT NULL;
ALTER TABLE room_types ADD CONSTRAINT chk_room_inventory_units
    CHECK (inventory_units IS NULL OR (inventory_units >= 0 AND inventory_units <= 10000));

-- Keep per-room/date locking narrow rather than locking every room type of a hotel.
CREATE INDEX idx_inventory_locks_sku_date_status ON inventory_locks (sku_id, service_date, status);
