package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.InventoryLock;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLockRepository extends JpaRepository<InventoryLock, Long> {
    List<InventoryLock> findByOrder(PlatformOrder order);
}
