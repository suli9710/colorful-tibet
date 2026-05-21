package com.tibet.tourism.modules.ai.web;

import com.tibet.tourism.modules.ai.application.AiGuideChatService;
import com.tibet.tourism.modules.ai.application.GuideChatUsageService;
import com.tibet.tourism.modules.ai.web.dto.GuideChatRequest;
import com.tibet.tourism.modules.ai.web.dto.GuideChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide")
public class AiGuideChatController {

    private final AiGuideChatService aiGuideChatService;
    private final GuideChatUsageService guideChatUsageService;

    public AiGuideChatController(AiGuideChatService aiGuideChatService, GuideChatUsageService guideChatUsageService) {
        this.aiGuideChatService = aiGuideChatService;
        this.guideChatUsageService = guideChatUsageService;
    }

    @PostMapping("/chat")
    public ResponseEntity<GuideChatResponse> chat(
            @Valid @RequestBody GuideChatRequest request,
            HttpServletRequest httpServletRequest) {
        GuideChatUsageService.Decision usage = guideChatUsageService.tryAcquire(resolveClientKey(httpServletRequest));
        if (!usage.allowed()) {
            GuideChatResponse body = new GuideChatResponse(
                    limitMessage(usage.reason()),
                    "usage-limit",
                    true,
                    null,
                    null,
                    true,
                    usage.retryAfterSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(usage.retryAfterSeconds()))
                    .body(body);
        }
        return ResponseEntity.ok(aiGuideChatService.chat(request, resolveLocale(request.getLocale(), httpServletRequest)));
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

    private String resolveClientKey(HttpServletRequest request) {
        String fingerprint = request.getHeader("X-Device-Fingerprint");
        if (fingerprint != null && !fingerprint.isBlank() && fingerprint.length() <= 256) {
            return "fp:" + fingerprint.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return "ip:" + (remoteAddr == null ? "unknown" : remoteAddr)
                + ":ua:" + (userAgent == null ? "" : userAgent);
    }

    private String limitMessage(String reason) {
        if ("daily".equals(reason)) {
            return "今天的小导游 AI 对话次数已经用完了。你仍然可以使用页面上的路线规划和景点信息，明天再继续问我西藏行程细节。";
        }
        return "小导游需要稍微休息一下，避免连续请求过多。请等一会儿再继续问西藏路线、景点或高原适应问题。";
    }
}
