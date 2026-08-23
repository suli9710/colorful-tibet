package com.tibet.tourism.modules.auth.application;
import com.tibet.tourism.common.logging.IpLocationService;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.common.security.antibot.AntibotProperties;
import com.tibet.tourism.common.security.antibot.RecaptchaService;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.auth.domain.AuthFailureException;
import com.tibet.tourism.modules.auth.domain.AuthForbiddenException;
import com.tibet.tourism.modules.auth.domain.AuthRateLimitException;
import com.tibet.tourism.modules.auth.domain.DuplicateRegistrationException;
import com.tibet.tourism.modules.auth.domain.SecondaryAuthRequiredException;
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
import java.util.OptionalDouble;
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
    private static final String STEP_UP_ERROR = "Additional verification required";
    private static final String REGISTRATION_FAILED = "Registration failed, please check your input";
    private static final String RECAPTCHA_HEADER = "X-Recaptcha-Token";
    private static final Pattern SAFE_USERNAME = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final CsrfTokenService csrfTokenService;
    private final PasswordEncoder passwordEncoder;
    private final IpLocationService ipLocationService;
    private final LoginAttemptService loginAttemptService;
    private final AdminMfaPolicy adminMfaPolicy;
    private final RecaptchaService recaptchaService;
    private final AntibotProperties antibotProperties;
    private final boolean requireStrongSecrets;
    private final boolean registrationRecaptchaRequired;
    private final Environment environment;

    public AuthApplicationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtUtils jwtUtils,
            CsrfTokenService csrfTokenService,
            PasswordEncoder passwordEncoder,
            IpLocationService ipLocationService,
            LoginAttemptService loginAttemptService,
            AdminMfaPolicy adminMfaPolicy,
            RecaptchaService recaptchaService,
            AntibotProperties antibotProperties,
            @Value("${app.security.require-strong-secrets:false}") boolean requireStrongSecrets,
            @Value("${app.security.registration-recaptcha-required:false}") boolean registrationRecaptchaRequired,
            Environment environment) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.csrfTokenService = csrfTokenService;
        this.passwordEncoder = passwordEncoder;
        this.ipLocationService = ipLocationService;
        this.loginAttemptService = loginAttemptService;
        this.adminMfaPolicy = adminMfaPolicy;
        this.recaptchaService = recaptchaService;
        this.antibotProperties = antibotProperties;
        this.requireStrongSecrets = requireStrongSecrets;
        this.registrationRecaptchaRequired = registrationRecaptchaRequired;
        this.environment = environment;
    }

    @PostConstruct
    void validateSuperAdminConfiguration() {
        adminMfaPolicy.validateStartup();
        boolean strictMode = requireStrongSecrets || isProdProfileActive();
        if (strictMode && registrationRecaptchaRequired && !isRecaptchaConfigured()) {
            throw new IllegalStateException("Registration reCAPTCHA is required but not configured");
        }
    }

    public LoginResult login(LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest == null || loginRequest.getUsername() == null
                ? ""
                : loginRequest.getUsername().trim();
        String password = loginRequest == null ? "" : loginRequest.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new AuthFailureException(GENERIC_LOGIN_ERROR);
        }
        String clientIp = resolveClientIp(request);

        LoginAttemptService.LoginAttemptDecision throttle = loginAttemptService.evaluate(username, clientIp);
        if (!throttle.allowed()) {
            logger.warn("Blocked login attempt by throttle: user={}, reason={}, retryAfter={}s",
                    userLogLabel(username), throttle.reason(), throttle.retryAfterSeconds());
            throw new AuthRateLimitException(RATE_LIMIT_ERROR, throttle.retryAfterSeconds());
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            logger.warn("Login failed for user={}", userLogLabel(username));
            LoginAttemptService.LoginAttemptDecision failure = loginAttemptService.recordFailure(username, clientIp);
            if (!failure.allowed()) {
                throw new AuthRateLimitException(RATE_LIMIT_ERROR, failure.retryAfterSeconds());
            }
            throw new AuthFailureException(GENERIC_LOGIN_ERROR);
        } catch (RuntimeException exception) {
            logger.warn("Login authentication provider failed for user={}: {}",
                    userLogLabel(username),
                    SensitiveLogSanitizer.exceptionSummary(exception));
            throw new AuthFailureException(GENERIC_LOGIN_ERROR);
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> {
                    logger.warn("Authenticated principal missing backing user: user={}",
                            userLogLabel(userDetails.getUsername()));
                    return new AuthFailureException(GENERIC_LOGIN_ERROR);
                });

        boolean adminMfaVerified = enforceAdminControls(user, loginRequest, clientIp);
        enforceAccountStepUpIfNeeded(user, throttle, request, clientIp);

        loginAttemptService.reset(username, clientIp);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String mfaBinding = adminMfaVerified
                ? adminMfaPolicy.currentBinding(user.getUsername())
                : "";
        String jwt = jwtUtils.generateJwtToken(
                authentication, user.getSessionVersion(), adminMfaVerified, mfaBinding);
        updateLoginLocation(user, request);
        String csrfToken = csrfTokenService.generateToken(jwt);

        Map<String, Object> response = new HashMap<>();
        String nickname = publicNickname(user);
        if (nickname != null) {
            response.put("nickname", nickname);
        }
        response.put("avatar", user.getAvatar());
        response.put("avatarUrl", user.getAvatar());
        response.put("role", user.getRole());
        response.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));

        return new LoginResult(jwt, csrfToken, response);
    }

    @Transactional
    public void register(RegisterRequest signUpRequest, HttpServletRequest request) {
        if (signUpRequest == null) {
            throw new IllegalArgumentException(REGISTRATION_FAILED);
        }
        enforceRegistrationRecaptcha(request);

        String username = signUpRequest.getUsername() == null ? "" : signUpRequest.getUsername().trim();
        String nickname = InputSanitizer.optionalPlainText(signUpRequest.getNickname(), 32, "nickname");
        String plainPassword = signUpRequest.getPassword();

        logger.debug("Register payload received. user={}, nicknamePresent={}, passwordEmpty={}",
                userLogLabel(username), StringUtils.hasText(nickname), !StringUtils.hasText(plainPassword));

        InputSanitizer.validatePassword(plainPassword);
        if (!SAFE_USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException(REGISTRATION_FAILED);
        }

        String encodedPassword = passwordEncoder.encode(plainPassword);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateRegistrationException(REGISTRATION_FAILED);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setNickname(registrationNickname(username, nickname));
        user.setRole(User.Role.USER);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            logger.warn("Registration failed due to duplicate key: user={}", userLogLabel(username));
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
            logger.debug("Failed to update user IP location: {}",
                    SensitiveLogSanitizer.exceptionSummary(exception));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        try {
            String clientIp = ipLocationService.getClientIpAddress(request);
            return StringUtils.hasText(clientIp) ? clientIp : "unknown";
        } catch (Exception e) {
            logger.debug("Failed to resolve client IP for login throttle: {}",
                    SensitiveLogSanitizer.exceptionSummary(e));
            return "unknown";
        }
    }

    private void enforceAccountStepUpIfNeeded(User user,
                                              LoginAttemptService.LoginAttemptDecision throttle,
                                              HttpServletRequest request,
                                              String clientIp) {
        if (!throttle.stepUpRequired() || adminMfaPolicy.isSuperAdmin(user)) {
            return;
        }
        if (!isRecaptchaConfigured()) {
            logger.error("Login account step-up rejected because reCAPTCHA is not configured: user={}, failures={}",
                    userLogLabel(user.getUsername()), throttle.accountFailures());
            throw new AuthForbiddenException(STEP_UP_ERROR);
        }

        String token = request.getHeader(RECAPTCHA_HEADER);
        OptionalDouble score = recaptchaService.verify(token, clientIp);
        double minScore = antibotProperties.getRecaptcha().getMinScore();
        if (score.isEmpty() || score.getAsDouble() < minScore) {
            logger.warn("Login account step-up rejected: user={}, recaptchaScore={}, minScore={}",
                    userLogLabel(user.getUsername()), score.isPresent() ? score.getAsDouble() : null, minScore);
            throw new AuthForbiddenException(STEP_UP_ERROR);
        }
    }

    private boolean isRecaptchaConfigured() {
        if (antibotProperties == null || !antibotProperties.isEnabled()) {
            return false;
        }
        AntibotProperties.Recaptcha recaptcha = antibotProperties.getRecaptcha();
        return recaptcha != null
                && recaptcha.isEnabled()
                && StringUtils.hasText(recaptcha.getSecretKey());
    }

    private void enforceRegistrationRecaptcha(HttpServletRequest request) {
        if (!registrationRecaptchaRequired) {
            return;
        }
        if (!isRecaptchaConfigured()) {
            logger.error("Registration reCAPTCHA is required but not configured");
            throw new AuthForbiddenException(REGISTRATION_FAILED);
        }
        String clientIp = resolveClientIp(request);
        OptionalDouble score = recaptchaService.verify(request.getHeader(RECAPTCHA_HEADER), clientIp);
        double minScore = antibotProperties.getRecaptcha().getMinScore();
        if (score.isEmpty() || score.getAsDouble() < minScore) {
            logger.warn("Registration rejected by reCAPTCHA: ip={}, score={}, minScore={}",
                    PiiMasker.maskIp(clientIp), score.isPresent() ? score.getAsDouble() : null, minScore);
            throw new AuthForbiddenException(REGISTRATION_FAILED);
        }
    }

    private boolean enforceAdminControls(User user, LoginRequest loginRequest, String clientIp) {
        AdminMfaPolicy.Verification verification =
                adminMfaPolicy.verify(user, loginRequest.getSecondaryPassword());
        if (verification == AdminMfaPolicy.Verification.NOT_REQUIRED) {
            return false;
        }
        if (verification == AdminMfaPolicy.Verification.UNCONFIGURED) {
            logger.error("Administrator TOTP secret is not configured; refusing login: user={}",
                    userLogLabel(user.getUsername()));
            throw new AuthForbiddenException(GENERIC_LOGIN_ERROR);
        }
        if (verification == AdminMfaPolicy.Verification.REQUIRED) {
            throw new SecondaryAuthRequiredException("Secondary authentication required");
        }
        if (verification == AdminMfaPolicy.Verification.INVALID) {
            logger.warn("Rejected administrator login with invalid or replayed TOTP code: user={}",
                    userLogLabel(user.getUsername()));
            LoginAttemptService.LoginAttemptDecision failure =
                    loginAttemptService.recordFailure(user.getUsername(), clientIp);
            if (!failure.allowed()) {
                throw new AuthRateLimitException(RATE_LIMIT_ERROR, failure.retryAfterSeconds());
            }
            throw new AuthFailureException(GENERIC_LOGIN_ERROR);
        }

        if (adminMfaPolicy.isSuperAdmin(user)) {
            enforceSuperAdminRolePresent(user);
        }
        return true;
    }

    private void enforceSuperAdminRolePresent(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }
        logger.error("Configured super-admin account is not ADMIN; refusing login until role is fixed out of band: user={}",
                userLogLabel(user.getUsername()));
        throw new AuthForbiddenException(GENERIC_LOGIN_ERROR);
    }

    private String userLogLabel(String username) {
        return "user#" + PiiMasker.shortHash(username);
    }

    private String publicNickname(User user) {
        String nickname = user.getNickname();
        if (!StringUtils.hasText(nickname)) {
            return null;
        }
        String normalizedNickname = nickname.trim();
        String username = user.getUsername();
        if (StringUtils.hasText(username) && normalizedNickname.equalsIgnoreCase(username.trim())) {
            return null;
        }
        return normalizedNickname;
    }

    private String registrationNickname(String username, String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return null;
        }
        String normalizedNickname = nickname.trim();
        if (StringUtils.hasText(username) && normalizedNickname.equalsIgnoreCase(username.trim())) {
            return null;
        }
        return normalizedNickname;
    }

    private boolean isProdProfileActive() {
        return environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }
}
