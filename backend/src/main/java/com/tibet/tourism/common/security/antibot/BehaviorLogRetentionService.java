package com.tibet.tourism.common.security.antibot;

import com.tibet.tourism.common.security.antibot.infra.BehaviorLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehaviorLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorLogRetentionService.class);

    private final BehaviorLogRepository behaviorLogRepository;
    private final AntibotProperties properties;
    private final Clock clock;

    @Autowired
    public BehaviorLogRetentionService(
            BehaviorLogRepository behaviorLogRepository,
            AntibotProperties properties) {
        this(behaviorLogRepository, properties, Clock.systemDefaultZone());
    }

    BehaviorLogRetentionService(
            BehaviorLogRepository behaviorLogRepository,
            AntibotProperties properties,
            Clock clock) {
        this.behaviorLogRepository = behaviorLogRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${app.security.antibot.retention.cleanup-cron:0 15 4 * * *}",
            zone = "${app.security.antibot.retention.cleanup-zone:UTC}")
    @Transactional
    public int purgeExpiredLogs() {
        AntibotProperties.Retention retention = properties.getRetention();
        if (retention == null || !retention.isEnabled()) {
            return 0;
        }

        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retention.getDays());
        int deleted = behaviorLogRepository.deleteCreatedBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged expired anti-bot behavior logs: count={}, retentionDays={}",
                    deleted, retention.getDays());
        }
        return deleted;
    }
}
