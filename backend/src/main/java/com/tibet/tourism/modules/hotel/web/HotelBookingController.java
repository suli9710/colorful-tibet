package com.tibet.tourism.modules.hotel.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.security.antibot.BehaviorData;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.common.security.antibot.RiskResult;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.hotel.application.HotelBookingService;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingRequest;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotel-bookings")
public class HotelBookingController {

    private static final Set<String> ALLOWED_BOOKING_SORT_FIELDS = Set.of(
            "id", "status", "checkInDate", "checkOutDate", "totalPrice", "createdAt");
    private static final Sort DEFAULT_BOOKING_SORT = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
    private static final String ERROR_NOT_AUTHENTICATED = "User not authenticated";
    private static final String ERROR_SECURITY_VALIDATION_FAILED = "Security validation failed";
    private static final String ERROR_ACCESS_DENIED = "Access denied";
    private static final String ERROR_INVALID_BOOKING_PARAMETERS = "Invalid booking parameters";
    private static final String ERROR_HOTEL_BOOKING_UNAVAILABLE =
            "Selected room is unavailable for the requested dates";
    private static final String ERROR_HOTEL_BOOKING_NOT_FOUND = "Hotel booking not found";
    private static final String ERROR_INVALID_STATUS = "Invalid status";
    private static final String ERROR_BOOKING_STATE_CONFLICT = "Booking state cannot be changed";

    private final HotelBookingService hotelBookingService;
    private final UserRepository userRepository;
    private final RiskAssessmentService riskAssessmentService;

    public HotelBookingController(HotelBookingService hotelBookingService,
                                  UserRepository userRepository,
                                  RiskAssessmentService riskAssessmentService) {
        this.hotelBookingService = hotelBookingService;
        this.userRepository = userRepository;
        this.riskAssessmentService = riskAssessmentService;
    }

    @GetMapping("/room-types/{hotelId}")
    public ResponseEntity<?> getRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelBookingService.getRoomTypes(hotelId));
    }

    @GetMapping("/hotels")
    public ResponseEntity<?> getAllHotels() {
        return ResponseEntity.ok(hotelBookingService.getAllHotels());
    }

    @GetMapping("/hotels/{id}")
    public ResponseEntity<?> getHotel(@PathVariable Long id) {
        return hotelBookingService.getHotel(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody HotelBookingRequest request,
            @RequestHeader(value = "X-Recaptcha-Token", required = false) String recaptchaToken,
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String fingerprint,
            @RequestHeader(value = "X-Behavior-Data", required = false) String behaviorData,
            HttpServletRequest httpRequest) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", ERROR_NOT_AUTHENTICATED));
        }

        RiskResult risk = riskAssessmentService.assess(
                recaptchaToken, fingerprint, user.getId(), behaviorData,
                httpRequest.getRemoteAddr(), "/api/hotel-bookings");
        if (risk.decision() != RiskResult.Decision.ALLOW) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", ERROR_SECURITY_VALIDATION_FAILED,
                    "code", "ANTIBOT_" + risk.decision().name()));
        }

        try {
            HotelBooking saved = hotelBookingService.createBooking(user, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Hotel inquiry created. Complete payment and fulfillment on a qualified third-party platform.",
                    "bookingId", saved.getId(),
                    "status", saved.getStatus(),
                    "totalPrice", saved.getTotalPrice()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", ERROR_INVALID_BOOKING_PARAMETERS));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", ERROR_HOTEL_BOOKING_UNAVAILABLE));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_HOTEL_BOOKING_NOT_FOUND));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", ERROR_NOT_AUTHENTICATED));
        }
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_BOOKING_SORT_FIELDS, DEFAULT_BOOKING_SORT, 20, 100);
        Page<HotelBookingResponse> bookings = hotelBookingService.getUserBookings(user, safePageable);
        return ResponseEntity.ok(PageResponse.from(bookings));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", ERROR_NOT_AUTHENTICATED));
        }

        try {
            Pageable safePageable = InputSanitizer.sanitizePageable(
                    pageable, ALLOWED_BOOKING_SORT_FIELDS, DEFAULT_BOOKING_SORT, 20, 100);
            Page<HotelBookingResponse> bookings = hotelBookingService.getAllBookings(user, safePageable);
            return ResponseEntity.ok(PageResponse.from(bookings));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", ERROR_ACCESS_DENIED));
        }
    }

    @GetMapping("/{id}/pii")
    @PreAuthorize("@hotelBookingPiiAccessGuard.canReveal(authentication, #id) and hasAuthority('HOTEL_BOOKING_PII_READ')")
    public ResponseEntity<?> revealBookingPii(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", ERROR_NOT_AUTHENTICATED));
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return ResponseEntity.ok(hotelBookingService.revealBookingPii(authentication, id));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", ERROR_ACCESS_DENIED));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_HOTEL_BOOKING_NOT_FOUND));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", ERROR_NOT_AUTHENTICATED));
        }

        try {
            hotelBookingService.updateStatus(user, id, payload.get("status"));
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", ERROR_ACCESS_DENIED));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", ERROR_INVALID_STATUS));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", ERROR_BOOKING_STATE_CONFLICT));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_HOTEL_BOOKING_NOT_FOUND));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", ERROR_NOT_AUTHENTICATED));
        }

        try {
            hotelBookingService.cancelBooking(user, id);
            return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", ERROR_ACCESS_DENIED));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", ERROR_BOOKING_STATE_CONFLICT));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_HOTEL_BOOKING_NOT_FOUND));
        }
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", ERROR_NOT_AUTHENTICATED));
        }

        try {
            hotelBookingService.deleteBooking(user, id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", ERROR_ACCESS_DENIED));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", ERROR_BOOKING_STATE_CONFLICT));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_HOTEL_BOOKING_NOT_FOUND));
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return userRepository.findByUsername(((UserDetails) auth.getPrincipal()).getUsername()).orElse(null);
        }
        return null;
    }
}
