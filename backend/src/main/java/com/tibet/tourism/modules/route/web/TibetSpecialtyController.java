package com.tibet.tourism.modules.route.web;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.route.application.TibetTravelKitService;
import com.tibet.tourism.modules.route.web.dto.specialty.HighlandAssessmentRequest;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tibet-specialty")
public class TibetSpecialtyController {

    private final TibetTravelKitService travelKitService;
    private final JwtAuthSupport jwtAuthSupport;

    public TibetSpecialtyController(TibetTravelKitService travelKitService, JwtAuthSupport jwtAuthSupport) {
        this.travelKitService = travelKitService;
        this.jwtAuthSupport = jwtAuthSupport;
    }

    @GetMapping("/itineraries/{itineraryId}/travel-kit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> travelKit(@PathVariable Long itineraryId, HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        try {
            return ResponseEntity.ok(travelKitService.buildTravelKit(user, itineraryId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "行程不存在"));
        }
    }

    @PostMapping("/highland-assessment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> highlandAssessment(@Valid @RequestBody(required = false) HighlandAssessmentRequest body,
                                                HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        try {
            return ResponseEntity.ok(travelKitService.assessHighlandRisk(user, body));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "行程不存在"));
        }
    }

    @GetMapping("/culture-tips")
    public ResponseEntity<?> cultureTips(@RequestParam(required = false) String scene) {
        try {
            return ResponseEntity.ok(travelKitService.getCultureTips(scene));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/phrasebook")
    public ResponseEntity<?> phrasebook(@RequestParam(required = false) String category) {
        try {
            return ResponseEntity.ok(travelKitService.getPhrasebook(category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sustainable-options")
    public ResponseEntity<?> sustainableOptions(@RequestParam(required = false) String region) {
        try {
            return ResponseEntity.ok(travelKitService.getSustainableOptions(region));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
