package com.tibet.tourism.common.security;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsrfCookieFilterTest {

    private static final String SECRET = "test-csrf-secret-that-is-long-enough-for-hmac-signing-2026-abcdef-abcdef";
    private static final String SESSION_TOKEN = "session.jwt.value";

    private CsrfTokenService csrfTokenService;
    private CsrfCookieFilter csrfCookieFilter;

    @BeforeEach
    void setUp() {
        csrfTokenService = new CsrfTokenService(SECRET);
        csrfCookieFilter = new CsrfCookieFilter(csrfTokenService, new TrustedProxyIpResolver(), "http://localhost:5173");
    }

    @Test
    void safeMethodBypassesCsrfCheck() throws Exception {
        MockHttpServletRequest request = apiRequest("GET", "/api/auth/me");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void stateChangingRequestWithoutAuthCookieBypassesCsrfCheck() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/login");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void loginRequestWithStaleAuthCookieBypassesCsrfCheck() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/login");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void publicLoginRejectsCrossSiteOriginEvenWithoutAuthCookie() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "https://evil.example");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void publicLoginAcceptsAllowedOriginWithoutCsrfToken() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Sec-Fetch-Site", "same-site");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void publicRegisterRejectsCrossSiteFetchMetadataWithoutAuthCookie() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/register");
        request.addHeader("Sec-Fetch-Site", "cross-site");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void publicGuideChatRejectsCrossSiteOriginWithoutAuthCookie() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/guide/chat");
        request.addHeader("Origin", "https://evil.example");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void publicGuideChatRejectsCrossSiteFetchMetadataWithoutAuthCookie() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/guide/chat");
        request.addHeader("Sec-Fetch-Site", "cross-site");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void publicGuideChatAcceptsAnonymousServerCallWithoutBrowserMetadata() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/guide/chat");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void publicGuideChatWithAuthCookieRequiresCsrfToken() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/guide/chat");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Sec-Fetch-Site", "same-site");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void publicGuideChatWithAuthCookieAcceptsSignedDoubleSubmitToken() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/guide/chat");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Sec-Fetch-Site", "same-site");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void wildcardAllowedOriginDoesNotAuthorizeCsrfOrigin() throws Exception {
        CsrfCookieFilter filter = new CsrfCookieFilter(csrfTokenService, new TrustedProxyIpResolver(), "https://*.example.com,https://app.example.com");
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);
        request.addHeader("Origin", "https://foo.example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void registerRequestWithStaleAuthCookieBypassesCsrfCheck() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/register");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void stateChangingRequestWithAuthCookieRequiresCsrfToken() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void stateChangingApiRequestUsesRequestUriWhenServletPathIsBlank() throws Exception {
        MockHttpServletRequest request = apiRequestWithBlankServletPath(
                "POST",
                "/api/auth/me/change-password");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));
        request.addHeader("Origin", "http://localhost:5173");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void stateChangingApiRequestStripsContextPathWhenServletPathIsBlank() throws Exception {
        MockHttpServletRequest request = apiRequestWithBlankServletPath(
                "POST",
                "/app/api/auth/me/change-password");
        request.setContextPath("/app");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));
        request.addHeader("Origin", "http://localhost:5173");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void publicPostUsesRequestUriWhenServletPathIsBlankForBrowserMetadataChecks() throws Exception {
        MockHttpServletRequest request = apiRequestWithBlankServletPath("POST", "/api/auth/login");
        request.addHeader("Origin", "https://evil.example");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void stateChangingRequestWithAuthCookieAndBearerStillRequiresCsrfToken() throws Exception {
        MockHttpServletRequest request = apiRequest("POST", "/api/orders");
        request.addHeader("Authorization", "Bearer api-token");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN));

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void stateChangingRequestAcceptsSignedDoubleSubmitToken() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Sec-Fetch-Site", "same-site");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void stateChangingRequestRejectsCrossSiteOriginEvenWithValidCsrfToken() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);
        request.addHeader("Origin", "https://evil.example");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void stateChangingRequestAcceptsOriginMatchingRequestOrigin() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);
        request.addHeader("Origin", "http://localhost:8080");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void stateChangingRequestAcceptsSameOriginFetchMetadataWithoutOrigin() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);
        request.addHeader("Sec-Fetch-Site", "same-origin");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(200, response.getStatus());
    }

    @Test
    void stateChangingRequestRejectsMissingOriginAndFetchMetadataEvenWithValidCsrfToken() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void stateChangingRequestRejectsTamperedHeaderToken() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken + "x");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void stateChangingRequestRejectsCrossSiteFetchMetadata() throws Exception {
        String csrfToken = csrfTokenService.generateToken(SESSION_TOKEN);
        MockHttpServletRequest request = apiRequest("POST", "/api/auth/me/change-password");
        request.setCookies(
                new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, SESSION_TOKEN),
                new Cookie(CookieAuthConstants.CSRF_COOKIE_NAME, csrfToken));
        request.addHeader(CookieAuthConstants.CSRF_HEADER_NAME, csrfToken);
        request.addHeader("Sec-Fetch-Site", "cross-site");

        MockHttpServletResponse response = doFilter(request);

        assertEquals(403, response.getStatus());
    }

    private MockHttpServletRequest apiRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        return request;
    }

    private MockHttpServletRequest apiRequestWithBlankServletPath(String method, String path) {
        MockHttpServletRequest request = apiRequest(method, path);
        request.setServletPath("");
        return request;
    }

    private MockHttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        csrfCookieFilter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
