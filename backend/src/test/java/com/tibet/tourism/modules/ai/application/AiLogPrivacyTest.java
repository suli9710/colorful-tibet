package com.tibet.tourism.modules.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiLogPrivacyTest {

    @Test
    void userRefDoesNotExposeRawUserId() {
        String userRef = AiLogPrivacy.userRef(987654321L);

        assertTrue(userRef.startsWith("user:"));
        assertFalse(userRef.contains("987654321"));
        assertEquals(userRef, AiLogPrivacy.userRef(987654321L));
    }

    @Test
    void exceptionSummaryOmitsMessageBody() {
        RuntimeException exception = new RuntimeException(
                "Authorization: Bearer raw.jwt.token responseBody={\"secret\":\"value\"}");

        assertEquals("RuntimeException", AiLogPrivacy.exceptionSummary(exception));
    }
}
