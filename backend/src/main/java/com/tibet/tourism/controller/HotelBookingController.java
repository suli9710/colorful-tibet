package com.tibet.tourism.controller;

import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.HotelRepository;
import com.tibet.tourism.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel-bookings")
@CrossOrigin(origins = "*")
public class HotelBookingController {

    @Autowired
    private HotelBookingRepository hotelBookingRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Long hotelId = Long.valueOf(payload.get("hotelId").toString());
        String roomName = (String) payload.get("roomName");
        BigDecimal roomPrice = new BigDecimal(payload.get("roomPrice").toString());
        Integer nights = (Integer) payload.get("nights");
        String checkInDate = (String) payload.get("checkInDate");
        String checkOutDate = (String) payload.get("checkOutDate");
        Integer guests = (Integer) payload.get("guests");
        String guestName = (String) payload.get("guestName");
        String phone = (String) payload.get("phone");
        String note = payload.containsKey("note") ? (String) payload.get("note") : null;

        BigDecimal subtotal = roomPrice.multiply(new BigDecimal(nights));
        BigDecimal serviceFee = subtotal.multiply(new BigDecimal("0.05")).setScale(0, BigDecimal.ROUND_HALF_UP);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal totalPrice = subtotal.add(serviceFee).subtract(discount);

        HotelBooking booking = new HotelBooking();
        booking.setUser(user);
        hotelRepository.findById(hotelId).ifPresent(booking::setHotel);
        booking.setRoomName(roomName);
        booking.setRoomPrice(roomPrice);
        booking.setNights(nights);
        booking.setCheckInDate(java.time.LocalDate.parse(checkInDate));
        booking.setCheckOutDate(java.time.LocalDate.parse(checkOutDate));
        booking.setGuests(guests);
        booking.setGuestName(guestName);
        booking.setPhone(phone);
        booking.setNote(note);
        booking.setSubtotal(subtotal);
        booking.setServiceFee(serviceFee);
        booking.setDiscount(discount);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(HotelBooking.Status.CONFIRMED);

        HotelBooking saved = hotelBookingRepository.save(booking);

        return ResponseEntity.ok(Map.of(
                "message", "Hotel booking created successfully!",
                "bookingId", saved.getId(),
                "totalPrice", totalPrice
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyBookings() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        List<HotelBooking> bookings = hotelBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping
    public ResponseEntity<?> getAllBookings() {
        User user = getCurrentUser();
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        List<HotelBooking> bookings = hotelBookingRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = getCurrentUser();
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        HotelBooking booking = hotelBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        String status = payload.get("status");
        booking.setStatus(HotelBooking.Status.valueOf(status));
        hotelBookingRepository.save(booking);
        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        HotelBooking booking = hotelBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        }
        booking.setStatus(HotelBooking.Status.CANCELLED);
        hotelBookingRepository.save(booking);
        return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return userRepository.findByUsername(((UserDetails) auth.getPrincipal()).getUsername()).orElse(null);
        }
        return null;
    }
}
