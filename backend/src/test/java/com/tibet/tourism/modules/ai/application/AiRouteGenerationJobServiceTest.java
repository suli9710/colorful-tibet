package com.tibet.tourism.modules.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateRequest;
import com.tibet.tourism.modules.user.domain.User;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class AiRouteGenerationJobServiceTest {

    @Mock
    private AiRouteService aiRouteService;

    @Mock
    private AiQuotaService aiQuotaService;

    @Mock
    private AiRouteRecordService aiRouteRecordService;

    @Test
    void jobSnapshotReceivesStreamedContentBeforeCompletion() throws Exception {
        Executor asyncExecutor = command -> new Thread(command, "ai-route-job-test").start();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                asyncExecutor,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(8);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 8, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(8), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(99L));

        CountDownLatch firstDeltaSent = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDelta("# Tibet");
            firstDeltaSent.countDown();
            if (!allowCompletion.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test did not release stream completion");
            }
            listener.onDelta(" route");
            listener.onDone("# Tibet route");
            return "# Tibet route";
        }).when(aiRouteService).streamRouteToListener(
                eq(8),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(8), eq("comfort"), eq("natural"), eq("zh"), eq("# Tibet route")))
                .thenReturn(record(99L));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("RUNNING", snapshot.status());
        assertEquals(99L, snapshot.routeRecordId());
        assertTrue(firstDeltaSent.await(2, TimeUnit.SECONDS));

        AiRouteJobSnapshot runningSnapshot = service.getJob(snapshot.jobId(), user);
        assertEquals("RUNNING", runningSnapshot.status());
        assertEquals("# Tibet", runningSnapshot.content());

        allowCompletion.countDown();
        AiRouteJobSnapshot completedSnapshot = waitForStatus(service, snapshot.jobId(), user, "COMPLETED");

        assertEquals("# Tibet route", completedSnapshot.content());
        verify(aiQuotaService).cacheRoute("cache-key", "# Tibet route");
    }

    @Test
    void fallbackRouteIsNeverCachedUnderTheRealRouteKey() {
        Executor directExecutor = Runnable::run;
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(108L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), anyString()))
                .thenReturn(record(108L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            // This is what AiRouteService.emitFallbackRoute does when the upstream model is
            // unavailable: it signals the fallback and then emits the canned local itinerary.
            listener.onFallback();
            listener.onDelta("# 本地兜底行程");
            listener.onDone("# 本地兜底行程");
            return "# 本地兜底行程";
        }).when(aiRouteService).streamRouteToListener(
                eq(5), eq("comfort"), eq("natural"), eq(user), eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        // Caching the template under the real route key would serve it to every later request for the
        // same parameters, long after the model recovered.
        verify(aiQuotaService, never()).cacheRoute(anyString(), anyString());
        // ...and the quota unit stays spent: refunding every fallback would make the daily quota
        // unlimited exactly while the model is down.
        verify(aiQuotaService, never()).releaseQuota(anyLong());
    }

    @Test
    void startJobRethrowsAndRollsBackWhenGenerationPoolRejects() {
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("ai generation pool saturated");
        };
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                rejectingExecutor,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(55L));

        // A saturated generation pool must surface as RejectedExecutionException (mapped to HTTP 503
        // by the controller) instead of running the long AI job on the calling Tomcat thread.
        assertThrows(RejectedExecutionException.class, () -> service.startJob(request, user, "zh"));
        // The half-started job is rolled back to a FAILED record rather than left RUNNING.
        verify(aiRouteRecordService).recordFailedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), anyString());
        // The quota unit was taken before the job was queued, so a rejected submission must hand it
        // back; otherwise a transient 503 permanently burns part of the user's daily allowance.
        verify(aiQuotaService).releaseQuota(7L);
    }

    @Test
    void invalidCachedRouteStartsFreshGeneration() {
        Executor directExecutor = Runnable::run;
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");
        String invalidCached = "# Broken cached route\n\n## 每日行程\n### 第1天：拉萨";

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(invalidCached);
        when(aiRouteService.normalizeCachedRoute(invalidCached, 5, "comfort", "natural", "zh")).thenReturn("");
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(101L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Fresh route")))
                .thenReturn(record(101L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDone("# Fresh route");
            return "# Fresh route";
        }).when(aiRouteService).streamRouteToListener(
                eq(5),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        assertEquals("# Fresh route", snapshot.content());
        verify(aiQuotaService).tryConsumeQuota(7L);
        verify(aiQuotaService).cacheRoute("cache-key", "# Fresh route");
    }

    @Test
    void validCachedRouteDoesNotConsumeQuota() {
        Executor directExecutor = Runnable::run;
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn("# Cached route");
        when(aiRouteService.normalizeCachedRoute("# Cached route", 5, "comfort", "natural", "zh"))
                .thenReturn("# Cached route");
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), eq(null), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Cached route")))
                .thenReturn(record(102L));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        assertEquals("# Cached route", snapshot.content());
        verify(aiQuotaService, never()).tryConsumeQuota(7L);
    }

    @Test
    void concurrentDuplicateStartReturnsActiveJobAndConsumesQuotaOnce() throws Exception {
        Executor holdingExecutor = command -> { };
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                holdingExecutor,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(105L));

        CountDownLatch quotaEntered = new CountDownLatch(1);
        CountDownLatch releaseQuota = new CountDownLatch(1);
        doAnswer(invocation -> {
            quotaEntered.countDown();
            if (!releaseQuota.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test did not release quota check");
            }
            return new AiQuotaService.QuotaConsumptionResult(true, 19);
        }).when(aiQuotaService).tryConsumeQuota(7L);

        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<AiRouteJobSnapshot> first = callers.submit(() -> service.startJob(request, user, "zh"));
            assertTrue(quotaEntered.await(2, TimeUnit.SECONDS));
            Future<AiRouteJobSnapshot> second = callers.submit(() -> service.startJob(request, user, "zh"));
            Thread.sleep(100);
            releaseQuota.countDown();

            AiRouteJobSnapshot firstSnapshot = first.get(2, TimeUnit.SECONDS);
            AiRouteJobSnapshot secondSnapshot = second.get(2, TimeUnit.SECONDS);

            assertEquals(firstSnapshot.jobId(), secondSnapshot.jobId());
            assertEquals("RUNNING", firstSnapshot.status());
            verify(aiQuotaService, times(1)).tryConsumeQuota(7L);
            verify(aiRouteRecordService, times(1))
                    .createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"));
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    void distributedLockHeldReturnsPersistedRunningRecordAndDoesNotConsumeQuota() {
        Executor holdingExecutor = command -> { };
        RedisFixture redis = redisFixture();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                holdingExecutor,
                new ObjectMapper(),
                provider(redis.template())
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");
        AiRouteRecord runningRecord = runningRecord(205L, "job-205", "# Partial route");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(redis.valueOperations().setIfAbsent(
                eq("ai:route-job:start:cache-key"), anyString(), any(Duration.class)))
                .thenReturn(false);
        when(redis.valueOperations().get("ai:route-job:start:cache-key")).thenReturn("job-205");
        when(aiRouteRecordService.findJobRecord(7L, "job-205")).thenReturn(Optional.of(runningRecord));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("job-205", snapshot.jobId());
        assertEquals(205L, snapshot.routeRecordId());
        assertEquals("RUNNING", snapshot.status());
        assertEquals("# Partial route", snapshot.content());
        verify(aiQuotaService, never()).tryConsumeQuota(7L);
        verify(aiRouteRecordService, never())
                .createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"));
    }

    @Test
    void distributedLockHeldBeforeRecordIsReadableStreamsRetryableInterruptedEvent() throws Exception {
        Executor holdingExecutor = command -> { };
        RedisFixture redis = redisFixture();
        CapturingSseEmitter capturingEmitter = new CapturingSseEmitter();
        ObjectMapper objectMapper = objectMapper();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                holdingExecutor,
                objectMapper,
                provider(redis.template())
        ) {
            @Override
            protected SseEmitter createEmitter() {
                return capturingEmitter;
            }
        };

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(redis.valueOperations().setIfAbsent(
                eq("ai:route-job:start:cache-key"), anyString(), any(Duration.class)))
                .thenReturn(false);
        when(redis.valueOperations().get("ai:route-job:start:cache-key")).thenReturn("job-205");
        when(redis.valueOperations().get("ai:route-job:pending:job-205"))
                .thenReturn(objectMapper.writeValueAsString(Map.of(
                        "userKey", testPendingUserKey(7L),
                        "days", 5,
                        "budget", "comfort",
                        "preference", "natural",
                        "locale", "zh")));
        when(aiRouteRecordService.findJobRecord(7L, "job-205")).thenReturn(Optional.empty());
        when(redis.streamOperations().reverseRange(
                eq("ai:route-job:events:job-205"), any(Range.class), any(Limit.class)))
                .thenReturn(List.of());

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");
        service.streamJob(snapshot.jobId(), user);

        assertEquals("job-205", snapshot.jobId());
        assertEquals("RUNNING", snapshot.status());
        assertEquals(5, snapshot.days());
        assertTrue(capturingEmitter.completed);
        assertTrue(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"snapshot\"")));
        assertTrue(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"interrupted\"")
                        && payload.contains("\"retryable\":true")
                        && payload.contains("running-record-not-ready")));
        verify(aiQuotaService, never()).tryConsumeQuota(7L);
    }

    @Test
    void distributedPendingStartMetadataDoesNotStoreRawUserIdOrRouteCacheKey() {
        Executor directExecutor = Runnable::run;
        RedisFixture redis = redisFixture();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                objectMapper(),
                provider(redis.template())
        );

        User user = new User();
        user.setId(1234567890123456789L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");
        String rawCacheKey = "raw-route-cache-key-for-user-1234567890123456789";

        when(aiQuotaService.buildCacheKey(user.getId(), 5, "comfort", "natural", "zh")).thenReturn(rawCacheKey);
        when(aiQuotaService.getCachedRoute(rawCacheKey)).thenReturn(null);
        when(redis.valueOperations().setIfAbsent(
                eq("ai:route-job:start:" + rawCacheKey), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(aiQuotaService.tryConsumeQuota(user.getId())).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(206L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Fresh route")))
                .thenReturn(record(206L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDone("# Fresh route");
            return "# Fresh route";
        }).when(aiRouteService).streamRouteToListener(
                eq(5),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        ArgumentCaptor<String> pendingJson = ArgumentCaptor.forClass(String.class);
        verify(redis.valueOperations()).set(
                eq("ai:route-job:pending:" + snapshot.jobId()),
                pendingJson.capture(),
                any(Duration.class));
        assertTrue(pendingJson.getValue().contains("\"userKey\":\"ai-route-job-user#"));
        assertFalse(pendingJson.getValue().contains("\"userId\""));
        assertFalse(pendingJson.getValue().contains("\"cacheKey\""));
        assertFalse(pendingJson.getValue().contains(user.getId().toString()));
        assertFalse(pendingJson.getValue().contains(rawCacheKey));
    }

    @Test
    void distributedLockIsReleasedWithJobTokenAfterCompletion() {
        Executor directExecutor = Runnable::run;
        RedisFixture redis = redisFixture();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper(),
                provider(redis.template())
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(redis.valueOperations().setIfAbsent(
                eq("ai:route-job:start:cache-key"), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(106L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Fresh route")))
                .thenReturn(record(106L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDone("# Fresh route");
            return "# Fresh route";
        }).when(aiRouteService).streamRouteToListener(
                eq(5),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        ArgumentCaptor<String> lockToken = ArgumentCaptor.forClass(String.class);
        verify(redis.valueOperations()).setIfAbsent(
                eq("ai:route-job:start:cache-key"), lockToken.capture(), any(Duration.class));
        assertEquals(snapshot.jobId(), lockToken.getValue());
        verify(redis.template()).execute(
                any(RedisScript.class), eq(List.of("ai:route-job:start:cache-key")), eq(snapshot.jobId()));
    }

    @Test
    void redisStartLockFailureFallsBackToLocalJobStart() {
        Executor directExecutor = Runnable::run;
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper(),
                provider(redisTemplate)
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(107L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Fallback route")))
                .thenReturn(record(107L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDone("# Fallback route");
            return "# Fallback route";
        }).when(aiRouteService).streamRouteToListener(
                eq(5),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        assertEquals("# Fallback route", snapshot.content());
        verify(aiQuotaService).tryConsumeQuota(7L);
    }

    @Test
    void productionRedisStartLockFailureFailsClosedBeforeQuotaConsumption() {
        Executor directExecutor = Runnable::run;
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper(),
                provider(redisTemplate),
                new CacheKeyHasher("test-cache-key-hmac-secret"),
                productionEnvironment()
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.startJob(request, user, "zh"));

        assertTrue(error.getMessage().contains("Redis start lock"));
        verify(aiQuotaService, never()).tryConsumeQuota(7L);
        verify(aiRouteRecordService, never())
                .createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"));
    }

    @Test
    void startupCleanupFailsStaleRunningRecordsUsingConfiguredTtl() {
        Executor holdingExecutor = command -> { };
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                holdingExecutor,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "staleRunningMinutes", 123L);

        service.cleanupStaleRunningRecordsOnStartup();

        verify(aiRouteRecordService).failStaleRunningRecords(Duration.ofMinutes(123));
    }

    @Test
    void getJobFallsBackToPersistedRunningRecordWhenMemoryStateIsMissing() {
        Executor holdingExecutor = command -> { };
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                holdingExecutor,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(7L);
        AiRouteRecord runningRecord = runningRecord(205L, "job-205", "# Partial route");
        when(aiRouteRecordService.findJobRecord(7L, "job-205")).thenReturn(Optional.of(runningRecord));

        AiRouteJobSnapshot snapshot = service.getJob("job-205", user);

        assertEquals("job-205", snapshot.jobId());
        assertEquals(205L, snapshot.routeRecordId());
        assertEquals("RUNNING", snapshot.status());
        assertEquals("# Partial route", snapshot.content());
        assertEquals(5, snapshot.days());
    }

    @Test
    void streamJobEmitsRetryableInterruptedEventWhenRedisStreamIsUnavailable() {
        Executor holdingExecutor = command -> { };
        CapturingSseEmitter capturingEmitter = new CapturingSseEmitter();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                holdingExecutor,
                objectMapper()
        ) {
            @Override
            protected SseEmitter createEmitter() {
                return capturingEmitter;
            }
        };

        User user = new User();
        user.setId(7L);
        AiRouteRecord runningRecord = runningRecord(205L, "job-205", "# Partial route");
        when(aiRouteRecordService.findJobRecord(7L, "job-205")).thenReturn(Optional.of(runningRecord));

        service.streamJob("job-205", user);

        assertTrue(capturingEmitter.completed);
        assertTrue(capturingEmitter.sendCount >= 1);
        assertTrue(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"snapshot\"")));
        assertTrue(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"interrupted\"")
                        && payload.contains("\"retryable\":true")));
        assertFalse(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"error\"")));
    }

    @Test
    void detachedPersistedRunningStreamRelaysRedisStreamEvents() {
        Executor directExecutor = Runnable::run;
        RedisFixture redis = redisFixture();
        CapturingSseEmitter capturingEmitter = new CapturingSseEmitter();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                objectMapper(),
                provider(redis.template())
        ) {
            @Override
            protected SseEmitter createEmitter() {
                return capturingEmitter;
            }
        };

        User user = new User();
        user.setId(7L);
        AiRouteRecord runningRecord = runningRecord(205L, "job-205", "# Partial route");
        when(aiRouteRecordService.findJobRecord(7L, "job-205")).thenReturn(Optional.of(runningRecord));

        String streamKey = "ai:route-job:events:job-205";
        MapRecord<String, String, String> latestReplace = redisEvent(
                streamKey,
                "10-0",
                "replace",
                "{\"type\":\"replace\",\"content\":\"# Partial route plus\",\"text\":\"# Partial route plus\"}");
        MapRecord<String, String, String> done = redisEvent(
                streamKey,
                "11-0",
                "done",
                "{\"type\":\"done\",\"content\":\"# Partial route plus done\"}");
        when(redis.streamOperations().reverseRange(
                eq(streamKey), any(Range.class), any(Limit.class)))
                .thenReturn(List.of(latestReplace));
        when(redis.streamOperations().read(
                any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(done));

        service.streamJob("job-205", user);

        assertTrue(capturingEmitter.completed);
        assertTrue(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"snapshot\"")));
        assertTrue(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"replace\"")
                        && payload.contains("# Partial route plus")));
        assertTrue(capturingEmitter.sentData().stream()
                .anyMatch(payload -> payload.contains("\"type\":\"done\"")
                        && payload.contains("# Partial route plus done")));
    }

    @Test
    void runningJobDoesNotPublishIntermediateRedisEventsBelowInitialThreshold() {
        Executor directExecutor = Runnable::run;
        RedisFixture redis = redisFixture();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                objectMapper(),
                provider(redis.template())
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(redis.valueOperations().setIfAbsent(
                eq("ai:route-job:start:cache-key"), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(106L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Fresh route")))
                .thenReturn(record(106L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDelta("# Fresh");
            listener.onDelta(" route");
            listener.onDone("# Fresh route");
            return "# Fresh route";
        }).when(aiRouteService).streamRouteToListener(
                eq(5),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> eventCaptor = ArgumentCaptor.forClass(Map.class);
        verify(redis.streamOperations(), times(2)).add(
                eq("ai:route-job:events:" + snapshot.jobId()), eventCaptor.capture());
        List<Map<String, String>> events = eventCaptor.getAllValues();
        // These two short deltas stay under the initial publish threshold, so only the terminal
        // events reach the stream - and they still carry the full content, encrypted.
        assertEquals("snapshot", events.get(0).get("type"));
        assertTrue(events.get(0).get("event").startsWith("enc:v1:"));
        assertFalse(events.get(0).get("event").contains("# Fresh route"));
        assertEquals("done", events.get(1).get("type"));
        assertTrue(events.get(1).get("event").startsWith("enc:v1:"));
        assertFalse(events.get(1).get("event").contains("# Fresh route"));
    }

    @Test
    void runningJobPublishesCumulativeContentAtGeometricThresholds() {
        Executor directExecutor = Runnable::run;
        RedisFixture redis = redisFixture();
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                objectMapper(),
                provider(redis.template())
        );

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        String chunk = "x".repeat(400);
        String fullContent = chunk.repeat(16);
        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(redis.valueOperations().setIfAbsent(
                eq("ai:route-job:start:cache-key"), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(107L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq(fullContent)))
                .thenReturn(record(107L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            for (int i = 0; i < 16; i++) {
                listener.onDelta(chunk);
            }
            listener.onDone(fullContent);
            return fullContent;
        }).when(aiRouteService).streamRouteToListener(
                eq(5), eq("comfort"), eq("natural"), eq(user), eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> eventCaptor = ArgumentCaptor.forClass(Map.class);
        verify(redis.streamOperations(), times(7)).add(
                eq("ai:route-job:events:" + snapshot.jobId()), eventCaptor.capture());
        List<Map<String, String>> events = eventCaptor.getAllValues();
        // Sixteen fixed-size chunks produce only five intermediate cumulative events
        // (400, 800, 1600, 3200, 6400) rather than one event per chunk. Their combined payload is
        // bounded by twice the final route length, eliminating the previous O(n²) write growth.
        assertEquals(List.of("replace", "replace", "replace", "replace", "replace", "snapshot", "done"),
                events.stream().map(event -> event.get("type")).toList());
    }

    @Test
    void terminalJobLookupFallsBackToPersistedCompletedSnapshotAfterMemoryRetention() {
        Executor directExecutor = Runnable::run;
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "jobRetentionMinutes", 0L);

        User user = new User();
        user.setId(7L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(7L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(aiQuotaService.tryConsumeQuota(7L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(103L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(user), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Fresh route")))
                .thenAnswer(invocation -> completedRecord(103L, invocation.getArgument(1), "# Fresh route"));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDone("# Fresh route");
            return "# Fresh route";
        }).when(aiRouteService).streamRouteToListener(
                eq(5),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, user, "zh");

        assertEquals("COMPLETED", snapshot.status());
        when(aiRouteRecordService.findJobRecord(7L, snapshot.jobId()))
                .thenReturn(Optional.of(completedRecord(103L, snapshot.jobId(), "# Fresh route")));

        AiRouteJobSnapshot restoredSnapshot = service.getJob(snapshot.jobId(), user);

        assertEquals(snapshot.jobId(), restoredSnapshot.jobId());
        assertEquals(103L, restoredSnapshot.routeRecordId());
        assertEquals("COMPLETED", restoredSnapshot.status());
        assertEquals("# Fresh route", restoredSnapshot.content());
    }

    @Test
    void rejectsJobLookupFromDifferentUserEvenForLargeBoxedIds() {
        Executor directExecutor = Runnable::run;
        AiRouteGenerationJobService service = new AiRouteGenerationJobService(
                aiRouteService,
                aiQuotaService,
                aiRouteRecordService,
                directExecutor,
                new ObjectMapper()
        );

        User owner = new User();
        owner.setId(1000L);
        User other = new User();
        other.setId(1000L);
        AiRouteGenerateRequest request = new AiRouteGenerateRequest();
        request.setDays(5);
        request.setBudget("comfort");
        request.setPreference("natural");

        when(aiQuotaService.buildCacheKey(1000L, 5, "comfort", "natural", "zh")).thenReturn("cache-key");
        when(aiQuotaService.getCachedRoute("cache-key")).thenReturn(null);
        when(aiQuotaService.tryConsumeQuota(1000L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteRecordService.createRunningRecord(eq(owner), any(), eq(5), eq("comfort"), eq("natural"), eq("zh")))
                .thenReturn(record(104L));
        when(aiRouteRecordService.recordCompletedRoute(
                eq(owner), any(), eq(5), eq("comfort"), eq("natural"), eq("zh"), eq("# Fresh route")))
                .thenReturn(record(104L));

        doAnswer(invocation -> {
            AiRouteService.RouteStreamListener listener = invocation.getArgument(5);
            listener.onDone("# Fresh route");
            return "# Fresh route";
        }).when(aiRouteService).streamRouteToListener(
                eq(5),
                eq("comfort"),
                eq("natural"),
                eq(owner),
                eq("zh"),
                any(AiRouteService.RouteStreamListener.class));

        AiRouteJobSnapshot snapshot = service.startJob(request, owner, "zh");

        assertEquals("COMPLETED", snapshot.status());
        assertEquals("# Fresh route", service.getJob(snapshot.jobId(), other).content());

        other.setId(1001L);
        assertThrows(IllegalArgumentException.class, () -> service.getJob(snapshot.jobId(), other));
    }

    private AiRouteJobSnapshot waitForStatus(AiRouteGenerationJobService service, String jobId,
                                             User user, String expectedStatus) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            AiRouteJobSnapshot snapshot = service.getJob(jobId, user);
            if (expectedStatus.equals(snapshot.status())) {
                return snapshot;
            }
            Thread.sleep(50);
        }
        fail("Job did not reach status " + expectedStatus);
        return null;
    }

    private static AiRouteRecord record(Long id) {
        AiRouteRecord record = new AiRouteRecord();
        record.setId(id);
        return record;
    }

    private static AiRouteRecord runningRecord(Long id, String jobId, String content) {
        AiRouteRecord record = record(id);
        record.setJobId(jobId);
        record.setContent(content);
        record.setDays(5);
        record.setBudget("comfort");
        record.setPreference("natural");
        record.setLocale("zh");
        record.setStatus(AiRouteRecord.Status.RUNNING);
        record.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    private static AiRouteRecord completedRecord(Long id, String jobId, String content) {
        AiRouteRecord record = runningRecord(id, jobId, content);
        record.setStatus(AiRouteRecord.Status.COMPLETED);
        return record;
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private static String testPendingUserKey(Long userId) {
        return new CacheKeyHasher("local-cache-key-hmac-secret")
                .cacheKey("ai-route-job-user", userId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RedisFixture redisFixture() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        StreamOperations<String, String, String> streamOperations = mock(StreamOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);
        return new RedisFixture(redisTemplate, valueOperations, streamOperations);
    }

    private static MapRecord<String, String, String> redisEvent(String streamKey, String id, String type, String event) {
        return MapRecord.create(streamKey, Map.of("type", type, "event", event))
                .withId(RecordId.of(id));
    }

    private static ObjectProvider<StringRedisTemplate> provider(StringRedisTemplate redisTemplate) {
        return new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject(Object... args) {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfUnique() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getObject() {
                return redisTemplate;
            }

            @Override
            public Stream<StringRedisTemplate> stream() {
                return redisTemplate == null ? Stream.empty() : Stream.of(redisTemplate);
            }

            @Override
            public Stream<StringRedisTemplate> orderedStream() {
                return stream();
            }
        };
    }

    private static MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private record RedisFixture(
            StringRedisTemplate template,
            ValueOperations<String, String> valueOperations,
            StreamOperations<String, String, String> streamOperations
    ) {
    }

    private static final class CapturingSseEmitter extends SseEmitter {
        private final List<SseEventBuilder> events = new ArrayList<>();
        private int sendCount;
        private boolean completed;

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            events.add(builder);
            sendCount++;
        }

        List<String> sentData() {
            return events.stream()
                    .map(event -> event.build().stream()
                            .map(ResponseBodyEmitter.DataWithMediaType::getData)
                            .map(String::valueOf)
                            .reduce("", String::concat))
                    .toList();
        }

        @Override
        public void complete() {
            completed = true;
        }
    }
}
