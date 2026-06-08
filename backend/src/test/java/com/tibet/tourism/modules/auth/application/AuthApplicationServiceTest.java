package com.tibet.tourism.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.logging.IpLocationService;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.common.security.antibot.AntibotProperties;
import com.tibet.tourism.common.security.antibot.RecaptchaService;
import com.tibet.tourism.modules.auth.domain.AuthFailureException;
import com.tibet.tourism.modules.auth.domain.AuthForbiddenException;
import com.tibet.tourism.modules.auth.domain.SecondaryAuthRequiredException;
import com.tibet.tourism.modules.auth.domain.AuthRateLimitException;
import com.tibet.tourism.modules.auth.web.dto.LoginRequest;
import com.tibet.tourism.modules.auth.web.dto.RegisterRequest;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.OptionalDouble;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.env.MockEnvironment;

class AuthApplicationServiceTest {

    private static final String TOTP_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private JwtUtils jwtUtils;
    private CsrfTokenService csrfTokenService;
    private PasswordEncoder passwordEncoder;
    private IpLocationService ipLocationService;
    private LoginAttemptService loginAttemptService;
    private TotpService totpService;
    private RecaptchaService recaptchaService;
    private AntibotProperties antibotProperties;
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
        totpService = new TotpService();
        recaptchaService = mock(RecaptchaService.class);
        antibotProperties = new AntibotProperties();
        httpRequest = mock(HttpServletRequest.class);
        service = serviceWithTotpSecret(TOTP_SECRET);

        when(loginAttemptService.evaluate(anyString(), anyString()))
                .thenReturn(new LoginAttemptService.LoginAttemptDecision(true, false, "", 0, 0));
        when(loginAttemptService.recordFailure(anyString(), anyString()))
                .thenReturn(new LoginAttemptService.LoginAttemptDecision(true, false, "", 0, 1));
        when(jwtUtils.generateJwtToken(any(Authentication.class), anyLong())).thenReturn("jwt-token");
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
    void superAdminLoginRequiresTotpCode() {
        User superAdmin = user("lzh", User.Role.ADMIN);
        stubAuthenticatedUser("lzh", superAdmin);

        assertThatThrownBy(() -> service.login(loginRequest("lzh", "031224", ""), httpRequest))
                .isInstanceOf(SecondaryAuthRequiredException.class);

        verify(loginAttemptService, never()).recordFailure(anyString(), anyString());
        assertThat(superAdmin.getAllowedLoginFingerprintHash()).isNull();
    }

    @Test
    void superAdminLoginAcceptsValidTotpCode() {
        User superAdmin = user("lzh", User.Role.ADMIN);
        stubAuthenticatedUser("lzh", superAdmin);
        String currentCode = totpService.generateCodeForTime(TOTP_SECRET, Instant.now());

        LoginResult result = service.login(loginRequest("lzh", "031224", currentCode), httpRequest);

        assertThat(result.jwt()).isEqualTo("jwt-token");
        assertThat(result.csrfToken()).isEqualTo("csrf-token");
        assertThat(result.user())
                .doesNotContainKeys("id", "username", "nickname")
                .containsEntry("avatar", "/avatars/lzh.png")
                .containsEntry("avatarUrl", "/avatars/lzh.png")
                .containsEntry("role", User.Role.ADMIN)
                .containsEntry("mustChangePassword", false);
        assertThat(superAdmin.getAllowedLoginFingerprintHash()).isNull();
    }

    @Test
    void invalidSuperAdminTotpUsesGenericLoginFailure() {
        User superAdmin = user("lzh", User.Role.ADMIN);
        stubAuthenticatedUser("lzh", superAdmin);

        assertThatThrownBy(() -> service.login(loginRequest("lzh", "031224", "not-digits"), httpRequest))
                .isInstanceOf(AuthFailureException.class)
                .hasMessageContaining("Invalid username or password");

        verify(loginAttemptService).recordFailure("lzh", "127.0.0.1");
    }

    @Test
    void blankLoginRequestUsesGenericFailureWithoutAttemptEvaluation() {
        assertThatThrownBy(() -> service.login(null, httpRequest))
                .isInstanceOf(AuthFailureException.class)
                .hasMessageContaining("Invalid username or password");

        verify(loginAttemptService, never()).evaluate(anyString(), anyString());
    }

