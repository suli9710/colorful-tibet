package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<RedisTemplate<String, Object>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        service = new LoginAttemptService(provider, "");
        configure(service, false);
    }

    @Test
    void accountFailuresDoNotHardLockUsername() {
        for (int i = 0; i < 2; i++) {
            LoginAttemptService.LoginAttemptDecision decision =
                    service.recordFailure("user1", "203.0.113." + i);
            assertThat(decision.allowed()).isTrue();
        }

        assertThat(service.isLocked("user1")).isFalse();
        assertThat(service.remainingLockSeconds("user1")).isZero();
        assertThat(service.failureCount("user1")).isEqualTo(2);
        assertThat(service.accountStepUpRequired("user1")).isFalse();
    }

    @Test
    void accountThresholdRequiresStepUpButStillDoesNotHardLockAccount() {
        service.recordFailure("user2", "203.0.113.1");
        service.recordFailure("user2", "203.0.113.2");
        LoginAttemptService.LoginAttemptDecision thirdFailure =
                service.recordFailure("user2", "203.0.113.3");

        assertThat(thirdFailure.allowed()).isTrue();
        assertThat(thirdFailure.stepUpRequired()).isTrue();
        assertThat(service.accountStepUpRequired("user2")).isTrue();
        assertThat(service.remainingLockSeconds("user2")).isZero();

        LoginAttemptService.LoginAttemptDecision fromNewIp = service.evaluate("user2", "203.0.113.99");
        assertThat(fromNewIp.allowed()).isTrue();
        assertThat(fromNewIp.stepUpRequired()).isTrue();
    }

    @Test
    void pairLimitBlocksSameIpAndUsernameOnly() {
        ReflectionTestUtils.setField(service, "ipMaxAttempts", 10);
        ReflectionTestUtils.setField(service, "networkMaxAttempts", 20);

        for (int i = 0; i < 4; i++) {
            assertThat(service.recordFailure("user3", "203.0.113.10").allowed()).isTrue();
        }

        LoginAttemptService.LoginAttemptDecision blocked =
                service.recordFailure("user3", "203.0.113.10");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo("pair");
        assertThat(blocked.retryAfterSeconds()).isPositive();
        assertThat(service.evaluate("user3", "203.0.113.10").allowed()).isFalse();
        assertThat(service.evaluate("user3", "203.0.114.10").allowed()).isTrue();
    }

    @Test
    void ipLimitBlocksFailuresAcrossAccounts() {
        assertThat(service.recordFailure("user-a", "198.51.100.10").allowed()).isTrue();
        assertThat(service.recordFailure("user-b", "198.51.100.10").allowed()).isTrue();

        LoginAttemptService.LoginAttemptDecision blocked =
                service.recordFailure("user-c", "198.51.100.10");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo("ip");
        assertThat(service.evaluate("user-d", "198.51.100.10").allowed()).isFalse();
        assertThat(service.evaluate("user-d", "198.51.100.11").allowed()).isTrue();
    }

    @Test
    void networkLimitBlocksDistributedFailuresInSameIpv4Slash24() {
        service.recordFailure("user-a", "192.0.2.10");
        service.recordFailure("user-b", "192.0.2.11");
        service.recordFailure("user-c", "192.0.2.12");
        service.recordFailure("user-d", "192.0.2.13");
        service.recordFailure("user-e", "192.0.2.14");

        LoginAttemptService.LoginAttemptDecision blocked = service.recordFailure("user-f", "192.0.2.15");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo("network");
        assertThat(service.evaluate("user-g", "192.0.3.15").allowed()).isTrue();
    }

    @Test
    void resetClearsAccountAndCurrentPairButNotIpThrottle() {
        ReflectionTestUtils.setField(service, "pairMaxAttempts", 10);

        for (int i = 0; i < 5; i++) {
            service.recordFailure("user4", "203.0.113.20");
        }
        assertThat(service.evaluate("user4", "203.0.113.20").allowed()).isFalse();

        service.reset("user4", "203.0.113.20");

        assertThat(service.failureCount("user4")).isZero();
        assertThat(service.evaluate("user4", "203.0.113.20").allowed()).isFalse();
        assertThat(service.evaluate("user4", "203.0.114.20").allowed()).isTrue();
    }

    @Test
    void usernameIsCaseInsensitiveForAccountAndPair() {
        service.recordFailure("ADMIN", "203.0.113.30");
        service.recordFailure("admin", "203.0.113.30");

        assertThat(service.failureCount("Admin")).isEqualTo(2);
    }

    @Test
    void disabledServiceAlwaysReturnsOpen() {
        ReflectionTestUtils.setField(service, "enabled", false);

        for (int i = 0; i < 10; i++) {
            assertThat(service.recordFailure("user6", "203.0.113.40").allowed()).isTrue();
        }
        assertThat(service.isLocked("user6")).isFalse();
        assertThat(service.remainingLockSeconds("user6")).isZero();
        assertThat(service.evaluate("user6", "203.0.113.40").allowed()).isTrue();
    }

    @Test
    void superAdminIsNotAccountLockedByUsernameOnly() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("lzh", "203.0.113." + i);
        }
        assertThat(service.isLocked("lzh")).isFalse();
        assertThat(service.remainingLockSeconds("lzh")).isZero();
        assertThat(service.accountStepUpRequired("lzh")).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void exemptUsernamesAreCaseInsensitive() {
        ObjectProvider<RedisTemplate<String, Object>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        service = new LoginAttemptService(provider, "SuperAdmin,BackupAdmin");
        configure(service, false);

        for (int i = 0; i < 10; i++) {
            service.recordFailure("superadmin", "203.0.113.50");
            service.recordFailure("backupadmin", "203.0.113.50");
        }
        assertThat(service.evaluate("SuperAdmin", "203.0.113.50").allowed()).isTrue();
        assertThat(service.evaluate("BackupAdmin", "203.0.113.50").allowed()).isTrue();
    }

    @Test
    void blankUsernameNeverLocked() {
        assertThat(service.isLocked("")).isFalse();
        assertThat(service.remainingLockSeconds("")).isZero();
        assertThat(service.recordFailure("", "203.0.113.60").allowed()).isTrue();
        assertThat(service.evaluate("  ", "203.0.113.60").allowed()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisJacksonMapPayloadStillCountsTowardAccountRisk() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);

        Map<String, Object> redisPayload = new LinkedHashMap<>();
        redisPayload.put("failures", 4);
        redisPayload.put("lastFailureAt", System.currentTimeMillis());
        when(operations.get("brute-force:mapuser")).thenReturn(redisPayload);

        service = serviceWithRedis(redisTemplate);

        LoginAttemptService.LoginAttemptDecision decision =
                service.recordFailure("MapUser", "203.0.113.70");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.stepUpRequired()).isTrue();
        assertThat(service.failureCount("MapUser")).isEqualTo(5);
        verify(operations).set(
                eq("brute-force:mapuser"),
                argThat(value -> value instanceof String text && text.startsWith("5:")),
                eq(7L * 24 * 60 * 60),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisStringPayloadStillCountsTowardAccountRisk() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get("brute-force:stringuser")).thenReturn("4:" + System.currentTimeMillis());

        service = serviceWithRedis(redisTemplate);

        assertThat(service.recordFailure("StringUser", "203.0.113.80").stepUpRequired()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsInMemoryFallbackWhenRedisReadFailsAfterSuccessfulSave() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get("brute-force:redisdown")).thenThrow(new RuntimeException("redis down"));

        service = serviceWithRedis(redisTemplate);

        assertThat(service.recordFailure("redisdown", "203.0.113.90").allowed()).isTrue();
        assertThat(service.failureCount("redisdown")).isEqualTo(1);
    }

    private void configure(LoginAttemptService target, boolean redisEnabled) {
        ReflectionTestUtils.setField(target, "enabled", true);
        ReflectionTestUtils.setField(target, "maxAttempts", 5);
        ReflectionTestUtils.setField(target, "accountStepUpAt", 3);
        ReflectionTestUtils.setField(target, "pairMaxAttempts", 5);
        ReflectionTestUtils.setField(target, "ipMaxAttempts", 3);
        ReflectionTestUtils.setField(target, "networkMaxAttempts", 6);
        ReflectionTestUtils.setField(target, "redisEnabled", redisEnabled);
    }

    @SuppressWarnings("unchecked")
    private LoginAttemptService serviceWithRedis(RedisTemplate<String, Object> redisTemplate) {
        ObjectProvider<RedisTemplate<String, Object>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        LoginAttemptService redisService = new LoginAttemptService(provider, "");
        configure(redisService, true);
        return redisService;
    }
}
