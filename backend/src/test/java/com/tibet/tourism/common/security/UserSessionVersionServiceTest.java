package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserSessionVersionServiceTest {

    private UserRepository userRepository;
    private JwtUtils jwtUtils;
    private UserSessionVersionService service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        jwtUtils = Mockito.mock(JwtUtils.class);
        service = new UserSessionVersionService(userRepository, jwtUtils);
    }

    @Test
    void tokenMatchesOnlyWhenDatabaseSessionVersionMatchesJwtClaim() {
        User user = new User();
        user.setUsername("traveler");
        user.setSessionVersion(3L);
        when(jwtUtils.getUserNameFromJwtToken("token")).thenReturn("traveler");
        when(jwtUtils.getSessionVersionFromJwtToken("token")).thenReturn(3L);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));

        assertThat(service.tokenMatchesCurrentSession("token")).isTrue();

        when(jwtUtils.getSessionVersionFromJwtToken("token")).thenReturn(2L);
        assertThat(service.tokenMatchesCurrentSession("token")).isFalse();
    }

    @Test
    void invalidatingUserIncrementsSessionVersion() {
        User user = new User();
        user.setId(11L);
        user.setSessionVersion(4L);

        service.invalidateUser(user);

        assertThat(user.getSessionVersion()).isEqualTo(5L);
        verify(userRepository).save(user);
    }
}
