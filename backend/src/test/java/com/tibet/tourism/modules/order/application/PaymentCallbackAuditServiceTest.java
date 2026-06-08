package com.tibet.tourism.modules.order.application;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.order.domain.OrderAuditLog;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import com.tibet.tourism.modules.order.infra.OrderAuditLogRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackAuditServiceTest {

    @Mock private OrderAuditLogRepository orderAuditLogRepository;
    @Mock private PlatformOrderRepository orderRepository;

    private PaymentCallbackAuditService service;

    @BeforeEach
    void setUp() {
        service = new PaymentCallbackAuditService(orderAuditLogRepository, orderRepository);
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
    void recordRejectedCallbackUsesIndependentTransaction() throws Exception {
        Method method = PaymentCallbackAuditService.class.getMethod(
                "recordRejectedCallback",
                PlatformOrder.class,
                PaymentTransaction.class,
                String.class,
                String.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertTrue(transactional != null);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
