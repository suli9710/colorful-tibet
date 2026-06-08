package com.tibet.tourism.modules.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateRequest;
import com.tibet.tourism.modules.user.domain.User;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiRouteGenerationJobService {

    private static final Logger log = LoggerFactory.getLogger(AiRouteGenerationJobService.class);
    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private static final int RUNNING_RECORD_PERSIST_DELTA_CHARS = 1200;

    private final AiRouteService aiRouteService;
    private final AiQuotaService aiQuotaService;
    private final AiRouteRecordService aiRouteRecordService;
    private final Executor taskExecutor;
    private final ObjectMapper objectMapper;
    private final Map<String, RouteJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, String> activeJobByCacheKey = new ConcurrentHashMap<>();

    @Value("${app.ai.route-jobs.retention-minutes:60}")
    private long jobRetentionMinutes = 60;

    @Value("${app.ai.route-jobs.max-retained:1000}")
    private int maxRetainedJobs = 1000;

    @Value("${app.ai.route-jobs.stale-running-minutes:120}")
    private long staleRunningMinutes = 120;

    public AiRouteGenerationJobService(AiRouteService aiRouteService,
                                       AiQuotaService aiQuotaService,
                                       AiRouteRecordService aiRouteRecordService,
                                       @Qualifier("taskExecutor") Executor taskExecutor,
                                       ObjectMapper objectMapper) {
        this.aiRouteService = aiRouteService;
        this.aiQuotaService = aiQuotaService;
        this.aiRouteRecordService = aiRouteRecordService;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
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

        String activeJobId = activeJobByCacheKey.get(cacheKey);
        if (activeJobId != null) {
            RouteJob activeJob = jobs.get(activeJobId);
            if (activeJob != null && activeJob.userId == userId && activeJob.isRunning()) {
                return activeJob.snapshot();
            }
            activeJobByCacheKey.remove(cacheKey, activeJobId);
        }

        AiQuotaService.QuotaConsumptionResult quota = aiQuotaService.tryConsumeQuota(userId);
        if (!quota.allowed()) {
            throw new AiRouteQuotaExceededException("AI route generation quota exceeded",
                    quota.remaining());
        }

        RouteJob job = RouteJob.running(userId, cacheKey, days, budget, preference, safeLocale);
        AiRouteRecord record = aiRouteRecordService.createRunningRecord(
                currentUser, job.jobId, days, budget, preference, safeLocale);
        job.setRouteRecordId(record.getId());
        jobs.put(job.jobId, job);
        activeJobByCacheKey.put(cacheKey, job.jobId);

        taskExecutor.execute(() -> runJob(job, currentUser, safeLocale));
        return job.snapshot();
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
            cleanupJobs();
        }
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
        if (job == null || currentUser == null || currentUser.getId() == null || job.userId != currentUser.getId().longValue()) {
            throw new IllegalArgumentException("AI route job not found");
        }
        return job;
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
        private int persistedContentLength;

        private RouteJob(long userId, String cacheKey, int days, String budget, String preference,
                         String locale, RouteJobStatus status, String content, boolean cached, Long routeRecordId) {
            this.jobId = UUID.randomUUID().toString();
            this.userId = userId;
            this.cacheKey = cacheKey;
            this.days = days;
            this.budget = budget;
            this.preference = preference;
            this.locale = locale;
            this.status = status;
            this.cached = cached;
            this.routeRecordId = routeRecordId;
            this.createdAt = Instant.now();
            this.updatedAt = this.createdAt;
            if (content != null && !content.isBlank()) {
                this.content.append(content);
                this.persistedContentLength = content.length();
            }
        }

        static RouteJob running(long userId, String cacheKey, int days, String budget, String preference, String locale) {
            return new RouteJob(userId, cacheKey, days, budget, preference, locale, RouteJobStatus.RUNNING, "", false, null);
        }

        static RouteJob completed(long userId, String cacheKey, int days, String budget, String preference,
                                  String locale, String content, boolean cached, Long routeRecordId) {
            return new RouteJob(userId, cacheKey, days, budget, preference, locale, RouteJobStatus.COMPLETED,
                    content, cached, routeRecordId);
        }

        void setRouteRecordId(Long routeRecordId) {
            this.routeRecordId = routeRecordId;
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
