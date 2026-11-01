package com.tibet.tourism.common.security.antibot.infra;
import com.tibet.tourism.common.security.antibot.domain.BehaviorLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BehaviorLogRepository extends JpaRepository<BehaviorLog, Long> {

    @Modifying(flushAutomatically = true)
    @Query("delete from BehaviorLog behaviorLog where behaviorLog.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying(flushAutomatically = true)
    @Query("delete from BehaviorLog behaviorLog where behaviorLog.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
