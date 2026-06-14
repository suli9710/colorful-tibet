package com.tibet.tourism.modules.ai.web;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.security.antibot.AntibotProperties;
import com.tibet.tourism.common.security.antibot.RecaptchaService;
import com.tibet.tourism.modules.ai.application.AiGuideChatService;
import com.tibet.tourism.modules.ai.application.GuideChatUsageService;
import com.tibet.tourism.modules.ai.web.dto.GuideChatRequest;
import com.tibet.tourism.modules.ai.web.dto.GuideChatResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.OptionalDouble;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide")
public class AiGuideChatController {

    private static final String RECAPTCHA_HEADER = "X-Recaptcha-Token";

    private final AiGuideChatService aiGuideChatService;
    private final GuideChatUsageService guideChatUsageService;
    private final JwtAuthSupport jwtAuthSupport;
    private final RecaptchaService recaptchaService;
    private final AntibotProperties antibotProperties;
    private final boolean anonymousRemoteAiEnabled;

    public AiGuideChatController(
            AiGuideChatService aiGuideChatService,
            GuideChatUsageService guideChatUsageService,
            JwtAuthSupport jwtAuthSupport,
            RecaptchaService recaptchaService,
            AntibotProperties antibotProperties,
            @Value("${app.security.guide-chat.anonymous-remote-ai-enabled:${GUIDE_CHAT_ANON_REMOTE_AI_ENABLED:false}}")
            boolean anonymousRemoteAiEnabled) {
        this.aiGuideChatService = aiGuideChatService;
        this.guideChatUsageService = guideChatUsageService;
        this.jwtAuthSupport = jwtAuthSupport;
        this.recaptchaService = recaptchaService;
        this.antibotProperties = antibotProperties;
        this.anonymousRemoteAiEnabled = anonymousRemoteAiEnabled;
    }

    @PostMapping("/chat")
    public ResponseEntity<GuideChatResponse> chat(
            @Valid @RequestBody GuideChatRequest request,
            HttpServletRequest httpServletRequest) {
        Optional<User> currentUser = jwtAuthSupport.resolveOptionalCurrentUser(httpServletRequest);
        boolean authenticated = currentUser.isPresent();
        boolean recaptchaVerified = !authenticated && verifyRecaptcha(httpServletRequest);
        GuideChatUsageService.ClientIdentity identity = authenticated
                ? GuideChatUsageService.ClientIdentity.authenticated(currentUser.get().getId())
                : GuideChatUsageService.ClientIdentity.anonymous(resolveAnonymousClientKey(httpServletRequest));

        GuideChatUsageService.Decision usage = guideChatUsageService.tryAcquire(identity, recaptchaVerified);
        if (usage.challengeRequired()) {
            GuideChatResponse body = limitedResponse(
                    "Additional verification is required before continuing with the guide chat.",
                    "usage-challenge",
                    usage);
            body.setChallengeRequired(true);
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(usage.retryAfterSeconds()))
                    .body(body);
        }
        if (!usage.allowed()) {
            GuideChatResponse body = limitedResponse(limitMessage(usage.reason()), "usage-limit", usage);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(usage.retryAfterSeconds()))
                    .body(body);
        }

        String locale = resolveLocale(request.getLocale(), httpServletRequest);
        if (!authenticated && !anonymousRemoteAiEnabled) {
            return ResponseEntity.ok(aiGuideChatService.localOnlyChat(request, locale));
        }
        return ResponseEntity.ok(aiGuideChatService.chat(request, locale));
    }

    private GuideChatResponse limitedResponse(String content, String model, GuideChatUsageService.Decision usage) {
        return new GuideChatResponse(
                content,
                model,
                true,
                null,
                null,
                true,
                usage.retryAfterSeconds());
    }

    private boolean verifyRecaptcha(HttpServletRequest request) {
        if (!isRecaptchaConfigured()) {
            return false;
        }
        String token = request.getHeader(RECAPTCHA_HEADER);
        if (!StringUtils.hasText(token)) {
            return false;
        }
        OptionalDouble score = recaptchaService.verify(token, request.getRemoteAddr());
        return score.isPresent() && score.getAsDouble() >= antibotProperties.getRecaptcha().getMinScore();
    }

    private boolean isRecaptchaConfigured() {
        if (antibotProperties == null || !antibotProperties.isEnabled()) {
            return false;
        }
        AntibotProperties.Recaptcha recaptcha = antibotProperties.getRecaptcha();
        return recaptcha != null
                && recaptcha.isEnabled()
                && StringUtils.hasText(recaptcha.getSecretKey());
    }

    private String resolveLocale(String requestLocale, HttpServletRequest request) {
        if (requestLocale != null && !requestLocale.isBlank()) {
            return requestLocale;
        }
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && acceptLanguage.toLowerCase().startsWith("bo")) {
            return "bo";
        }
        return "zh";
    }

    private String resolveAnonymousClientKey(HttpServletRequest request) {
        // 匿名配额按 IP 计量。绝不能让客户端可控的 X-Device-Fingerprint 头单独决定配额桶，
        // 否则只需轮换该头即可绕过匿名限流/每日配额（见配置 anonymous-daily-quota-per-ip）。
        String remoteAddr = request.getRemoteAddr();
        return "ip#" + PiiMasker.shortHash(remoteAddr);
    }

    private String limitMessage(String reason) {
        if ("daily".equals(reason)) {
            return "The guide chat daily quota has been used. Please continue tomorrow or sign in for a higher quota.";
        }
        if ("anonymous-disabled".equals(reason)) {
            return "Please sign in before using guide chat.";
        }
        return "Guide chat is receiving too many requests. Please wait before continuing.";
    }
}
