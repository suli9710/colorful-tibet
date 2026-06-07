package com.tibet.tourism.modules.spot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PriceUpdateServiceTest {

    @Mock
    private ScenicSpotRepository scenicSpotRepository;

    @Mock
    private PriceFetchService priceFetchService;

    private PriceUpdateService service;

    @BeforeEach
    void setUp() {
        service = new PriceUpdateService();
        ReflectionTestUtils.setField(service, "scenicSpotRepository", scenicSpotRepository);
        ReflectionTestUtils.setField(service, "priceFetchService", priceFetchService);
    }

    @Test
    void referenceOnlyPriceDoesNotPublishToScenicSpot() {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(1L);
        spot.setName("Potala Palace");

        PriceInfo priceInfo = new PriceInfo(new BigDecimal("200.00"), "Scrapling");
        priceInfo.setConfidence(1.0);
        priceInfo.setReferenceOnly(true);

        when(scenicSpotRepository.findById(1L)).thenReturn(Optional.of(spot));
        when(priceFetchService.fetchPrice(spot)).thenReturn(priceInfo);
        when(priceFetchService.isPublishablePrice(priceInfo)).thenReturn(false);

        PriceUpdateService.PriceUpdateResult result = service.updateSpotPrice(1L, true);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("SKIPPED_REFERENCE_PRICE");
        assertThat(result.getPriceInfo()).isSameAs(priceInfo);
        assertThat(spot.getTicketPrice()).isNull();
        verify(scenicSpotRepository, never()).save(any(ScenicSpot.class));
    }
}
