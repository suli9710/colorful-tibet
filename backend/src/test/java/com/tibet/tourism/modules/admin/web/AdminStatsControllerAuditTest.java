package com.tibet.tourism.modules.admin.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.admin.application.AdminStatsService;
import com.tibet.tourism.modules.recommendation.application.ItemBasedRecommendationService;
import com.tibet.tourism.modules.recommendation.application.RecommendationEvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminStatsControllerAuditTest {

    @Mock
    private AdminStatsService adminStatsService;

    @Mock
    private ItemBasedRecommendationService itemBasedRecommendationService;

    @Mock
    private RecommendationEvaluationService recommendationEvaluationService;

    @Mock
    private AdminAuditLogService auditLogService;

    @Test
    void completedPrecomputeRecordsCompletedOutcome() {
        AdminAuditLogService.Attempt attempt = attempt();
        when(auditLogService.begin(
                "item_similarity", null, "item_similarity_precompute")).thenReturn(attempt);
        when(itemBasedRecommendationService.precomputeItemSimilarityMatrix()).thenReturn(true);

        var response = controller().precomputeItemSimilarity();

        assertThat(response.getBody()).containsEntry("success", true);
        verify(auditLogService).complete(
                attempt,
                AdminAuditLogService.Result.SUCCESS,
                "completed");
    }

    @Test
    void concurrentPrecomputeRecordsRejectedOutcome() {
        AdminAuditLogService.Attempt attempt = attempt();
        when(auditLogService.begin(
                "item_similarity", null, "item_similarity_precompute")).thenReturn(attempt);
        when(itemBasedRecommendationService.precomputeItemSimilarityMatrix()).thenReturn(false);

        var response = controller().precomputeItemSimilarity();

        assertThat(response.getBody()).containsEntry("success", false);
        verify(auditLogService).complete(
                attempt,
                AdminAuditLogService.Result.FAILURE,
                "already_in_progress");
    }

    @Test
    void failedPrecomputeRecordsOnlyExceptionTypeAndRethrows() {
        AdminAuditLogService.Attempt attempt = attempt();
        when(auditLogService.begin(
                "item_similarity", null, "item_similarity_precompute")).thenReturn(attempt);
        IllegalStateException failure = new IllegalStateException(
                "redisPassword=secret&jdbcUrl=private-host");
        when(itemBasedRecommendationService.precomputeItemSimilarityMatrix()).thenThrow(failure);

        assertThatThrownBy(() -> controller().precomputeItemSimilarity()).isSameAs(failure);

        verify(auditLogService).complete(
                attempt,
                AdminAuditLogService.Result.FAILURE,
                "exception_IllegalStateException");
    }

    @Test
    void unavailableAuditPreflightBlocksPrecompute() {
        when(auditLogService.begin(
                "item_similarity", null, "item_similarity_precompute"))
                .thenThrow(new AdminAuditLogService.AuditUnavailableException());

        assertThatThrownBy(() -> controller().precomputeItemSimilarity())
                .isInstanceOf(AdminAuditLogService.AuditUnavailableException.class);

        verify(itemBasedRecommendationService, never()).precomputeItemSimilarityMatrix();
    }

    private AdminAuditLogService.Attempt attempt() {
        return new AdminAuditLogService.Attempt(
                1L, 2L, "actor#hmac", null, "target#hmac", "item_similarity_precompute");
    }

    private AdminStatsController controller() {
        return new AdminStatsController(
                adminStatsService,
                itemBasedRecommendationService,
                recommendationEvaluationService,
                auditLogService);
    }
}
