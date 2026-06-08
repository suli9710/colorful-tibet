package com.tibet.tourism.modules.user.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.api.ApiErrorResponder;
import com.tibet.tourism.common.error.ApiExceptionHandler;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.user.application.CurrentUserApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class CurrentUserControllerContractTest {

    @Mock
    private JwtAuthSupport jwtAuthSupport;

    @Mock
    private CurrentUserApplicationService currentUserApplicationService;

    @Mock
    private ApiErrorResponder apiErrorResponder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CurrentUserController controller = new CurrentUserController(
                jwtAuthSupport, currentUserApplicationService, apiErrorResponder);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void updateNicknameUsesValidatedDtoAndKeepsServiceSanitizerBoundary() throws Exception {
        when(jwtAuthSupport.resolveCurrentUserId(any(HttpServletRequest.class))).thenReturn(7L);
        when(currentUserApplicationService.updateNickname(7L, "  Snow Road  "))
                .thenReturn("Snow Road");

        mockMvc.perform(put("/api/auth/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "  Snow Road  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nickname updated successfully"))
                .andExpect(jsonPath("$.nickname").value("Snow Road"));

        verify(currentUserApplicationService).updateNickname(7L, "  Snow Road  ");
    }

    @Test
    void invalidNicknameReturnsStableBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(put("/api/auth/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(content().string(not(containsString("nickname"))));

        verify(currentUserApplicationService, never()).updateNickname(anyLong(), any());
    }

    @Test
    void nicknameConflictReturnsStableErrorWithoutDatabaseDetails() throws Exception {
        when(jwtAuthSupport.resolveCurrentUserId(any(HttpServletRequest.class))).thenReturn(7L);
        when(currentUserApplicationService.updateNickname(7L, "Taken"))
                .thenThrow(new DataIntegrityViolationException("duplicate key nickname=Taken"));

        mockMvc.perform(put("/api/auth/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "Taken"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nickname update failed"))
                .andExpect(content().string(not(containsString("duplicate key"))));
    }

    @Test
    void updateAvatarUsesValidatedDtoAndKeepsServiceSanitizerBoundary() throws Exception {
        when(jwtAuthSupport.resolveCurrentUserId(any(HttpServletRequest.class))).thenReturn(7L);
        when(currentUserApplicationService.updateAvatar(7L, "  /uploads/avatars/user.png  "))
                .thenReturn("/uploads/avatars/user.png");

        mockMvc.perform(put("/api/auth/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarUrl": "  /uploads/avatars/user.png  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Avatar updated successfully"))
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatars/user.png"));

        verify(currentUserApplicationService).updateAvatar(7L, "  /uploads/avatars/user.png  ");
    }

    @Test
    void updateAvatarAcceptsLegacyLocalAvatarPath() throws Exception {
        when(jwtAuthSupport.resolveCurrentUserId(any(HttpServletRequest.class))).thenReturn(7L);
        when(currentUserApplicationService.updateAvatar(7L, "/avatars/u7.png"))
                .thenReturn("/avatars/u7.png");

        mockMvc.perform(put("/api/auth/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarUrl": "/avatars/u7.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/avatars/u7.png"));

        verify(currentUserApplicationService).updateAvatar(7L, "/avatars/u7.png");
    }

    @Test
    void unsafeAvatarUrlsReturnStableBadRequestWithoutCallingService() throws Exception {
        expectInvalidAvatarUrl("https://cdn.example.com/avatar.png", "cdn.example.com");
        expectInvalidAvatarUrl("https://cdn.example.com/avatar.png?utm_source=profile&signature=secret", "signature=secret");
        expectInvalidAvatarUrl("javascript:alert(1)", "javascript");
        expectInvalidAvatarUrl("data:image/svg+xml,<svg></svg>", "data:image");

        verify(currentUserApplicationService, never()).updateAvatar(anyLong(), any());
    }

    @Test
    void missingAvatarUrlReturnsStableBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(put("/api/auth/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));

        verify(currentUserApplicationService, never()).updateAvatar(anyLong(), any());
    }

    @Test
    void avatarServiceFailureUsesGenericResponderWithoutRawExceptionMessage() throws Exception {
        RuntimeException rawFailure = new IllegalStateException("s3://private-bucket/avatar-token-secret failed");

        when(jwtAuthSupport.resolveCurrentUserId(any(HttpServletRequest.class))).thenReturn(7L);
        when(currentUserApplicationService.updateAvatar(7L, "/uploads/avatars/user.png"))
                .thenThrow(rawFailure);
        ResponseEntity<?> genericFailure = ResponseEntity.badRequest()
                .body(Map.of("error", "Request processing failed"));
        doReturn(genericFailure).when(apiErrorResponder).authenticatedRequest(rawFailure);

        mockMvc.perform(put("/api/auth/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarUrl": "/uploads/avatars/user.png"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request processing failed"))
                .andExpect(content().string(not(containsString("private-bucket"))))
                .andExpect(content().string(not(containsString("avatar-token-secret"))));

        verify(apiErrorResponder).authenticatedRequest(rawFailure);
    }

    @Test
    void currentUserEndpointsRequireAuthenticatedPrincipal() {
        PreAuthorize preAuthorize = CurrentUserController.class.getAnnotation(PreAuthorize.class);

        org.assertj.core.api.Assertions.assertThat(preAuthorize).isNotNull();
        org.assertj.core.api.Assertions.assertThat(preAuthorize.value()).isEqualTo("isAuthenticated()");
    }

    private void expectInvalidAvatarUrl(String avatarUrl, String leakedValue) throws Exception {
        mockMvc.perform(put("/api/auth/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarUrl": "%s"
                                }
                                """.formatted(avatarUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(content().string(not(containsString(leakedValue))))
                .andExpect(content().string(not(containsString("avatarUrl"))));
    }
}
