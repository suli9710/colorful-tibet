package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.CacheKeyHasher;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class GuideChatUsageServiceTest {

    private static final CacheKeyHasher CACHE_KEY_HASHER = new CacheKeyHasher("test-cache-key-hmac-secret");

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

    @Test
    void productionRedisMissingFailsClosed() {
        GuideChatUsageService service = newService(null, productionEnvironment(), 10, 5, 60);

        GuideChatUsageService.Decision decision =
                service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(123456789L), false);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.challengeRequired()).isFalse();
        assertThat(decision.reason()).isEqualTo("usage-backend-unavailable");
        assertThat(decision.retryAfterSeconds()).isGreaterThanOrEqualTo(60);
    }

    @Test
    void productionRedisFailureFailsClosed() {
        RedisFixture redis = redisFixture();
        GuideChatUsageService service = newService(redis.template(), productionEnvironment(), 10, 5, 60);
        when(redis.valueOps().get(anyString())).thenThrow(new RuntimeException("redis unavailable"));

        GuideChatUsageService.Decision decision =
                service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(123456789L), false);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("usage-backend-unavailable");
        verify(redis.valueOps(), never()).increment(anyString());
    }

    @Test
    void redisUsageKeysUseOpaqueClientLabels() {
        RedisFixture redis = redisFixture();
        GuideChatUsageService service = newService(redis.template(), 10, 5, 60);
        when(redis.valueOps().get(anyString())).thenReturn("0", "0");
        when(redis.template().getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(60L);
        when(redis.valueOps().increment(anyString())).thenReturn(1L, 1L);

        GuideChatUsageService.Decision decision =
                service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(123456789L), false);

        assertThat(decision.allowed()).isTrue();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redis.valueOps(), times(2)).increment(keyCaptor.capture());
        assertThat(keyCaptor.getAllValues()).allSatisfy(key -> assertThat(key)
                .contains("guide-chat-auth-client#")
                .doesNotContain("123456789"));
    }

    @Test
    void inMemoryFallbackUsageKeysUseOpaqueClientLabels() {
        GuideChatUsageService service = newService(10, 5, 60);

        service.tryAcquire(GuideChatUsageService.ClientIdentity.authenticated(123456789L), false);

        @SuppressWarnings("unchecked")
        Map<String, ?> fallbackUsage = (Map<String, ?>) ReflectionTestUtils.getField(service, "fallbackUsage");

        assertThat(fallbackUsage).isNotNull();
        assertThat(fallbackUsage.keySet()).allSatisfy(key -> assertThat(key)
                .contains("guide-chat-auth-client#")
                .doesNotContain("123456789"));
    }

    private GuideChatUsageService newService(int dailyLimit, int windowLimit, int windowSeconds) {
        GuideChatUsageService service = new GuideChatUsageService(nullRedisProvider(), CACHE_KEY_HASHER, null);
        configureLimits(service, dailyLimit, windowLimit, windowSeconds);
        return service;
    }

    private GuideChatUsageService newService(
            StringRedisTemplate redisTemplate,
            int dailyLimit,
            int windowLimit,
            int windowSeconds) {
        return newService(redisTemplate, null, dailyLimit, windowLimit, windowSeconds);
    }

    private GuideChatUsageService newService(
            StringRedisTemplate redisTemplate,
            MockEnvironment environment,
            int dailyLimit,
            int windowLimit,
            int windowSeconds) {
        GuideChatUsageService service = new GuideChatUsageService(
                redisProvider(redisTemplate),
                CACHE_KEY_HASHER,
                environment);
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

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }
}