    @Test
    void unexpectedAuthenticationProviderFailureUsesGenericFailureWithoutLockingUser() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new IllegalStateException("jdbc password=secret-token"));

        assertThatThrownBy(() -> service.login(loginRequest("admin", "admin-pass", ""), httpRequest))
                .isInstanceOf(AuthFailureException.class)
                .hasMessageContaining("Invalid username or password")
                .hasMessageNotContaining("secret-token");

        verify(loginAttemptService, never()).recordFailure(anyString(), anyString());
        verify(jwtUtils, never()).generateJwtToken(any(Authentication.class), anyLong());
    }

    @Test
    void authenticatedPrincipalWithoutUserRecordUsesGenericLoginFailure() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("ghost")
                .password("encoded")
                .roles("USER")
                .build();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginRequest("ghost", "admin-pass", ""), httpRequest))
                .isInstanceOf(AuthFailureException.class)
                .hasMessageContaining("Invalid username or password");

        verify(jwtUtils, never()).generateJwtToken(any(Authentication.class), anyLong());
    }

    @Test
    void superAdminLoginAllowsDifferentDeviceWhenTotpIsValid() {
        User superAdmin = user("lzh", User.Role.ADMIN);
        superAdmin.setAllowedLoginFingerprintHash("legacy-fingerprint-hash");
        stubAuthenticatedUser("lzh", superAdmin);
        when(httpRequest.getHeader("X-Device-Fingerprint")).thenReturn("new-device");
        String currentCode = totpService.generateCodeForTime(TOTP_SECRET, Instant.now());

        LoginResult result = service.login(loginRequest("lzh", "031224", currentCode), httpRequest);

        assertThat(result.jwt()).isEqualTo("jwt-token");
        assertThat(result.user()).containsEntry("role", User.Role.ADMIN);
        assertThat(superAdmin.getAllowedLoginFingerprintHash()).isEqualTo("legacy-fingerprint-hash");
    }

    @Test
    void verifiedSuperAdminLoginRejectsNonAdminRole() {
        User superAdmin = user("lzh", User.Role.USER);
        stubAuthenticatedUser("lzh", superAdmin);
        String currentCode = totpService.generateCodeForTime(TOTP_SECRET, Instant.now());

        assertThatThrownBy(() -> service.login(loginRequest("lzh", "031224", currentCode), httpRequest))
                .isInstanceOf(AuthForbiddenException.class);

        assertThat(superAdmin.getRole()).isEqualTo(User.Role.USER);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void regularAdminLoginDoesNotRequireSecondaryPassword() {
        User admin = user("admin", User.Role.ADMIN);
        stubAuthenticatedUser("admin", admin);

        LoginResult result = service.login(loginRequest("admin", "admin-pass", ""), httpRequest);

        assertThat(result.jwt()).isEqualTo("jwt-token");
        assertThat(result.user())
                .doesNotContainKeys("id", "username", "nickname")
                .containsEntry("role", User.Role.ADMIN)
                .containsEntry("mustChangePassword", false);
    }

    @Test
    void loginReturnsDistinctPublicNickname() {
        User traveler = user("traveler", User.Role.USER);
        traveler.setNickname(" Snow Road Guest ");
        stubAuthenticatedUser("traveler", traveler);

        LoginResult result = service.login(loginRequest("traveler", "user-pass", ""), httpRequest);

        assertThat(result.user())
                .doesNotContainKeys("id", "username")
                .containsEntry("nickname", "Snow Road Guest")
                .containsEntry("role", User.Role.USER);
    }

    @Test
    void loginRateLimitsByIpOrPairBeforePasswordCheck() {
        when(loginAttemptService.evaluate("admin", "127.0.0.1"))
                .thenReturn(new LoginAttemptService.LoginAttemptDecision(false, false, "ip", 45, 0));

        assertThatThrownBy(() -> service.login(loginRequest("admin", "bad-pass", ""), httpRequest))
                .isInstanceOf(AuthRateLimitException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(45L);
    }

    @Test
    void accountStepUpUsesRecaptchaInsteadOfHardAccountLock() {
        User admin = user("admin", User.Role.ADMIN);
        stubAuthenticatedUser("admin", admin);
        antibotProperties.setEnabled(true);
        antibotProperties.getRecaptcha().setEnabled(true);
        antibotProperties.getRecaptcha().setSecretKey("secret");
        antibotProperties.getRecaptcha().setMinScore(0.5);
        when(httpRequest.getHeader("X-Recaptcha-Token")).thenReturn("valid-token");
        when(loginAttemptService.evaluate("admin", "127.0.0.1"))
                .thenReturn(new LoginAttemptService.LoginAttemptDecision(true, true, "", 0, 8));
        when(recaptchaService.verify("valid-token", "127.0.0.1")).thenReturn(OptionalDouble.of(0.9));

        LoginResult result = service.login(loginRequest("admin", "admin-pass", ""), httpRequest);

        assertThat(result.jwt()).isEqualTo("jwt-token");
        verify(loginAttemptService).reset("admin", "127.0.0.1");
    }

    @Test
    void accountStepUpRejectsMissingRecaptchaWhenConfigured() {
        User admin = user("admin", User.Role.ADMIN);
        stubAuthenticatedUser("admin", admin);
        antibotProperties.setEnabled(true);
        antibotProperties.getRecaptcha().setEnabled(true);
        antibotProperties.getRecaptcha().setSecretKey("secret");
        when(loginAttemptService.evaluate("admin", "127.0.0.1"))
                .thenReturn(new LoginAttemptService.LoginAttemptDecision(true, true, "", 0, 8));
        when(recaptchaService.verify(null, "127.0.0.1")).thenReturn(OptionalDouble.empty());

        assertThatThrownBy(() -> service.login(loginRequest("admin", "admin-pass", ""), httpRequest))
                .isInstanceOf(AuthForbiddenException.class)
                .hasMessageContaining("Additional verification required");
    }

    @Test
    void missingTotpSecretConfigBlocksSuperAdminLogin() {
        service = serviceWithTotpSecret("");
        User superAdmin = user("lzh", User.Role.ADMIN);
        stubAuthenticatedUser("lzh", superAdmin);

        assertThatThrownBy(() -> service.login(loginRequest("lzh", "031224", "123456"), httpRequest))
                .isInstanceOf(AuthForbiddenException.class);
    }

    @Test
    void rejectsMissingTotpSecretInStrictMode() {
        service = serviceWithTotpSecret("", true);

        assertThatThrownBy(service::validateSuperAdminConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
    }

    @Test
    void rejectsInvalidTotpSecretDuringStartup() {
        service = serviceWithTotpSecret("not-a-valid-secret!");

        assertThatThrownBy(service::validateSuperAdminConfiguration)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid Base32");
    }

    @Test
    void rejectsShortTotpSecretDuringStartup() {
        service = serviceWithTotpSecret("JBSWY3DPEHPK3PXP");

        assertThatThrownBy(service::validateSuperAdminConfiguration)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128 bits");
    }

    @Test
    void registerRejectsHomoglyphUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("adm\u0456n");
        request.setPassword("Strong1!");

        assertThatThrownBy(() -> service.register(request, httpRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Registration failed");
    }

    @Test
    void registerDoesNotDefaultNicknameToLoginUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("traveler");
        request.setPassword("Strong1!");
        when(passwordEncoder.encode("Strong1!")).thenReturn("encoded-password");
        when(userRepository.existsByUsernameIgnoreCase("traveler")).thenReturn(false);

        service.register(request, httpRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("traveler");
        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void registerStoresTrimmedDistinctPublicNickname() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("traveler");
        request.setNickname(" Snow Road Guest ");
        request.setPassword("Strong1!");
        when(passwordEncoder.encode("Strong1!")).thenReturn("encoded-password");
        when(userRepository.existsByUsernameIgnoreCase("traveler")).thenReturn(false);

        service.register(request, httpRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("Snow Road Guest");
    }

    @Test
    void registerSuppressesNicknameMatchingLoginUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("traveler");
        request.setNickname(" TRAVELER ");
        request.setPassword("Strong1!");
        when(passwordEncoder.encode("Strong1!")).thenReturn("encoded-password");
        when(userRepository.existsByUsernameIgnoreCase("traveler")).thenReturn(false);

        service.register(request, httpRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void duplicateRegistrationPaysPasswordHashCostBeforeGenericFailure() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("traveler");
        request.setPassword("Strong1!");
        when(passwordEncoder.encode("Strong1!")).thenReturn("encoded-password");
        when(userRepository.existsByUsernameIgnoreCase("traveler")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request, httpRequest))
                .isInstanceOf(com.tibet.tourism.modules.auth.domain.DuplicateRegistrationException.class)
                .hasMessageContaining("Registration failed");

        verify(passwordEncoder).encode("Strong1!");
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registrationRecaptchaLogMasksClientIp() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/tibet/tourism/modules/auth/application/AuthApplicationService.java"));

        assertThat(source).contains("PiiMasker.maskIp(clientIp)");
        assertThat(source).doesNotContain("Registration rejected by reCAPTCHA: ip={}, score={}, minScore={}\",\n                    clientIp");
    }

    private AuthApplicationService serviceWithTotpSecret(String totpSecret) {
        return serviceWithTotpSecret(totpSecret, false);
    }

    private AuthApplicationService serviceWithTotpSecret(String totpSecret, boolean requireStrongSecrets) {
        return new AuthApplicationService(
                authenticationManager,
                userRepository,
                jwtUtils,
                csrfTokenService,
                passwordEncoder,
                ipLocationService,
                loginAttemptService,
                totpService,
                recaptchaService,
                antibotProperties,
                "lzh",
                totpSecret,
                requireStrongSecrets,
                false,
                new MockEnvironment());
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
        user.setAvatar("/avatars/" + username + ".png");
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
