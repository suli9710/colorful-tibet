package com.tibet.tourism.modules.community.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.common.security.UserSessionVersionService;
import com.tibet.tourism.modules.auth.application.AuthApplicationService;
import com.tibet.tourism.modules.auth.application.LoginResult;
import com.tibet.tourism.modules.auth.web.AuthController;
import com.tibet.tourism.modules.auth.web.dto.LoginRequest;
import com.tibet.tourism.modules.community.application.TravelQAService;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, TravelQAController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(JwtAuthSupport.class)
class OptionalLikeStatusRevocationTest {

    private static final String TOKEN = "jwt-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthApplicationService authApplicationService;

    @MockBean
    private TravelQAService qaService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserSessionVersionService userSessionVersionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TrustedProxyIpResolver trustedProxyIpResolver;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutRevokedTokenMakesPublicQuestionLikeStatusAnonymous() throws Exception {
        AtomicBoolean revoked = new AtomicBoolean(false);
        User user = user();
        TravelQuestion question = question(user);

        when(authApplicationService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenReturn(new LoginResult(TOKEN, "csrf-token", Map.of("id", 7L, "username", "alice")));
        when(jwtUtils.validateJwtToken(TOKEN)).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken(TOKEN)).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userSessionVersionService.resolveCurrentUser(TOKEN)).thenReturn(Optional.of(user));
        when(tokenRevocationService.isRevoked(TOKEN)).thenAnswer(invocation -> revoked.get());
        doAnswer(invocation -> {
            revoked.set(true);
            return null;
        }).when(tokenRevocationService).revoke(TOKEN);
        when(qaService.likeQuestion(42L, 7L)).thenReturn(true);
        when(qaService.getQuestion(42L)).thenReturn(question);
        when(qaService.isLikedByUser(42L, 7L)).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/questions/42/like")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isNoContent());

        clearInvocations(jwtUtils, tokenRevocationService, userRepository, userSessionVersionService, qaService);

        mockMvc.perform(get("/api/community/questions/42/like-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false));

        verify(jwtUtils).validateJwtToken(TOKEN);
        verify(tokenRevocationService).isRevoked(TOKEN);
        verify(userSessionVersionService, never()).resolveCurrentUser(TOKEN);
        verify(jwtUtils, never()).getUserNameFromJwtToken(TOKEN);
        verify(userRepository, never()).findByUsername(any());
        verify(qaService, never()).isLikedByUser(anyLong(), anyLong());
    }

    private static User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        return user;
    }

    private static TravelQuestion question(User user) {
        TravelQuestion question = new TravelQuestion();
        question.setId(42L);
        question.setAuthor(user);
        question.setLikeCount(1);
        return question;
    }
}
