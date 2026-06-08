package com.tibet.tourism.common.api;
import com.tibet.tourism.common.error.AuthenticationRequiredException;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponder {

    private static final Logger logger = LoggerFactory.getLogger(ApiErrorResponder.class);
    private static final String AUTHENTICATION_REQUIRED_MESSAGE = "Authentication required";
    private static final String ACCESS_DENIED_MESSAGE = "Access denied";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    private static final String REQUEST_FAILED_MESSAGE = "Request processing failed";

    public ResponseEntity<?> authenticatedRequest(Exception exception) {
        logger.warn("Authenticated user request failed: {}",
                SensitiveLogSanitizer.exceptionSummary(exception));
        if (exception instanceof AuthenticationRequiredException) {
            return error(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED_MESSAGE);
        }
        if (exception instanceof UnauthorizedActionException || exception instanceof SecurityException) {
            return error(HttpStatus.FORBIDDEN, ACCESS_DENIED_MESSAGE);
        }
        if (exception instanceof ResourceNotFoundException) {
            return error(HttpStatus.NOT_FOUND, RESOURCE_NOT_FOUND_MESSAGE);
        }
        if (exception instanceof BusinessException || exception instanceof IllegalArgumentException) {
            return error(HttpStatus.BAD_REQUEST, INVALID_REQUEST_MESSAGE);
        }
        return error(HttpStatus.BAD_REQUEST, REQUEST_FAILED_MESSAGE);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
