package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.RefundOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundOrderRepository extends JpaRepository<RefundOrder, Long> {
}
