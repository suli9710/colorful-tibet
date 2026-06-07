package com.tibet.tourism.common.security.antibot;
import com.fasterxml.jackson.databind.JsonNode;
import com.tibet.tourism.common.security.PiiMasker;
import java.time.Duration;
import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);
    private static final double RECAPTCHA_V2_SUCCESS_SCORE = 1.0;

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
        if (!StringUtils.hasText(token)) {
            log.warn("reCAPTCHA token is missing for ip={}", PiiMasker.maskIp(remoteIp));
            return OptionalDouble.empty();
        }
        if (!StringUtils.hasText(cfg.getSecretKey())) {
            log.error("reCAPTCHA is enabled but secret key is not configured");
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
                JsonNode scoreNode = response.get("score");
                double score = scoreNode != null && scoreNode.isNumber()
                        ? scoreNode.asDouble()
                        : RECAPTCHA_V2_SUCCESS_SCORE;
                log.debug("reCAPTCHA verification succeeded score={} for ip={}", score, PiiMasker.maskIp(remoteIp));
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
