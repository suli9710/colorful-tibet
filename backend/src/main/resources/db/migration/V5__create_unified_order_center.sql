CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(96) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    product_summary VARCHAR(220) NULL,
    total_amount DECIMAL(19, 2) DEFAULT 0,
    discount_amount DECIMAL(19, 2) DEFAULT 0,
    payable_amount DECIMAL(19, 2) DEFAULT 0,
    customer_name VARCHAR(64) NULL,
    customer_phone VARCHAR(32) NULL,
    customer_note TEXT NULL,
    support_note TEXT NULL,
    source_type VARCHAR(48) NULL,
    source_reference_id BIGINT NULL,
    locked_until DATETIME NULL,
    expires_at DATETIME NULL,
    paid_at DATETIME NULL,
    confirmed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT uk_orders_user_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    product_id BIGINT NULL,
    sku_id BIGINT NULL,
    product_name VARCHAR(160) NOT NULL,
    sku_name VARCHAR(160) NULL,
    service_start_date DATE NULL,
    service_end_date DATE NULL,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(19, 2) DEFAULT 0,
    subtotal DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'LOCKED',
    cancellation_policy_id BIGINT NULL,
    legacy_reference_type VARCHAR(48) NULL,
    legacy_reference_id BIGINT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    transaction_no VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NULL,
    amount DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    signature_valid BOOLEAN DEFAULT FALSE,
    request_payload TEXT NULL,
    callback_payload TEXT NULL,
    paid_at DATETIME NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_transactions_no UNIQUE (transaction_no),
    CONSTRAINT fk_payment_transactions_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NULL,
    refund_no VARCHAR(64) NOT NULL,
    amount DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    reason TEXT NULL,
    requested_at DATETIME NULL,
    processed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refunds_no UNIQUE (refund_no),
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_refunds_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS vouchers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NULL,
    voucher_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
    valid_from DATE NULL,
    valid_until DATE NULL,
    issued_at DATETIME NULL,
    consumed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vouchers_code UNIQUE (voucher_code),
    CONSTRAINT fk_vouchers_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_vouchers_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    invoice_no VARCHAR(64) NOT NULL,
    invoice_title VARCHAR(160) NOT NULL,
    tax_no VARCHAR(64) NULL,
    amount DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    requested_at DATETIME NULL,
    issued_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_no UNIQUE (invoice_no),
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cancellation_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_type VARCHAR(32) NOT NULL,
    policy_name VARCHAR(120) NOT NULL,
    free_cancel_before_hours INT DEFAULT 24,
    refund_rate DECIMAL(5, 4) DEFAULT 1,
    rules TEXT NULL,
    active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    created_at DATETIME NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inventory_locks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    product_id BIGINT NULL,
    sku_id BIGINT NULL,
    service_date DATE NULL,
    quantity INT DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'LOCKED',
    expires_at DATETIME NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inventory_locks_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_locks_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    actor_user_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NULL,
    note TEXT NULL,
    created_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_audit_logs_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

DELIMITER //

CREATE PROCEDURE add_order_center_index_if_missing(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64),
    IN create_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = target_index
    ) THEN
        SET @ddl = create_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL add_order_center_index_if_missing('orders', 'idx_orders_user_created',
    'CREATE INDEX idx_orders_user_created ON orders (user_id, created_at)');
CALL add_order_center_index_if_missing('orders', 'idx_orders_status_created',
    'CREATE INDEX idx_orders_status_created ON orders (status, created_at)');
CALL add_order_center_index_if_missing('orders', 'idx_orders_source',
    'CREATE INDEX idx_orders_source ON orders (source_type, source_reference_id)');
CALL add_order_center_index_if_missing('order_items', 'idx_order_items_order_sort',
    'CREATE INDEX idx_order_items_order_sort ON order_items (order_id, sort_order)');
CALL add_order_center_index_if_missing('order_items', 'idx_order_items_product',
    'CREATE INDEX idx_order_items_product ON order_items (product_type, product_id)');
CALL add_order_center_index_if_missing('payment_transactions', 'idx_payment_transactions_order',
    'CREATE INDEX idx_payment_transactions_order ON payment_transactions (order_id, created_at)');
CALL add_order_center_index_if_missing('refunds', 'idx_refunds_order',
    'CREATE INDEX idx_refunds_order ON refunds (order_id, requested_at)');
CALL add_order_center_index_if_missing('vouchers', 'idx_vouchers_order',
    'CREATE INDEX idx_vouchers_order ON vouchers (order_id, issued_at)');
CALL add_order_center_index_if_missing('invoices', 'idx_invoices_order',
    'CREATE INDEX idx_invoices_order ON invoices (order_id, requested_at)');
CALL add_order_center_index_if_missing('cancellation_policies', 'idx_cancellation_policies_product',
    'CREATE INDEX idx_cancellation_policies_product ON cancellation_policies (product_type, active, priority)');
CALL add_order_center_index_if_missing('inventory_locks', 'idx_inventory_locks_product_date',
    'CREATE INDEX idx_inventory_locks_product_date ON inventory_locks (product_type, product_id, service_date)');
CALL add_order_center_index_if_missing('inventory_locks', 'idx_inventory_locks_expires',
    'CREATE INDEX idx_inventory_locks_expires ON inventory_locks (expires_at, status)');
CALL add_order_center_index_if_missing('order_audit_logs', 'idx_order_audit_logs_order_created',
    'CREATE INDEX idx_order_audit_logs_order_created ON order_audit_logs (order_id, created_at)');

DROP PROCEDURE add_order_center_index_if_missing;
