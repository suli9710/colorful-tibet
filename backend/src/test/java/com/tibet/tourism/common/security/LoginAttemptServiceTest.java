package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        service = new LoginAttemptService(provider, "");
        configure(service, false);
    }

    @Test
    void accountFailuresBelowThresholdDoNotHardLockUsername() {
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
    void accountStepUpThresholdStillAllowsUntilHardLockThreshold() {
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
    void accountMaxAttemptsHardLocksUsernameAcrossDistributedSources() {
        for (int i = 1; i < 5; i++) {
            LoginAttemptService.LoginAttemptDecision decision =
                    service.recordFailure("locked-user", "198.51." + i + ".10");
            assertThat(decision.allowed()).isTrue();
        }

        LoginAttemptService.LoginAttemptDecision fifthFailure =
                service.recordFailure("locked-user", "198.51.5.10");

        assertThat(fifthFailure.allowed()).isFalse();
        assertThat(fifthFailure.reason()).isEqualTo("account");
        assertThat(fifthFailure.stepUpRequired()).isTrue();
        assertThat(fifthFailure.accountFailures()).isEqualTo(5);
        assertThat(fifthFailure.retryAfterSeconds()).isPositive();
        assertThat(service.isLocked("locked-user")).isTrue();
        assertThat(service.remainingLockSeconds("locked-user")).isPositive();

        LoginAttemptService.LoginAttemptDecision fromNewIp =
                service.evaluate("locked-user", "203.0.200.10");
        assertThat(fromNewIp.allowed()).isFalse();
        assertThat(fromNewIp.reason()).isEqualTo("account");
        assertThat(fromNewIp.stepUpRequired()).isTrue();
        assertThat(fromNewIp.accountFailures()).isEqualTo(5);
        assertThat(service.remainingLockSeconds("locked-user", "203.0.201.10")).isPositive();
    }

    @Test
    void pairLimitBlocksSameIpAndUsernameOnly() {
        ReflectionTestUtils.setField(service, "maxAttempts", 10);
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
    void superAdminNameIsStillAccountLockedByUsernameUnlessExempt() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("lzh", "203.0." + i + ".10");
        }

        assertThat(service.isLocked("lzh")).isTrue();
        assertThat(service.remainingLockSeconds("lzh")).isPositive();
        assertThat(service.accountStepUpRequired("lzh")).isTrue();

        LoginAttemptService.LoginAttemptDecision fromNewIp = service.evaluate("lzh", "203.0.200.10");
        assertThat(fromNewIp.allowed()).isFalse();
        assertThat(fromNewIp.reason()).isEqualTo("account");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exemptUsernamesAreCaseInsensitive() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
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
    void redisStringPayloadAtThresholdLocksAccount() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get(argThat((String key) -> key != null
                && key.startsWith("brute-force:acct:")
                && !key.contains("stringuser"))))
                .thenReturn("5:" + System.currentTimeMillis());

        service = serviceWithRedis(redisTemplate);

        LoginAttemptService.LoginAttemptDecision decision =
                service.evaluate("StringUser", "203.0.113.80");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("account");
        assertThat(decision.stepUpRequired()).isTrue();
        assertThat(decision.retryAfterSeconds()).isPositive();
        assertThat(service.remainingLockSeconds("StringUser")).isPositive();
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsInMemoryFallbackWhenRedisReadFailsAfterSuccessfulSave() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get(argThat((String key) -> key != null
                && key.startsWith("brute-force:acct:")
                && !key.contains("redisdown"))))
                .thenThrow(new RuntimeException("redis down"));

        service = serviceWithRedis(redisTemplate);

        assertThat(service.recordFailure("redisdown", "203.0.113.90").allowed()).isTrue();
        assertThat(service.failureCount("redisdown")).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisKeysDoNotContainRawUsernameOrEmail() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);

        service = serviceWithRedis(redisTemplate);

        service.recordFailure("Traveler.Email@example.com", "203.0.113.91");

        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(operations, org.mockito.Mockito.atLeastOnce())
                .set(keyCaptor.capture(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        assertThat(keyCaptor.getAllValues())
                .allSatisfy(key -> assertThat(key)
                        .startsWith("brute-force:")
                        .doesNotContain("Traveler", "traveler", "Email", "email", "example.com", "@"));
    }

    private void configure(LoginAttemptService target, boolean redisEnabled) {
        ReflectionTestUtils.setField(target, "enabled", true);
        ReflectionTestUtils.setField(target, "maxAttempts", 5);
        ReflectionTestUtils.setField(target, "accountStepUpAt", 3);
        ReflectionTestUtils.setField(target, "pairMaxAttempts", 5);
        ReflectionTestUtils.setField(target, "ipMaxAttempts", 3);
        ReflectionTestUtils.setField(target, "networkMaxAttempts", 6);
        ReflectionTestUtils.setField(target, "redisEnabled", redisEnabled);
        ReflectionTestUtils.setField(target, "redisFailClosed", false);
    }

    @SuppressWarnings("unchecked")
    private LoginAttemptService serviceWithRedis(StringRedisTemplate redisTemplate) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        LoginAttemptService redisService = new LoginAttemptService(provider, "");
        configure(redisService, true);
        return redisService;
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisReadFailureFailsClosedWhenStrict() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get(argThat((String key) -> key != null && key.startsWith("brute-force:"))))
                .thenThrow(new RuntimeException("redis down"));

        service = serviceWithRedis(redisTemplate);
        ReflectionTestUtils.setField(service, "redisFailClosed", true);

        LoginAttemptService.LoginAttemptDecision decision =
                service.evaluate("strict-user", "203.0.113.100");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("backend");
        assertThat(decision.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void missingRedisBackendFailsClosedWhenStrict() {
        configure(service, true);
        ReflectionTestUtils.setField(service, "redisFailClosed", true);

        LoginAttemptService.LoginAttemptDecision decision =
                service.evaluate("strict-user", "203.0.113.100");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("backend");
        assertThat(decision.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisWriteFailureFailsClosedWhenStrict() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(operations)
                .set(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());

        service = serviceWithRedis(redisTemplate);
        ReflectionTestUtils.setField(service, "redisFailClosed", true);

        LoginAttemptService.LoginAttemptDecision decision =
                service.recordFailure("strict-user", "203.0.113.100");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("backend");
        assertThat(decision.retryAfterSeconds()).isEqualTo(60);
    }
}
