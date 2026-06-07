package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

class AuthTokenFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerHeaderTakesPrecedenceOverAuthCookie() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService);

        when(tokenRevocationService.isRevoked("bearer-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("bearer-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("bearer-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("bearer-token")).thenReturn("bearer-user");
        when(userDetailsService.loadUserByUsername("bearer-user")).thenReturn(User
                .withUsername("bearer-user")
                .password("unused")
                .roles("USER")
                .build());

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer bearer-token");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, "cookie-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("bearer-user");
        verify(jwtUtils).validateJwtToken("bearer-token");
        verify(jwtUtils, never()).validateJwtToken("cookie-token");
    }

    @Test
    void invalidBearerHeaderDoesNotFallBackToAuthCookie() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService);

        when(tokenRevocationService.isRevoked("bad-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("bad-token")).thenReturn(false);

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, "cookie-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtils).validateJwtToken("bad-token");
        verify(jwtUtils, never()).validateJwtToken("cookie-token");
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    private MockHttpServletRequest authenticatedApiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setServletPath("/api/orders");
        return request;
    }
}
