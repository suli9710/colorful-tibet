package com.tibet.tourism.modules.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateRequest;
import com.tibet.tourism.modules.user.domain.User;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiRouteGenerationJobService {

    private static final Logger log = LoggerFactory.getLogger(AiRouteGenerationJobService.class);
    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private static final int RUNNING_RECORD_PERSIST_DELTA_CHARS = 1200;
    private static final String START_LOCK_PREFIX = "ai:route-job:start:";
    private static final int START_LOCK_RECORD_LOOKUP_ATTEMPTS = 4;
    private static final Duration START_LOCK_RECORD_LOOKUP_DELAY = Duration.ofMillis(50);
    private static final RedisScript<Long> RELEASE_START_LOCK_SCRIPT = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final AiRouteService aiRouteService;
    private final AiQuotaService aiQuotaService;
    private final AiRouteRecordService aiRouteRecordService;
    private final Executor taskExecutor;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, RouteJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, String> activeJobByCacheKey = new ConcurrentHashMap<>();

    @Value("${app.ai.route-jobs.retention-minutes:60}")
    private long jobRetentionMinutes = 60;

    @Value("${app.ai.route-jobs.max-retained:1000}")
    private int maxRetainedJobs = 1000;

    @Value("${app.ai.route-jobs.stale-running-minutes:120}")
    private long staleRunningMinutes = 120;

    @Value("${app.ai.route-jobs.start-lock-ttl-seconds:1800}")
    private long startLockTtlSeconds = 1800;

    @Autowired
    public AiRouteGenerationJobService(AiRouteService aiRouteService,
                                       AiQuotaService aiQuotaService,
                                       AiRouteRecordService aiRouteRecordService,
                                       @Qualifier("taskExecutor") Executor taskExecutor,
                                       ObjectMapper objectMapper,
                                       ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.aiRouteService = aiRouteService;
        this.aiQuotaService = aiQuotaService;
        this.aiRouteRecordService = aiRouteRecordService;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable();
    }

    AiRouteGenerationJobService(AiRouteService aiRouteService,
                                AiQuotaService aiQuotaService,
                                AiRouteRecordService aiRouteRecordService,
                                Executor taskExecutor,
                                ObjectMapper objectMapper) {
        this(aiRouteService, aiQuotaService, aiRouteRecordService, taskExecutor, objectMapper, null);
    }

    public AiRouteJobSnapshot startJob(AiRouteGenerateRequest request, User currentUser, String locale) {
        AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
        long userId = currentUser.getId();
        int days = normalizeDays(safeRequest.getDays());
        String budget = normalizeKey(safeRequest.getBudget());
        String preference = normalizeKey(safeRequest.getPreference());
        String safeLocale = normalizeLocale(locale);
        String cacheKey = aiQuotaService.buildCacheKey(userId, days, budget, preference, safeLocale);
        cleanupJobs();

        String cached = aiQuotaService.getCachedRoute(cacheKey);
        if (cached != null && !cached.isBlank()) {
            String normalizedCached = aiRouteService.normalizeCachedRoute(cached, days, budget, preference, safeLocale);
            if (!normalizedCached.isBlank()) {
                if (!normalizedCached.equals(cached.trim())) {
                    aiQuotaService.cacheRoute(cacheKey, normalizedCached);
                }
                AiRouteRecord record = aiRouteRecordService.recordCompletedRoute(
                        currentUser, null, days, budget, preference, safeLocale, normalizedCached);
                RouteJob cachedJob = RouteJob.completed(
                        userId, cacheKey, days, budget, preference, safeLocale,
                        normalizedCached, true, record.getId());
                jobs.put(cachedJob.jobId, cachedJob);
                return cachedJob.snapshot();
            }
            log.warn("Ignoring invalid cached AI route content: user={}, days={}, budgetProvided={}, preferenceProvided={}",
                    AiLogPrivacy.userRef(userId), days, AiLogPrivacy.hasText(budget), AiLogPrivacy.hasText(preference));
        }

        RouteJob localActiveJob = findLocalActiveJob(cacheKey, userId);
        if (localActiveJob != null) {
            return localActiveJob.snapshot();
        }

        String candidateJobId = UUID.randomUUID().toString();
        StartLockAttempt startLockAttempt = acquireDistributedStartLock(cacheKey, candidateJobId);
        if (startLockAttempt.state() == StartLockState.HELD) {
            return snapshotForDistributedActiveJob(
                    startLockAttempt.ownerJobId(), currentUser, userId, cacheKey,
                    days, budget, preference, safeLocale);
        }

        StartLockHandle startLock = startLockAttempt.lock();
        boolean releaseStartLockOnExit = startLock != null;
        RouteJob job = null;
        try {
            synchronized (activeJobByCacheKey) {
                RouteJob activeJob = findLocalActiveJobLocked(cacheKey, userId);
                if (activeJob != null) {
                    return activeJob.snapshot();
                }

                AiQuotaService.QuotaConsumptionResult quota = aiQuotaService.tryConsumeQuota(userId);
                if (!quota.allowed()) {
                    throw new AiRouteQuotaExceededException("AI route generation quota exceeded",
                            quota.remaining());
                }

                job = RouteJob.running(candidateJobId, userId, cacheKey, days, budget, preference, safeLocale);
                AiRouteRecord record = aiRouteRecordService.createRunningRecord(
                        currentUser, job.jobId, days, budget, preference, safeLocale);
                job.setRouteRecordId(record.getId());
                if (startLock != null) {
                    job.setStartLock(startLock);
                    releaseStartLockOnExit = false;
                }
                jobs.put(job.jobId, job);
                activeJobByCacheKey.put(cacheKey, job.jobId);
            }

            RouteJob submittedJob = job;
            taskExecutor.execute(() -> runJob(submittedJob, currentUser, safeLocale));
            return job.snapshot();
        } catch (RuntimeException e) {
            if (job != null) {
                jobs.remove(job.jobId, job);
                activeJobByCacheKey.remove(job.cacheKey, job.jobId);
                job.fail("AI route generation failed to start");
                recordFailedRoute(job, currentUser, "AI route generation failed to start");
                releaseStartLock(job);
            }
            throw e;
        } finally {
            if (releaseStartLockOnExit) {
                releaseStartLock(startLock);
            }
        }
    }

    public AiRouteJobSnapshot getJob(String jobId, User currentUser) {
        cleanupJobs();
        RouteJob job = requireOwnedJob(jobId, currentUser);
        return job.snapshot();
    }

    public SseEmitter streamJob(String jobId, User currentUser) {
        cleanupJobs();
        RouteJob job = requireOwnedJob(jobId, currentUser);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        if (job.isTerminal()) {
            sendEvent(emitter, "snapshot", job.snapshot());
            sendTerminalEvents(emitter, job);
            return emitter;
        }

        job.subscribers.add(emitter);
        emitter.onCompletion(() -> job.subscribers.remove(emitter));
        emitter.onTimeout(() -> job.subscribers.remove(emitter));
        emitter.onError(error -> job.subscribers.remove(emitter));
        sendEvent(emitter, "snapshot", job.snapshot());
        if (job.isTerminal()) {
            job.subscribers.remove(emitter);
            sendTerminalEvents(emitter, job);
        }
        return emitter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupStaleRunningRecordsOnStartup() {
        Duration staleTtl = Duration.ofMinutes(Math.max(0, staleRunningMinutes));
        try {
            int recovered = aiRouteRecordService.failStaleRunningRecords(staleTtl);
            if (recovered > 0) {
                log.info("Recovered {} stale AI route RUNNING records on startup", recovered);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to recover stale AI route RUNNING records on startup: {}",
                    AiLogPrivacy.exceptionSummary(e));
        }
    }

    private RouteJob findLocalActiveJob(String cacheKey, long userId) {
        synchronized (activeJobByCacheKey) {
            return findLocalActiveJobLocked(cacheKey, userId);
        }
    }

    private RouteJob findLocalActiveJobLocked(String cacheKey, long userId) {
        String activeJobId = activeJobByCacheKey.get(cacheKey);
        if (activeJobId == null) {
            return null;
        }
        RouteJob activeJob = jobs.get(activeJobId);
        if (activeJob != null && activeJob.userId == userId && activeJob.isRunning()) {
            return activeJob;
        }
        activeJobByCacheKey.remove(cacheKey, activeJobId);
        return null;
    }

    private StartLockAttempt acquireDistributedStartLock(String cacheKey, String candidateJobId) {
        if (redisTemplate == null) {
            return StartLockAttempt.unavailable();
        }

        String lockKey = startLockKey(cacheKey);
        Duration ttl = Duration.ofSeconds(Math.max(60, startLockTtlSeconds));
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, candidateJobId, ttl);
            if (Boolean.TRUE.equals(acquired)) {
                return StartLockAttempt.acquired(new StartLockHandle(lockKey, candidateJobId));
            }
            return StartLockAttempt.held(readLockOwner(lockKey));
        } catch (RuntimeException e) {
            log.warn("Redis AI route start lock unavailable, using local fallback: {}",
                    AiLogPrivacy.exceptionSummary(e));
            return StartLockAttempt.unavailable();
        }
    }

    private String readLockOwner(String lockKey) {
        try {
            return redisTemplate.opsForValue().get(lockKey);
        } catch (RuntimeException e) {
            log.warn("Could not read AI route start lock owner; preserving duplicate-start guard: {}",
                    AiLogPrivacy.exceptionSummary(e));
            return null;
        }
    }

    private AiRouteJobSnapshot snapshotForDistributedActiveJob(String ownerJobId, User currentUser, long userId,
                                                               String cacheKey, int days, String budget,
                                                               String preference, String locale) {
        RouteJob localActiveJob = findLocalActiveJob(cacheKey, userId);
        if (localActiveJob != null) {
            return localActiveJob.snapshot();
        }

        if (!StringUtils.hasText(ownerJobId)) {
            throw new IllegalStateException("AI route generation already in progress; retry shortly");
        }

        for (int attempt = 0; attempt < START_LOCK_RECORD_LOOKUP_ATTEMPTS; attempt++) {
            RouteJob recordBacked = aiRouteRecordService.findJobRecord(userId, ownerJobId)
                    .map(record -> recordBackedJob(record, userId))
                    .orElse(null);
            if (recordBacked != null) {
                return recordBacked.snapshot();
            }
            pauseBeforeRecordRetry(attempt);
        }

        log.info("Returning pending AI route job snapshot for distributed lock owner: jobId={}, user={}",
                ownerJobId, AiLogPrivacy.userRef(currentUser == null ? userId : currentUser.getId()));
        return RouteJob.running(ownerJobId, userId, cacheKey, days, budget, preference, locale).snapshot();
    }

    private void pauseBeforeRecordRetry(int attempt) {
        if (attempt >= START_LOCK_RECORD_LOOKUP_ATTEMPTS - 1) {
            return;
        }
        try {
            Thread.sleep(START_LOCK_RECORD_LOOKUP_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runJob(RouteJob job, User currentUser, String locale) {
        try {
            String streamedContent = aiRouteService.streamRouteToListener(
                    job.days,
                    job.budget,
                    job.preference,
                    currentUser,
                    locale,
                    new AiRouteService.RouteStreamListener() {
                        @Override
                        public void onDelta(String text) {
                            if (text == null || text.isEmpty()) {
                                return;
                            }
                            job.appendContent(text);
                            notifyDeltaSubscribers(job, text);
                            persistRunningContentIfDue(job);
                        }

                        @Override
                        public void onReplace(String content) {
                            if (content == null) {
                                return;
                            }
                            job.replaceContent(content);
                            notifyReplaceSubscribers(job, content);
                            persistRunningContent(job, content);
                        }

                        @Override
                        public void onDone(String content) {
                            if (content != null && !content.isBlank()) {
                                job.replaceContent(content);
                            }
                        }
                    });

            String content = job.contentText().trim();
            if (content.isBlank() && streamedContent != null) {
                content = streamedContent.trim();
            }
            if (content.isBlank()) {
                job.fail("AI route generation returned empty content");
                recordFailedRoute(job, currentUser, "AI route generation returned empty content");
                notifySubscribers(job);
                return;
            }

            aiQuotaService.cacheRoute(job.cacheKey, content);
            recordCompletedRoute(job, currentUser, content);
            job.complete(content);
            notifySubscribers(job);
        } catch (Exception e) {
            log.warn("AI route job failed: jobId={}, user={}, days={}, reason={}",
                    job.jobId, AiLogPrivacy.userRef(job.userId), job.days, AiLogPrivacy.exceptionSummary(e));
            job.fail("AI route generation failed");
            recordFailedRoute(job, currentUser, "AI route generation failed");
            notifySubscribers(job);
        } finally {
            activeJobByCacheKey.remove(job.cacheKey, job.jobId);
            releaseStartLock(job);
            cleanupJobs();
        }
    }

    private void releaseStartLock(RouteJob job) {
        if (job == null) {
            return;
        }
        StartLockHandle startLock = job.clearStartLock();
        releaseStartLock(startLock);
    }

    private void releaseStartLock(StartLockHandle startLock) {
        if (startLock == null || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.execute(RELEASE_START_LOCK_SCRIPT, List.of(startLock.lockKey()), startLock.token());
        } catch (RuntimeException e) {
            log.warn("Failed to release AI route start lock; ttl will expire it: {}",
                    AiLogPrivacy.exceptionSummary(e));
        }
    }

    private String startLockKey(String cacheKey) {
        return START_LOCK_PREFIX + cacheKey;
    }

    private void cleanupJobs() {
        Instant now = Instant.now();
        Duration terminalRetention = Duration.ofMinutes(Math.max(0, jobRetentionMinutes));
        Duration staleRunningRetention = Duration.ofMinutes(Math.max(0, staleRunningMinutes));

        jobs.entrySet().removeIf(entry -> shouldRemoveJob(entry.getValue(), now, terminalRetention, staleRunningRetention));
        cleanupActiveJobIndex();

        int maxJobs = Math.max(0, maxRetainedJobs);
        if (jobs.size() <= maxJobs) {
            return;
        }

        List<RouteJob> terminalJobs = jobs.values().stream()
                .filter(RouteJob::isTerminal)
                .sorted(Comparator.comparing(job -> job.updatedAt))
                .toList();
        for (RouteJob job : terminalJobs) {
            if (jobs.size() <= maxJobs) {
                break;
            }
            jobs.remove(job.jobId, job);
        }
        cleanupActiveJobIndex();
    }

    private boolean shouldRemoveJob(RouteJob job, Instant now, Duration terminalRetention, Duration staleRunningRetention) {
        if (job.isTerminal()) {
            return !job.updatedAt.plus(terminalRetention).isAfter(now);
        }
        return !job.updatedAt.plus(staleRunningRetention).isAfter(now);
    }

    private void cleanupActiveJobIndex() {
        activeJobByCacheKey.entrySet().removeIf(entry -> {
            RouteJob job = jobs.get(entry.getValue());
            return job == null || !job.isRunning();
        });
    }

    private RouteJob requireOwnedJob(String jobId, User currentUser) {
        RouteJob job = jobs.get(jobId);
        if (job != null && currentUser != null && currentUser.getId() != null
                && job.userId == currentUser.getId().longValue()) {
            return job;
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("AI route job not found");
        }
        return aiRouteRecordService.findJobRecord(currentUser.getId(), jobId)
                .map(record -> recordBackedJob(record, currentUser.getId()))
                .orElseThrow(() -> new IllegalArgumentException("AI route job not found"));
    }

    private RouteJob recordBackedJob(AiRouteRecord record, long userId) {
        RouteJob job = new RouteJob(
                record.getJobId(),
                userId,
                "",
                safeDays(record.getDays()),
                record.getBudget(),
                record.getPreference(),
                record.getLocale(),
                toJobStatus(record.getStatus()),
                record.getContent(),
                false,
                record.getId(),
                toInstant(record.getCreatedAt()),
                toInstant(record.getUpdatedAt()));
        job.errorMessage = record.getErrorMessage();
        job.markContentPersisted();
        return job;
    }

    private static RouteJobStatus toJobStatus(AiRouteRecord.Status status) {
        if (status == AiRouteRecord.Status.COMPLETED) {
            return RouteJobStatus.COMPLETED;
        }
        if (status == AiRouteRecord.Status.FAILED) {
            return RouteJobStatus.FAILED;
        }
        return RouteJobStatus.RUNNING;
    }

    private static int safeDays(Integer days) {
        return days == null ? 5 : Math.max(1, days);
    }

    private static Instant toInstant(LocalDateTime value) {
        return (value == null ? LocalDateTime.now() : value)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    private void persistRunningContentIfDue(RouteJob job) {
        String content = job.contentForThrottledPersist(RUNNING_RECORD_PERSIST_DELTA_CHARS);
        if (content != null) {
            persistRunningContent(job, content);
        }
    }

    private void persistRunningContent(RouteJob job, String content) {
        try {
            aiRouteRecordService.updateRunningContent(job.userId, job.jobId, content);
            job.markContentPersisted();
        } catch (Exception e) {
            log.debug("Failed to persist running AI route record: jobId={}, reason={}",
                    job.jobId, AiLogPrivacy.exceptionSummary(e));
        }
    }

    private void recordCompletedRoute(RouteJob job, User currentUser, String content) {
        try {
            AiRouteRecord record = aiRouteRecordService.recordCompletedRoute(
                    currentUser, job.jobId, job.days, job.budget, job.preference, job.locale, content);
            job.setRouteRecordId(record.getId());
            job.markContentPersisted();
        } catch (Exception e) {
            log.warn("Failed to persist completed AI route record: jobId={}, user={}, reason={}",
                    job.jobId, AiLogPrivacy.userRef(job.userId), AiLogPrivacy.exceptionSummary(e));
        }
    }

    private void recordFailedRoute(RouteJob job, User currentUser, String message) {
        try {
            aiRouteRecordService.recordFailedRoute(
                    currentUser, job.jobId, job.days, job.budget, job.preference, job.locale, message);
        } catch (Exception e) {
            log.debug("Failed to persist failed AI route record: jobId={}, reason={}",
                    job.jobId, AiLogPrivacy.exceptionSummary(e));
        }
    }

    private void notifySubscribers(RouteJob job) {
        for (SseEmitter emitter : job.subscribers) {
            sendEvent(emitter, "snapshot", job.snapshot());
            if (job.isTerminal()) {
                sendTerminalEvents(emitter, job);
            }
        }
        if (job.isTerminal()) {
            job.subscribers.clear();
        }
    }

    private void notifyDeltaSubscribers(RouteJob job, String text) {
        for (SseEmitter emitter : job.subscribers) {
            sendEvent(emitter, "delta", Map.of("text", text));
        }
    }

    private void notifyReplaceSubscribers(RouteJob job, String content) {
        for (SseEmitter emitter : job.subscribers) {
            sendEvent(emitter, "replace", Map.of("content", content, "text", content));
        }
    }

    private void sendTerminalEvents(SseEmitter emitter, RouteJob job) {
        if (job.status == RouteJobStatus.COMPLETED) {
            sendEvent(emitter, "done", Map.of("content", job.content.toString()));
        } else if (job.status == RouteJobStatus.FAILED) {
            sendEvent(emitter, "error", Map.of("message", job.errorMessage));
        }
        emitter.complete();
    }

    private void sendEvent(SseEmitter emitter, String type, Object payload) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            if (payload instanceof Map<?, ?> map) {
                map.forEach((key, value) -> event.put(String.valueOf(key), value));
            } else {
                event.put("payload", payload);
            }
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (IOException e) {
            log.debug("Failed to send AI route job SSE event type={}: {}", type, AiLogPrivacy.exceptionSummary(e));
        }
    }

    private static int normalizeDays(Integer days) {
        return Math.max(1, Math.min(15, days == null ? 5 : days));
    }

    private static String normalizeKey(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toLowerCase();
    }

    private static String normalizeLocale(String value) {
        return value == null || value.isBlank() ? "zh" : value.trim().toLowerCase();
    }

    private enum RouteJobStatus {
        RUNNING,
        COMPLETED,
        FAILED
    }

    private enum StartLockState {
        ACQUIRED,
        HELD,
        UNAVAILABLE
    }

    private record StartLockHandle(String lockKey, String token) {
    }

    private record StartLockAttempt(StartLockState state, StartLockHandle lock, String ownerJobId) {
        static StartLockAttempt acquired(StartLockHandle lock) {
            return new StartLockAttempt(StartLockState.ACQUIRED, lock, null);
        }

        static StartLockAttempt held(String ownerJobId) {
            return new StartLockAttempt(StartLockState.HELD, null, ownerJobId);
        }

        static StartLockAttempt unavailable() {
            return new StartLockAttempt(StartLockState.UNAVAILABLE, null, null);
        }
    }

    private static final class RouteJob {
        private final String jobId;
        private final long userId;
        private final String cacheKey;
        private final int days;
        private final String budget;
        private final String preference;
        private final String locale;
        private final StringBuilder content = new StringBuilder();
        private final boolean cached;
        private final Instant createdAt;
        private final CopyOnWriteArrayList<SseEmitter> subscribers = new CopyOnWriteArrayList<>();
        private volatile Long routeRecordId;
        private volatile RouteJobStatus status;
        private volatile String errorMessage;
        private volatile Instant updatedAt;
        private volatile StartLockHandle startLock;
        private int persistedContentLength;

        private RouteJob(long userId, String cacheKey, int days, String budget, String preference,
                         String locale, RouteJobStatus status, String content, boolean cached, Long routeRecordId) {
            this(UUID.randomUUID().toString(), userId, cacheKey, days, budget, preference, locale, status,
                    content, cached, routeRecordId, Instant.now(), Instant.now());
        }

        private RouteJob(String jobId, long userId, String cacheKey, int days, String budget, String preference,
                         String locale, RouteJobStatus status, String content, boolean cached, Long routeRecordId,
                         Instant createdAt, Instant updatedAt) {
            this.jobId = jobId;
            this.userId = userId;
            this.cacheKey = cacheKey;
            this.days = days;
            this.budget = budget;
            this.preference = preference;
            this.locale = locale;
            this.status = status;
            this.cached = cached;
            this.routeRecordId = routeRecordId;
            this.createdAt = createdAt == null ? Instant.now() : createdAt;
            this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
            if (content != null && !content.isBlank()) {
                this.content.append(content);
                this.persistedContentLength = content.length();
            }
        }

        static RouteJob running(long userId, String cacheKey, int days, String budget, String preference, String locale) {
            return new RouteJob(userId, cacheKey, days, budget, preference, locale, RouteJobStatus.RUNNING, "", false, null);
        }

        static RouteJob running(String jobId, long userId, String cacheKey, int days,
                                String budget, String preference, String locale) {
            return new RouteJob(jobId, userId, cacheKey, days, budget, preference, locale,
                    RouteJobStatus.RUNNING, "", false, null, Instant.now(), Instant.now());
        }

        static RouteJob completed(long userId, String cacheKey, int days, String budget, String preference,
                                  String locale, String content, boolean cached, Long routeRecordId) {
            return new RouteJob(userId, cacheKey, days, budget, preference, locale, RouteJobStatus.COMPLETED,
                    content, cached, routeRecordId);
        }

        void setRouteRecordId(Long routeRecordId) {
            this.routeRecordId = routeRecordId;
        }

        void setStartLock(StartLockHandle startLock) {
            this.startLock = startLock;
        }

        StartLockHandle clearStartLock() {
            StartLockHandle current = startLock;
            startLock = null;
            return current;
        }

        synchronized void complete(String newContent) {
            content.setLength(0);
            content.append(newContent);
            status = RouteJobStatus.COMPLETED;
            errorMessage = null;
            updatedAt = Instant.now();
            persistedContentLength = content.length();
        }

        synchronized void appendContent(String text) {
            content.append(text);
            updatedAt = Instant.now();
        }

        synchronized void replaceContent(String newContent) {
            content.setLength(0);
            content.append(newContent);
            updatedAt = Instant.now();
        }

        synchronized void fail(String message) {
            status = RouteJobStatus.FAILED;
            errorMessage = message;
            updatedAt = Instant.now();
        }

        boolean isRunning() {
            return status == RouteJobStatus.RUNNING;
        }

        boolean isTerminal() {
            return status == RouteJobStatus.COMPLETED || status == RouteJobStatus.FAILED;
        }

        synchronized String contentText() {
            return content.toString();
        }

        synchronized String contentForThrottledPersist(int minDeltaChars) {
            if (content.length() == 0 || content.length() - persistedContentLength < minDeltaChars) {
                return null;
            }
            return content.toString();
        }

        synchronized void markContentPersisted() {
            persistedContentLength = content.length();
        }

        synchronized AiRouteJobSnapshot snapshot() {
            return new AiRouteJobSnapshot(
                    jobId,
                    routeRecordId,
                    status.name(),
                    content.toString(),
                    errorMessage,
                    days,
                    budget,
                    preference,
                    cached,
                    createdAt,
                    updatedAt
            );
        }
    }
}
