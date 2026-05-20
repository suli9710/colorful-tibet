package com.tibet.tourism.controller.admin;

import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.security.LoginAttemptService;
import com.tibet.tourism.service.FileStorageService;
import com.tibet.tourism.service.admin.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final FileStorageService fileStorageService;
    private final AdminUserService adminUserService;

    public AdminUserController(UserRepository userRepository,
                               LoginAttemptService loginAttemptService,
                               FileStorageService fileStorageService,
                               AdminUserService adminUserService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.fileStorageService = fileStorageService;
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserSummary>> getAllUsers(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AdminUserSummary> users = userRepository.findAll(pageable)
                .map(user -> AdminUserSummary.from(user, loginAttemptService));
        return ResponseEntity.ok(users);
    }

    public record AdminUserSummary(
            Long id,
            String username,
            String nickname,
            User.Role role,
            LocalDateTime createdAt,
            boolean locked,
            long lockRemainingSeconds,
            int failureCount
    ) {
        static AdminUserSummary from(User user, LoginAttemptService service) {
            long remaining = service.remainingLockSeconds(user.getUsername());
            return new AdminUserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getRole(),
                    user.getCreatedAt(),
                    remaining > 0,
                    remaining,
                    service.failureCount(user.getUsername()));
        }
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        loginAttemptService.reset(user.getUsername());
        return ResponseEntity.ok(Map.of("message", "已解除登录锁定"));
    }

    @PostMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id,
                                            @RequestBody Map<String, String> request,
                                            Authentication authentication) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AdminUserService.RoleUpdateResult result = adminUserService.updateRole(
                userOpt.get(), request.get("role"), authentication);
        if (result.success()) {
            return ResponseEntity.ok(Map.of("message", result.message()));
        }
        return ResponseEntity.status(result.status()).body(Map.of("error", result.message()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String error = adminUserService.deleteUser(userOpt.get(), authentication);
        if (error != null) {
            if (error.contains("超级管理员可以删除")) {
                return ResponseEntity.status(403).body(Map.of("error", error));
            }
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        return ResponseEntity.ok(Map.of("message", "用户删除成功"));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = fileStorageService.storeAdminImage(file);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传文件不符合要求"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败，请稍后重试"));
        }
    }
}
