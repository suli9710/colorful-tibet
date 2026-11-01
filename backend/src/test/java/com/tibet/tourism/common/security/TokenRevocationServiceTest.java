package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class TokenRevocationServiceTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "a".repeat(64));
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86_400_000);
        ReflectionTestUtils.setField(jwtUtils, "requireStrongSecrets", false);
        ReflectionTestUtils.setField(jwtUtils, "environment", new MockEnvironment());
        jwtUtils.validateJwtConfiguration();
    }

    @Test
    void revokedTokenIsRejectedFromLocalMirror() {
        TokenRevocationService service = new TokenRevocationService(provider(null), jwtUtils);
        String token = jwtUtils.generateJwtToken(authentication("traveler"));

        service.revoke(token);

        assertThat(service.isRevoked(token)).isTrue();
    }

    @Test
    void nonRevokedTokenIsAllowed() {
        TokenRevocationService service = new TokenRevocationService(provider(null), jwtUtils);
        String token = jwtUtils.generateJwtToken(authentication("traveler"));

        assertThat(service.isRevoked(token)).isFalse();
    }

    @Test
    void localRevocationMirrorHasHardMaximumSize() {
        TokenRevocationService service = new TokenRevocationService(provider(null), jwtUtils);

        assertThat(service.inMemoryMaximumSize()).isEqualTo(20_000);
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisFailureFallsBackToLocalRevocationState() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get(org.mockito.ArgumentMatchers.anyString())).thenThrow(new RuntimeException("redis down"));
        TokenRevocationService service = new TokenRevocationService(provider(redisTemplate), jwtUtils);
        String token = jwtUtils.generateJwtToken(authentication("traveler"));

        service.revoke(token);

        assertThat(service.isRevoked(token)).isTrue();
    }

    private Authentication authentication(String username) {
        var principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("encoded")
                .roles("USER")
                .build();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private ObjectProvider<StringRedisTemplate> provider(StringRedisTemplate redisTemplate) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        return provider;
    }
}
