UPDATE user_visit_history keeper
JOIN (
    SELECT
        MAX(id) AS keeper_id,
        COALESCE(SUM(click_count), 0) AS merged_click_count,
        COALESCE(SUM(dwell_seconds), 0) AS merged_dwell_seconds,
        MAX(visit_date) AS latest_visit_date,
        MAX(rating) AS merged_rating
    FROM user_visit_history
    WHERE user_id IS NOT NULL
      AND spot_id IS NOT NULL
    GROUP BY user_id, spot_id
    HAVING COUNT(*) > 1
) merged ON merged.keeper_id = keeper.id
SET keeper.click_count = merged.merged_click_count,
    keeper.dwell_seconds = merged.merged_dwell_seconds,
    keeper.visit_date = COALESCE(merged.latest_visit_date, keeper.visit_date),
    keeper.rating = COALESCE(merged.merged_rating, keeper.rating);

DELETE uvh
FROM user_visit_history uvh
JOIN user_visit_history newer
  ON newer.user_id = uvh.user_id
 AND newer.spot_id = uvh.spot_id
 AND newer.id > uvh.id
WHERE uvh.user_id IS NOT NULL
  AND uvh.spot_id IS NOT NULL;

ALTER TABLE user_visit_history
    ADD CONSTRAINT uk_user_visit_history_user_spot UNIQUE (user_id, spot_id);
