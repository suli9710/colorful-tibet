package com.tibet.tourism.common.security.antibot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.OutboundUrlValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class RecaptchaServiceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    @Test
    void verificationPostsSecretAndTokenInFormBodyNotQueryString() throws Exception {
        CapturedRequest captured = new CapturedRequest();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/recaptcha/api/siteverify", exchange -> handleSiteverify(exchange, captured));
        server.start();

        try {
            String verifyUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/recaptcha/api/siteverify";
            AntibotProperties properties = enabledProperties("form-secret value+&=", verifyUrl);
            RecaptchaService service = new RecaptchaService(
                    properties, WebClient.builder(), new OutboundUrlValidator());

            OptionalDouble result = service.verify("token value+&=", "203.0.113.10");

            assertThat(result).isPresent();
            assertThat(result.getAsDouble()).isEqualTo(0.82);
            assertThat(captured.method.get()).isEqualTo("POST");
            assertThat(captured.path.get()).isEqualTo("/recaptcha/api/siteverify");
            assertThat(captured.rawQuery.get()).isNull();
            assertThat(captured.contentType.get()).startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);

            Map<String, String> form = parseFormBody(captured.body.get());
            assertThat(form).containsEntry("secret", "form-secret value+&=");
            assertThat(form).containsEntry("response", "token value+&=");
            assertThat(form).containsEntry("remoteip", "203.0.113.10");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void verificationSummaryOmitsRawProviderFields() throws Exception {
        String providerResponse = """
                {
                  "success": false,
                  "hostname": "private.example",
                  "action": "checkout-with-sensitive-context",
                  "error-codes": ["invalid-input-response", "unexpected code with spaces"]
                }
                """;

        String summary = RecaptchaService.summarizeVerificationResponse(OBJECT_MAPPER.readTree(providerResponse));

        assertThat(summary).contains("success=false", "score=none", "invalid-input-response", "invalid-code");
        assertThat(summary).doesNotContain("private.example");
        assertThat(summary).doesNotContain("checkout-with-sensitive-context");
        assertThat(summary).doesNotContain("unexpected code with spaces");
    }

    private RecaptchaService serviceWithResponse(String body) {
        AntibotProperties properties = enabledProperties(
                "test-secret",
                "https://www.recaptcha.net/recaptcha/api/siteverify");

        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(body)
                        .build()));

        return new RecaptchaService(properties, builder, new OutboundUrlValidator());
    }

    private static AntibotProperties enabledProperties(String secretKey, String verifyUrl) {
        AntibotProperties properties = new AntibotProperties();
        properties.setEnabled(true);
        properties.getRecaptcha().setEnabled(true);
        properties.getRecaptcha().setSecretKey(secretKey);
        properties.getRecaptcha().setVerifyUrl(verifyUrl);
        return properties;
    }

    private static void handleSiteverify(HttpExchange exchange, CapturedRequest captured) throws java.io.IOException {
        try {
            captured.method.set(exchange.getRequestMethod());
            captured.path.set(exchange.getRequestURI().getPath());
            captured.rawQuery.set(exchange.getRequestURI().getRawQuery());
            captured.contentType.set(exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            captured.body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] response = "{\"success\":true,\"score\":0.82}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(HttpStatus.OK.value(), response.length);
            exchange.getResponseBody().write(response);
        } finally {
            exchange.close();
        }
    }

    private static Map<String, String> parseFormBody(String body) {
        Map<String, String> form = new LinkedHashMap<>();
        for (String part : body.split("&")) {
            String[] entry = part.split("=", 2);
            String key = URLDecoder.decode(entry[0], StandardCharsets.UTF_8);
            String value = entry.length == 2
                    ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8)
                    : "";
            form.put(key, value);
        }
        return form;
    }

    private static final class CapturedRequest {
        private final AtomicReference<String> method = new AtomicReference<>();
        private final AtomicReference<String> path = new AtomicReference<>();
        private final AtomicReference<String> rawQuery = new AtomicReference<>();
        private final AtomicReference<String> contentType = new AtomicReference<>();
        private final AtomicReference<String> body = new AtomicReference<>();
    }
}
