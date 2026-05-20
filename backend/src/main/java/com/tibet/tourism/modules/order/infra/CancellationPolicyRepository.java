package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.CancellationPolicy;
import com.tibet.tourism.modules.order.domain.OrderItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {
    Optional<CancellationPolicy> findFirstByProductTypeAndActiveTrueOrderByPriorityDesc(OrderItem.ProductType productType);
}
