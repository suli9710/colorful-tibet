package com.tibet.tourism.modules.ai.web;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.ai.application.AiQuotaService;
import com.tibet.tourism.modules.ai.application.AiRouteGenerationJobService;
import com.tibet.tourism.modules.ai.application.AiRouteJobSnapshot;
import com.tibet.tourism.modules.ai.application.AiRouteQuotaExceededException;
import com.tibet.tourism.modules.ai.application.AiRouteRecordService;
import com.tibet.tourism.modules.ai.application.AiRouteService;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateRequest;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateResponse;
import com.tibet.tourism.modules.ai.web.dto.AiRouteRecordSummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/routes")
@PreAuthorize("isAuthenticated()")
public class AiRouteController {

    private static final Logger logger = LoggerFactory.getLogger(AiRouteController.class);

    private final AiRouteService aiRouteService;
    private final AiQuotaService aiQuotaService;
    private final AiRouteGenerationJobService aiRouteGenerationJobService;
    private final AiRouteRecordService aiRouteRecordService;
    private final JwtAuthSupport jwtAuthSupport;

    public AiRouteController(AiRouteService aiRouteService,
                             AiQuotaService aiQuotaService,
                             AiRouteGenerationJobService aiRouteGenerationJobService,
                             AiRouteRecordService aiRouteRecordService,
                             JwtAuthSupport jwtAuthSupport) {
        this.aiRouteService = aiRouteService;
        this.aiQuotaService = aiQuotaService;
        this.aiRouteGenerationJobService = aiRouteGenerationJobService;
        this.aiRouteRecordService = aiRouteRecordService;
        this.jwtAuthSupport = jwtAuthSupport;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateRoute(@Valid @RequestBody(required = false) AiRouteGenerateRequest request,
                                           HttpServletRequest httpServletRequest) {
        try {
            AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
            User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);

            int days = safeRequest.getDays() == null ? 5 : safeRequest.getDays();
            String locale = resolveLocale(safeRequest.getLocale(), httpServletRequest);
            String cacheKey = aiQuotaService.buildCacheKey(
                    currentUser.getId(), days, safeRequest.getBudget(), safeRequest.getPreference(), locale);

            String cached = aiQuotaService.getCachedRoute(cacheKey);
            if (cached != null && !cached.isBlank()) {
                String normalizedCached = aiRouteService.normalizeCachedRoute(
                        cached, days, safeRequest.getBudget(), safeRequest.getPreference(), locale);
                if (!normalizedCached.isBlank()) {
                    if (!normalizedCached.equals(cached.trim())) {
                        aiQuotaService.cacheRoute(cacheKey, normalizedCached);
                    }
                    aiRouteRecordService.recordCompletedRoute(
                            currentUser, null, days, safeRequest.getBudget(), safeRequest.getPreference(), locale, normalizedCached);
                    return ResponseEntity.ok(Map.of("content", normalizedCached, "cached", true));
                }
                logger.warn("Ignoring invalid cached AI route content for sync endpoint: days={}", days);
            }

            AiQuotaService.QuotaConsumptionResult quota = aiQuotaService.tryConsumeQuota(currentUser.getId());
            if (!quota.allowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "今日 AI 路线生成次数已用完，请明天再试", "remaining", quota.remaining()));
            }

            AiRouteGenerateResponse result = aiRouteService.generateRoute(
                    days, safeRequest.getBudget(), safeRequest.getPreference(), currentUser, locale);

