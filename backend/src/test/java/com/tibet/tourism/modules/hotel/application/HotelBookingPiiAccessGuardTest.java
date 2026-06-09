package com.tibet.tourism.modules.hotel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class HotelBookingPiiAccessGuardTest {

    @Mock private UserRepository userRepository;
    @Mock private HotelBookingPiiAuditService piiAuditService;

    private HotelBookingPiiAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new HotelBookingPiiAccessGuard(userRepository, piiAuditService);
    }

    @Test
    void unauthenticatedRevealIsAuditedAndRejected() {
        assertThat(guard.canReveal(null, 99L)).isFalse();

        verify(piiAuditService).recordRevealRejected(null, 99L, "unauthenticated");
    }

    @Test
    void nonAdminRevealIsAuditedAndRejectedBeforeAuthorityCheck() {
        User user = user(1L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));

        assertThat(guard.canReveal(authentication("traveler", HotelBookingPiiAccessGuard.PII_READ_AUTHORITY), 99L))
                .isFalse();

        verify(piiAuditService).recordRevealRejected(user, 99L, "not_admin");
    }

    @Test
    void adminWithoutPiiAuthorityIsAuditedAndRejected() {
        User admin = user(2L, "admin", User.Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThat(guard.canReveal(authentication("admin", "ROLE_ADMIN"), 99L)).isFalse();

        verify(piiAuditService).recordRevealRejected(admin, 99L, "missing_authority");
    }

    @Test
    void adminWithPiiAuthorityIsAllowedWithoutRejectedAudit() {
        User admin = user(2L, "admin", User.Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThat(guard.canReveal(authentication(
                "admin", "ROLE_ADMIN", HotelBookingPiiAccessGuard.PII_READ_AUTHORITY), 99L))
                .isTrue();

        verify(piiAuditService, never()).recordRevealRejected(admin, 99L, "missing_authority");
    }

    private User user(Long id, String username, User.Role role) {
        User user = new User();
        user.setId(id);
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
}
