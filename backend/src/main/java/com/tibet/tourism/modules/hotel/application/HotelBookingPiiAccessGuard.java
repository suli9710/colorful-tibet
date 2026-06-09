package com.tibet.tourism.modules.hotel.application;

import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HotelBookingPiiAccessGuard {

    public static final String PII_READ_AUTHORITY = "HOTEL_BOOKING_PII_READ";

    private final UserRepository userRepository;
    private final HotelBookingPiiAuditService piiAuditService;

    public HotelBookingPiiAccessGuard(UserRepository userRepository,
                                      HotelBookingPiiAuditService piiAuditService) {
        this.userRepository = userRepository;
        this.piiAuditService = piiAuditService;
    }

    public boolean canReveal(Authentication authentication, Long bookingId) {
        return evaluate(authentication, bookingId).allowed();
    }

    public User requireReveal(Authentication authentication, Long bookingId) {
        Decision decision = evaluate(authentication, bookingId);
        if (!decision.allowed()) {
            throw new SecurityException("Hotel booking PII read authority required");
        }
        return decision.actor();
    }

    private Decision evaluate(Authentication authentication, Long bookingId) {
        Optional<User> actor = authenticatedActor(authentication);
        if (actor.isEmpty()) {
            piiAuditService.recordRevealRejected(null, bookingId, "unauthenticated");
            return Decision.rejected();
        }

        User user = actor.get();
        if (user.getRole() != User.Role.ADMIN) {
            piiAuditService.recordRevealRejected(user, bookingId, "not_admin");
            return Decision.rejected();
        }

        if (!hasPiiReadAuthority(authentication)) {
            piiAuditService.recordRevealRejected(user, bookingId, "missing_authority");
            return Decision.rejected();
        }

        return Decision.allowed(user);
    }

    private Optional<User> authenticatedActor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String username = username(authentication);
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    private String username(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }

    private boolean hasPiiReadAuthority(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PII_READ_AUTHORITY::equals);
    }

    private record Decision(boolean allowed, User actor) {
        private static Decision allowed(User actor) {
            return new Decision(true, actor);
        }

        private static Decision rejected() {
            return new Decision(false, null);
        }
    }
}
