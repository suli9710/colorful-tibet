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
import com.tibet.tourism.modules.auth.application.AdminMfaPolicy;
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
        AdminMfaPolicy adminMfaPolicy = mock(AdminMfaPolicy.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService,
                adminMfaPolicy);

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
    void validCookieAuthenticatesOptionalPublicSpotDetail() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService);

        when(tokenRevocationService.isRevoked("public-session-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("public-session-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("public-session-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("public-session-token")).thenReturn("traveler");
        when(userDetailsService.loadUserByUsername("traveler")).thenReturn(User
                .withUsername("traveler")
                .password("unused")
                .roles("USER")
                .build());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/spots/42");
        request.setServletPath("/api/spots/42");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, "public-session-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .extracting(authentication -> authentication.getName())
                .isEqualTo("traveler");
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
        AdminMfaPolicy adminMfaPolicy = mock(AdminMfaPolicy.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService,
                adminMfaPolicy);
        ListAppender<ILoggingEvent> appender = attachDebugAppender();
        String rawUsername = "admin.secret@example.com";

        when(tokenRevocationService.isRevoked("admin-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("admin-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("admin-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("admin-token")).thenReturn(rawUsername);
        when(jwtUtils.isMfaVerified("admin-token")).thenReturn(true);
        when(jwtUtils.getMfaBindingFromJwtToken("admin-token"))
                .thenReturn("0123456789abcdef0123456789abcdef");
        when(adminMfaPolicy.matchesCurrentBinding(
                rawUsername, "0123456789abcdef0123456789abcdef"))
                .thenReturn(true);
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

    @Test
    void administratorRequiresSignedMethodsAndCurrentMfaCredentialBinding() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AdminMfaPolicy adminMfaPolicy = mock(AdminMfaPolicy.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils,
                userDetailsService,
                tokenRevocationService,
                userSessionVersionService,
                adminMfaPolicy);

        when(tokenRevocationService.isRevoked(anyString())).thenReturn(false);
        when(jwtUtils.validateJwtToken(anyString())).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession(anyString())).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("admin-no-amr")).thenReturn("admin");
        when(jwtUtils.getUserNameFromJwtToken("admin-pwd-only")).thenReturn("admin");
        when(jwtUtils.getUserNameFromJwtToken("admin-pwd-otp-no-binding")).thenReturn("admin");
        when(jwtUtils.getUserNameFromJwtToken("admin-stale-binding")).thenReturn("admin");
        when(jwtUtils.getUserNameFromJwtToken("admin-current-binding")).thenReturn("admin");
        when(jwtUtils.isMfaVerified("admin-pwd-otp-no-binding")).thenReturn(true);
        when(jwtUtils.isMfaVerified("admin-stale-binding")).thenReturn(true);
        when(jwtUtils.isMfaVerified("admin-current-binding")).thenReturn(true);
        when(jwtUtils.getMfaBindingFromJwtToken("admin-stale-binding"))
                .thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        when(jwtUtils.getMfaBindingFromJwtToken("admin-current-binding"))
                .thenReturn("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        when(adminMfaPolicy.matchesCurrentBinding(
                "admin", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
                .thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(User
                .withUsername("admin")
                .password("unused")
                .roles("ADMIN")
                .build());

        assertAdminTokenRejected(filter, "admin-no-amr");
        assertAdminTokenRejected(filter, "admin-pwd-only");
        assertAdminTokenRejected(filter, "admin-pwd-otp-no-binding");
        assertAdminTokenRejected(filter, "admin-stale-binding");

        MockHttpServletRequest accepted = authenticatedApiRequest();
        accepted.addHeader("Authorization", "Bearer admin-current-binding");
        filter.doFilter(accepted, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .extracting(authentication -> authentication.getName())
                .isEqualTo("admin");
    }

    @Test
    void ordinaryUserTokenRemainsCompatibleWithoutMfaClaim() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils, userDetailsService, tokenRevocationService, userSessionVersionService);

        when(tokenRevocationService.isRevoked("user-old-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("user-old-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("user-old-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("user-old-token")).thenReturn("traveler");
        when(userDetailsService.loadUserByUsername("traveler")).thenReturn(User
                .withUsername("traveler")
                .password("unused")
                .roles("USER")
                .build());

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer user-old-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .extracting(authentication -> authentication.getName())
                .isEqualTo("traveler");
    }

    private void assertAdminTokenRejected(AuthTokenFilter filter, String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void revokedTokenDoesNotAuthenticateEvenThoughItStillValidates() throws Exception {
        // Every other test in this file stubs isRevoked -> false, so revocation was never actually
        // exercised: a broken TokenRevocationService wiring would have passed the whole suite.
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils, userDetailsService, tokenRevocationService, userSessionVersionService);

        when(tokenRevocationService.isRevoked("revoked-token")).thenReturn(true);

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer revoked-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void tokenFromASupersededSessionDoesNotAuthenticate() throws Exception {
        // Same gap for the session-version check: it is what makes "log out everywhere" and forced
        // password changes actually invalidate tokens that are otherwise still signed and unexpired.
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        UserSessionVersionService userSessionVersionService = mock(UserSessionVersionService.class);
        AuthTokenFilter filter = new AuthTokenFilter(
                jwtUtils, userDetailsService, tokenRevocationService, userSessionVersionService);

        when(tokenRevocationService.isRevoked("stale-session-token")).thenReturn(false);
        when(jwtUtils.validateJwtToken("stale-session-token")).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession("stale-session-token")).thenReturn(false);

        MockHttpServletRequest request = authenticatedApiRequest();
        request.addHeader("Authorization", "Bearer stale-session-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
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
