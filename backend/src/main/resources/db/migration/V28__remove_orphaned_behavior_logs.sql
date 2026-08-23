DELETE behavior_log
FROM behavior_logs AS behavior_log
LEFT JOIN users AS user_account
  ON user_account.id = behavior_log.user_id
WHERE behavior_log.user_id IS NOT NULL
  AND user_account.id IS NULL;
