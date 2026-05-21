package com.tibet.tourism.modules.auth.domain;

public class AuthForbiddenException extends RuntimeException {
    public AuthForbiddenException(String message) {
        super(message);
    }
}
