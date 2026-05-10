package com.tibet.tourism.controller;

import com.tibet.tourism.dto.AiRouteGenerateRequest;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.security.JwtAuthSupport;
import com.tibet.tourism.service.AiRouteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/routes")
public class AiRouteController {

    private final AiRouteService aiRouteService;
    private final JwtAuthSupport jwtAuthSupport;

    public AiRouteController(AiRouteService aiRouteService, JwtAuthSupport jwtAuthSupport) {
        this.aiRouteService = aiRouteService;
        this.jwtAuthSupport = jwtAuthSupport;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateRoute(@RequestBody(required = false) AiRouteGenerateRequest request,
                                           HttpServletRequest httpServletRequest) {
        try {
            AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
            User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);

            return ResponseEntity.ok(
                    aiRouteService.generateRoute(
                            safeRequest.getDays() == null ? 5 : safeRequest.getDays(),
                            safeRequest.getBudget(),
                            safeRequest.getPreference(),
                            currentUser
                    )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "AI route generation failed", "detail", e.getMessage()));
        }
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> generateRouteStream(
            @RequestBody(required = false) AiRouteGenerateRequest request,
            HttpServletRequest httpServletRequest) {

        AiRouteGenerateRequest safeRequest = request == null ? new AiRouteGenerateRequest() : request;
        User currentUser = jwtAuthSupport.resolveCurrentUser(httpServletRequest);

        SseEmitter emitter = new SseEmitter(180_000L);

        aiRouteService.streamRoute(
                safeRequest.getDays() == null ? 5 : safeRequest.getDays(),
                safeRequest.getBudget(),
                safeRequest.getPreference(),
                currentUser,
                emitter
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
