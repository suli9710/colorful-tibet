package com.tibet.tourism.controller;

import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.*;
import com.tibet.tourism.security.JwtUtils;
import com.tibet.tourism.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtils jwtUtils;

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
    com.tibet.tourism.service.PasswordEncryptionService passwordEncryptionService;

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return user.getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    // 获取当前用户信息
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("nickname", user.getNickname());
            response.put("avatar", user.getAvatar());
            response.put("phone", user.getPhone());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 修改密码
    @PostMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String oldPassword = payload.get("oldPassword");
            String newPassword = payload.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Old password and new password are required"));
            }

            userService.changePassword(userId, oldPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    // 更新昵称
    @PutMapping("/me/nickname")
    public ResponseEntity<?> updateNickname(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String nickname = payload.get("nickname");
            
            if (nickname == null || nickname.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "昵称不能为空"));
            }
            
            nickname = nickname.trim();
            
            // 检查昵称是否已被其他用户使用
            User existingUser = userRepository.findByNickname(nickname).orElse(null);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "该昵称已被使用，请选择其他昵称"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setNickname(nickname);
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("message", "昵称更新成功", "nickname", nickname));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 更新头像URL（用于使用外部链接）
    @PutMapping("/me/avatar")
    public ResponseEntity<?> updateAvatar(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String avatarUrl = payload.get("avatarUrl");
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("message", "头像更新成功", "avatarUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.get("username"), loginRequest.get("password")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        // 获取IP地址并解析城市
        try {
            String ipAddress = ipLocationService.getClientIpAddress(request);
            String city = ipLocationService.getCityByIp(ipAddress);
            
            // 更新用户IP和城市信息
            user.setIpAddress(ipAddress);
            user.setCity(city);
            user.setLastLoginAt(java.time.LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            // 如果IP解析失败，不影响登录流程
            System.err.println("Failed to update user IP location: " + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("nickname", user.getNickname());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Error: Username is already taken!"));
        }

        // Create new user's account
        String plainPassword = signUpRequest.getPassword();
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        // 双重存储：BCrypt用于验证，AES用于管理员查看
        user.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt哈希
        user.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES加密
        user.setNickname(signUpRequest.getNickname());
        user.setRole(User.Role.USER);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }
}
