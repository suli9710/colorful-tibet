package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
