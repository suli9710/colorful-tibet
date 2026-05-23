package com.tibet.tourism.modules.spot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PriceBatchUpdateJobServiceTest {

    @Mock
    private ScenicSpotRepository scenicSpotRepository;
    @Mock
    private PriceUpdateService priceUpdateService;

    private PriceBatchUpdateJobService service;

    @BeforeEach
    void setUp() {
        service = new PriceBatchUpdateJobService(scenicSpotRepository, priceUpdateService, Runnable::run);
        ReflectionTestUtils.setField(service, "maxBatchSpots", 1000);
    }

    @Test
    void batchJobTracksProgressAndFinalCounts() {
        when(scenicSpotRepository.findAllWithoutTags(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        spot(1L, "Potala", null),
                        spot(2L, "Jokhang", BigDecimal.TEN),
                        spot(3L, "Yamdrok", null)
                )));
        when(priceUpdateService.updateSpotPrice(1L, true))
                .thenReturn(new PriceUpdateService.PriceUpdateResult(true, "ok", null));
        when(priceUpdateService.updateSpotPrice(2L, true))
                .thenReturn(new PriceUpdateService.PriceUpdateResult(false, "skip", null));
        when(priceUpdateService.updateSpotPrice(3L, true))
                .thenReturn(new PriceUpdateService.PriceUpdateResult(false, "failed", null));

        PriceBatchUpdateJobService.PriceBatchUpdateJobSnapshot snapshot = service.startJob(true);

        assertEquals("COMPLETED", snapshot.status());
        assertEquals(3, snapshot.total());
        assertEquals(3, snapshot.processed());
        assertEquals(1, snapshot.success());
        assertEquals(1, snapshot.skipped());
        assertEquals(1, snapshot.failed());
    }

    @Test
    void terminalJobExpiresAfterRetentionWindow() {
        ReflectionTestUtils.setField(service, "jobRetentionMinutes", 0L);
        when(scenicSpotRepository.findAllWithoutTags(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PriceBatchUpdateJobService.PriceBatchUpdateJobSnapshot snapshot = service.startJob(true);

        assertEquals("COMPLETED", snapshot.status());
        assertThrows(IllegalArgumentException.class, () -> service.getJob(snapshot.jobId()));
    }

    private ScenicSpot spot(Long id, String name, BigDecimal price) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        spot.setName(name);
        spot.setTicketPrice(price);
        return spot;
    }
}
