package com.tibet.tourism.controller;

import com.tibet.tourism.dto.ChangePasswordRequest;
import com.tibet.tourism.dto.LoginRequest;
import com.tibet.tourism.dto.RegisterRequest;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.*;
import com.tibet.tourism.security.CookieAuthConstants;
import com.tibet.tourism.security.CsrfTokenService;
import com.tibet.tourism.security.InputSanitizer;
import com.tibet.tourism.security.JwtAuthSupport;
import com.tibet.tourism.security.JwtUtils;
import com.tibet.tourism.security.LoginAttemptService;
import com.tibet.tourism.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final Duration AUTH_COOKIE_MAX_AGE = Duration.ofDays(1);

    @org.springframework.beans.factory.annotation.Value("${app.security.cookie-secure:true}")
    private boolean secureCookies;

    @org.springframework.beans.factory.annotation.Value("${app.super-admin-username:lzh}")
    private String superAdminUsername;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    JwtAuthSupport jwtAuthSupport;

    @Autowired
    CsrfTokenService csrfTokenService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    SharedRouteRepository sharedRouteRepository;

    @Autowired
    RouteCommentRepository routeCommentRepository;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    com.tibet.tourism.service.UserService userService;

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    com.tibet.tourism.service.IpLocationService ipLocationService;

    @Autowired
    LoginAttemptService loginAttemptService;

    private Long getCurrentUserId(HttpServletRequest request) {
        return jwtAuthSupport.resolveCurrentUserId(request);
    }

    private ResponseEntity<?> errorResponse(Exception e) {
        logger.warn("Authenticated user request failed: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", "请求处理失败，请检查输入后重试"));
    }

    // 获取当前用户信息
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            User user = userRepository.findById(getCurrentUserId(request))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("nickname", user.getNickname());
            response.put("avatar", user.getAvatar());
            response.put("phone", user.getPhone());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt());
            response.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // 获取用户统计数据
    @GetMapping("/me/stats")
    public ResponseEntity<?> getUserStats(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> stats = new HashMap<>();
            stats.put("routeCount", sharedRouteRepository.findByAuthorOrderByCreatedAtDesc(user).size());
            stats.put("commentCount", routeCommentRepository.countByUser(user) + commentRepository.countByUser(user));
            stats.put("bookingCount", bookingRepository.findByUserId(userId).size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // 修改密码
    @PostMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            userService.changePassword(userId, payload.getOldPassword(), payload.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // 获取用户的评论（包括景点评论和路线评论）
    @GetMapping("/me/comments")
    public ResponseEntity<?> getMyComments(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> comments = new HashMap<>();
            // 景点评论
            comments.put("spotComments", commentRepository.findByUserOrderByCreatedAtDesc(user));
            // 路线评论
            comments.put("routeComments", routeCommentRepository.findByUserOrderByCreatedAtDesc(user));

            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // 上传头像
    @PostMapping("/me/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String avatarUrl = fileStorageService.storeAvatar(file);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传文件不符合要求"));
        } catch (Exception e) {
            logger.warn("Avatar upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "上传失败，请稍后重试"));
        }
    }

    // 更新昵称
    @PutMapping("/me/nickname")
    @Transactional
    public ResponseEntity<?> updateNickname(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String nickname = InputSanitizer.requiredPlainText(payload.get("nickname"), 32, "昵称");

            // 检查昵称是否已被其他用户使用
            if (userRepository.existsByNickname(nickname)) {
                User existingNicknameUser = userRepository.findByNickname(nickname).orElse(null);
                if (existingNicknameUser != null && !existingNicknameUser.getId().equals(userId)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "昵称更新失败，请检查输入后重试"));
                }
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setNickname(nickname);
            userRepository.saveAndFlush(user);

            return ResponseEntity.ok(Map.of("message", "昵称更新成功", "nickname", nickname));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "昵称更新失败，请检查输入后重试"));
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // 更新头像URL（用于使用外部链接）
    @PutMapping("/me/avatar")
    public ResponseEntity<?> updateAvatar(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String avatarUrl = InputSanitizer.optionalPublicImageUrl(payload.get("avatarUrl"), "头像地址");
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("message", "头像更新成功", "avatarUrl", avatarUrl));
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername().trim();

        long remainingLock = loginAttemptService.remainingLockSeconds(username);
        if (remainingLock > 0) {
            logger.warn("Blocked login attempt for locked account: username={}, remainingLock={}s", username, remainingLock);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "请求过于频繁，请稍后重试"));
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            logger.warn("Login failed for username={}", username);
            long lockSeconds = loginAttemptService.recordFailure(username);
            if (lockSeconds > 0) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "请求过于频繁，请稍后重试"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户名或密码错误"));
        }

        loginAttemptService.reset(username);

        // 超管账户仅允许本地登录
        if (superAdminUsername.equals(username) && !isLocalRequest(request)) {
            logger.warn("Rejected remote super-admin login attempt: username={}, ip={}", username, request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "用户名或密码错误"));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        // 获取IP地址并解析城市
        try {
            String ipAddress = ipLocationService.getClientIpAddress(request);
            String city = ipLocationService.getCityByIp(ipAddress);
            
            // 更新用户IP和城市信息
            user.setIpAddress(InputSanitizer.sha256HexForStorage(ipAddress));
            user.setCity(city);
            user.setLastLoginAt(java.time.LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            // 如果IP解析失败，不影响登录流程
            logger.debug("Failed to update user IP location", e);
        }

        String csrfToken = csrfTokenService.generateToken(jwt);
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("nickname", user.getNickname());
        response.put("role", user.getRole());
        response.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, authCookie(jwt, AUTH_COOKIE_MAX_AGE).toString())
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, csrfCookie(csrfToken, AUTH_COOKIE_MAX_AGE).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.noContent()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, authCookie("", Duration.ZERO).toString())
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, csrfCookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        String username = signUpRequest.getUsername().trim();
        String nickname = InputSanitizer.optionalPlainText(signUpRequest.getNickname(), 32, "昵称");
        String plainPassword = signUpRequest.getPassword();

        logger.debug("Register payload received. username={}, nickname={}, passwordEmpty={}",
                username, nickname, !StringUtils.hasText(plainPassword));

        try {
            InputSanitizer.validatePassword(plainPassword);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "注册失败，请检查输入后重试"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setRole(User.Role.USER);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    private ResponseCookie authCookie(String value, Duration maxAge) {
        return ResponseCookie.from(CookieAuthConstants.AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie csrfCookie(String value, Duration maxAge) {
        return ResponseCookie.from(CookieAuthConstants.CSRF_COOKIE_NAME, value)
                .httpOnly(false)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private boolean isLocalRequest(HttpServletRequest request) {
        try {
            InetAddress addr = InetAddress.getByName(request.getRemoteAddr());
            if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()) {
                return true;
            }

            // In local Docker Compose, nginx reaches the backend over a private bridge
            // address while the browser still uses http://localhost.
            return addr.isSiteLocalAddress() && isLocalServerName(request.getServerName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLocalServerName(String serverName) {
        return "localhost".equalsIgnoreCase(serverName)
                || "127.0.0.1".equals(serverName)
                || "::1".equals(serverName)
                || "0:0:0:0:0:0:0:1".equals(serverName);
    }
}
