package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

class AiQuotaServiceTest {

    @Test
    void redisLuaConsumeAllowsUntilLimitAndRejectsWithoutExtraCount() {
        StringRedisTemplate redisTemplate = redisTemplate();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1L, 1L))
                .thenReturn(List.of(1L, 0L))
                .thenReturn(List.of(0L, 0L))
                .thenReturn(List.of(0L, 0L));

        AiQuotaService service = new AiQuotaService(provider(redisTemplate));
        ReflectionTestUtils.setField(service, "dailyLimit", 2);

        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(true, 1));
        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(true, 0));
        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));
        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));

        verify(redisTemplate, times(4)).execute(any(DefaultRedisScript.class), anyList(), any(), any());
    }

    @Test
    void redisFailureUsesMirroredFallbackQuota() {
        StringRedisTemplate redisTemplate = redisTemplate();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1L, 1L))
                .thenReturn(List.of(1L, 0L))
                .thenThrow(new RuntimeException("redis unavailable"));

        AiQuotaService service = new AiQuotaService(provider(redisTemplate));
        ReflectionTestUtils.setField(service, "dailyLimit", 2);

        assertThat(service.tryConsumeQuota(7L).allowed()).isTrue();
        assertThat(service.tryConsumeQuota(7L).allowed()).isTrue();

        AiQuotaService.QuotaConsumptionResult fallbackDecision = service.tryConsumeQuota(7L);

        assertThat(fallbackDecision.allowed()).isFalse();
        assertThat(fallbackDecision.remaining()).isZero();
        assertThat(service.getRemainingQuota(7L)).isZero();
    }

    @Test
    void inMemoryFallbackEnforcesLimitWhenRedisIsMissing() {
        AiQuotaService service = new AiQuotaService(provider(null));
        ReflectionTestUtils.setField(service, "dailyLimit", 1);

        assertThat(service.tryConsumeQuota(9L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(true, 0));
        assertThat(service.tryConsumeQuota(9L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));
        assertThat(service.getRemainingQuota(9L)).isZero();
    }

    @Test
    void inMemoryFallbackConsumeIsAtomicUnderConcurrency() throws Exception {
        AiQuotaService service = new AiQuotaService(provider(null));
        ReflectionTestUtils.setField(service, "dailyLimit", 5);

        int attempts = 30;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();

        try {
            for (int i = 0; i < attempts; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (service.tryConsumeQuota(11L).allowed()) {
                        allowed.incrementAndGet();
                    }
                });
            }
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(allowed.get()).isEqualTo(5);
        assertThat(service.getRemainingQuota(11L)).isZero();
    }

    @SuppressWarnings("unchecked")
    private static StringRedisTemplate redisTemplate() {
        return mock(StringRedisTemplate.class);
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
}
