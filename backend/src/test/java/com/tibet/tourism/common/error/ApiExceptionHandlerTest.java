package com.tibet.tourism.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
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
}
