package com.tibet.tourism.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void recordsFailuresWithoutLockBeforeThreshold() {
        assertThat(service.recordFailure("user1")).isZero();
        assertThat(service.recordFailure("user1")).isZero();
        assertThat(service.recordFailure("user1")).isZero();
        assertThat(service.recordFailure("user1")).isZero();

        assertThat(service.isLocked("user1")).isFalse();
        assertThat(service.remainingLockSeconds("user1")).isZero();
    }

    @Test
    void locksAccountAfterMaxAttempts() {
        for (int i = 0; i < 4; i++) {
            assertThat(service.recordFailure("user2")).isZero();
        }
        long lockSeconds = service.recordFailure("user2");

        assertThat(lockSeconds).isEqualTo(30);
        assertThat(service.isLocked("user2")).isTrue();
        assertThat(service.remainingLockSeconds("user2")).isPositive();
        assertThat(service.remainingLockSeconds("user2")).isLessThanOrEqualTo(30);
    }

    @Test
    void lockTimeFollowsFixedProgression() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("user3");
        }
        assertThat(service.recordFailure("user3")).isEqualTo(30);

        assertThat(service.recordFailure("user3")).isEqualTo(5 * 60);

        assertThat(service.recordFailure("user3")).isEqualTo(30 * 60);

        assertThat(service.recordFailure("user3")).isEqualTo(12 * 3600);

        assertThat(service.recordFailure("user3")).isEqualTo(7 * 86400);
    }

    @Test
    void lockTimeCappedAt7Days() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("user4");
        }
        service.recordFailure("user4");
        for (int i = 0; i < 20; i++) {
            service.recordFailure("user4");
        }

        long lockSeconds = service.recordFailure("user4");
        assertThat(lockSeconds).isEqualTo(7 * 86400);
    }

    @Test
    void resetClearsLock() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("user5");
        }
        assertThat(service.isLocked("user5")).isTrue();

        service.reset("user5");
        assertThat(service.isLocked("user5")).isFalse();
        assertThat(service.remainingLockSeconds("user5")).isZero();
    }

    @Test
    void usernameIsCaseInsensitive() {
        service.recordFailure("ADMIN");
        service.recordFailure("admin");

        assertThat(service.isLocked("Admin")).isFalse();

        for (int i = 0; i < 3; i++) {
            service.recordFailure("Admin");
        }
        assertThat(service.isLocked("admin")).isTrue();
    }

    @Test
    void disabledServiceAlwaysReturnsOpen() {
        ReflectionTestUtils.setField(service, "enabled", false);

        for (int i = 0; i < 10; i++) {
            assertThat(service.recordFailure("user6")).isZero();
        }
        assertThat(service.isLocked("user6")).isFalse();
        assertThat(service.remainingLockSeconds("user6")).isZero();
    }

    @Test
    void superAdminIsNotExempt() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("lzh");
        }
        assertThat(service.isLocked("lzh")).isTrue();
        assertThat(service.remainingLockSeconds("lzh")).isPositive();
    }

    @Test
    @SuppressWarnings("unchecked")
    void exemptUsernamesAreCaseInsensitive() {
        ObjectProvider<RedisTemplate<String, Object>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        service = new LoginAttemptService(provider, "SuperAdmin,BackupAdmin");
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "redisEnabled", false);

        for (int i = 0; i < 10; i++) {
            service.recordFailure("superadmin");
            service.recordFailure("backupadmin");
        }
        assertThat(service.isLocked("SuperAdmin")).isFalse();
        assertThat(service.isLocked("BackupAdmin")).isFalse();
    }

    @Test
    void blankUsernameNeverLocked() {
        assertThat(service.isLocked("")).isFalse();
        assertThat(service.isLocked("  ")).isFalse();
        assertThat(service.remainingLockSeconds("")).isZero();
        assertThat(service.recordFailure("")).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisJacksonMapPayloadStillCountsTowardLock() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);

        Map<String, Object> redisPayload = new LinkedHashMap<>();
        redisPayload.put("failures", 4);
        redisPayload.put("lastFailureAt", System.currentTimeMillis());
        when(operations.get("brute-force:mapuser")).thenReturn(redisPayload);

        service = serviceWithRedis(redisTemplate);

        assertThat(service.recordFailure("MapUser")).isEqualTo(30);
        verify(operations).set(
                eq("brute-force:mapuser"),
                argThat(value -> value instanceof String text && text.startsWith("5:")),
                eq(7L * 24 * 60 * 60),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisStringPayloadStillCountsTowardLock() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get("brute-force:stringuser")).thenReturn("4:" + System.currentTimeMillis());

        service = serviceWithRedis(redisTemplate);

        assertThat(service.recordFailure("StringUser")).isEqualTo(30);
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsInMemoryFallbackWhenRedisReadFailsAfterSuccessfulSave() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get("brute-force:redisdown")).thenThrow(new RuntimeException("redis down"));

        service = serviceWithRedis(redisTemplate);

        assertThat(service.recordFailure("redisdown")).isZero();
        assertThat(service.failureCount("redisdown")).isEqualTo(1);
    }

    private void configure(LoginAttemptService target, boolean redisEnabled) {
        ReflectionTestUtils.setField(target, "enabled", true);
        ReflectionTestUtils.setField(target, "maxAttempts", 5);
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
