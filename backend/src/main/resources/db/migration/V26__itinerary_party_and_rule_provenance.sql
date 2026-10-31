-- Legacy party size and policy are deliberately unknown, not silently set to two.
ALTER TABLE itineraries ADD COLUMN travelers INT NULL;
ALTER TABLE itineraries ADD COLUMN generation_rule_version VARCHAR(48) NULL;
