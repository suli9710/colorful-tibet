UPDATE comments SET like_count = 0 WHERE like_count IS NULL;

UPDATE heritage_items SET view_count = 0 WHERE view_count IS NULL;
UPDATE heritage_items SET like_count = 0 WHERE like_count IS NULL;
UPDATE heritage_items SET comment_count = 0 WHERE comment_count IS NULL;

ALTER TABLE comments MODIFY like_count INT NOT NULL DEFAULT 0;

ALTER TABLE heritage_items MODIFY view_count INT NOT NULL DEFAULT 0;
ALTER TABLE heritage_items MODIFY like_count INT NOT NULL DEFAULT 0;
ALTER TABLE heritage_items MODIFY comment_count INT NOT NULL DEFAULT 0;
ALTER TABLE heritage_items ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
