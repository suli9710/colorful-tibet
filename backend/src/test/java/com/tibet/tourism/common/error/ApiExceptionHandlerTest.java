package com.tibet.tourism.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tibet.tourism.modules.auth.web.dto.LoginRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new LoginProbeController())
            .setControllerAdvice(handler)
            .build();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private boolean originalAdditive;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        originalAdditive = logger.isAdditive();
        logger.setAdditive(false);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setAdditive(originalAdditive);
        appender.stop();
    }

    @Test
    void reportsDataIntegrityViolationAsRetryableConflictWithoutLeakingDetail() {
        // This handler is what turns a concurrent duplicate submit into a retryable client error:
        // OrderCenterService can no longer recover in-place, because the constraint violation marks
        // the transaction rollback-only. Reported as 500 it looked like a server fault and the client
        // had no reason to retry.
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry 'idem-key-1' for key 'orders.uk_orders_user_idempotency'");

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error"))
                .doesNotContain("idem-key-1")
                .doesNotContain("uk_orders_user_idempotency");
        assertThat(appender.list).noneMatch(this::isErrorOrHigher);
    }

    @Test
    void handlesAsyncClientDisconnectWithoutErrorLog() {
        AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException(
                "ServletOutputStream failed to flush: java.io.IOException: Broken pipe");

        handler.handleAsyncRequestNotUsable(exception);

        assertThat(ApiExceptionHandler.isClientDisconnect(exception)).isTrue();
        assertThat(appender.list).noneMatch(this::isErrorOrHigher);
    }

    @Test
    void suppressesWrappedClientDisconnectsInFallbackHandler() {
        IOException exception = new IOException("ServletOutputStream failed to flush",
                new IOException("Broken pipe"));

        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(appender.list).noneMatch(this::isErrorOrHigher);
    }

    @Test
    void handlesMalformedJsonAsBadRequestWithoutErrorLog() {
        ResponseEntity<Map<String, String>> response = handler.handleUnreadableMessage(
                new HttpMessageNotReadableException("JSON parse error"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid request");
        assertThat(appender.list).noneMatch(this::isErrorOrHigher);
    }

    @Test
    void validationConstraintAndMissingParameterUseStableBadRequestMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
        assertSafeResponse(handler.handleConstraintViolation(
                        new ConstraintViolationException("constraint token=secret", Set.of())),
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "constraint token=secret");
        assertSafeResponse(handler.handleMissingParameter(
                        new MissingServletRequestParameterException("email=suli@example.com", "String")),
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "email=suli@example.com");
    }

    @Test
    void illegalArgumentUsesGenericClientMessageAndLogsSanitizedSummary() {
        String secretMessage = "Authorization: Bearer token-from-request-body {\"password\":\"secret\"}";

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException(secretMessage));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid request");
        assertThat(response.getBody()).doesNotContainValue(secretMessage);
        assertThat(formattedLogMessages()).anySatisfy(message -> {
            assertThat(message).contains("type=IllegalArgumentException");
            assertThat(message).contains("messageHash=");
        });
        assertThat(formattedLogMessages()).noneMatch(this::containsSecretProbe);
    }

    @Test
    void illegalStateUsesGenericClientMessageAndLogsSanitizedSummary() {
        String secretMessage = "AI stream upstream error: {\"token\":\"route-body-token\"}";

        ResponseEntity<Map<String, String>> response = handler.handleIllegalState(
                new IllegalStateException(secretMessage));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "当前状态不允许该操作");
        assertThat(response.getBody()).doesNotContainValue(secretMessage);
        assertThat(formattedLogMessages()).anySatisfy(message -> {
            assertThat(message).contains("type=IllegalStateException");
            assertThat(message).contains("messageHash=");
        });
        assertThat(formattedLogMessages()).noneMatch(this::containsSecretProbe);
    }

    @Test
    void securityExceptionUsesGenericClientMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleSecurity(
                new SecurityException("Bearer token-from-request-body"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "无权访问该资源");
        assertThat(response.getBody()).doesNotContainValue("Bearer token-from-request-body");
    }

    @Test
    void authAuthorizationNotFoundAndBusinessErrorsUseStableMessages() {
        assertSafeResponse(handler.handleAuthenticationRequired(
                        new AuthenticationRequiredException("auth raw token=secret")),
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "auth raw token=secret");
        assertSafeResponse(handler.handleUnauthorizedAction(
                        new UnauthorizedActionException("owner email=suli@example.com")),
                HttpStatus.FORBIDDEN,
                "Access denied",
                "owner email=suli@example.com");
        assertSafeResponse(handler.handleNotFound(
                        new ResourceNotFoundException("missing private-id=12345")),
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "missing private-id=12345");
        assertSafeResponse(handler.handleBusiness(
                        new BusinessException("bad request password=secret")),
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "bad request password=secret");

        assertThat(formattedLogMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("token=secret");
            assertThat(message).doesNotContain("suli@example.com");
            assertThat(message).doesNotContain("private-id=12345");
            assertThat(message).doesNotContain("password=secret");
        });
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_XML)
                        .content("<login><username>x</username><password>y</password></login>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("不支持的请求内容类型"));
    }

    @Test
    void logsUnexpectedServerErrors() {
        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(
                new RuntimeException("request body {\"token\":\"token-from-request-body\"}"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(appender.list)
                .anyMatch(event -> event.getLevel().equals(Level.ERROR)
                        && event.getFormattedMessage().contains("Unhandled API exception")
                        && event.getThrowableProxy() == null);
        assertThat(formattedLogMessages()).noneMatch(this::containsSecretProbe);
    }

    private boolean isErrorOrHigher(ILoggingEvent event) {
        return event.getLevel().levelInt >= Level.ERROR_INT;
    }

    private void assertSafeResponse(ResponseEntity<Map<String, String>> response,
                                    HttpStatus status,
                                    String safeMessage,
                                    String rawMessage) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).containsEntry("error", safeMessage);
        assertThat(response.getBody()).doesNotContainValue(rawMessage);
    }

    private java.util.List<String> formattedLogMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private boolean containsSecretProbe(String message) {
        return message.contains("Bearer")
                || message.contains("token-from-request-body")
                || message.contains("route-body-token")
                || message.contains("\"password\"")
                || message.contains("\"token\"");
    }

    @RestController
    private static class LoginProbeController {
        @PostMapping(path = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String login(@Valid @RequestBody LoginRequest request) {
            return "ok";
        }
    }
}
