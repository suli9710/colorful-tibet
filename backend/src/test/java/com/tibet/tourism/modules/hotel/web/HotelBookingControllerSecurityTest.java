package com.tibet.tourism.modules.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static com.tibet.tourism.modules.admin.application.AdminAuditTestSupport.passthroughAuditService;

import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.modules.hotel.application.HotelBookingService;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class HotelBookingControllerSecurityTest {

    @Mock
    private HotelBookingService hotelBookingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteBookingConflictDoesNotExposeRawStateException() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("alice", "password", java.util.List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("payment provider state: paid=true, ledgerId=secret-ledger"))
                .when(hotelBookingService)
                .deleteBooking(user, 99L);
        HotelBookingController controller = new HotelBookingController(
                hotelBookingService, userRepository, riskAssessmentService, passthroughAuditService());

        ResponseEntity<?> response = controller.deleteBooking(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Booking state cannot be changed");
        assertThat(body.toString()).doesNotContain("paid=true", "secret-ledger");
    }
}
