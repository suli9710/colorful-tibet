package com.tibet.tourism.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tibet.tourism.common.error.AuthenticationRequiredException;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiErrorResponderSecurityTest {

    private static final String SECRET_MESSAGE = "Bearer raw-token password=secret@example.com";

    private final ApiErrorResponder responder = new ApiErrorResponder();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private boolean originalAdditive;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(ApiErrorResponder.class);
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
    void authenticatedRequestUsesStableMessagesWithoutRawExceptionText() {
        assertError(new AuthenticationRequiredException(SECRET_MESSAGE),
                HttpStatus.UNAUTHORIZED, "Authentication required");
        assertError(new UnauthorizedActionException(SECRET_MESSAGE),
                HttpStatus.FORBIDDEN, "Access denied");
        assertError(new ResourceNotFoundException(SECRET_MESSAGE),
                HttpStatus.NOT_FOUND, "Resource not found");
        assertError(new BusinessException(SECRET_MESSAGE),
                HttpStatus.BAD_REQUEST, "Invalid request");
        assertError(new IllegalArgumentException(SECRET_MESSAGE),
                HttpStatus.BAD_REQUEST, "Invalid request");
        assertError(new RuntimeException(SECRET_MESSAGE),
                HttpStatus.BAD_REQUEST, "Request processing failed");

        assertThat(formattedLogMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("type=AuthenticationRequiredException")
                        .contains("messageHash="));
        assertThat(formattedLogMessages())
                .allSatisfy(message -> assertThat(message).doesNotContain(SECRET_MESSAGE));
    }

    @SuppressWarnings("unchecked")
    private void assertError(Exception exception, HttpStatus status, String message) {
        ResponseEntity<?> response = responder.authenticatedRequest(exception);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat((Map<String, String>) response.getBody()).containsEntry("error", message);
        assertThat((Map<String, String>) response.getBody()).doesNotContainValue(SECRET_MESSAGE);
    }

    private java.util.List<String> formattedLogMessages() {
        return appender.list.stream()
                .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
