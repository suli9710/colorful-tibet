package com.tibet.tourism.modules.order.web;
import com.tibet.tourism.common.security.antibot.BehaviorData;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.common.security.antibot.RiskResult;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.order.web.dto.BookingRequest;
import com.tibet.tourism.modules.order.web.dto.BookingResponse;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private static final String ERROR_NOT_AUTHENTICATED = "User not authenticated";
    private static final String ERROR_SECURITY_VALIDATION_FAILED = "Security validation failed";
    private static final String ERROR_SCENIC_SPOT_NOT_FOUND = "Scenic spot not found";
    private static final String ERROR_BOOKING_NOT_FOUND = "Booking not found";
    private static final String ERROR_DELETE_REQUIRES_CANCELLED = "Only cancelled bookings can be deleted";
    private static final String ERROR_DELETE_FAILED = "Failed to delete booking";

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ScenicSpotRepository scenicSpotRepository;

    @Autowired
    OrderCenterService orderCenterService;

    @Autowired
    RiskAssessmentService riskAssessmentService;

    @PostMapping
    @Transactional
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest payload,
            @RequestHeader(value = "X-Recaptcha-Token", required = false) String recaptchaToken,
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String fingerprint,
            @RequestHeader(value = "X-Behavior-Data", required = false) String behaviorData,
            HttpServletRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ERROR_NOT_AUTHENTICATED);
        }

        RiskResult risk = riskAssessmentService.assess(
                recaptchaToken, fingerprint, user.getId(), behaviorData,
                request.getRemoteAddr(), "/api/bookings");
        if (risk.decision() != RiskResult.Decision.ALLOW) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", ERROR_SECURITY_VALIDATION_FAILED,
                    "code", "ANTIBOT_" + risk.decision().name()));
        }

        ScenicSpot spot = scenicSpotRepository.findById(payload.getSpotId()).orElse(null);
        if (spot == null) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_SCENIC_SPOT_NOT_FOUND));
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSpot(spot);
        booking.setVisitDate(payload.getVisitDate());
        booking.setTicketCount(payload.getTicketCount());
        booking.setStatus(Booking.Status.PENDING);

        // Calculate total price with seasonal rules (按“每年”月份/日期判断，而不是具体年份):
        // 旺季：5月1日 - 10月31日
        // 淡季：11月1日 - 次年4月30日
        // 其中每年 1 月 1 日 - 3 月 31 日统一实施免票政策
        BigDecimal unitPrice = spot.getTicketPrice() == null ? BigDecimal.ZERO : spot.getTicketPrice();
        MonthDay md = MonthDay.from(payload.getVisitDate());
        MonthDay may1 = MonthDay.of(5, 1);
        MonthDay oct31 = MonthDay.of(10, 31);

        // 1-3 月统一免票（全区冬季旅游季政策）
        int month = payload.getVisitDate().getMonthValue();
        if (month >= 1 && month <= 3) {
            unitPrice = BigDecimal.ZERO;
        } else {
            // 其余日期再按旺季/淡季和景点自身价格配置来计算
            boolean isPeakSeason = !md.isBefore(may1) && !md.isAfter(oct31);
            if (isPeakSeason && spot.getPeakSeasonPrice() != null) {
                // 旺季价格（5月1日-10月31日）
                unitPrice = spot.getPeakSeasonPrice();
            } else if (!isPeakSeason && spot.getOffSeasonPrice() != null) {
                // 淡季价格（11月1日-次年4月30日）
                unitPrice = spot.getOffSeasonPrice();
            }
        }

        booking.setTotalPrice(unitPrice.multiply(new BigDecimal(payload.getTicketCount())));

        bookingRepository.save(booking);
        orderCenterService.createFromLegacySpotBooking(booking);

        return ResponseEntity.ok(Map.of(
                "message", "Ticket inquiry created. Complete payment and fulfillment on a qualified third-party platform.",
                "bookingId", booking.getId(),
                "status", booking.getStatus()));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyBookings() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ERROR_NOT_AUTHENTICATED);
        }

        List<BookingResponse> bookings = bookingRepository.findByUserId(user.getId()).stream()
                .map(BookingResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ERROR_NOT_AUTHENTICATED);
        }

        Booking booking = bookingRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (booking == null || !isOwner(booking, user)) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_BOOKING_NOT_FOUND));
        }

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            return ResponseEntity.ok(Map.of("message", "Booking already cancelled"));
        }

        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);
        orderCenterService.cancelLegacyMirror(user, "LEGACY_SPOT_BOOKING", booking.getId(), "旧景点预订取消");

        return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ERROR_NOT_AUTHENTICATED);
        }

        Booking booking = bookingRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (booking == null || !isOwner(booking, user)) {
            return ResponseEntity.status(404).body(Map.of("error", ERROR_BOOKING_NOT_FOUND));
        }

        if (booking.getStatus() != Booking.Status.CANCELLED) {
            return ResponseEntity.status(409).body(Map.of("error", ERROR_DELETE_REQUIRES_CANCELLED));
        }

        try {
            int deleted = bookingRepository.deleteByIdAndUserIdAndStatus(id, user.getId(), Booking.Status.CANCELLED);
            if (deleted == 0) {
                return ResponseEntity.status(404).body(Map.of("error", ERROR_BOOKING_NOT_FOUND));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", ERROR_DELETE_FAILED));
        }

        return ResponseEntity.ok(Map.of("message", "Booking deleted successfully!"));
    }

    private boolean isOwner(Booking booking, User user) {
        return booking.getUser() != null
                && user.getId() != null
                && user.getId().equals(booking.getUser().getId());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        }
        return null;
    }
}
