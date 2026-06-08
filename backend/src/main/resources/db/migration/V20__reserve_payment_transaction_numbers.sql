CREATE TABLE IF NOT EXISTS payment_transaction_reservations (
    transaction_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    reference_no VARCHAR(64) NULL,
    usage_type VARCHAR(16) NOT NULL DEFAULT 'PAYMENT',
    provider VARCHAR(32) NULL,
    amount DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NULL,
    PRIMARY KEY (transaction_no),
    CONSTRAINT fk_payment_transaction_reservations_order_no
        FOREIGN KEY (order_no) REFERENCES orders (order_no) ON DELETE CASCADE
);

INSERT IGNORE INTO payment_transaction_reservations (
    transaction_no,
    order_no,
    reference_no,
    usage_type,
    provider,
    amount,
    status,
    created_at
)
SELECT
    pt.transaction_no,
    o.order_no,
    NULL,
    CASE WHEN pt.status = 'REFUNDED' THEN 'REFUND' ELSE 'PAYMENT' END,
    pt.provider,
    pt.amount,
    pt.status,
    COALESCE(pt.created_at, NOW())
FROM payment_transactions pt
JOIN orders o ON o.id = pt.order_id;
