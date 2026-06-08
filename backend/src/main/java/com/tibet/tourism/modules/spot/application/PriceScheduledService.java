package com.tibet.tourism.modules.spot.application;

import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PriceScheduledService {

    private static final Logger logger = LoggerFactory.getLogger(PriceScheduledService.class);
    private static final String LOCK_PREFIX = "price-update:scheduled:";
    private static final String LOCAL_LOCK_TOKEN = "local-lock-disabled";
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final PriceUpdateService priceUpdateService;
    private final StringRedisTemplate redisTemplate;

    @Value("${price.update.enabled:true}")
    private boolean priceUpdateEnabled;

    @Value("${price.update.force:false}")
    private boolean forceUpdate;

    @Value("${price.update.lock.enabled:true}")
    private boolean lockEnabled;

    @Value("${price.update.lock.ttl-seconds:1800}")
    private long lockTtlSeconds;

    public PriceScheduledService(PriceUpdateService priceUpdateService,
                                 ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.priceUpdateService = priceUpdateService;
        this.redisTemplate = redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable();
    }

    @Scheduled(cron = "${price.update.cron:0 0 2 * * ?}")
    public void scheduledPriceUpdate() {
        runLockedJob("daily", forceUpdate, "daily price update");
    }

    @Scheduled(cron = "${price.update.weekly.cron:0 0 3 ? * MON}")
    public void weeklyForcePriceUpdate() {
        runLockedJob("weekly", true, "weekly force price update");
    }

    @Scheduled(cron = "${price.update.hourly.cron:0 0 * * * ?}")
    public void hourlyPriceUpdateForMissing() {
        runLockedJob("hourly", false, "hourly missing-price update");
    }

    private void runLockedJob(String lockName, boolean force, String label) {
        if (!priceUpdateEnabled) {
            logger.info("Price update job disabled; skipping {}", label);
            return;
        }

        String lockToken = acquireJobLock(lockName, label);
        if (lockToken == null) {
            return;
        }

        try {
            logger.info("Starting {}", label);
            PriceUpdateService.BatchUpdateResult result = priceUpdateService.batchUpdatePrices(force);
            logger.info("{} completed - success: {}, failed: {}, skipped: {}, total: {}",
                    label,
                    result.getSuccessCount(),
                    result.getFailCount(),
                    result.getSkipCount(),
                    result.getTotalCount());
        } catch (Exception e) {
            logger.error("{} failed: {}", label, SensitiveLogSanitizer.exceptionSummary(e));
        } finally {
            releaseJobLock(lockName, lockToken);
        }
    }

    private String acquireJobLock(String lockName, String label) {
        if (!lockEnabled) {
            return LOCAL_LOCK_TOKEN;
        }
        if (redisTemplate == null) {
            logger.warn("Skipping {} because Redis-backed price job locking is enabled but Redis is unavailable", label);
            return null;
        }

        String token = UUID.randomUUID().toString();
        Duration ttl = Duration.ofSeconds(Math.max(60, lockTtlSeconds));
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey(lockName), token, ttl);
            if (!Boolean.TRUE.equals(acquired)) {
                logger.info("Skipping {} because another instance holds the distributed lock", label);
                return null;
            }
            return token;
        } catch (RuntimeException e) {
            logger.warn("Skipping {} because the distributed lock could not be acquired: {}",
                    label, SensitiveLogSanitizer.exceptionSummary(e));
            return null;
        }
    }

    private void releaseJobLock(String lockName, String lockToken) {
        if (!lockEnabled || LOCAL_LOCK_TOKEN.equals(lockToken) || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey(lockName)), lockToken);
        } catch (RuntimeException e) {
            logger.warn("Failed to release price update lock {}: {}",
                    lockName, SensitiveLogSanitizer.exceptionSummary(e));
        }
    }

    private String lockKey(String lockName) {
        return LOCK_PREFIX + lockName;
    }
}
