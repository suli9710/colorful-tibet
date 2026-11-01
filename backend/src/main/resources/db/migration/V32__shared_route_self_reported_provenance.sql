-- Self-report is not verification. Historical rows remain unknown, not retroactively certified.
ALTER TABLE shared_routes ADD COLUMN travel_status VARCHAR(20) NULL;
ALTER TABLE shared_routes ADD COLUMN travel_month VARCHAR(7) NULL;
