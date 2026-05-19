package com.tibet.tourism.repository;

import com.tibet.tourism.entity.InventoryLock;
import com.tibet.tourism.entity.PlatformOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLockRepository extends JpaRepository<InventoryLock, Long> {
    List<InventoryLock> findByOrder(PlatformOrder order);
}
