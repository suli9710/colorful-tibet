package com.tibet.tourism.modules.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateRequest;
import com.tibet.tourism.modules.user.domain.User;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
