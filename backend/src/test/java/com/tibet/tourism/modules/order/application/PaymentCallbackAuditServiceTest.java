package com.tibet.tourism.modules.order.application;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.order.domain.OrderAuditLog;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import com.tibet.tourism.modules.order.infra.OrderAuditLogRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackAuditServiceTest {

    @Mock private OrderAuditLogRepository orderAuditLogRepository;
    @Mock private PlatformOrderRepository orderRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private PaymentCallbackAuditService service;

    @BeforeEach
    void setUp() {
        service = new PaymentCallbackAuditService(orderAuditLogRepository, orderRepository, transactionManager);
    }

    @Test
    void recordRejectedCallbackSavesAuditLogWithoutOccupyingTransactionNumber() {
        PlatformOrder order = new PlatformOrder();
        order.setId(99L);
        order.setOrderNo("ORD-BAD-SIG");
        order.setStatus(PlatformOrder.Status.PENDING_PAYMENT);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionNo("PAY-BAD-SIG");
        transaction.setProvider("MOCK");
        transaction.setAmount(new BigDecimal("600.00"));
        transaction.setStatus(PaymentTransaction.Status.FAILED);
        transaction.setSignatureValid(false);
        when(orderRepository.getReferenceById(99L)).thenReturn(order);

        service.recordRejectedCallback(
                order,
                transaction,
                "PAYMENT_CALLBACK_REJECTED",
                "Payment callback rejected: invalid signature");

        assertNull(transaction.getOrder());
        assertEquals("PAY-BAD-SIG", transaction.getTransactionNo());
        assertEquals(PaymentTransaction.Status.FAILED, transaction.getStatus());
        assertFalse(transaction.getSignatureValid());

        ArgumentCaptor<OrderAuditLog> auditCaptor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(orderAuditLogRepository).save(auditCaptor.capture());
        OrderAuditLog savedAudit = auditCaptor.getValue();
        assertSame(order, savedAudit.getOrder());
        assertEquals("PAYMENT_CALLBACK_REJECTED", savedAudit.getAction());
        assertEquals("PENDING_PAYMENT", savedAudit.getFromStatus());
        assertEquals("PENDING_PAYMENT", savedAudit.getToStatus());
        assertEquals("Payment callback rejected: invalid signature", savedAudit.getNote());
    }

    @Test
    void recordRejectedCallbackStillAuditsDuplicateTransactionNumber() {
        PlatformOrder order = new PlatformOrder();
        order.setId(99L);
        order.setStatus(PlatformOrder.Status.CONFIRMED);

        PaymentTransaction rejected = new PaymentTransaction();
        rejected.setTransactionNo("PAY-DUP");
        rejected.setStatus(PaymentTransaction.Status.FAILED);
        rejected.setSignatureValid(false);

        when(orderRepository.getReferenceById(99L)).thenReturn(order);

        service.recordRejectedCallback(order, rejected, "PAYMENT_CALLBACK_REJECTED", "Invalid duplicate signature");

        ArgumentCaptor<OrderAuditLog> auditCaptor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(orderAuditLogRepository).save(auditCaptor.capture());
        assertSame(order, auditCaptor.getValue().getOrder());
        assertEquals("PAYMENT_CALLBACK_REJECTED", auditCaptor.getValue().getAction());
        assertEquals("Invalid duplicate signature", auditCaptor.getValue().getNote());
    }

    @Test
    void recordRejectedCallbackDefersWriteUntilCallerReleasesTheOrderRowLock() {
        PlatformOrder order = new PlatformOrder();
        order.setId(99L);
        order.setStatus(PlatformOrder.Status.PENDING_PAYMENT);

        PaymentTransaction rejected = new PaymentTransaction();
        rejected.setTransactionNo("PAY-LOCKED");
        rejected.setStatus(PaymentTransaction.Status.FAILED);

        TransactionSynchronizationManager.initSynchronization();
        List<TransactionSynchronization> synchronizations;
        try {
            service.recordRejectedCallback(
                    order,
                    rejected,
                    "PAYMENT_CALLBACK_REJECTED",
                    "Payment callback rejected while the order row is locked");

            // The caller still holds SELECT ... FOR UPDATE on this order. Inserting the audit row now would
            // need a shared lock on that row for the order_audit_logs foreign key and would stall for the
            // full innodb_lock_wait_timeout, so nothing may be written yet.
            verifyNoInteractions(orderAuditLogRepository);

            synchronizations = List.copyOf(TransactionSynchronizationManager.getSynchronizations());
            assertEquals(1, synchronizations.size());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        when(orderRepository.getReferenceById(99L)).thenReturn(order);
        synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        ArgumentCaptor<OrderAuditLog> auditCaptor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(orderAuditLogRepository).save(auditCaptor.capture());
        assertSame(order, auditCaptor.getValue().getOrder());
        assertEquals("PAYMENT_CALLBACK_REJECTED", auditCaptor.getValue().getAction());
        assertEquals("PENDING_PAYMENT", auditCaptor.getValue().getFromStatus());
    }
}
