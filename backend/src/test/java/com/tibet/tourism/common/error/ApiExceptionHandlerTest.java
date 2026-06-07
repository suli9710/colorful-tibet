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
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
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
        assertThat(response.getBody()).containsEntry("error", "请求体格式不正确");
        assertThat(appender.list).noneMatch(this::isErrorOrHigher);
    }

    @Test
    void illegalArgumentUsesGenericClientMessageAndLogsOriginal() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("PII_KEYS and PII_ACTIVE_KID must be configured"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "请求参数不合法");
        assertThat(response.getBody()).doesNotContainValue("PII_KEYS and PII_ACTIVE_KID must be configured");
        assertThat(appender.list)
                .anyMatch(event -> event.getLevel().equals(Level.WARN)
                        && event.getFormattedMessage().contains("PII_KEYS"));
    }

    @Test
    void illegalStateUsesGenericClientMessageAndLogsOriginal() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalState(
                new IllegalStateException("AI stream upstream error: HTTP 502"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "当前状态不允许该操作");
        assertThat(response.getBody()).doesNotContainValue("AI stream upstream error: HTTP 502");
        assertThat(appender.list)
                .anyMatch(event -> event.getLevel().equals(Level.WARN)
                        && event.getFormattedMessage().contains("AI stream upstream error"));
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
        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(appender.list)
                .anyMatch(event -> event.getLevel().equals(Level.ERROR)
                        && event.getFormattedMessage().contains("Unhandled API exception"));
    }

    private boolean isErrorOrHigher(ILoggingEvent event) {
        return event.getLevel().levelInt >= Level.ERROR_INT;
    }

    @RestController
    private static class LoginProbeController {
        @PostMapping(path = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String login(@Valid @RequestBody LoginRequest request) {
            return "ok";
        }
    }
}
