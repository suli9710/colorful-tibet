package com.tibet.tourism.modules.auth.domain;

public class SecondaryAuthRequiredException extends RuntimeException {
    public SecondaryAuthRequiredException(String message) {
        super(message);
    }
}
