package com.tibet.tourism.modules.spot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.domain.SpotPriceObservation;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.infra.SpotPriceObservationRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class PriceUpdateServiceTest {

    @Mock
    private ScenicSpotRepository scenicSpotRepository;

    @Mock
    private PriceFetchService priceFetchService;

    @Mock
    private SingleSpotPriceUpdateService singleSpotPriceUpdateService;

    @Mock
    private SpotPriceObservationRepository priceObservationRepository;

    private PriceUpdateService service;
    private SingleSpotPriceUpdateService transactionalService;

    @BeforeEach
    void setUp() {
        service = new PriceUpdateService(scenicSpotRepository, singleSpotPriceUpdateService, Runnable::run);
        transactionalService = new SingleSpotPriceUpdateService(
                scenicSpotRepository,
                priceFetchService,
                priceObservationRepository);
        ReflectionTestUtils.setField(service, "maxBatchSpots", 1000);
    }

    @Test
    void singleSpotUpdaterUsesIndependentTransaction() throws Exception {
        Method method = SingleSpotPriceUpdateService.class.getMethod("updateSpotPrice", Long.class, boolean.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
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

        PriceUpdateService.PriceUpdateResult result = transactionalService.updateSpotPrice(1L, true);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("SKIPPED_REFERENCE_PRICE");
        assertThat(result.getPriceInfo()).isSameAs(priceInfo);
        assertThat(spot.getTicketPrice()).isNull();
        verify(scenicSpotRepository, never()).save(any(ScenicSpot.class));

        ArgumentCaptor<SpotPriceObservation> observationCaptor =
                ArgumentCaptor.forClass(SpotPriceObservation.class);
        verify(priceObservationRepository).save(observationCaptor.capture());
        SpotPriceObservation observation = observationCaptor.getValue();
        assertThat(observation.getSpot()).isSameAs(spot);
        assertThat(observation.getBasePrice()).isEqualByComparingTo("200.00");
        assertThat(observation.isReferenceOnly()).isTrue();
        assertThat(observation.isPublishable()).isFalse();
        assertThat(observation.getStatus()).isEqualTo(SpotPriceObservation.Status.REVIEW_REQUIRED);
        assertThat(observation.getReviewReason()).contains("Reference-only");
    }

    @Test
    void publishedPriceIsRecordedAsObservation() {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(1L);
        spot.setName("Potala Palace");

        PriceInfo priceInfo = new PriceInfo(new BigDecimal("200.00"), "Scrapling verified");
        priceInfo.setConfidence(0.95);

        when(scenicSpotRepository.findById(1L)).thenReturn(Optional.of(spot));
        when(priceFetchService.fetchPrice(spot)).thenReturn(priceInfo);
        when(priceFetchService.isPublishablePrice(priceInfo)).thenReturn(true);

        PriceUpdateService.PriceUpdateResult result = transactionalService.updateSpotPrice(1L, true);

        assertThat(result.isSuccess()).isTrue();
        assertThat(spot.getTicketPrice()).isEqualByComparingTo("200.00");
        verify(scenicSpotRepository).save(spot);

        ArgumentCaptor<SpotPriceObservation> observationCaptor =
                ArgumentCaptor.forClass(SpotPriceObservation.class);
        verify(priceObservationRepository).save(observationCaptor.capture());
        SpotPriceObservation observation = observationCaptor.getValue();
        assertThat(observation.getSpot()).isSameAs(spot);
        assertThat(observation.getSource()).isEqualTo("Scrapling verified");
        assertThat(observation.isPublishable()).isTrue();
        assertThat(observation.getStatus()).isEqualTo(SpotPriceObservation.Status.PUBLISHED);
    }

    @Test
    void singleSpotUpdaterLetsUnexpectedFailuresEscapeForRollback() {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(2L);
        spot.setName("Namtso");

        when(scenicSpotRepository.findById(2L)).thenReturn(Optional.of(spot));
        when(priceFetchService.fetchPrice(spot))
                .thenThrow(new IllegalStateException("upstream save should roll back"));

        assertThatThrownBy(() -> transactionalService.updateSpotPrice(2L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("upstream save should roll back");

        verify(scenicSpotRepository, never()).save(any(ScenicSpot.class));
    }

    @Test
    void priceFetchFailureDoesNotExposeRawExceptionMessage() {
        when(singleSpotPriceUpdateService.updateSpotPrice(2L, true))
                .thenThrow(new IllegalStateException("upstream token leaked in provider response"));

        PriceUpdateService.PriceUpdateResult result = service.updateSpotPrice(2L, true);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("PRICE_UPDATE_FAILED");
        assertThat(result.getMessage()).doesNotContain("token", "provider response");
        assertThat(result.getPriceInfo()).isNull();
    }

    @Test
    void updateSpotPricePreservesMissingSpotException() {
        ResourceNotFoundException missing = new ResourceNotFoundException("Scenic spot not found");
        when(singleSpotPriceUpdateService.updateSpotPrice(404L, false)).thenThrow(missing);

        assertThatThrownBy(() -> service.updateSpotPrice(404L, false)).isSameAs(missing);
    }

    @Test
    void batchUpdatePricesUsesSingleSpotTransactionAndIsolatesFailures() {
        when(scenicSpotRepository.findAllWithoutTags(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        spot(1L, "Potala"),
                        spot(2L, "Jokhang"),
                        spot(3L, "Yamdrok")
                )));
        when(singleSpotPriceUpdateService.updateSpotPrice(1L, true))
                .thenReturn(new PriceUpdateService.PriceUpdateResult(true, "PRICE_UPDATED", null));
        when(singleSpotPriceUpdateService.updateSpotPrice(2L, true))
                .thenThrow(new IllegalStateException("provider token leaked in error"));
        when(singleSpotPriceUpdateService.updateSpotPrice(3L, true))
                .thenReturn(new PriceUpdateService.PriceUpdateResult(false, "SKIPPED_EXISTING_PRICE", null));

        PriceUpdateService.BatchUpdateResult result = service.batchUpdatePrices(true);

        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getSkipCount()).isEqualTo(1);
        verify(singleSpotPriceUpdateService).updateSpotPrice(1L, true);
        verify(singleSpotPriceUpdateService).updateSpotPrice(2L, true);
        verify(singleSpotPriceUpdateService).updateSpotPrice(3L, true);
    }

    private ScenicSpot spot(Long id, String name) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        spot.setName(name);
        return spot;
    }
}
