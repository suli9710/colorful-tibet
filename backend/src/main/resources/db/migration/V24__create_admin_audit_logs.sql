CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NULL,
    actor_ref VARCHAR(64) NOT NULL,
    target_id BIGINT NULL,
    target_ref VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    result VARCHAR(16) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    before_role VARCHAR(32) NULL,
    after_role VARCHAR(32) NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_admin_audit_actor_created
    ON admin_audit_logs (actor_id, created_at);

CREATE INDEX idx_admin_audit_target_created
    ON admin_audit_logs (target_id, created_at);

CREATE INDEX idx_admin_audit_action_created
    ON admin_audit_logs (action, created_at);
