package com.tibet.tourism.modules.route.web;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.route.application.ItineraryService;
import com.tibet.tourism.modules.route.domain.Itinerary;
import com.tibet.tourism.modules.route.web.dto.itinerary.BookItineraryItemRequest;
import com.tibet.tourism.modules.route.web.dto.itinerary.CreateItineraryVersionRequest;
import com.tibet.tourism.modules.route.web.dto.itinerary.GenerateItineraryRequest;
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
@RequestMapping("/api/itineraries")
@PreAuthorize("isAuthenticated()")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final JwtAuthSupport jwtAuthSupport;

    public ItineraryController(ItineraryService itineraryService, JwtAuthSupport jwtAuthSupport) {
        this.itineraryService = itineraryService;
        this.jwtAuthSupport = jwtAuthSupport;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody(required = false) GenerateItineraryRequest request,
                                      HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        GenerateItineraryRequest safeRequest = request == null ? new GenerateItineraryRequest() : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(itineraryService.generateItinerary(user, safeRequest));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myItineraries(HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        return ResponseEntity.ok(itineraryService.getMyItineraries(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.ok(itineraryService.getItinerary(user, id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "行程不存在"));
        }
    }

    @GetMapping("/{id}/quote")
    public ResponseEntity<?> quote(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.ok(itineraryService.quoteItinerary(user, id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "行程不存在"));
        }
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<?> createVersion(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) CreateItineraryVersionRequest request,
                                           HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        CreateItineraryVersionRequest safeRequest = request == null ? new CreateItineraryVersionRequest() : request;
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(itineraryService.createVersion(user, id, safeRequest));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "行程不存在"));
        }
    }

    @PostMapping("/{id}/items/{itemId}/bookings")
    public ResponseEntity<?> bookItem(@PathVariable Long id,
                                      @PathVariable Long itemId,
                                      @Valid @RequestBody(required = false) BookItineraryItemRequest request,
                                      HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        BookItineraryItemRequest safeRequest = request == null ? new BookItineraryItemRequest() : request;
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(itineraryService.bookItem(user, id, itemId, safeRequest));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "行程节点不存在"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权操作该资源"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() == null ? "预订参数不合法" : e.getMessage()));
        }
    }
}
