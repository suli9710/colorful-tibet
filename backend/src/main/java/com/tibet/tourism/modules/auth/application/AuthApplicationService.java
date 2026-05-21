package com.tibet.tourism.modules.auth.application;
import com.tibet.tourism.common.logging.IpLocationService;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.auth.domain.AuthFailureException;
import com.tibet.tourism.modules.auth.domain.AuthForbiddenException;
import com.tibet.tourism.modules.auth.domain.AuthRateLimitException;
import com.tibet.tourism.modules.auth.domain.DuplicateRegistrationException;
import com.tibet.tourism.modules.auth.web.dto.LoginRequest;
import com.tibet.tourism.modules.auth.web.dto.RegisterRequest;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthApplicationService.class);
    private static final String GENERIC_LOGIN_ERROR = "Invalid username or password";
    private static final String RATE_LIMIT_ERROR = "Too many requests, please try again later";
    private static final String REGISTRATION_FAILED = "Registration failed, please check your input";
    private static final Pattern SAFE_USERNAME = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final CsrfTokenService csrfTokenService;
    private final PasswordEncoder passwordEncoder;
    private final IpLocationService ipLocationService;
    private final LoginAttemptService loginAttemptService;
    private final TotpService totpService;
    private final String superAdminUsername;
    private final String superAdminTotpSecret;
    private final boolean requireStrongSecrets;
    private final Environment environment;

    public AuthApplicationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtUtils jwtUtils,
            CsrfTokenService csrfTokenService,
            PasswordEncoder passwordEncoder,
            IpLocationService ipLocationService,
            LoginAttemptService loginAttemptService,
            TotpService totpService,
            @Value("${app.super-admin-username:lzh}") String superAdminUsername,
            @Value("${app.security.super-admin-totp-secret:}") String superAdminTotpSecret,
            @Value("${app.security.require-strong-secrets:false}") boolean requireStrongSecrets,
            Environment environment) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.csrfTokenService = csrfTokenService;
        this.passwordEncoder = passwordEncoder;
        this.ipLocationService = ipLocationService;
        this.loginAttemptService = loginAttemptService;
        this.totpService = totpService;
        this.superAdminUsername = superAdminUsername;
        this.superAdminTotpSecret = superAdminTotpSecret;
        this.requireStrongSecrets = requireStrongSecrets;
        this.environment = environment;
    }

    @PostConstruct
    void validateSuperAdminConfiguration() {
        boolean strictMode = requireStrongSecrets || isProdProfileActive();
        if (!StringUtils.hasText(superAdminUsername)) {
            if (strictMode) {
                throw new IllegalStateException("Super-admin username must be configured");
            }
            logger.warn("Super-admin username is not configured; TOTP enforcement is disabled");
            return;
        }

        if (!StringUtils.hasText(superAdminTotpSecret)) {
            if (strictMode) {
                throw new IllegalStateException("Super-admin TOTP secret must be configured");
            }
            logger.warn("Super-admin TOTP secret is not configured; super-admin login will be blocked");
            return;
        }

        totpService.validateSecret(superAdminTotpSecret);
    }

    public LoginResult login(LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername().trim();

        long remainingLock = loginAttemptService.remainingLockSeconds(username);
        if (remainingLock > 0) {
            logger.warn("Blocked login attempt for locked account: username={}, remainingLock={}s", username, remainingLock);
            throw new AuthRateLimitException(RATE_LIMIT_ERROR);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            logger.warn("Login failed for username={}", username);
            long lockSeconds = loginAttemptService.recordFailure(username);
            if (lockSeconds > 0) {
                throw new AuthRateLimitException(RATE_LIMIT_ERROR);
            }
            throw new AuthFailureException(GENERIC_LOGIN_ERROR);
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        enforceSuperAdminControls(user, loginRequest);

        loginAttemptService.reset(username);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);
        updateLoginLocation(user, request);
        String csrfToken = csrfTokenService.generateToken(jwt);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("nickname", user.getNickname());
        response.put("role", user.getRole());
        response.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));

        return new LoginResult(jwt, csrfToken, response);
    }

    @Transactional
    public void register(RegisterRequest signUpRequest) {
        String username = signUpRequest.getUsername() == null ? "" : signUpRequest.getUsername().trim();
        String nickname = InputSanitizer.optionalPlainText(signUpRequest.getNickname(), 32, "nickname");
        String plainPassword = signUpRequest.getPassword();

        logger.debug("Register payload received. username={}, nickname={}, passwordEmpty={}",
                username, nickname, !StringUtils.hasText(plainPassword));

        InputSanitizer.validatePassword(plainPassword);
        if (!SAFE_USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException(REGISTRATION_FAILED);
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateRegistrationException(REGISTRATION_FAILED);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setRole(User.Role.USER);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            logger.warn("Registration failed due to duplicate key: username={}", username);
            throw new DuplicateRegistrationException(REGISTRATION_FAILED);
        }
    }

    private void updateLoginLocation(User user, HttpServletRequest request) {
        try {
            String ipAddress = ipLocationService.getClientIpAddress(request);
            String city = ipLocationService.getCityByIp(ipAddress);

            user.setIpAddress(InputSanitizer.sha256HexForStorage(ipAddress));
            user.setCity(city);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception exception) {
            logger.debug("Failed to update user IP location", exception);
        }
    }

    private void enforceSuperAdminControls(User user, LoginRequest loginRequest) {
        if (!superAdminUsername.equalsIgnoreCase(user.getUsername())) {
            return;
        }

        enforceSuperAdminTotp(user, loginRequest);
        if (ensureSuperAdminRole(user)) {
            userRepository.saveAndFlush(user);
        }
    }

    private void enforceSuperAdminTotp(User user, LoginRequest loginRequest) {
        if (!StringUtils.hasText(superAdminTotpSecret)) {
            logger.error("Super-admin TOTP secret is not configured: username={}", user.getUsername());
            throw new AuthForbiddenException(GENERIC_LOGIN_ERROR);
        }

        String provided = loginRequest.getSecondaryPassword();
        if (!StringUtils.hasText(provided) || !totpService.isValidCode(superAdminTotpSecret, provided)) {
            logger.warn("Rejected super-admin login with invalid TOTP code: username={}", user.getUsername());
            long lockSeconds = loginAttemptService.recordFailure(user.getUsername());
            if (lockSeconds > 0) {
                throw new AuthRateLimitException(RATE_LIMIT_ERROR);
            }
            throw new AuthForbiddenException(GENERIC_LOGIN_ERROR);
        }
    }

    private boolean ensureSuperAdminRole(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return false;
        }
        logger.warn("Promoting configured super-admin account to ADMIN during verified login: username={}",
                user.getUsername());
        user.setRole(User.Role.ADMIN);
        return true;
    }

    private boolean isProdProfileActive() {
        return environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }
}
