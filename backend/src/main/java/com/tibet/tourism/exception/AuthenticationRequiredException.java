package com.tibet.tourism.exception;

public class AuthenticationRequiredException extends BusinessException {
    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
