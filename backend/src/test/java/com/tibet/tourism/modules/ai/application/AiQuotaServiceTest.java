package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.CacheKeyHasher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class AiQuotaServiceTest {

    private static final CacheKeyHasher CACHE_KEY_HASHER = new CacheKeyHasher("test-cache-key-hmac-secret");

    @Test
    void redisLuaConsumeAllowsUntilLimitAndRejectsWithoutExtraCount() {
        StringRedisTemplate redisTemplate = redisTemplate();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1L, 1L))
                .thenReturn(List.of(1L, 0L))
                .thenReturn(List.of(0L, 0L))
                .thenReturn(List.of(0L, 0L));

        AiQuotaService service = new AiQuotaService(provider(redisTemplate), CACHE_KEY_HASHER);
        ReflectionTestUtils.setField(service, "dailyLimit", 2);

        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(true, 1));
        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(true, 0));
        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));
        assertThat(service.tryConsumeQuota(7L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, times(4)).execute(any(DefaultRedisScript.class), keysCaptor.capture(), any(), any());
        assertThat(keysCaptor.getAllValues()).allSatisfy(keys -> {
            assertThat(keys).hasSize(1);
            assertThat(keys.get(0).toString())
                    .startsWith("ai:quota:daily:" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ":ai-quota-user#")
                    .doesNotContain(":7");
        });
    }

    @Test
    void redisFailureUsesMirroredFallbackQuota() {
        StringRedisTemplate redisTemplate = redisTemplate();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1L, 1L))
                .thenReturn(List.of(1L, 0L))
                .thenThrow(new RuntimeException("redis unavailable"));

        AiQuotaService service = new AiQuotaService(provider(redisTemplate), CACHE_KEY_HASHER);
        ReflectionTestUtils.setField(service, "dailyLimit", 2);

        assertThat(service.tryConsumeQuota(7L).allowed()).isTrue();
        assertThat(service.tryConsumeQuota(7L).allowed()).isTrue();

        AiQuotaService.QuotaConsumptionResult fallbackDecision = service.tryConsumeQuota(7L);

        assertThat(fallbackDecision.allowed()).isFalse();
        assertThat(fallbackDecision.remaining()).isZero();
        assertThat(service.getRemainingQuota(7L)).isZero();
    }

    @Test
    void failClosedDeniesConsumeWhenRedisFails() {
        StringRedisTemplate redisTemplate = redisTemplate();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenThrow(new RuntimeException("redis unavailable"));

        AiQuotaService service = new AiQuotaService(provider(redisTemplate));
        ReflectionTestUtils.setField(service, "dailyLimit", 5);
        ReflectionTestUtils.setField(service, "failClosedOnRedisOutage", true);

        AiQuotaService.QuotaConsumptionResult decision = service.tryConsumeQuota(7L);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
    }

    @Test
    void inMemoryFallbackEnforcesLimitWhenRedisIsMissing() {
        AiQuotaService service = new AiQuotaService(provider(null), CACHE_KEY_HASHER);
        ReflectionTestUtils.setField(service, "dailyLimit", 1);

        assertThat(service.tryConsumeQuota(9L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(true, 0));
        assertThat(service.tryConsumeQuota(9L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));
        assertThat(service.getRemainingQuota(9L)).isZero();
    }

    @Test
    void productionRedisMissingFailsClosedForQuotaControls() {
        AiQuotaService service = new AiQuotaService(provider(null), CACHE_KEY_HASHER, productionEnvironment());
        ReflectionTestUtils.setField(service, "dailyLimit", 20);

        assertThat(service.tryConsumeQuota(9L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));
        assertThat(service.isQuotaExceeded(9L)).isTrue();
        assertThat(service.getRemainingQuota(9L)).isZero();
    }

    @Test
    void productionRedisFailureFailsClosedForQuotaControls() {
        StringRedisTemplate redisTemplate = redisTemplate();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenThrow(new RuntimeException("redis unavailable"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenThrow(new RuntimeException("redis unavailable"));

        AiQuotaService service = new AiQuotaService(provider(redisTemplate), CACHE_KEY_HASHER, productionEnvironment());
        ReflectionTestUtils.setField(service, "dailyLimit", 20);

        assertThat(service.tryConsumeQuota(9L)).isEqualTo(new AiQuotaService.QuotaConsumptionResult(false, 0));
        assertThat(service.isQuotaExceeded(9L)).isTrue();
        assertThat(service.getRemainingQuota(9L)).isZero();
    }

    @Test
    void inMemoryFallbackConsumeIsAtomicUnderConcurrency() throws Exception {
        AiQuotaService service = new AiQuotaService(provider(null), CACHE_KEY_HASHER);
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

    @Test
    void routeCacheKeyDoesNotExposeRawUserIdOrPreferences() {
        AiQuotaService service = new AiQuotaService(provider(null), CACHE_KEY_HASHER);

        String cacheKey = service.buildCacheKey(123456789L, 5, "Comfort", "Natural Wonders", "zh");
        String otherCacheKey = service.buildCacheKey(123456789L, 5, "Comfort", "Culture", "zh");

        assertThat(cacheKey)
                .startsWith("ai-route#")
                .doesNotContain("123456789")
                .doesNotContain("comfort")
                .doesNotContain("natural")
                .doesNotContain("wonders")
                .doesNotContain("zh");
        assertThat(cacheKey).isNotEqualTo(otherCacheKey);
    }

    @Test
    void inMemoryFallbackQuotaKeysDoNotExposeRawUserIds() {
        AiQuotaService service = new AiQuotaService(provider(null), CACHE_KEY_HASHER);
        ReflectionTestUtils.setField(service, "dailyLimit", 2);

        service.tryConsumeQuota(123456789L);

        @SuppressWarnings("unchecked")
        Map<String, AtomicInteger> fallbackQuota =
                (Map<String, AtomicInteger>) ReflectionTestUtils.getField(service, "fallbackQuota");

        assertThat(fallbackQuota).isNotNull();
        assertThat(fallbackQuota.keySet()).allSatisfy(key -> assertThat(key)
                .startsWith("ai:quota:daily:")
                .contains(":ai-quota-user#")
                .doesNotContain("123456789"));
    }

    @Test
    void redisRouteCacheKeysUseOpaqueRouteCacheKey() {
        StringRedisTemplate redisTemplate = redisTemplate();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AiQuotaService service = new AiQuotaService(provider(redisTemplate), CACHE_KEY_HASHER);
        String cacheKey = service.buildCacheKey(123456789L, 5, "Comfort", "Natural Wonders", "zh");

        service.cacheRoute(cacheKey, "# route");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), any(), any(java.time.Duration.class));
        assertThat(keyCaptor.getValue())
                .startsWith("ai:cache:route:ai-route#")
                .doesNotContain("123456789")
                .doesNotContain("comfort")
                .doesNotContain("natural")
                .doesNotContain("zh");
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

    private static MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }
}
