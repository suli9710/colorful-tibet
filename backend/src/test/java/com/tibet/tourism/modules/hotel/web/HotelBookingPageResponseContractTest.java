package com.tibet.tourism.modules.hotel.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tibet.tourism.modules.admin.application.AdminAuditTestSupport.passthroughAuditService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.modules.hotel.application.HotelBookingService;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
class HotelBookingPageResponseContractTest {

    @Mock
    private HotelBookingService hotelBookingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HotelBookingController controller = new HotelBookingController(
                hotelBookingService, userRepository, riskAssessmentService, passthroughAuditService());
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myBookingsReturnsStablePageEnvelopeWithoutUserLeakage() throws Exception {
        User user = authenticatedUser("traveler", User.Role.USER);
        PageRequest pageRequest = PageRequest.of(1, 2);
        when(hotelBookingService.getUserBookings(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookingResponse(99L)), pageRequest, 5));

        mockMvc.perform(get("/api/hotel-bookings/my?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(99))
                .andExpect(jsonPath("$.content[0].user").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].username").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(content().string(not(containsString("\"user\""))))
                .andExpect(content().string(not(containsString("\"userId\""))))
                .andExpect(content().string(not(containsString("\"username\""))))
                .andExpect(content().string(not(containsString("traveler"))));
    }

    @Test
    void allBookingsReturnsStablePageEnvelopeWithoutUserLeakage() throws Exception {
        User admin = authenticatedUser("admin", User.Role.ADMIN);
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(hotelBookingService.getAllBookings(eq(admin), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookingResponse(100L)), pageRequest, 1));

        mockMvc.perform(get("/api/hotel-bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].user").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].username").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(content().string(not(containsString("\"user\""))))
                .andExpect(content().string(not(containsString("\"userId\""))))
                .andExpect(content().string(not(containsString("\"username\""))))
                .andExpect(content().string(not(containsString("admin"))));
    }

    private User authenticatedUser(String username, User.Role role) {
        User user = new User();
        user.setId(7L);
        user.setUsername(username);
        user.setRole(role);
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "password", List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        return user;
    }

    private HotelBookingResponse bookingResponse(Long id) {
        return new HotelBookingResponse(
                id,
                new HotelBookingResponse.HotelSummary(5L, "Lhasa Hotel", "Lhasa", "/images/hotel.jpg"),
                5L,
                "Deluxe King",
                8L,
                new BigDecimal("880.00"),
                2,
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-03"),
                2,
                "Alice Zhang",
                "138****8000",
                null,
                new BigDecimal("1760.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1760.00"),
                "CONFIRMED",
                LocalDateTime.parse("2026-06-01T12:00:00"));
    }
}
