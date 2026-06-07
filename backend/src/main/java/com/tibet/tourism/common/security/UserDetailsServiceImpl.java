package com.tibet.tourism.common.security;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Value("${app.security.hotel-booking-pii-read-usernames:${app.super-admin-username:}}")
    private String hotelBookingPiiReaderUsernames;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // 根据用户角色设置权限
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            // Spring Security需要ROLE_前缀
            String roleName = "ROLE_" + user.getRole().name();
            authorities.add(new SimpleGrantedAuthority(roleName));
            if (user.getRole() == User.Role.ADMIN && piiReaderUsernames().contains(user.getUsername())) {
                authorities.add(new SimpleGrantedAuthority("HOTEL_BOOKING_PII_READ"));
            }
            logger.debug("Loaded authorities for user {}: {}", username, roleName);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

    private Set<String> piiReaderUsernames() {
        if (!StringUtils.hasText(hotelBookingPiiReaderUsernames)) {
            return Set.of();
        }
        return Arrays.stream(hotelBookingPiiReaderUsernames.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }
}
