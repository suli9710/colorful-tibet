package com.tibet.tourism.repository;

import com.tibet.tourism.entity.CancellationPolicy;
import com.tibet.tourism.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {
    Optional<CancellationPolicy> findFirstByProductTypeAndActiveTrueOrderByPriorityDesc(OrderItem.ProductType productType);
}
