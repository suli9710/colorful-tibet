package com.tibet.tourism.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebMvcConfigTest {

    @Test
    void customWebClientKeepsJackson2TreesAndMapperConfiguration() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/route", exchange -> {
            byte[] body = "{\"display_name\":\"Potala Palace\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            WebClient client = new WebMvcConfig().webClientBuilder(objectMapper)
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .build();

            JsonNode tree = client.get().uri("/route")
                    .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(5));
            NamedResponse response = client.get().uri("/route")
                    .retrieve().bodyToMono(NamedResponse.class).block(Duration.ofSeconds(5));

            assertThat(tree).isNotNull();
            assertThat(tree.path("display_name").asText()).isEqualTo("Potala Palace");
            assertThat(response).isNotNull();
            assertThat(response.displayName()).isEqualTo("Potala Palace");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ignoresWildcardCorsOriginsWhenCredentialsAreEnabled() {
        WebMvcConfig config = new WebMvcConfig();
        ReflectionTestUtils.setField(
                config,
                "allowedOrigins",
                "https://lengzhehao.xin,https://*.example.com,https://example.com*,*");
        CapturingCorsRegistry registry = new CapturingCorsRegistry();

        config.addCorsMappings(registry);

        assertThat(registry.registration).isNotNull();
        org.springframework.web.cors.CorsConfiguration corsConfiguration =
                ReflectionTestUtils.invokeMethod(registry.registration, "getCorsConfiguration");

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOriginPatterns())
                .containsExactly("https://lengzhehao.xin");
    }

    private static final class CapturingCorsRegistry extends CorsRegistry {
        private CorsRegistration registration;

        @Override
        public CorsRegistration addMapping(String pathPattern) {
            registration = super.addMapping(pathPattern);
            return registration;
        }
    }

    private record NamedResponse(String displayName) {
    }
}
