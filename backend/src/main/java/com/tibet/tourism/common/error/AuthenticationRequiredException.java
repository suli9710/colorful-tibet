package com.tibet.tourism.common.error;

public class AuthenticationRequiredException extends BusinessException {
    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
