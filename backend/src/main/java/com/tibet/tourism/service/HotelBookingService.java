package com.tibet.tourism.service;

import com.tibet.tourism.dto.HotelBookingRequest;
import com.tibet.tourism.entity.Hotel;
import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.entity.RoomType;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.HotelRepository;
import com.tibet.tourism.repository.RoomTypeRepository;
import com.tibet.tourism.security.InputSanitizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class HotelBookingService {

    private static final int MAX_NIGHTS = 30;

    private final HotelBookingRepository hotelBookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    public HotelBookingService(HotelBookingRepository hotelBookingRepository,
                               HotelRepository hotelRepository,
                               RoomTypeRepository roomTypeRepository) {
        this.hotelBookingRepository = hotelBookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomType> getRoomTypes(Long hotelId) {
        return roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId);
    }

    @Transactional(readOnly = true)
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Hotel> getHotel(Long id) {
        return hotelRepository.findById(id);
    }

    @Transactional
    public HotelBooking createBooking(User user, HotelBookingRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new NoSuchElementException("Hotel not found"));
        RoomType roomType = roomTypeRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NoSuchElementException("Room type not found"));

        validateBookingRequest(request, roomType);

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
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
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setGuests(request.getGuests());
        booking.setGuestName(InputSanitizer.requiredPlainText(request.getGuestName(), 64, "入住人姓名"));
        booking.setPhone(InputSanitizer.requiredPlainText(request.getPhone(), 32, "手机号"));
        booking.setNote(InputSanitizer.optionalTextBlock(request.getNote(), 500, "备注"));
        booking.setSubtotal(subtotal);
        booking.setServiceFee(serviceFee);
        booking.setDiscount(discount);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(HotelBooking.Status.CONFIRMED);

        return hotelBookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Page<HotelBooking> getUserBookings(User user, Pageable pageable) {
        return hotelBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<HotelBooking> getAllBookings(User user, Pageable pageable) {
        requireAdmin(user);
        return hotelBookingRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public HotelBooking updateStatus(User user, Long id, String status) {
        requireAdmin(user);
        HotelBooking booking = hotelBookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("Invalid status");
        }
        booking.setStatus(HotelBooking.Status.valueOf(status.trim().toUpperCase()));
        return hotelBookingRepository.save(booking);
    }

    @Transactional
    public HotelBooking cancelBooking(User user, Long id) {
        HotelBooking booking = hotelBookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Unauthorized");
        }

        booking.setStatus(HotelBooking.Status.CANCELLED);
        return hotelBookingRepository.save(booking);
    }

    @Transactional
    public void deleteBooking(User user, Long id) {
        requireAdmin(user);
        if (!hotelBookingRepository.existsById(id)) {
            throw new NoSuchElementException("Booking not found");
        }
        hotelBookingRepository.deleteById(id);
    }

    private void validateBookingRequest(HotelBookingRequest request, RoomType roomType) {
        if (roomType.getHotel() == null || !request.getHotelId().equals(roomType.getHotel().getId())) {
            throw new IllegalArgumentException("Room type does not belong to this hotel");
        }
        if (!StringUtils.hasText(request.getGuestName()) || !StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("Guest name and phone are required");
        }

        int capacity = Math.max(1, roomType.getCapacity() == null ? 1 : roomType.getCapacity());
        if (request.getGuests() == null || request.getGuests() < 1 || request.getGuests() > capacity) {
            throw new IllegalArgumentException("Invalid guest count");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (request.getCheckInDate().isBefore(LocalDate.now()) || nights < 1 || nights > MAX_NIGHTS) {
            throw new IllegalArgumentException("Invalid check-in or check-out date");
        }
    }

    private void requireAdmin(User user) {
        if (user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Admin access required");
        }
    }
}
