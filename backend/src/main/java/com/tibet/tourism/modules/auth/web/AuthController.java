package com.tibet.tourism.modules.auth.web;
import com.tibet.tourism.common.security.CookieAuthConstants;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.UserSessionVersionService;
import com.tibet.tourism.modules.auth.application.AuthApplicationService;
import com.tibet.tourism.modules.auth.application.LoginResult;
import com.tibet.tourism.modules.auth.domain.AuthFailureException;
import com.tibet.tourism.modules.auth.domain.AuthForbiddenException;
import com.tibet.tourism.modules.auth.domain.AuthRateLimitException;
import com.tibet.tourism.modules.auth.domain.DuplicateRegistrationException;
import com.tibet.tourism.modules.auth.domain.SecondaryAuthRequiredException;
import com.tibet.tourism.modules.auth.web.dto.LoginRequest;
import com.tibet.tourism.modules.auth.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Duration AUTH_COOKIE_MAX_AGE = Duration.ofDays(1);

    private final AuthApplicationService authApplicationService;
    private final TokenRevocationService tokenRevocationService;
    private final UserSessionVersionService userSessionVersionService;
    private final boolean secureCookies;

    public AuthController(
            AuthApplicationService authApplicationService,
            TokenRevocationService tokenRevocationService,
            UserSessionVersionService userSessionVersionService,
            @Value("${app.security.cookie-secure:true}") boolean secureCookies) {
        this.authApplicationService = authApplicationService;
        this.tokenRevocationService = tokenRevocationService;
        this.userSessionVersionService = userSessionVersionService;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            LoginResult result = authApplicationService.login(loginRequest, request);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, authCookie(result.jwt(), AUTH_COOKIE_MAX_AGE).toString())
                    .header(HttpHeaders.SET_COOKIE, csrfCookie(result.csrfToken(), AUTH_COOKIE_MAX_AGE).toString())
                    .body(result.user());
        } catch (AuthRateLimitException exception) {
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
            if (exception.getRetryAfterSeconds() > 0) {
                builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()));
            }
            return builder.body(Map.of("error", exception.getMessage()));
        } catch (SecondaryAuthRequiredException exception) {
            return ResponseEntity.status(449).body(Map.of(
                    "error", exception.getMessage(),
                    "requiresSecondaryAuth", true));
        } catch (AuthForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
        } catch (AuthFailureException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            tokenRevocationService.revoke(token);
            userSessionVersionService.invalidateTokenSubject(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookie("", Duration.ZERO).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest, HttpServletRequest request) {
        try {
            authApplicationService.register(signUpRequest, request);
            return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
        } catch (AuthForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (DuplicateRegistrationException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private ResponseCookie authCookie(String value, Duration maxAge) {
        return ResponseCookie.from(CookieAuthConstants.AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie csrfCookie(String value, Duration maxAge) {
        return ResponseCookie.from(CookieAuthConstants.CSRF_COOKIE_NAME, value)
                .httpOnly(false)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String resolveToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        var authCookie = WebUtils.getCookie(request, CookieAuthConstants.AUTH_COOKIE_NAME);
        return authCookie == null ? null : authCookie.getValue();
    }
}
