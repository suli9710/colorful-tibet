package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtAuthSupportTest {

    private static final String TOKEN = "jwt-token";

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
    private final UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
    private final JwtAuthSupport support = new JwtAuthSupport(
            jwtUtils,
            tokenRevocationService,
            userSessionVersionService);

    @Test
    void optionalCurrentUserTreatsRevocationFailureAsAnonymous() {
        when(jwtUtils.validateJwtToken(TOKEN)).thenReturn(true);
        when(tokenRevocationService.isRevoked(TOKEN))
                .thenThrow(new IllegalStateException("redis password=secret-token"));

        Optional<User> result = support.resolveOptionalCurrentUser(bearerRequest());

        assertThat(result).isEmpty();
        verify(userSessionVersionService, never()).resolveCurrentUser(TOKEN);
    }

    @Test
    void requiredCurrentUserTreatsRevocationFailureAsInvalidToken() {
        when(jwtUtils.validateJwtToken(TOKEN)).thenReturn(true);
        when(tokenRevocationService.isRevoked(TOKEN))
                .thenThrow(new IllegalStateException("redis password=secret-token"));

        assertThatThrownBy(() -> support.resolveCurrentUser(bearerRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid or expired JWT token")
                .hasMessageNotContaining("secret-token");

        verify(userSessionVersionService, never()).resolveCurrentUser(TOKEN);
    }

    @Test
    void optionalCurrentUserTreatsSessionLookupFailureAsAnonymous() {
        when(jwtUtils.validateJwtToken(TOKEN)).thenReturn(true);
        when(tokenRevocationService.isRevoked(TOKEN)).thenReturn(false);
        when(userSessionVersionService.resolveCurrentUser(TOKEN))
                .thenThrow(new IllegalStateException("stale session for traveler"));

        Optional<User> result = support.resolveOptionalCurrentUser(bearerRequest());

        assertThat(result).isEmpty();
    }

    @Test
    void requiredCurrentUserTreatsSessionLookupFailureAsInvalidToken() {
        when(jwtUtils.validateJwtToken(TOKEN)).thenReturn(true);
        when(tokenRevocationService.isRevoked(TOKEN)).thenReturn(false);
        when(userSessionVersionService.resolveCurrentUser(TOKEN))
                .thenThrow(new IllegalStateException("stale session for traveler"));

        assertThatThrownBy(() -> support.resolveCurrentUser(bearerRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid or expired JWT token")
                .hasMessageNotContaining("traveler");
    }

    private MockHttpServletRequest bearerRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }
}
