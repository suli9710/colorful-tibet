package com.tibet.tourism.modules.recommendation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationLogPrivacyTest {

    @Test
    void userRefDoesNotExposeRawUserId() {
        String userRef = RecommendationLogPrivacy.userRef(123456789L);

        assertTrue(userRef.startsWith("user:"));
        assertFalse(userRef.contains("123456789"));
        assertEquals(userRef, RecommendationLogPrivacy.userRef(123456789L));
    }

    @Test
    void exceptionSummaryOmitsMessageBody() {
        RuntimeException exception = new RuntimeException(
                "Authorization: Bearer raw.jwt.token responseBody={\"secret\":\"value\"}");

        String summary = RecommendationLogPrivacy.exceptionSummary(exception);

        assertTrue(summary.startsWith("type=RuntimeException,messageHash="));
        assertFalse(summary.contains("Authorization"));
        assertFalse(summary.contains("Bearer"));
        assertFalse(summary.contains("raw.jwt.token"));
        assertFalse(summary.contains("responseBody"));
        assertFalse(summary.contains(exception.getMessage()));
    }

    @Test
    void collectionSizeSummarizesPreferenceValues() {
        assertEquals(2, RecommendationLogPrivacy.collectionSize(List.of("temple", "hiking")));
        assertEquals(0, RecommendationLogPrivacy.collectionSize(null));
    }
}
