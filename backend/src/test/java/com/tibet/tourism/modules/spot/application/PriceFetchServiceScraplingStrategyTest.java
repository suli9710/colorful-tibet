package com.tibet.tourism.modules.spot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class PriceFetchServiceScraplingStrategyTest {

    @Test
    void mapsScraplingPriceResponseToPriceInfo() {
        String body = """
                {
                  "basePrice": 200.0,
                  "peakSeasonPrice": 200.0,
                  "offSeasonPrice": 100.0,
                  "source": "Scrapling (basic)",
                  "confidence": 0.82,
                  "rawData": "{\\"evidence\\":[{\\"price\\":200}]}"
                }
                """;
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(body)
                        .build()));
        ScenicSpot spot = new ScenicSpot();
        spot.setName("布达拉宫");
        spot.setLocation("拉萨");

        PriceFetchService.ScraplingServiceStrategy strategy = new PriceFetchService.ScraplingServiceStrategy(
                builder,
                "http://scrapling:8000/",
                Duration.ofSeconds(1),
                4,
                "basic",
                "test-token");

        PriceInfo info = strategy.fetch(spot);

        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(info.getBasePrice()));
        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(info.getPeakSeasonPrice()));
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(info.getOffSeasonPrice()));
        assertEquals("Scrapling (basic)", info.getSource());
        assertEquals(0.82, info.getConfidence());
        assertEquals("{\"evidence\":[{\"price\":200}]}", info.getRawData());
        assertTrue(info.isReferenceOnly());
    }

    @Test
    void mapsExplicitPublishableScraplingResponseAsPublishableCandidate() {
        String body = """
                {
                  "basePrice": 200.0,
                  "source": "Scrapling (verified)",
                  "confidence": 0.95,
                  "referenceOnly": false
                }
                """;
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(body)
                        .build()));
        ScenicSpot spot = new ScenicSpot();
        spot.setName("Potala Palace");

        PriceFetchService.ScraplingServiceStrategy strategy = new PriceFetchService.ScraplingServiceStrategy(
                builder,
                "http://scrapling:8000/",
                Duration.ofSeconds(1),
                4,
                "verified",
                "test-token");

        PriceInfo info = strategy.fetch(spot);

        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(info.getBasePrice()));
        assertEquals(0.95, info.getConfidence());
        assertFalse(info.isReferenceOnly());
    }

    @Test
    void returnsNullWhenScraplingServiceIsNotConfigured() {
        PriceFetchService.ScraplingServiceStrategy strategy = new PriceFetchService.ScraplingServiceStrategy(
                WebClient.builder(),
                "",
                Duration.ofSeconds(1),
                4,
                "",
                "");

        assertNull(strategy.fetch(new ScenicSpot()));
    }
}
