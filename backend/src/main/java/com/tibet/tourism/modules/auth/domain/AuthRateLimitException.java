package com.tibet.tourism.modules.auth.domain;

public class AuthRateLimitException extends RuntimeException {
    public AuthRateLimitException(String message) {
        super(message);
    }
}
