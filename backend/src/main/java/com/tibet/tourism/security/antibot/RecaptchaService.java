package com.tibet.tourism.security.antibot;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.OptionalDouble;

@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);

    private final AntibotProperties properties;
    private final WebClient webClient;

    public RecaptchaService(AntibotProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    public OptionalDouble verify(String token, String remoteIp) {
        AntibotProperties.Recaptcha cfg = properties.getRecaptcha();
        if (!properties.isEnabled() || !cfg.isEnabled()) {
            return OptionalDouble.empty();
        }
        if (!StringUtils.hasText(token) || !StringUtils.hasText(cfg.getSecretKey())) {
            return OptionalDouble.empty();
        }
        try {
            JsonNode response = webClient.post()
                    .uri(cfg.getVerifyUrl(), uriBuilder -> uriBuilder
                            .queryParam("secret", cfg.getSecretKey())
                            .queryParam("response", token)
                            .queryParam("remoteip", remoteIp)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(3));

            if (response != null && response.path("success").asBoolean(false)) {
                double score = response.path("score").asDouble(0.0);
                log.debug("reCAPTCHA score={} for ip={}", score, remoteIp);
                return OptionalDouble.of(score);
            }
            log.warn("reCAPTCHA verification failed: {}", response);
            return OptionalDouble.empty();
        } catch (Exception e) {
            log.warn("reCAPTCHA call failed: {}", e.getMessage());
            return OptionalDouble.empty();
        }
    }
}
