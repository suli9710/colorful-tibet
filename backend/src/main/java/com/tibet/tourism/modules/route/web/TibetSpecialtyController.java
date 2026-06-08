package com.tibet.tourism.modules.route.web;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.modules.route.application.TibetTravelKitService;
import com.tibet.tourism.modules.route.web.dto.specialty.HighlandAssessmentRequest;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tibet-specialty")
public class TibetSpecialtyController {

    private static final Logger log = LoggerFactory.getLogger(TibetSpecialtyController.class);
    private static final String ITINERARY_NOT_FOUND_ERROR = "Itinerary not found";
    private static final String INVALID_SPECIALTY_REQUEST_ERROR = "Specialty request could not be processed";

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
            log.warn("Travel kit itinerary not found: itineraryId={}, detail={}",
                    itineraryId, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ITINERARY_NOT_FOUND_ERROR));
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
            Long itineraryId = body == null ? null : body.itineraryId();
            log.warn("Highland assessment itinerary not found: itineraryId={}, detail={}",
                    itineraryId, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ITINERARY_NOT_FOUND_ERROR));
        }
    }

    @GetMapping("/culture-tips")
    public ResponseEntity<?> cultureTips(@RequestParam(required = false) String scene) {
        try {
            return ResponseEntity.ok(travelKitService.getCultureTips(scene));
        } catch (IllegalArgumentException e) {
            log.warn("Culture tips request rejected: filter=scene, detail={}",
                    SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", INVALID_SPECIALTY_REQUEST_ERROR));
        }
    }

    @GetMapping("/phrasebook")
    public ResponseEntity<?> phrasebook(@RequestParam(required = false) String category) {
        try {
            return ResponseEntity.ok(travelKitService.getPhrasebook(category));
        } catch (IllegalArgumentException e) {
            log.warn("Phrasebook request rejected: filter=category, detail={}",
                    SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", INVALID_SPECIALTY_REQUEST_ERROR));
        }
    }

    @GetMapping("/sustainable-options")
    public ResponseEntity<?> sustainableOptions(@RequestParam(required = false) String region) {
        try {
            return ResponseEntity.ok(travelKitService.getSustainableOptions(region));
        } catch (IllegalArgumentException e) {
            log.warn("Sustainable options request rejected: filter=region, detail={}",
                    SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", INVALID_SPECIALTY_REQUEST_ERROR));
        }
    }
}
