package com.tibet.tourism.controller;

import com.tibet.tourism.entity.Hotel;
import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.entity.RoomType;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.HotelRepository;
import com.tibet.tourism.repository.RoomTypeRepository;
import com.tibet.tourism.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel-bookings")
public class HotelBookingController {

    @Autowired
    private HotelBookingRepository hotelBookingRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @GetMapping("/room-types/{hotelId}")
    public ResponseEntity<?> getRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId));
    }

    @GetMapping("/hotels")
    public ResponseEntity<?> getAllHotels() {
        return ResponseEntity.ok(hotelRepository.findAll());
    }

    @GetMapping("/hotels/{id}")
    public ResponseEntity<?> getHotel(@PathVariable Long id) {
        return hotelRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Long hotelId = Long.valueOf(payload.get("hotelId").toString());
        Long roomId = Long.valueOf(payload.get("roomId").toString());
        Integer guests = Integer.valueOf(payload.get("guests").toString());
        String guestName = payload.get("guestName") == null ? "" : payload.get("guestName").toString().trim();
        String phone = payload.get("phone") == null ? "" : payload.get("phone").toString().trim();
        String note = payload.containsKey("note") ? (String) payload.get("note") : null;
        LocalDate checkInDate = LocalDate.parse(payload.get("checkInDate").toString());
        LocalDate checkOutDate = LocalDate.parse(payload.get("checkOutDate").toString());

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found"));
        RoomType roomType = roomTypeRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room type not found"));
        if (roomType.getHotel() == null || !hotelId.equals(roomType.getHotel().getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Room type does not belong to this hotel"));
        }
        if (!StringUtils.hasText(guestName) || !StringUtils.hasText(phone)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Guest name and phone are required"));
        }
        if (guests == null || guests < 1 || guests > Math.max(1, roomType.getCapacity() == null ? 1 : roomType.getCapacity())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid guest count"));
        }
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (checkInDate.isBefore(LocalDate.now()) || nights < 1 || nights > 30) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid check-in or check-out date"));
        }

        BigDecimal roomPrice = roomType.getPrice() == null ? BigDecimal.ZERO : roomType.getPrice();
        BigDecimal subtotal = roomPrice.multiply(BigDecimal.valueOf(nights));
        BigDecimal serviceFee = subtotal.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal totalPrice = subtotal.add(serviceFee).subtract(discount);

        HotelBooking booking = new HotelBooking();
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setRoomName(roomType.getName());
        booking.setRoomPrice(roomPrice);
        booking.setNights((int) nights);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
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
    public ResponseEntity<?> getMyBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        Page<HotelBooking> bookings = hotelBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping
    public ResponseEntity<?> getAllBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        Page<HotelBooking> bookings = hotelBookingRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        if (user.getRole() != User.Role.ADMIN) {
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
        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
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
