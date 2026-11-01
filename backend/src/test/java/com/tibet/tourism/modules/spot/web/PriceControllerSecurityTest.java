package com.tibet.tourism.modules.spot.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.spot.application.PriceBatchUpdateJobService;
import com.tibet.tourism.modules.spot.application.PriceFetchService;
import com.tibet.tourism.modules.spot.application.PriceUpdateService;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static com.tibet.tourism.modules.admin.application.AdminAuditTestSupport.passthroughAuditService;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PriceControllerSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Mock
    private PriceFetchService priceFetchService;
    @Mock
    private PriceUpdateService priceUpdateService;
    @Mock
    private PriceBatchUpdateJobService priceBatchUpdateJobService;
    @Mock
    private ScenicSpotRepository scenicSpotRepository;

    private PriceController controller;

    @BeforeEach
    void setUp() {
        controller = new PriceController(
                priceFetchService,
                priceUpdateService,
                priceBatchUpdateJobService,
                scenicSpotRepository,
                passthroughAuditService());
    }

    @Test
    void fetchPriceRequiresAdminRoleBecauseItTriggersExternalPriceLookup() throws NoSuchMethodException {
        PreAuthorize annotation = PriceController.class
                .getMethod("fetchPrice", Long.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void fetchPriceFailureDoesNotExposeRawMessageInBodyOrLogs(CapturedOutput output) {
        String rawMessage = "upstream token=secret-token user=alice@example.com";
        ScenicSpot spot = new ScenicSpot();
        spot.setId(7L);
        when(scenicSpotRepository.findById(7L)).thenReturn(Optional.of(spot));
        when(priceFetchService.fetchPrice(spot)).thenThrow(new IllegalStateException(rawMessage));

        ResponseEntity<?> response = controller.fetchPrice(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Price operation could not be processed")
                .doesNotContain("secret-token", "alice@example.com", rawMessage);
        assertThat(output).contains("type=IllegalStateException", "messageHash=")
                .doesNotContain(rawMessage, "secret-token", "alice@example.com");
    }

    @Test
    void fetchPriceSuccessDoesNotSerializeRawPriceEvidence() throws Exception {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(7L);
        spot.setTicketPrice(BigDecimal.valueOf(100));
        PriceInfo priceInfo = new PriceInfo(BigDecimal.valueOf(200), "Scrapling");
        priceInfo.setRawData("{\"debug\":\"token=secret-token\",\"phone\":\"13800138000\"}");
        when(scenicSpotRepository.findById(7L)).thenReturn(Optional.of(spot));
        when(priceFetchService.fetchPrice(spot)).thenReturn(priceInfo);

        ResponseEntity<?> response = controller.fetchPrice(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String json = objectMapper.writeValueAsString(response.getBody());
        assertThat(json)
                .contains("\"priceInfo\"", "\"basePrice\":200")
                .doesNotContain("rawData", "secret-token", "13800138000");
    }

    @Test
    void updatePriceNotFoundDoesNotExposeRawResourceMessage(CapturedOutput output) {
        String rawMessage = "spot row missing tenant=internal-ledger alice@example.com";
        when(priceUpdateService.updateSpotPrice(8L, false))
                .thenThrow(new ResourceNotFoundException(rawMessage));

        ResponseEntity<?> response = controller.updatePrice(8L, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertBodyError(response, "Price resource not found")
                .doesNotContain("internal-ledger", "alice@example.com", rawMessage);
        assertThat(output).contains("type=ResourceNotFoundException", "messageHash=")
                .doesNotContain(rawMessage, "internal-ledger", "alice@example.com");
    }

    @Test
    void startBatchUpdateJobDoesNotExposeRawServiceMessage(CapturedOutput output) {
        String rawMessage = "executor rejected queue=price-admin token=job-secret";
        when(priceBatchUpdateJobService.startJob(true)).thenThrow(new IllegalStateException(rawMessage));

        ResponseEntity<?> response = controller.startBatchUpdateJob(true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Price operation could not be processed")
                .doesNotContain("job-secret", rawMessage);
        assertThat(output).contains("type=IllegalStateException", "messageHash=")
                .doesNotContain("job-secret", rawMessage);
    }

    private static org.assertj.core.api.AbstractStringAssert<?> assertBodyError(
            ResponseEntity<?> response,
            String expected) {
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();

        return assertThat(body.get("error")).isEqualTo(expected);
    }
}
