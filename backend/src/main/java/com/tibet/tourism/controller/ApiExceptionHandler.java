package com.tibet.tourism.controller;

import com.tibet.tourism.exception.AuthenticationRequiredException;
import com.tibet.tourism.exception.BusinessException;
import com.tibet.tourism.exception.ResourceNotFoundException;
import com.tibet.tourism.exception.UnauthorizedActionException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数: " + exception.getParameterName()));
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationRequired(AuthenticationRequiredException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedAction(UnauthorizedActionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "请求参数不合法" : exception.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权访问该资源"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException exception) {
        String message = exception.getMessage() == null ? "无权访问该资源" : exception.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        String message = exception.getMessage() == null ? "当前状态不允许该操作" : exception.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "上传文件过大"));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        logger.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "数据已被其他操作修改，请刷新后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        logger.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "服务器处理失败，请稍后重试"));
    }
}
