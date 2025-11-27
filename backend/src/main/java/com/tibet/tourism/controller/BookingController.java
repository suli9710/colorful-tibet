package com.tibet.tourism.controller;

import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ScenicSpotRepository scenicSpotRepository;

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        Long spotId = Long.valueOf(payload.get("spotId").toString());
        String dateStr = (String) payload.get("visitDate");
        Integer ticketCount = (Integer) payload.get("ticketCount");

        ScenicSpot spot = scenicSpotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found"));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSpot(spot);
        LocalDate visitDate = LocalDate.parse(dateStr);
        booking.setVisitDate(visitDate);
        booking.setTicketCount(ticketCount);
        booking.setStatus(Booking.Status.CONFIRMED); // Auto-confirm for now

        // Calculate total price with seasonal rules (按“每年”月份/日期判断，而不是具体年份):
        // 旺季：5月1日 - 10月31日
        // 淡季：11月1日 - 次年4月30日
        // 其中每年 1 月 1 日 - 3 月 31 日统一实施免票政策
        BigDecimal unitPrice = spot.getTicketPrice();
        MonthDay md = MonthDay.from(visitDate);
        MonthDay may1 = MonthDay.of(5, 1);
        MonthDay oct31 = MonthDay.of(10, 31);

        // 1-3 月统一免票（全区冬季旅游季政策）
        int month = visitDate.getMonthValue();
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

        booking.setTotalPrice(unitPrice.multiply(new BigDecimal(ticketCount)));

        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of("message", "Booking created successfully!", "bookingId", booking.getId()));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyBookings() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        List<Booking> bookings = bookingRepository.findByUserId(user.getId());
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized to cancel this booking");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully!"));
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
