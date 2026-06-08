package com.tibet.tourism.modules.user.web;
import com.tibet.tourism.common.api.ApiErrorResponder;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.modules.user.application.CurrentUserApplicationService;
import com.tibet.tourism.modules.user.web.dto.ChangePasswordRequest;
import com.tibet.tourism.modules.user.web.dto.UpdateAvatarRequest;
import com.tibet.tourism.modules.user.web.dto.UpdateNicknameRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@PreAuthorize("isAuthenticated()")
public class CurrentUserController {

    private static final Logger logger = LoggerFactory.getLogger(CurrentUserController.class);

    private final JwtAuthSupport jwtAuthSupport;
    private final CurrentUserApplicationService currentUserApplicationService;
    private final ApiErrorResponder apiErrorResponder;

    public CurrentUserController(
            JwtAuthSupport jwtAuthSupport,
            CurrentUserApplicationService currentUserApplicationService,
            ApiErrorResponder apiErrorResponder) {
        this.jwtAuthSupport = jwtAuthSupport;
        this.currentUserApplicationService = currentUserApplicationService;
        this.apiErrorResponder = apiErrorResponder;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(currentUserApplicationService.getProfile(currentUserId(request)));
        } catch (Exception exception) {
            return apiErrorResponder.authenticatedRequest(exception);
        }
    }

    @GetMapping("/me/stats")
    public ResponseEntity<?> getUserStats(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(currentUserApplicationService.getStats(currentUserId(request)));
        } catch (Exception exception) {
            return apiErrorResponder.authenticatedRequest(exception);
        }
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest payload, HttpServletRequest request) {
        try {
            currentUserApplicationService.changePassword(
                    currentUserId(request),
                    payload.getOldPassword(),
                    payload.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception exception) {
            return apiErrorResponder.authenticatedRequest(exception);
        }
    }

    @GetMapping("/me/comments")
    public ResponseEntity<?> getMyComments(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(currentUserApplicationService.getComments(currentUserId(request)));
        } catch (Exception exception) {
            return apiErrorResponder.authenticatedRequest(exception);
        }
    }

    @PostMapping("/me/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String avatarUrl = currentUserApplicationService.uploadAvatar(currentUserId(request), file);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid upload file"));
        } catch (Exception exception) {
            logger.warn("Avatar upload failed: {}", SensitiveLogSanitizer.exceptionSummary(exception));
            return ResponseEntity.badRequest().body(Map.of("error", "Avatar upload failed"));
        }
    }

    @PutMapping("/me/nickname")
    public ResponseEntity<?> updateNickname(@Valid @RequestBody UpdateNicknameRequest payload, HttpServletRequest request) {
        try {
            String nickname = currentUserApplicationService.updateNickname(currentUserId(request), payload.nickname());
            return ResponseEntity.ok(Map.of("message", "Nickname updated successfully", "nickname", nickname));
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nickname update failed"));
        } catch (Exception exception) {
            return apiErrorResponder.authenticatedRequest(exception);
        }
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<?> updateAvatar(@Valid @RequestBody UpdateAvatarRequest payload, HttpServletRequest request) {
        try {
            String avatarUrl = currentUserApplicationService.updateAvatar(currentUserId(request), payload.avatarUrl());
            return ResponseEntity.ok(Map.of("message", "Avatar updated successfully", "avatarUrl", avatarUrl));
        } catch (Exception exception) {
            return apiErrorResponder.authenticatedRequest(exception);
        }
    }

    private Long currentUserId(HttpServletRequest request) {
        return jwtAuthSupport.resolveCurrentUserId(request);
    }
}
