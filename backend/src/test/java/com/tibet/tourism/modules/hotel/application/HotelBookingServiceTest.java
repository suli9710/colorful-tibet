package com.tibet.tourism.modules.hotel.application;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelBookingServiceTest {

    @Mock private HotelBookingRepository hotelBookingRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private OrderCenterService orderCenterService;

    private HotelBookingService hotelBookingService;
    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        hotelBookingService = new HotelBookingService(
                hotelBookingRepository,
                hotelRepository,
                roomTypeRepository,
                orderCenterService);

        user = new User();
        user.setId(1L);
        user.setRole(User.Role.USER);

        admin = new User();
        admin.setId(2L);
        admin.setRole(User.Role.ADMIN);
    }

    @Test
    void userCanDeleteOwnCancelledHotelBooking() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.CANCELLED);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        hotelBookingService.deleteBooking(user, 99L);

        assertNotNull(booking.getDeletedAt());
        verify(hotelBookingRepository).save(booking);
    }

    @Test
    void userCannotDeleteOwnActiveHotelBooking() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.CONFIRMED);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> hotelBookingService.deleteBooking(user, 99L));

        assertEquals("仅已取消酒店预订可以删除", error.getMessage());
        assertNull(booking.getDeletedAt());
        verify(hotelBookingRepository, never()).save(any());
    }

    @Test
    void userCannotDeleteSomeoneElsesHotelBooking() {
        User owner = new User();
        owner.setId(3L);
        owner.setRole(User.Role.USER);
        HotelBooking booking = booking(99L, owner, HotelBooking.Status.CANCELLED);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        assertThrows(SecurityException.class, () -> hotelBookingService.deleteBooking(user, 99L));

        assertNull(booking.getDeletedAt());
        verify(hotelBookingRepository, never()).save(any());
    }

    @Test
    void adminCanDeleteActiveHotelBooking() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.PENDING);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        hotelBookingService.deleteBooking(admin, 99L);

        assertEquals(HotelBooking.Status.CANCELLED, booking.getStatus());
        assertNotNull(booking.getDeletedAt());
        verify(hotelBookingRepository).save(booking);
    }

    @Test
    void adminBookingListMasksGuestNameAndPhoneByDefault() {
        HotelBooking booking = bookingWithPii(99L, user);
        when(hotelBookingRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        Page<?> page = hotelBookingService.getAllBookings(admin, PageRequest.of(0, 20));
        var response = (com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse) page.getContent().get(0);

        assertNotEquals("Alice Zhang", response.guestName());
        assertEquals("A***g", response.guestName());
        assertEquals("138****8000", response.phone());
    }

    @Test
    void userBookingListKeepsOwnGuestNameButMasksPhone() {
        HotelBooking booking = bookingWithPii(99L, user);
        when(hotelBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        Page<?> page = hotelBookingService.getUserBookings(user, PageRequest.of(0, 20));
        var response = (com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse) page.getContent().get(0);

        assertEquals("Alice Zhang", response.guestName());
        assertEquals("138****8000", response.phone());
    }

    @Test
    void revealBookingPiiReturnsFullGuestNameAndPhoneForAdmin() {
        HotelBooking booking = bookingWithPii(99L, user);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        var response = hotelBookingService.revealBookingPii(admin, 99L);

        assertEquals("Alice Zhang", response.guestName());
        assertEquals("13800138000", response.phone());
    }

    private HotelBooking booking(Long id, User owner, HotelBooking.Status status) {
        HotelBooking booking = new HotelBooking();
        booking.setId(id);
        booking.setUser(owner);
        booking.setStatus(status);
        return booking;
    }

    private HotelBooking bookingWithPii(Long id, User owner) {
        Hotel hotel = new Hotel();
        hotel.setId(5L);
        hotel.setName("Lhasa Hotel");

        HotelBooking booking = booking(id, owner, HotelBooking.Status.CONFIRMED);
        booking.setHotel(hotel);
        booking.setRoomName("Deluxe");
        booking.setRoomTypeId(6L);
        booking.setRoomPrice(new BigDecimal("880"));
        booking.setNights(2);
        booking.setCheckInDate(LocalDate.parse("2026-08-01"));
        booking.setCheckOutDate(LocalDate.parse("2026-08-03"));
        booking.setGuests(2);
        booking.setGuestName("Alice Zhang");
        booking.setPhone("13800138000");
        booking.setTotalPrice(new BigDecimal("1848"));
        booking.setCreatedAt(LocalDateTime.parse("2026-06-01T12:00:00"));
        return booking;
    }
}
