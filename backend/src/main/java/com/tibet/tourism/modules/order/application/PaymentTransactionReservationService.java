package com.tibet.tourism.modules.order.application;

import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import com.tibet.tourism.modules.order.domain.PaymentTransactionReservation;
import com.tibet.tourism.modules.order.infra.PaymentTransactionReservationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentTransactionReservationService {

    private final PaymentTransactionReservationRepository reservationRepository;
    private final TransactionTemplate requiresNewTransaction;

    public PaymentTransactionReservationService(PaymentTransactionReservationRepository reservationRepository,
                                                PlatformTransactionManager transactionManager) {
        this.reservationRepository = reservationRepository;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean reservePaymentTransaction(
            String transactionNo,
            String orderNo,
            String provider,
            BigDecimal amount,
            PaymentTransaction.Status status) {
        return reserve(
                transactionNo,
                orderNo,
                null,
                PaymentTransactionReservation.UsageType.PAYMENT,
                provider,
                amount,
                status);
    }

    public boolean reserveRefundTransaction(String transactionNo, String orderNo, String refundNo, BigDecimal amount) {
        return reserve(
                transactionNo,
                orderNo,
                refundNo,
                PaymentTransactionReservation.UsageType.REFUND,
                "MANUAL_REFUND",
                amount,
                PaymentTransaction.Status.REFUNDED);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentTransactionReservation> findReservation(String transactionNo) {
        return reservationRepository.findById(transactionNo);
    }

    private boolean reserve(
            String transactionNo,
            String orderNo,
            String referenceNo,
            PaymentTransactionReservation.UsageType usageType,
            String provider,
            BigDecimal amount,
            PaymentTransaction.Status status) {
        PaymentTransactionReservation reservation = new PaymentTransactionReservation();
        reservation.setTransactionNo(transactionNo);
        reservation.setOrderNo(orderNo);
        reservation.setReferenceNo(referenceNo);
        reservation.setUsageType(usageType);
        reservation.setProvider(provider);
        reservation.setAmount(amount);
        reservation.setStatus(status);
        try {
            return Boolean.TRUE.equals(requiresNewTransaction.execute(transactionStatus ->
                    reservationRepository.insertIgnore(
                            reservation.getTransactionNo(),
                            reservation.getOrderNo(),
                            reservation.getReferenceNo(),
                            reservation.getUsageType().name(),
                            reservation.getProvider(),
                            reservation.getAmount(),
                            reservation.getStatus().name()) == 1));
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }
}
