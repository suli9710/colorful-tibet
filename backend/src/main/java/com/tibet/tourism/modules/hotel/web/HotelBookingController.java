package com.tibet.tourism.modules.hotel.web;
import com.tibet.tourism.common.security.antibot.BehaviorData;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.common.security.antibot.RiskResult;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.hotel.application.HotelBookingService;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingRequest;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse;
import com.tibet.tourism.modules.order.domain.Booking;
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
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        RiskResult risk = riskAssessmentService.assess(
                recaptchaToken, fingerprint, user.getId(), behaviorData,
                httpRequest.getRemoteAddr(), "/api/hotel-bookings");
        if (risk.decision() != RiskResult.Decision.ALLOW) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", risk.decision() == RiskResult.Decision.BLOCK ? "请求被安全系统拦截" : "请完成安全验证",
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
            return ResponseEntity.badRequest().body(Map.of("error", "酒店预订参数不合法"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", "所选日期房型不可预订"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "酒店预订资源不存在"));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_BOOKING_SORT_FIELDS, DEFAULT_BOOKING_SORT, 20, 100);
        Page<HotelBookingResponse> bookings = hotelBookingService.getUserBookings(user, safePageable);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            Pageable safePageable = InputSanitizer.sanitizePageable(
                    pageable, ALLOWED_BOOKING_SORT_FIELDS, DEFAULT_BOOKING_SORT, 20, 100);
            return ResponseEntity.ok(hotelBookingService.getAllBookings(user, safePageable));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作该资源"));
        }
    }

    @GetMapping("/{id}/pii")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('HOTEL_BOOKING_PII_READ')")
    public ResponseEntity<?> revealBookingPii(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            return ResponseEntity.ok(hotelBookingService.revealBookingPii(user, id));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Hotel booking not found"));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            hotelBookingService.updateStatus(user, id, payload.get("status"));
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作该资源"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", "预订状态不允许这样变更"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "酒店预订资源不存在"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            hotelBookingService.cancelBooking(user, id);
            return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作该资源"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", "预订状态不允许这样变更"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "酒店预订资源不存在"));
        }
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            hotelBookingService.deleteBooking(user, id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作该资源"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "酒店预订资源不存在"));
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
