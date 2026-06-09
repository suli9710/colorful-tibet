package com.tibet.tourism.modules.admin.web;

import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.modules.admin.application.AdminUserService;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<PageResponse<AdminUserSummary>> getAllUsers(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(userRepository.findAll(pageable)
                .map(user -> AdminUserSummary.from(
                        user,
                        loginAttemptService,
                        adminUserService.actionPolicyFor(user, authentication)))));
    }

    public record AdminUserSummary(
            Long id,
            String username,
            String nickname,
            User.Role role,
            LocalDateTime createdAt,
            boolean locked,
            long lockRemainingSeconds,
            int failureCount,
            boolean protectedAccount,
            boolean deletable,
            boolean roleMutable
    ) {
        static AdminUserSummary from(
                User user,
                LoginAttemptService service,
                AdminUserService.UserActionPolicy actionPolicy) {
            long remaining = service.remainingLockSeconds(user.getUsername());
            return new AdminUserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getRole(),
                    user.getCreatedAt(),
                    remaining > 0,
                    remaining,
                    service.failureCount(user.getUsername()),
                    actionPolicy.protectedAccount(),
                    actionPolicy.deletable(),
                    actionPolicy.roleMutable());
        }
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id, Authentication authentication) {
        AdminUserService.UnlockUserResult result = adminUserService.unlockLogin(id, authentication);
        if (result.success()) {
            return ResponseEntity.ok(Map.of("message", result.message()));
        }
        if (result.status() == 404) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(result.status()).body(Map.of("error", result.message()));
    }

    @PostMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id,
                                            @RequestBody Map<String, String> request,
                                            Authentication authentication) {
        AdminUserService.RoleUpdateResult result = adminUserService.updateRole(
                id, request.get("role"), authentication);
        if (result.success()) {
            return ResponseEntity.ok(Map.of("message", result.message()));
        }
        if (result.status() == 404) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(result.status()).body(Map.of("error", result.message()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        AdminUserService.DeleteUserResult result = adminUserService.deleteUser(id, authentication);
        if (result.success()) {
            return ResponseEntity.ok(Map.of("message", result.message()));
        }
        if (result.status() == 404) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(result.status()).body(Map.of("error", result.message()));
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
