package com.tibet.tourism.modules.ai.web;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.ai.application.AiQuotaService;
import com.tibet.tourism.modules.ai.application.AiRouteService;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateRequest;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/routes")
@PreAuthorize("isAuthenticated()")
public class AiRouteController {

    private static final Logger logger = LoggerFactory.getLogger(AiRouteController.class);

    private final AiRouteService aiRouteService;
    private final AiQuotaService aiQuotaService;
    private final JwtAuthSupport jwtAuthSupport;

    public AiRouteController(AiRouteService aiRouteService, AiQuotaService aiQuotaService, JwtAuthSupport jwtAuthSupport) {
        this.aiRouteService = aiRouteService;
        this.aiQuotaService = aiQuotaService;
        this.jwtAuthSupport = jwtAuthSupport;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateRoute(@Valid @RequestBody(required = false) AiRouteGenerateRequest request,
                                           HttpServletRequest httpServletRequest) {
        try {
            AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
            User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);

            if (aiQuotaService.isQuotaExceeded(currentUser.getId())) {
                int remaining = aiQuotaService.getRemainingQuota(currentUser.getId());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "今日 AI 路线生成次数已用完，请明天再试",
                                     "remaining", remaining));
            }

            int days = safeRequest.getDays() == null ? 5 : safeRequest.getDays();
            String locale = resolveLocale(safeRequest.getLocale(), httpServletRequest);
            String cacheKey = aiQuotaService.buildCacheKey(
                    currentUser.getId(), days, safeRequest.getBudget(), safeRequest.getPreference(), locale);

            String cached = aiQuotaService.getCachedRoute(cacheKey);
            if (cached != null) {
                return ResponseEntity.ok(Map.of("content", cached, "cached", true));
            }

            aiQuotaService.incrementQuota(currentUser.getId());

            AiRouteGenerateResponse result = aiRouteService.generateRoute(
                    days, safeRequest.getBudget(), safeRequest.getPreference(), currentUser, locale);

            if (result.getContent() != null && !result.getContent().isBlank()) {
                aiQuotaService.cacheRoute(cacheKey, result.getContent());
            }

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            logger.warn("AI route generation configuration unavailable: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "AI 路线生成配置不可用"));
        } catch (Exception e) {
            logger.error("AI route generation failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "AI route generation failed"));
        }
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> generateRouteStream(
            @Valid @RequestBody(required = false) AiRouteGenerateRequest request,
            HttpServletRequest httpServletRequest) {

        AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
        User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);

        if (aiQuotaService.isQuotaExceeded(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "今日 AI 路线生成次数已用完，请明天再试",
                                 "remaining", 0));
        }

        aiQuotaService.incrementQuota(currentUser.getId());

        SseEmitter emitter = new SseEmitter(180_000L);

        aiRouteService.streamRoute(
                safeRequest.getDays() == null ? 5 : safeRequest.getDays(),
                safeRequest.getBudget(),
                safeRequest.getPreference(),
                currentUser,
                resolveLocale(safeRequest.getLocale(), httpServletRequest),
                emitter
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
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
}
