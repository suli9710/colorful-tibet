package com.tibet.tourism.modules.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateRequest;
import com.tibet.tourism.modules.user.domain.User;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

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
    void terminalJobsExpireAfterRetentionWindow() {
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
                .thenReturn(record(103L));

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
        assertThrows(IllegalArgumentException.class, () -> service.getJob(snapshot.jobId(), user));
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

    @SuppressWarnings("unchecked")
    private static RedisFixture redisFixture() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new RedisFixture(redisTemplate, valueOperations);
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

    private record RedisFixture(
            StringRedisTemplate template,
            ValueOperations<String, String> valueOperations
    ) {
    }
}
