package com.tibet.tourism.modules.order.application;
import com.tibet.tourism.modules.order.domain.OrderAuditLog;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import com.tibet.tourism.modules.order.infra.OrderAuditLogRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCallbackAuditService {

    private final OrderAuditLogRepository orderAuditLogRepository;
    private final PlatformOrderRepository orderRepository;

    public PaymentCallbackAuditService(OrderAuditLogRepository orderAuditLogRepository,
                                       PlatformOrderRepository orderRepository) {
        this.orderAuditLogRepository = orderAuditLogRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejectedCallback(PlatformOrder order,
                                       PaymentTransaction transaction,
                                       String action,
                                       String note) {
        PlatformOrder auditOrder = order.getId() == null ? order : orderRepository.getReferenceById(order.getId());

        OrderAuditLog auditLog = new OrderAuditLog();
        auditLog.setOrder(auditOrder);
        auditLog.setAction(action);
        auditLog.setFromStatus(statusName(order));
        auditLog.setToStatus(statusName(order));
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
