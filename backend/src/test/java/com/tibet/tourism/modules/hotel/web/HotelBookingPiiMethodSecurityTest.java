package com.tibet.tourism.modules.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static com.tibet.tourism.modules.admin.application.AdminAuditTestSupport.passthroughAuditService;

import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.hotel.application.HotelBookingPiiAccessGuard;
import com.tibet.tourism.modules.hotel.application.HotelBookingPiiAuditService;
import com.tibet.tourism.modules.hotel.application.HotelBookingService;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(HotelBookingPiiMethodSecurityTest.TestConfig.class)
class HotelBookingPiiMethodSecurityTest {

    @Autowired private HotelBookingController controller;
    @Autowired private HotelBookingService hotelBookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private HotelBookingPiiAuditService piiAuditService;

    @BeforeEach
    void setUp() {
        reset(hotelBookingService, userRepository, piiAuditService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminWithoutPiiAuthorityIsDeniedBeforeControllerBodyAndAudited() {
        User admin = user("admin", User.Role.ADMIN);
        Authentication authentication = authentication("admin", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> controller.revealBookingPii(99L))
                .isInstanceOf(AccessDeniedException.class);

        verify(piiAuditService).recordRevealRejected(admin, 99L, "missing_authority");
        verifyNoInteractions(hotelBookingService);
    }

    @Test
    void adminWithPiiAuthorityCanRevealThroughMethodSecurity() {
        User admin = user("admin", User.Role.ADMIN);
        Authentication authentication = authentication(
                "admin", "ROLE_ADMIN", HotelBookingPiiAccessGuard.PII_READ_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        HotelBookingResponse response = bookingResponse();
        when(hotelBookingService.revealBookingPii(eq(authentication), eq(99L))).thenReturn(response);

        var result = controller.revealBookingPii(99L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(hotelBookingService).revealBookingPii(authentication, 99L);
    }

    private User user(String username, User.Role role) {
        User user = new User();
        user.setId(2L);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private Authentication authentication(String username, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "password", grantedAuthorities);
        return new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
    }

    private HotelBookingResponse bookingResponse() {
        return new HotelBookingResponse(
                99L,
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
                "13800138000",
                "Late arrival",
                new BigDecimal("1760.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1760.00"),
                "CONFIRMED",
                LocalDateTime.parse("2026-06-01T12:00:00"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        HotelBookingController hotelBookingController(HotelBookingService hotelBookingService,
                                                      UserRepository userRepository,
                                                      RiskAssessmentService riskAssessmentService,
                                                      AdminAuditLogService auditLogService) {
            return new HotelBookingController(
                    hotelBookingService, userRepository, riskAssessmentService, auditLogService);
        }

        @Bean
        HotelBookingPiiAccessGuard hotelBookingPiiAccessGuard(UserRepository userRepository,
                                                              HotelBookingPiiAuditService piiAuditService) {
            return new HotelBookingPiiAccessGuard(userRepository, piiAuditService);
        }

        @Bean
        HotelBookingService hotelBookingService() {
            return mock(HotelBookingService.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        RiskAssessmentService riskAssessmentService() {
            return mock(RiskAssessmentService.class);
        }

        @Bean
        AdminAuditLogService auditLogService() {
            return passthroughAuditService();
        }

        @Bean
        HotelBookingPiiAuditService piiAuditService() {
            return mock(HotelBookingPiiAuditService.class);
        }
    }
}
