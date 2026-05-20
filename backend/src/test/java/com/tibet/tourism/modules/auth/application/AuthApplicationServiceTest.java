package com.tibet.tourism.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.logging.IpLocationService;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.modules.auth.domain.AuthForbiddenException;
import com.tibet.tourism.modules.auth.web.dto.LoginRequest;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthApplicationServiceTest {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private JwtUtils jwtUtils;
    private CsrfTokenService csrfTokenService;
    private PasswordEncoder passwordEncoder;
    private IpLocationService ipLocationService;
    private LoginAttemptService loginAttemptService;
    private HttpServletRequest httpRequest;
    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userRepository = mock(UserRepository.class);
        jwtUtils = mock(JwtUtils.class);
        csrfTokenService = mock(CsrfTokenService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        ipLocationService = mock(IpLocationService.class);
        loginAttemptService = mock(LoginAttemptService.class);
        httpRequest = mock(HttpServletRequest.class);
        service = serviceWithSecondaryPassword("lzh031224");

        when(loginAttemptService.remainingLockSeconds(anyString())).thenReturn(0L);
        when(jwtUtils.generateJwtToken(any(Authentication.class))).thenReturn("jwt-token");
        when(csrfTokenService.generateToken("jwt-token")).thenReturn("csrf-token");
        when(ipLocationService.getClientIpAddress(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(ipLocationService.getCityByIp("127.0.0.1")).thenReturn("拉萨");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void superAdminLoginRequiresSecondaryPassword() {
        User superAdmin = user("lzh", User.Role.ADMIN);
        stubAuthenticatedUser("lzh", superAdmin);

        assertThatThrownBy(() -> service.login(loginRequest("lzh", "031224", ""), httpRequest))
                .isInstanceOf(AuthForbiddenException.class);

        verify(loginAttemptService).recordFailure("lzh");
        assertThat(superAdmin.getAllowedLoginFingerprintHash()).isNull();
    }

    @Test
    void superAdminLoginAcceptsConfiguredSecondaryPassword() {
        User superAdmin = user("lzh", User.Role.ADMIN);
        stubAuthenticatedUser("lzh", superAdmin);

        LoginResult result = service.login(loginRequest("lzh", "031224", "lzh031224"), httpRequest);

        assertThat(result.jwt()).isEqualTo("jwt-token");
        assertThat(result.csrfToken()).isEqualTo("csrf-token");
        assertThat(result.user()).containsEntry("username", "lzh");
        assertThat(superAdmin.getAllowedLoginFingerprintHash()).isNull();
    }

    @Test
    void regularAdminLoginDoesNotRequireSecondaryPassword() {
        User admin = user("admin", User.Role.ADMIN);
        stubAuthenticatedUser("admin", admin);

        LoginResult result = service.login(loginRequest("admin", "admin-pass", ""), httpRequest);

        assertThat(result.jwt()).isEqualTo("jwt-token");
        assertThat(result.user()).containsEntry("username", "admin");
    }

    @Test
    void missingSecondaryPasswordConfigBlocksSuperAdminLogin() {
        service = serviceWithSecondaryPassword("");
        User superAdmin = user("lzh", User.Role.ADMIN);
        stubAuthenticatedUser("lzh", superAdmin);

        assertThatThrownBy(() -> service.login(loginRequest("lzh", "031224", "lzh031224"), httpRequest))
                .isInstanceOf(AuthForbiddenException.class);
    }

    private AuthApplicationService serviceWithSecondaryPassword(String secondaryPassword) {
        return new AuthApplicationService(
                authenticationManager,
                userRepository,
                jwtUtils,
                csrfTokenService,
                passwordEncoder,
                ipLocationService,
                loginAttemptService,
                "lzh",
                secondaryPassword);
    }

    private void stubAuthenticatedUser(String username, User user) {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("encoded")
                .roles(user.getRole().name())
                .build();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    private static User user(String username, User.Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setNickname(username);
        user.setRole(role);
        user.setPassword("encoded");
        return user;
    }

    private static LoginRequest loginRequest(String username, String password, String secondaryPassword) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setSecondaryPassword(secondaryPassword);
        return request;
    }
}