            if (result.getContent() != null && !result.getContent().isBlank()) {
                aiQuotaService.cacheRoute(cacheKey, result.getContent());
                aiRouteRecordService.recordCompletedRoute(
                        currentUser, null, days, safeRequest.getBudget(), safeRequest.getPreference(), locale, result.getContent());
            }

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            logger.warn("AI route generation configuration unavailable: {}", exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", "AI 路线生成配置不可用"));
        } catch (Exception e) {
            logger.error("AI route generation failed: {}", exceptionSummary(e));
            return ResponseEntity.internalServerError().body(Map.of("error", "AI route generation failed"));
        }
    }

    @GetMapping("/ai/latest")
    public ResponseEntity<?> getLatestAiRoute(HttpServletRequest httpServletRequest) {
        User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);
        return aiRouteRecordService.latestFor(currentUser)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/ai/saved")
    public ResponseEntity<List<AiRouteRecordSummaryResponse>> getSavedAiRoutes(
            HttpServletRequest httpServletRequest,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);
        return pagedContent(aiRouteRecordService.savedFor(currentUser, pageable));
    }

    @GetMapping("/ai/saved/{id}")
    public ResponseEntity<?> getSavedAiRoute(@PathVariable Long id,
                                             HttpServletRequest httpServletRequest) {
        User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);
        return ResponseEntity.ok(aiRouteRecordService.savedDetailFor(id, currentUser));
    }

    @PostMapping("/ai/{id}/save")
    public ResponseEntity<?> saveAiRoute(@PathVariable Long id,
                                         HttpServletRequest httpServletRequest) {
        User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);
        return ResponseEntity.ok(aiRouteRecordService.saveForUser(id, currentUser));
    }

    @PostMapping("/generate/jobs")
    public ResponseEntity<?> startGenerateRouteJob(@Valid @RequestBody(required = false) AiRouteGenerateRequest request,
                                                   HttpServletRequest httpServletRequest) {
        try {
            AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
            User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);
            String locale = resolveLocale(safeRequest.getLocale(), httpServletRequest);
            AiRouteJobSnapshot snapshot = aiRouteGenerationJobService.startJob(safeRequest, currentUser, locale);
            return ResponseEntity.ok(snapshot);
        } catch (AiRouteQuotaExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "今日 AI 路线生成次数已用完，请明天再试", "remaining", e.getRemaining()));
        } catch (Exception e) {
            logger.error("AI route job start failed: {}", exceptionSummary(e));
            return ResponseEntity.internalServerError().body(Map.of("error", "AI route generation failed"));
        }
    }

    @GetMapping("/generate/jobs/{jobId}")
    public ResponseEntity<?> getGenerateRouteJob(@PathVariable String jobId,
                                                 HttpServletRequest httpServletRequest) {
        try {
            User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);
            return ResponseEntity.ok(aiRouteGenerationJobService.getJob(jobId, currentUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/generate/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamGenerateRouteJob(@PathVariable String jobId,
                                                             HttpServletRequest httpServletRequest) {
        try {
            User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(aiRouteGenerationJobService.streamJob(jobId, currentUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> generateRouteStream(
            @Valid @RequestBody(required = false) AiRouteGenerateRequest request,
            HttpServletRequest httpServletRequest) {

        AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
        User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);

        AiQuotaService.QuotaConsumptionResult quota = aiQuotaService.tryConsumeQuota(currentUser.getId());
        if (!quota.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(completedErrorEmitter("今日 AI 路线生成次数已用完，请明天再试"));
        }

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

    private SseEmitter completedErrorEmitter(String message) {
        SseEmitter emitter = new SseEmitter(10_000L);
        try {
            emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"message\":\"" + message + "\"}"));
        } catch (Exception e) {
            logger.warn("Failed to write AI route SSE error event: {}", exceptionSummary(e));
        }
        emitter.complete();
        return emitter;
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

    private String exceptionSummary(Exception exception) {
        return exception == null ? "unknown" : exception.getClass().getSimpleName();
    }

    private <T> ResponseEntity<List<T>> pagedContent(Page<T> page) {
        return ResponseEntity.ok()
                .header("X-Page", String.valueOf(page.getNumber()))
                .header("X-Size", String.valueOf(page.getSize()))
                .header("X-Total-Elements", String.valueOf(page.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(page.getTotalPages()))
                .body(page.getContent());
    }
}
