package com.tibet.tourism.modules.order.infra;

import com.tibet.tourism.modules.order.domain.PaymentTransactionReservation;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentTransactionReservationRepository extends JpaRepository<PaymentTransactionReservation, String> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO payment_transaction_reservations (
                transaction_no,
                order_no,
                reference_no,
                usage_type,
                provider,
                amount,
                status,
                created_at
            ) VALUES (
                :transactionNo,
                :orderNo,
                :referenceNo,
                :usageType,
                :provider,
                :amount,
                :status,
                CURRENT_TIMESTAMP
            )
            """, nativeQuery = true)
    int insertIgnore(
            @Param("transactionNo") String transactionNo,
            @Param("orderNo") String orderNo,
            @Param("referenceNo") String referenceNo,
            @Param("usageType") String usageType,
            @Param("provider") String provider,
            @Param("amount") BigDecimal amount,
            @Param("status") String status);
}
