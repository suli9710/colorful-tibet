package com.tibet.tourism.modules.hotel.application;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
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

    private HotelBooking booking(Long id, User owner, HotelBooking.Status status) {
        HotelBooking booking = new HotelBooking();
        booking.setId(id);
        booking.setUser(owner);
        booking.setStatus(status);
        return booking;
    }
}
