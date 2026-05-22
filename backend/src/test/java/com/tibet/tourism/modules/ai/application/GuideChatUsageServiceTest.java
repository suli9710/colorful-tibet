package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private GuideChatUsageService newService(int dailyLimit, int windowLimit, int windowSeconds) {
        GuideChatUsageService service = new GuideChatUsageService(nullRedisProvider());
        ReflectionTestUtils.setField(service, "anonymousDailyLimit", dailyLimit);
        ReflectionTestUtils.setField(service, "authenticatedDailyLimit", dailyLimit);
        ReflectionTestUtils.setField(service, "windowLimit", windowLimit);
        ReflectionTestUtils.setField(service, "windowSeconds", windowSeconds);
        ReflectionTestUtils.setField(service, "anonymousEnabled", true);
        ReflectionTestUtils.setField(service, "requireRecaptchaAfter", Integer.MAX_VALUE);
        return service;
    }

    private ObjectProvider<StringRedisTemplate> nullRedisProvider() {
        return new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject() {
                return null;
            }

            @Override
            public StringRedisTemplate getObject(Object... args) {
                return null;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return null;
            }

            @Override
            public StringRedisTemplate getIfUnique() {
                return null;
            }
        };
    }
}
