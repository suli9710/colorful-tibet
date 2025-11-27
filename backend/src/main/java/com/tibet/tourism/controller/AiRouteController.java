package com.tibet.tourism.controller;

import com.tibet.tourism.service.AiRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*")
public class AiRouteController {

    @Autowired
    private AiRouteService aiRouteService;

    @PostMapping("/generate")
    public Mono<String> generateRoute(@RequestBody Map<String, Object> params) {
        int days = (int) params.getOrDefault("days", 5);
        String budget = (String) params.getOrDefault("budget", "中等");
        String preference = (String) params.getOrDefault("preference", "自然风光");
        
        return aiRouteService.generateRoute(days, budget, preference);
    }
}
