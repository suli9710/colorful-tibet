package com.tibet.tourism.common.security.antibot;
import com.fasterxml.jackson.databind.JsonNode;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;

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
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("secret", cfg.getSecretKey());
            formData.add("response", token);
            if (StringUtils.hasText(remoteIp)) {
                formData.add("remoteip", remoteIp);
            }

            JsonNode response = webClient.post()
                    .uri(cfg.getVerifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
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
            log.warn("reCAPTCHA verification failed: {}", summarizeVerificationResponse(response));
            return OptionalDouble.empty();
        } catch (Exception e) {
            log.warn("reCAPTCHA call failed: {}", SensitiveLogSanitizer.exceptionSummary(e));
            return OptionalDouble.empty();
        }
    }

    static String summarizeVerificationResponse(JsonNode response) {
        if (response == null || response.isNull()) {
            return "success=false,score=none,errorCodes=none";
        }

        String score = response.hasNonNull("score") && response.get("score").isNumber()
                ? String.valueOf(response.get("score").asDouble())
                : "none";
        return "success=" + response.path("success").asBoolean(false)
                + ",score=" + score
                + ",errorCodes=" + summarizeErrorCodes(response.get("error-codes"));
    }

    private static String summarizeErrorCodes(JsonNode errorCodes) {
        if (errorCodes == null || !errorCodes.isArray() || errorCodes.isEmpty()) {
            return "none";
        }

        List<String> safeCodes = new ArrayList<>();
        for (JsonNode errorCode : errorCodes) {
            String code = errorCode.asText("");
            if (code.matches("[A-Za-z0-9_-]{1,64}")) {
                safeCodes.add(code);
            } else {
                safeCodes.add("invalid-code");
            }
            if (safeCodes.size() == 3) {
                break;
            }
        }
        return String.join(",", safeCodes);
    }
}
