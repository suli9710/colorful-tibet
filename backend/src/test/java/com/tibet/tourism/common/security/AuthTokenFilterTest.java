package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Test
    void invalidBearerHeaderClearsExistingSecurityContext() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("previous-user", "unused"));
        when(tokenRevocationService.isRevoked("bad-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("bad-token")).thenReturn(false);

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer bad-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authResolutionErrorClearsExistingSecurityContext() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("previous-user", "unused"));
        when(tokenRevocationService.isRevoked("valid-looking-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("valid-looking-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("valid-looking-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid-looking-token")).thenReturn("missing-user");
        when(userDetailsService.loadUserByUsername("missing-user")).thenThrow(new RuntimeException("not found"));

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer valid-looking-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authResolutionErrorLogDoesNotExposeBearerTokenOrRequestBody() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService);
        ListAppender<ILoggingEvent> appender = attachAppender();

        when(tokenRevocationService.isRevoked("valid-looking-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("valid-looking-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("valid-looking-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid-looking-token")).thenReturn("missing-user");
        when(userDetailsService.loadUserByUsername("missing-user"))
                .thenThrow(new RuntimeException(
                        "Authorization: Bearer valid-looking-token {\"password\":\"secret\"}"));

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer valid-looking-token");

        try {
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        } finally {
            detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getFormattedMessage()).contains("type=RuntimeException");
            assertThat(event.getFormattedMessage()).contains("messageHash=");
        });
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains("valid-looking-token")
                        || message.contains("Bearer")
                        || message.contains("\"password\"")
                        || message.contains("secret"));
    }

    @Test
    void adminDebugLogDoesNotExposeRawUsername() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService);
        ListAppender<ILoggingEvent> appender = attachDebugAppender();
        String rawUsername = "admin.secret@example.com";

        when(tokenRevocationService.isRevoked("admin-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("admin-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("admin-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("admin-token")).thenReturn(rawUsername);
        when(userDetailsService.loadUserByUsername(rawUsername)).thenReturn(User
                .withUsername(rawUsername)
                .password("unused")
                .roles("ADMIN")
                .build());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.setServletPath("/api/admin/users");
        request.addHeader("Authorization", "Bearer admin-token");

        try {
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        } finally {
            detachDebugAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event ->
                assertThat(event.getFormattedMessage()).contains("Admin auth OK: user=user#"));
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains(rawUsername));
    }

    private MockHttpServletRequest authenticatedApiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setServletPath("/api/orders");
        return request;
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthTokenFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthTokenFilter.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private ListAppender<ILoggingEvent> attachDebugAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthTokenFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        return appender;
    }

    private void detachDebugAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthTokenFilter.class);
        logger.detachAppender(appender);
        logger.setLevel(null);
        appender.stop();
    }
}
