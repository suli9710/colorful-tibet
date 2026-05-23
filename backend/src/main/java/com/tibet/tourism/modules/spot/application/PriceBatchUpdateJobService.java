package com.tibet.tourism.modules.spot.application;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PriceBatchUpdateJobService {

    private static final Logger log = LoggerFactory.getLogger(PriceBatchUpdateJobService.class);

    private final ScenicSpotRepository scenicSpotRepository;
    private final PriceUpdateService priceUpdateService;
    private final Executor priceUpdateExecutor;
    private final Map<String, PriceBatchUpdateJob> jobs = new ConcurrentHashMap<>();

    @Value("${app.price-update.max-batch-spots:1000}")
    private int maxBatchSpots = 1000;

    @Value("${app.price-update.jobs.retention-minutes:60}")
    private long jobRetentionMinutes = 60;

    @Value("${app.price-update.jobs.max-retained:200}")
    private int maxRetainedJobs = 200;

    @Value("${app.price-update.jobs.stale-running-minutes:1440}")
    private long staleRunningMinutes = 1440;

    public PriceBatchUpdateJobService(ScenicSpotRepository scenicSpotRepository,
                                      PriceUpdateService priceUpdateService,
                                      @Qualifier("priceUpdateExecutor") Executor priceUpdateExecutor) {
        this.scenicSpotRepository = scenicSpotRepository;
        this.priceUpdateService = priceUpdateService;
        this.priceUpdateExecutor = priceUpdateExecutor;
    }

    public PriceBatchUpdateJobSnapshot startJob(boolean forceUpdate) {
        cleanupJobs();
        PriceBatchUpdateJob job = new PriceBatchUpdateJob(forceUpdate);
        jobs.put(job.jobId, job);
        priceUpdateExecutor.execute(() -> runJob(job));
        return job.snapshot();
    }

    public PriceBatchUpdateJobSnapshot getJob(String jobId) {
        cleanupJobs();
        PriceBatchUpdateJob job = jobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Price batch update job not found");
        }
        return job.snapshot();
    }

    private void runJob(PriceBatchUpdateJob job) {
        try {
            var spots = scenicSpotRepository.findAllWithoutTags(
                    PageRequest.of(0, Math.max(1, maxBatchSpots), Sort.by("id"))).getContent();
            job.setTotal(spots.size());

            for (ScenicSpot spot : spots) {
                job.startSpot(spot);
                boolean alreadyPriced = !job.forceUpdate
                        && spot.getTicketPrice() != null
                        && spot.getTicketPrice().compareTo(BigDecimal.ZERO) > 0;
                try {
                    PriceUpdateService.PriceUpdateResult result =
                            priceUpdateService.updateSpotPrice(spot.getId(), job.forceUpdate);
                    if (result.isSuccess()) {
                        job.incrementSuccess();
                    } else if (alreadyPriced || isSkipMessage(result.getMessage())) {
                        job.incrementSkipped();
                    } else {
                        job.incrementFailed();
                    }
                } catch (Exception e) {
                    job.incrementFailed();
                    log.warn("Price batch job spot failed: jobId={}, spotId={}, error={}",
                            job.jobId, spot.getId(), e.getMessage());
                }
            }

            job.complete();
        } catch (Exception e) {
            log.error("Price batch update job failed: jobId={}", job.jobId, e);
            job.fail("Price batch update failed");
        } finally {
            cleanupJobs();
        }
    }

    private void cleanupJobs() {
        Instant now = Instant.now();
        Duration terminalRetention = Duration.ofMinutes(Math.max(0, jobRetentionMinutes));
        Duration staleRunningRetention = Duration.ofMinutes(Math.max(0, staleRunningMinutes));

        jobs.entrySet().removeIf(entry -> shouldRemoveJob(entry.getValue(), now, terminalRetention, staleRunningRetention));

        int maxJobs = Math.max(0, maxRetainedJobs);
        if (jobs.size() <= maxJobs) {
            return;
        }

        List<PriceBatchUpdateJob> terminalJobs = jobs.values().stream()
                .filter(PriceBatchUpdateJob::isTerminal)
                .sorted(Comparator.comparing(job -> job.updatedAt))
                .toList();
        for (PriceBatchUpdateJob job : terminalJobs) {
            if (jobs.size() <= maxJobs) {
                break;
            }
            jobs.remove(job.jobId, job);
        }
    }

    private boolean shouldRemoveJob(PriceBatchUpdateJob job, Instant now, Duration terminalRetention, Duration staleRunningRetention) {
        if (job.isTerminal()) {
            return !job.updatedAt.plus(terminalRetention).isAfter(now);
        }
        return !job.updatedAt.plus(staleRunningRetention).isAfter(now);
    }

    private boolean isSkipMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("跳过") || message.contains("skip") || message.contains("璺宠繃");
    }

    public record PriceBatchUpdateJobSnapshot(
            String jobId,
            String status,
            int total,
            int processed,
            int success,
            int failed,
            int skipped,
            String currentSpot,
            String errorMessage,
            Instant startedAt,
            Instant updatedAt,
            boolean forceUpdate
    ) {
    }

    private static final class PriceBatchUpdateJob {
        private final String jobId = UUID.randomUUID().toString();
        private final boolean forceUpdate;
        private final Instant startedAt = Instant.now();
        private volatile String status = "RUNNING";
        private volatile int total;
        private volatile int processed;
        private volatile int success;
        private volatile int failed;
        private volatile int skipped;
        private volatile String currentSpot = "";
        private volatile String errorMessage;
        private volatile Instant updatedAt = startedAt;

        private PriceBatchUpdateJob(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
        }

        synchronized void setTotal(int total) {
            this.total = total;
            this.updatedAt = Instant.now();
        }

        synchronized void startSpot(ScenicSpot spot) {
            this.currentSpot = spot.getName() == null ? String.valueOf(spot.getId()) : spot.getName();
            this.updatedAt = Instant.now();
        }

        synchronized void incrementSuccess() {
            success++;
            processed++;
            updatedAt = Instant.now();
        }

        synchronized void incrementFailed() {
            failed++;
            processed++;
            updatedAt = Instant.now();
        }

        synchronized void incrementSkipped() {
            skipped++;
            processed++;
            updatedAt = Instant.now();
        }

        synchronized void complete() {
            status = "COMPLETED";
            currentSpot = "";
            updatedAt = Instant.now();
        }

        synchronized void fail(String message) {
            status = "FAILED";
            errorMessage = message;
            updatedAt = Instant.now();
        }

        boolean isTerminal() {
            return "COMPLETED".equals(status) || "FAILED".equals(status);
        }

        synchronized PriceBatchUpdateJobSnapshot snapshot() {
            return new PriceBatchUpdateJobSnapshot(
                    jobId,
                    status,
                    total,
                    processed,
                    success,
                    failed,
                    skipped,
                    currentSpot,
                    errorMessage,
                    startedAt,
                    updatedAt,
                    forceUpdate
            );
        }
    }
}
