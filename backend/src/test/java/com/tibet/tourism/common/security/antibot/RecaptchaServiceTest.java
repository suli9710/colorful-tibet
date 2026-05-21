package com.tibet.tourism.common.security.antibot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class RecaptchaServiceTest {

    @Test
    void v2SuccessWithoutScoreCountsAsVerified() {
        RecaptchaService service = serviceWithResponse("""
                {
                  "success": true,
                  "challenge_ts": "2026-05-22T00:00:00Z",
                  "hostname": "lengzhehao.xin"
                }
                """);

        OptionalDouble result = service.verify("v2-token", "203.0.113.10");

        assertThat(result).isPresent();
        assertThat(result.getAsDouble()).isEqualTo(1.0);
    }

    @Test
    void v3SuccessUsesReturnedScore() {
        RecaptchaService service = serviceWithResponse("""
                {
                  "success": true,
                  "score": 0.7,
                  "action": "hotel_booking"
                }
                """);

        OptionalDouble result = service.verify("v3-token", "203.0.113.10");

        assertThat(result).isPresent();
        assertThat(result.getAsDouble()).isEqualTo(0.7);
    }

    @Test
    void failedVerificationReturnsEmpty() {
        RecaptchaService service = serviceWithResponse("""
                {
                  "success": false,
                  "error-codes": ["invalid-input-response"]
                }
                """);

        OptionalDouble result = service.verify("bad-token", "203.0.113.10");

        assertThat(result).isEmpty();
    }

    private RecaptchaService serviceWithResponse(String body) {
        AntibotProperties properties = new AntibotProperties();
        properties.setEnabled(true);
        properties.getRecaptcha().setEnabled(true);
        properties.getRecaptcha().setSecretKey("test-secret");
        properties.getRecaptcha().setVerifyUrl("https://www.recaptcha.net/recaptcha/api/siteverify");

        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(body)
                        .build()));

        return new RecaptchaService(properties, builder);
    }
}
