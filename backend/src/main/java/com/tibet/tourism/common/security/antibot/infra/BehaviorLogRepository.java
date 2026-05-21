package com.tibet.tourism.common.security.antibot.infra;
import com.tibet.tourism.common.security.antibot.domain.BehaviorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehaviorLogRepository extends JpaRepository<BehaviorLog, Long> {
}
