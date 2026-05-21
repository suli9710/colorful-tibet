package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
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

    private Authentication authentication(String username) {
        var principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("encoded")
                .roles("USER")
                .build();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private ObjectProvider<RedisTemplate<String, Object>> provider(RedisTemplate<String, Object> redisTemplate) {
        ObjectProvider<RedisTemplate<String, Object>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        return provider;
    }
}
