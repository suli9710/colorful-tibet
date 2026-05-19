package com.tibet.tourism.controller;

import com.tibet.tourism.dto.HotelBookingRequest;
import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.HotelBookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/hotel-bookings")
public class HotelBookingController {

    private final HotelBookingService hotelBookingService;
    private final UserRepository userRepository;

    public HotelBookingController(HotelBookingService hotelBookingService, UserRepository userRepository) {
        this.hotelBookingService = hotelBookingService;
        this.userRepository = userRepository;
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
    public ResponseEntity<?> createBooking(@Valid @RequestBody HotelBookingRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            HotelBooking saved = hotelBookingService.createBooking(user, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Hotel booking created successfully and is pending payment.",
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
    public ResponseEntity<?> getMyBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        Page<HotelBooking> bookings = hotelBookingService.getUserBookings(user, pageable);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping
    public ResponseEntity<?> getAllBookings(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            return ResponseEntity.ok(hotelBookingService.getAllBookings(user, pageable));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作该资源"));
        }
    }

    @PutMapping("/{id}/status")
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
