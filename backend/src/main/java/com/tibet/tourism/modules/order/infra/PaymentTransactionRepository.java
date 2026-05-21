package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionNo(String transactionNo);
}
