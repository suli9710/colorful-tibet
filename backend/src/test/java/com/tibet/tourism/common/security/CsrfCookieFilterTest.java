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
        csrfCookieFilter = new CsrfCookieFilter(csrfTokenService, "http://localhost:5173");
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

    private MockHttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        csrfCookieFilter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
