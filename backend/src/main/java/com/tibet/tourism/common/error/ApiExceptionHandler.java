package com.tibet.tourism.common.error;

import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent;
import com.tibet.tourism.common.security.AdminAccessDeniedAuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String AUTHENTICATION_REQUIRED_MESSAGE = "Authentication required";
    private static final String ACCESS_DENIED_MESSAGE = "Access denied";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    private static final String RESOURCE_ACCESS_DENIED_MESSAGE = "\u65e0\u6743\u8bbf\u95ee\u8be5\u8d44\u6e90";
    private static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE =
            "\u4e0d\u652f\u6301\u7684\u8bf7\u6c42\u5185\u5bb9\u7c7b\u578b";
    private static final String CONFLICT_MESSAGE =
            "\u5f53\u524d\u72b6\u6001\u4e0d\u5141\u8bb8\u8be5\u64cd\u4f5c";
    private static final String UPLOAD_TOO_LARGE_MESSAGE =
            "\u4e0a\u4f20\u6587\u4ef6\u8fc7\u5927";
    private static final String OPTIMISTIC_LOCK_MESSAGE =
            "\u6570\u636e\u5df2\u88ab\u5176\u4ed6\u64cd\u4f5c\u4fee\u6539\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5";
    private static final String INTERNAL_ERROR_MESSAGE =
            "\u670d\u52a1\u5668\u5904\u7406\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5";
    private final AdminAccessDeniedAuditPublisher accessDeniedAuditPublisher;

    public ApiExceptionHandler() {
        this.accessDeniedAuditPublisher = null;
    }

    @Autowired
    ApiExceptionHandler(ObjectProvider<AdminAccessDeniedAuditPublisher> auditPublisherProvider) {
        this.accessDeniedAuditPublisher = auditPublisherProvider.getIfAvailable();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Map.of("error", UNSUPPORTED_MEDIA_TYPE_MESSAGE));
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationRequired(AuthenticationRequiredException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", AUTHENTICATION_REQUIRED_MESSAGE));
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedAction(UnauthorizedActionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ACCESS_DENIED_MESSAGE));
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", RESOURCE_NOT_FOUND_MESSAGE));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        logger.warn("Rejected invalid API argument: {}", SensitiveLogSanitizer.exceptionSummary(exception));
        return ResponseEntity.badRequest().body(Map.of("error", INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(HttpServletRequest request) {
        publishAccessDeniedAudit(request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", RESOURCE_ACCESS_DENIED_MESSAGE));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", RESOURCE_ACCESS_DENIED_MESSAGE));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        logger.warn("Rejected invalid API state: {}", SensitiveLogSanitizer.exceptionSummary(exception));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", CONFLICT_MESSAGE));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", UPLOAD_TOO_LARGE_MESSAGE));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        logger.warn("Optimistic lock conflict: {}", SensitiveLogSanitizer.exceptionSummary(ex));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", OPTIMISTIC_LOCK_MESSAGE));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // A violated unique/foreign-key constraint is a conflict the caller can retry, not a server
        // fault. Reporting it as 500 also hid concurrent duplicate submissions behind a generic error.
        logger.warn("Data integrity conflict: {}", SensitiveLogSanitizer.exceptionSummary(ex));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", CONFLICT_MESSAGE));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException exception) {
        logClientDisconnect(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        if (isClientDisconnect(exception)) {
            logClientDisconnect(exception);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        logger.error("Unhandled API exception: {}", SensitiveLogSanitizer.exceptionSummary(exception));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", INTERNAL_ERROR_MESSAGE));
    }

    static boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            if ("org.apache.catalina.connector.ClientAbortException".equals(className)) {
                return true;
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset")
                        || normalized.contains("clientabortexception")
                        || normalized.contains("client aborted")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void logClientDisconnect(Throwable throwable) {
        if (logger.isDebugEnabled()) {
            logger.debug("Client disconnected before API response completed: {}",
                    SensitiveLogSanitizer.exceptionSummary(throwable));
        }
    }

    private void publishAccessDeniedAudit(HttpServletRequest request) {
        if (accessDeniedAuditPublisher == null) {
            return;
        }
        try {
            accessDeniedAuditPublisher.publish(
                    request,
                    AdminAccessDeniedAuditEvent.Source.METHOD_SECURITY);
        } catch (RuntimeException exception) {
            // Method authorization has already denied the operation; preserve its 403 response.
            logger.warn("Administrator method-denial audit failed: source=method_security, error={}",
                    SensitiveLogSanitizer.exceptionSummary(exception));
        }
    }
}
