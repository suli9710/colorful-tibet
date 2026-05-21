package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class GuideChatUsageServiceTest {

    @Test
    void inMemoryWindowLimitBlocksBurstForSameClient() {
        GuideChatUsageService service = newService(10, 2, 60);

        assertThat(service.tryAcquire("client-a").allowed()).isTrue();
        assertThat(service.tryAcquire("client-a").allowed()).isTrue();

        GuideChatUsageService.Decision blocked = service.tryAcquire("client-a");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo("window");
        assertThat(blocked.retryAfterSeconds()).isPositive();
        assertThat(service.tryAcquire("client-b").allowed()).isTrue();
    }

    @Test
    void inMemoryDailyLimitBlocksAfterDailyQuota() {
        GuideChatUsageService service = newService(2, 10, 60);

        assertThat(service.tryAcquire("client-a").allowed()).isTrue();
        assertThat(service.tryAcquire("client-a").allowed()).isTrue();

        GuideChatUsageService.Decision blocked = service.tryAcquire("client-a");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo("daily");
        assertThat(blocked.retryAfterSeconds()).isPositive();
    }

    private GuideChatUsageService newService(int dailyLimit, int windowLimit, int windowSeconds) {
        GuideChatUsageService service = new GuideChatUsageService(nullRedisProvider());
        ReflectionTestUtils.setField(service, "dailyLimit", dailyLimit);
        ReflectionTestUtils.setField(service, "windowLimit", windowLimit);
        ReflectionTestUtils.setField(service, "windowSeconds", windowSeconds);
        return service;
    }

    private ObjectProvider<RedisTemplate<String, Object>> nullRedisProvider() {
        return new ObjectProvider<>() {
            @Override
            public RedisTemplate<String, Object> getObject() {
                return null;
            }

            @Override
            public RedisTemplate<String, Object> getObject(Object... args) {
                return null;
            }

            @Override
            public RedisTemplate<String, Object> getIfAvailable() {
                return null;
            }

            @Override
            public RedisTemplate<String, Object> getIfUnique() {
                return null;
            }
        };
    }
}
