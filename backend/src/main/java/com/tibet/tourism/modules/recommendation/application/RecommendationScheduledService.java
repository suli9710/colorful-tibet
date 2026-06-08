package com.tibet.tourism.modules.recommendation.application;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RecommendationScheduledService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationScheduledService.class);

    @Autowired
    private ItemBasedRecommendationService itemBasedRecommendationService;

    @Autowired
    @Qualifier("recommendationExecutor")
    private Executor recommendationExecutor;

    @Value("${recommendation.item-similarity.enabled:true}")
    private boolean itemSimilarityEnabled;

    @Value("${recommendation.item-similarity.preload-on-startup:true}")
    private boolean preloadOnStartup;

    @PostConstruct
    public void preloadItemSimilarityMatrix() {
        if (!itemSimilarityEnabled || !preloadOnStartup) {
            return;
        }
        if (itemBasedRecommendationService.loadSimilarityMatrixFromCache()) {
            return;
        }
        refreshItemSimilarityMatrixAsync();
    }

    @Scheduled(cron = "${recommendation.item-similarity.cron:0 30 3 * * ?}")
    public void refreshItemSimilarityMatrixIfStale() {
        if (!itemSimilarityEnabled) {
            logger.debug("Item similarity matrix refresh disabled");
            return;
        }
        if (!itemBasedRecommendationService.isSimilarityMatrixStale()) {
            logger.debug("Item similarity matrix still fresh; skipping refresh");
            return;
        }
        itemBasedRecommendationService.precomputeItemSimilarityMatrix();
    }

    public void refreshItemSimilarityMatrixAsync() {
        recommendationExecutor.execute(() -> {
            try {
                itemBasedRecommendationService.precomputeItemSimilarityMatrix();
            } catch (RuntimeException ex) {
                logger.error("Async item similarity matrix refresh failed: error={}",
                        RecommendationLogPrivacy.exceptionSummary(ex));
            }
        });
    }
}
