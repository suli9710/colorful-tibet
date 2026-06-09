ALTER TABLE orders
    ADD COLUMN user_hidden_at DATETIME NULL;

CREATE INDEX idx_orders_user_hidden_created
    ON orders (user_id, user_hidden_at, created_at);

ALTER TABLE order_audit_logs
    DROP FOREIGN KEY fk_order_audit_logs_order;

ALTER TABLE order_audit_logs
    ADD CONSTRAINT fk_order_audit_logs_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE RESTRICT;
