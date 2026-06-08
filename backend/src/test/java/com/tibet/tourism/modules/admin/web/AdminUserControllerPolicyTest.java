package com.tibet.tourism.modules.admin.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.modules.admin.application.AdminUserService;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerPolicyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private Authentication authentication;

    @Test
    void usersListIncludesConfiguredActionPolicyFields() {
        User user = user(1L, "configured-root", User.Role.ADMIN);
        AdminUserService.UserActionPolicy actionPolicy =
                new AdminUserService.UserActionPolicy(true, false, false);
        AdminUserController controller = new AdminUserController(
                userRepository, loginAttemptService, fileStorageService, adminUserService);

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(loginAttemptService.remainingLockSeconds("configured-root")).thenReturn(0L);
        when(loginAttemptService.failureCount("configured-root")).thenReturn(2);
        when(adminUserService.actionPolicyFor(user, authentication)).thenReturn(actionPolicy);

        var body = controller.getAllUsers(PageRequest.of(0, 10), authentication).getBody();

        assertNotNull(body);
        assertEquals(1, body.content().size());
        AdminUserController.AdminUserSummary summary = body.content().get(0);
        assertTrue(summary.protectedAccount());
        assertFalse(summary.deletable());
        assertFalse(summary.roleMutable());
        assertEquals(2, summary.failureCount());
    }

    private User user(Long id, String username, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname("Root");
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        return user;
    }
}
