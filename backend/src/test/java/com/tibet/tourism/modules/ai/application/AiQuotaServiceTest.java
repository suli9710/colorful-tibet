package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class AiQuotaServiceTest {

    @Test
    void redisReadFailureUsesMirroredFallbackQuota() {
        StringRedisTemplate redisTemplate = redisTemplate();
        ValueOperations<String, String> valueOperations = valueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L);

        AiQuotaService service = new AiQuotaService(provider(redisTemplate));
        ReflectionTestUtils.setField(service, "dailyLimit", 2);

        service.incrementQuota(7L);
        service.incrementQuota(7L);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("redis unavailable"));

        assertThat(service.isQuotaExceeded(7L)).isTrue();
        assertThat(service.getRemainingQuota(7L)).isZero();
    }

    @Test
    void inMemoryFallbackEnforcesLimitWhenRedisIsMissing() {
        AiQuotaService service = new AiQuotaService(provider(null));
        ReflectionTestUtils.setField(service, "dailyLimit", 1);

        assertThat(service.isQuotaExceeded(9L)).isFalse();

        service.incrementQuota(9L);

        assertThat(service.isQuotaExceeded(9L)).isTrue();
        assertThat(service.getRemainingQuota(9L)).isZero();
    }

    @SuppressWarnings("unchecked")
    private static StringRedisTemplate redisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    @SuppressWarnings("unchecked")
    private static ValueOperations<String, String> valueOperations() {
        return mock(ValueOperations.class);
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
