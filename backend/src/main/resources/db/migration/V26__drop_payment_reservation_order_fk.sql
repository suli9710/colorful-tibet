-- payment_transaction_reservations is a uniqueness ledger for payment/refund transaction numbers.
-- Its foreign key to orders(order_no) forced every reservation INSERT to take a shared lock on the
-- referenced orders row. Reservations are written from a REQUIRES_NEW transaction while the caller
-- still holds SELECT ... FOR UPDATE on that same row (OrderCenterService#handlePaymentCallback and
-- #reviewRefund), so the insert blocked on a lock the suspended outer transaction owned. InnoDB
-- cannot detect that as a cycle -- the outer transaction waits in application code, not on a lock --
-- so the insert stalled for the full innodb_lock_wait_timeout and then failed with error 1205,
-- rolling back every payment callback after the money had already been taken.
--
-- The constraint is not needed for correctness here: orders are only ever soft-deleted (V23), and a
-- reservation must outlive its order anyway so a transaction number can never be reused.

DELIMITER //

CREATE PROCEDURE drop_payment_reservation_order_fk_if_present()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'payment_transaction_reservations'
          AND constraint_name = 'fk_payment_transaction_reservations_order_no'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE payment_transaction_reservations
            DROP FOREIGN KEY fk_payment_transaction_reservations_order_no;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'payment_transaction_reservations'
          AND index_name = 'idx_payment_transaction_reservations_order_no'
    ) THEN
        CREATE INDEX idx_payment_transaction_reservations_order_no
            ON payment_transaction_reservations (order_no);
    END IF;
END //

DELIMITER ;

CALL drop_payment_reservation_order_fk_if_present();

DROP PROCEDURE drop_payment_reservation_order_fk_if_present;
