package com.tibet.tourism.modules.order.application;
import com.tibet.tourism.modules.order.domain.OrderAuditLog;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import com.tibet.tourism.modules.order.infra.OrderAuditLogRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentCallbackAuditService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCallbackAuditService.class);

    private final OrderAuditLogRepository orderAuditLogRepository;
    private final PlatformOrderRepository orderRepository;
    private final TransactionTemplate requiresNewTransaction;

    public PaymentCallbackAuditService(OrderAuditLogRepository orderAuditLogRepository,
                                       PlatformOrderRepository orderRepository,
                                       PlatformTransactionManager transactionManager) {
        this.orderAuditLogRepository = orderAuditLogRepository;
        this.orderRepository = orderRepository;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Records a rejected payment callback in its own transaction so the audit row survives the caller's
     * rollback.
     *
     * <p>Callers reach this method while holding a {@code SELECT ... FOR UPDATE} lock on the order row
     * (see {@code PlatformOrderRepository#findByOrderNoForUpdate}). {@code order_audit_logs} has a foreign
     * key to {@code orders(id)}, so an independent transaction inserting the audit row now would need a
     * shared lock on that exclusively locked parent row. The outer transaction waits in application code
     * rather than on a lock, so InnoDB sees no cycle to break and the insert would stall for the full
     * {@code innodb_lock_wait_timeout}. The write is therefore deferred until the outer transaction has
     * completed and released its locks.
     */
    public void recordRejectedCallback(PlatformOrder order,
                                       PaymentTransaction transaction,
                                       String action,
                                       String note) {
        Long orderId = order == null ? null : order.getId();
        String status = statusName(order);

        if (orderId == null) {
            requiresNewTransaction.executeWithoutResult(ignored -> saveAuditLog(order, action, status, note));
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int completionStatus) {
                    try {
                        writeDetachedAuditLog(orderId, action, status, note);
                    } catch (RuntimeException exception) {
                        logger.warn("Deferred rejected-callback audit failed for action={}, reason={}",
                                action, exception.getClass().getSimpleName());
                    }
                }
            });
            return;
        }

        writeDetachedAuditLog(orderId, action, status, note);
    }

    private void writeDetachedAuditLog(Long orderId, String action, String status, String note) {
        requiresNewTransaction.executeWithoutResult(ignored ->
                saveAuditLog(orderRepository.getReferenceById(orderId), action, status, note));
    }

    private void saveAuditLog(PlatformOrder auditOrder, String action, String status, String note) {
        OrderAuditLog auditLog = new OrderAuditLog();
        auditLog.setOrder(auditOrder);
        auditLog.setAction(action);
        auditLog.setFromStatus(status);
        auditLog.setToStatus(status);
        auditLog.setNote(note);
        orderAuditLogRepository.save(auditLog);
    }

    private String statusName(PlatformOrder order) {
        if (order == null || order.getStatus() == null) {
            return null;
        }
        return order.getStatus().name();
    }
}
