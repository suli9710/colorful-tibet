package com.tibet.tourism.modules.ai.application;

public class AiRouteQuotaExceededException extends RuntimeException {

    private final int remaining;

    public AiRouteQuotaExceededException(String message, int remaining) {
        super(message);
        this.remaining = remaining;
    }

    public int getRemaining() {
        return remaining;
    }
}
