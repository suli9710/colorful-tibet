package com.tibet.tourism.modules.order.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.antibot.RiskResult;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.order.web.dto.BookingRequest;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
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

    @Test
    void createSpotBookingMirrorsIntoUnifiedOrderCenter() {
        BookingRequest request = new BookingRequest();
        request.setSpotId(10L);
        request.setVisitDate(LocalDate.of(2026, 6, 1));
        request.setTicketCount(2);

        ScenicSpot spot = new ScenicSpot();
        spot.setId(10L);
        spot.setName("Lhasa spot");
        spot.setTicketPrice(BigDecimal.valueOf(200));
        spot.setPeakSeasonPrice(BigDecimal.valueOf(300));

        when(riskAssessmentService.assess(any(), any(), eq(user.getId()), any(), any(), eq("/api/bookings")))
                .thenReturn(RiskResult.allow());
        when(scenicSpotRepository.findById(10L)).thenReturn(Optional.of(spot));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(88L);
            return saved;
        });

        ResponseEntity<?> response = controller.createBooking(
                request,
                null,
                null,
                null,
                new MockHttpServletRequest());

        assertEquals(200, response.getStatusCode().value());
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(orderCenterService).createFromLegacySpotBooking(bookingCaptor.capture());
        Booking mirrored = bookingCaptor.getValue();
        assertEquals(88L, mirrored.getId());
        assertEquals(user, mirrored.getUser());
        assertEquals(spot, mirrored.getSpot());
        assertEquals(Booking.Status.PENDING, mirrored.getStatus());
        assertEquals(BigDecimal.valueOf(600), mirrored.getTotalPrice());
    }

    private Booking booking(Long id, User owner, Booking.Status status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(owner);
        booking.setStatus(status);
        return booking;
    }
}
