package com.tibet.tourism.modules.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.UserSessionVersionService;
import com.tibet.tourism.modules.auth.application.AuthApplicationService;
import com.tibet.tourism.modules.auth.domain.AuthRateLimitException;
import com.tibet.tourism.modules.auth.domain.AuthFailureException;
import com.tibet.tourism.modules.auth.domain.DuplicateRegistrationException;
import com.tibet.tourism.modules.auth.domain.SecondaryAuthRequiredException;
import com.tibet.tourism.modules.auth.web.dto.LoginRequest;
import com.tibet.tourism.modules.auth.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthControllerTest {

    private final AuthApplicationService authApplicationService = mock(AuthApplicationService.class);
    private final AuthController controller = new AuthController(
            authApplicationService,
            mock(TokenRevocationService.class),
            mock(UserSessionVersionService.class),
            true);

    @Test
    void rateLimitedLoginReturnsStructuredRetrySeconds() {
        LoginRequest request = new LoginRequest();
        when(authApplicationService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new AuthRateLimitException("too many attempts", 45));

        ResponseEntity<?> response = controller.authenticateUser(request, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("45");
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("retryAfterSeconds", 45L);
        assertThat(body).containsEntry("error", "Too many requests, please try again later");
        assertThat(body.toString()).doesNotContain("too many attempts");
    }

    @Test
    void rateLimitedLoginReturnsStableRetryMetadataWhenDelayIsUnknown() {
        LoginRequest request = new LoginRequest();
        when(authApplicationService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new AuthRateLimitException("too many attempts", 0));

        ResponseEntity<?> response = controller.authenticateUser(request, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().containsKey(HttpHeaders.RETRY_AFTER)).isFalse();
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("retryAfterSeconds", 0L);
    }

    @Test
    void failedLoginDoesNotEchoDomainExceptionMessage() {
        LoginRequest request = new LoginRequest();
        when(authApplicationService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new AuthFailureException("user does not exist; password probe"));

        ResponseEntity<?> response = controller.authenticateUser(request, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        assertThat(response.getBody().toString()).contains("Invalid username or password");
        assertThat(response.getBody().toString()).doesNotContain("user does not exist");
        assertThat(response.getBody().toString()).doesNotContain("password probe");
    }

    @Test
    void unexpectedLoginExceptionDoesNotExposeRawMessage() {
        LoginRequest request = new LoginRequest();
        when(authApplicationService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new IllegalStateException("jdbc password=secret-token"));

        ResponseEntity<?> response = controller.authenticateUser(request, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().toString()).contains("Invalid username or password");
        assertThat(response.getBody().toString()).doesNotContain("secret-token");
    }

    @Test
    void secondaryAuthPromptPreservesFrontendFlag() {
        LoginRequest request = new LoginRequest();
        when(authApplicationService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new SecondaryAuthRequiredException("Secondary authentication required"));

        ResponseEntity<?> response = controller.authenticateUser(request, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(449);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("requiresSecondaryAuth", true);
    }

    @Test
    void duplicateRegistrationDoesNotExposeRawExceptionMessage() {
        RegisterRequest request = new RegisterRequest();
        doThrow(new DuplicateRegistrationException("username traveler already exists"))
                .when(authApplicationService)
                .register(any(RegisterRequest.class), any(HttpServletRequest.class));

        ResponseEntity<?> response = controller.registerUser(request, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("Registration failed, please check your input");
        assertThat(response.getBody().toString()).doesNotContain("traveler already exists");
    }
}
