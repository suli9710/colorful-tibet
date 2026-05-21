package com.tibet.tourism.common.api;
import com.tibet.tourism.common.error.AuthenticationRequiredException;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import com.tibet.tourism.modules.user.domain.User;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponder {

    private static final Logger logger = LoggerFactory.getLogger(ApiErrorResponder.class);

    public ResponseEntity<?> authenticatedRequest(Exception exception) {
        logger.warn("Authenticated user request failed: {}", exception.getMessage());
        if (exception instanceof AuthenticationRequiredException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
        }
        if (exception instanceof UnauthorizedActionException || exception instanceof SecurityException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
        }
        if (exception instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
        }
        if (exception instanceof BusinessException || exception instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Request processing failed"));
    }
}
