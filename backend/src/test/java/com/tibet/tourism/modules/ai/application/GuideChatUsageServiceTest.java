package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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

    @Test
    void anonymousRequestsRequireCaptchaAfterConfiguredThreshold() {
        GuideChatUsageService service = newService(5, 10, 60);
        ReflectionTestUtils.setField(service, "requireRecaptchaAfter", 2);

        assertThat(service.tryAcquire(GuideChatUsageService.ClientIdentity.anonymous("client-a"), false).allowed()).isTrue();
        assertThat(service.tryAcquire(GuideChatUsageService.ClientIdentity.anonymous("client-a"), false).allowed()).isTrue();

        GuideChatUsageService.Decision challenge =
                service.tryAcquire(GuideChatUsageService.ClientIdentity.anonymous("client-a"), false);

        assertThat(challenge.allowed()).isFalse();
        assertThat(challenge.challengeRequired()).isTrue();
        assertThat(challenge.reason()).isEqualTo("captcha");
        assertThat(service.tryAcquire(GuideChatUsageService.ClientIdentity.anonymous("client-a"), true).allowed()).isTrue();
    }

    @Test
    void authenticatedClientsUseSeparateDailyQuotaAndDoNotRequireCaptcha() {
        GuideChatUsageService service = newService(1, 10, 60);
        ReflectionTestUtils.setField(service, "authenticatedDailyLimit", 3);
        ReflectionTestUtils.setField(service, "requireRecaptchaAfter", 0);

        assertThat(service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(7L), false).allowed()).isTrue();
        assertThat(service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(7L), false).allowed()).isTrue();
        assertThat(service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(7L), false).allowed()).isTrue();
        assertThat(service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(7L), false).allowed()).isFalse();
    }

    @Test
    void redisWindowLimitDoesNotConsumeDailyQuotaWhenWindowIsAlreadyFull() {
        RedisFixture redis = redisFixture();
        GuideChatUsageService service = newService(redis.template(), 10, 2, 60);
        when(redis.valueOps().get(anyString())).thenReturn("1", "2");
        when(redis.template().getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(45L);

        GuideChatUsageService.Decision blocked =
                service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(7L), false);

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo("window");
        assertThat(blocked.retryAfterSeconds()).isEqualTo(45);
        verify(redis.valueOps(), never()).increment(anyString());
        verify(redis.valueOps(), never()).decrement(anyString());
    }

    @Test
    void redisCompensatesDailyQuotaWhenWindowIncrementRacesPastLimit() {
        RedisFixture redis = redisFixture();
        GuideChatUsageService service = newService(redis.template(), 10, 2, 60);
        when(redis.valueOps().get(anyString())).thenReturn("1", "1");
        when(redis.template().getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(45L);
        when(redis.valueOps().increment(anyString())).thenReturn(2L, 3L);

        GuideChatUsageService.Decision blocked =
                service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(7L), false);

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo("window");
        verify(redis.valueOps()).decrement(anyString());
    }

    private GuideChatUsageService newService(int dailyLimit, int windowLimit, int windowSeconds) {
        GuideChatUsageService service = new GuideChatUsageService(nullRedisProvider());
        configureLimits(service, dailyLimit, windowLimit, windowSeconds);
        return service;
    }

    private GuideChatUsageService newService(
            StringRedisTemplate redisTemplate,
            int dailyLimit,
            int windowLimit,
            int windowSeconds) {
        GuideChatUsageService service = new GuideChatUsageService(redisProvider(redisTemplate));
        configureLimits(service, dailyLimit, windowLimit, windowSeconds);
        return service;
    }

    private void configureLimits(GuideChatUsageService service, int dailyLimit, int windowLimit, int windowSeconds) {
        ReflectionTestUtils.setField(service, "anonymousDailyLimit", dailyLimit);
        ReflectionTestUtils.setField(service, "authenticatedDailyLimit", dailyLimit);
        ReflectionTestUtils.setField(service, "windowLimit", windowLimit);
        ReflectionTestUtils.setField(service, "windowSeconds", windowSeconds);
        ReflectionTestUtils.setField(service, "anonymousEnabled", true);
        ReflectionTestUtils.setField(service, "requireRecaptchaAfter", Integer.MAX_VALUE);
    }

    private ObjectProvider<StringRedisTemplate> nullRedisProvider() {
        return redisProvider(null);
    }

    private ObjectProvider<StringRedisTemplate> redisProvider(StringRedisTemplate redisTemplate) {
        return new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject() {
                return redisTemplate;
            }

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
        };
    }

    @SuppressWarnings("unchecked")
    private RedisFixture redisFixture() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new RedisFixture(redisTemplate, valueOperations);
    }

    private record RedisFixture(
            StringRedisTemplate template,
            ValueOperations<String, String> valueOps) {
    }
}
