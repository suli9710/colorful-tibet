package com.tibet.tourism.modules.order.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class BookingControllerAnonymousOwnerTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ScenicSpotRepository scenicSpotRepository;
    @Mock private OrderCenterService orderCenterService;
    @Mock private RiskAssessmentService riskAssessmentService;

    private BookingController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new BookingController();
        controller.bookingRepository = bookingRepository;
        controller.userRepository = userRepository;
        controller.scenicSpotRepository = scenicSpotRepository;
        controller.orderCenterService = orderCenterService;
        controller.riskAssessmentService = riskAssessmentService;

        user = new User();
        user.setId(1L);
        user.setUsername("traveler");
        user.setRole(User.Role.USER);

        var principal = org.springframework.security.core.userdetails.User
                .withUsername("traveler")
                .password("unused")
                .authorities("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancelAnonymizedSpotBookingReturnsNotFoundWithoutSaving() {
        Booking booking = booking(42L, null, Booking.Status.PENDING);
        when(bookingRepository.findByIdAndUserId(42L, user.getId())).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.cancelBooking(42L);

        assertEquals(404, response.getStatusCode().value());
        verify(bookingRepository, never()).findById(42L);
        verify(bookingRepository, never()).save(any());
        verify(orderCenterService, never()).cancelLegacyMirror(any(), anyString(), any(), anyString());
    }

    @Test
    void deleteAnonymizedSpotBookingReturnsNotFoundWithoutDeleting() {
        Booking booking = booking(42L, null, Booking.Status.CANCELLED);
        when(bookingRepository.findByIdAndUserId(42L, user.getId())).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.deleteBooking(42L);

        assertEquals(404, response.getStatusCode().value());
        verify(bookingRepository, never()).findById(42L);
        verify(bookingRepository, never()).deleteByIdAndUserIdAndStatus(any(), any(), any());
    }

    private Booking booking(Long id, User owner, Booking.Status status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(owner);
        booking.setStatus(status);
        return booking;
    }
}
