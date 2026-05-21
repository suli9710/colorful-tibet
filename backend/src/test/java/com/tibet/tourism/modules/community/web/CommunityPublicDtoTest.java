package com.tibet.tourism.modules.community.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.modules.community.application.SharedRouteService;
import com.tibet.tourism.modules.community.application.TravelQAService;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {SharedRouteController.class, TravelQAController.class})
@AutoConfigureMockMvc(addFilters = false)
class CommunityPublicDtoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SharedRouteService routeService;

    @MockBean
    private TravelQAService qaService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthSupport jwtAuthSupport;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TrustedProxyIpResolver trustedProxyIpResolver;

    @Test
    void sharedRouteListReturnsPublicAuthorOnly() throws Exception {
        when(routeService.getRoutes(
                nullable(Integer.class),
                nullable(String.class),
                nullable(String.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sharedRoute())));

        mockMvc.perform(get("/api/routes/shared"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.id").value(7))
                .andExpect(jsonPath("$.content[0].author.username").doesNotExist())
                .andExpect(jsonPath("$.content[0].author.phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].author.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.content[0].author.allowedLoginFingerprintHash").doesNotExist());
    }

    @Test
    void sharedRouteDetailReturnsPublicAuthorOnly() throws Exception {
        SharedRoute route = sharedRoute();
        when(routeService.getRoute(100L)).thenReturn(route);

        mockMvc.perform(get("/api/routes/shared/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.id").value(7))
                .andExpect(jsonPath("$.author.nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.author.avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.author.username").doesNotExist())
                .andExpect(jsonPath("$.author.phone").doesNotExist())
                .andExpect(jsonPath("$.author.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.author.allowedLoginFingerprintHash").doesNotExist())
                .andExpect(jsonPath("$.author.password").doesNotExist());
    }

    @Test
    void sharedRouteCommentsDoNotExposeNestedUserOrRouteEntities() throws Exception {
        RouteComment comment = new RouteComment();
        comment.setId(300L);
        comment.setRoute(sharedRoute());
        comment.setUser(publicUser());
        comment.setContent("Nice route");
        comment.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        when(routeService.getComments(100L)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/routes/shared/100/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.id").value(7))
                .andExpect(jsonPath("$[0].route").doesNotExist())
                .andExpect(jsonPath("$[0].user.username").doesNotExist())
                .andExpect(jsonPath("$[0].user.phone").doesNotExist())
                .andExpect(jsonPath("$[0].user.ipAddress").doesNotExist())
                .andExpect(jsonPath("$[0].user.allowedLoginFingerprintHash").doesNotExist());
    }

    @Test
    void questionListReturnsPublicAuthorOnly() throws Exception {
        when(qaService.getQuestions(
                nullable(String.class),
                any(String.class),
                nullable(String.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(question())));

        mockMvc.perform(get("/api/community/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.id").value(7))
                .andExpect(jsonPath("$.content[0].isResolved").value(false))
                .andExpect(jsonPath("$.content[0].author.username").doesNotExist())
                .andExpect(jsonPath("$.content[0].author.phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].author.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.content[0].author.allowedLoginFingerprintHash").doesNotExist());
    }

    @Test
    void questionDetailReturnsPublicAuthorOnly() throws Exception {
        TravelQuestion question = question();
        when(qaService.getQuestion(200L)).thenReturn(question);

        mockMvc.perform(get("/api/community/questions/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.id").value(7))
                .andExpect(jsonPath("$.author.nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.isResolved").value(false))
                .andExpect(jsonPath("$.author.username").doesNotExist())
                .andExpect(jsonPath("$.author.phone").doesNotExist())
                .andExpect(jsonPath("$.author.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.author.allowedLoginFingerprintHash").doesNotExist());
    }

    @Test
    void answersDoNotExposeNestedQuestionOrSensitiveUserFields() throws Exception {
        TravelAnswer answer = new TravelAnswer();
        answer.setId(400L);
        answer.setQuestion(question());
        answer.setUser(publicUser());
        answer.setContent("Bring warm clothes.");
        answer.setLikeCount(5);
        answer.setIsAccepted(true);
        answer.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        when(qaService.getAnswers(200L)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/community/questions/200/answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.id").value(7))
                .andExpect(jsonPath("$[0].isAccepted").value(true))
                .andExpect(jsonPath("$[0].question").doesNotExist())
                .andExpect(jsonPath("$[0].user.username").doesNotExist())
                .andExpect(jsonPath("$[0].user.phone").doesNotExist())
                .andExpect(jsonPath("$[0].user.ipAddress").doesNotExist())
                .andExpect(jsonPath("$[0].user.allowedLoginFingerprintHash").doesNotExist());
    }

    private static User publicUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("login-name");
        user.setPassword("hashed-password");
        user.setNickname("Public Nickname");
        user.setAvatar("/avatars/u7.png");
        user.setPhone("13800138000");
        user.setIpAddress("203.0.113.99");
        user.setAllowedLoginFingerprintHash("fingerprint-hash");
        return user;
    }

    private static SharedRoute sharedRoute() {
        SharedRoute route = new SharedRoute();
        route.setId(100L);
        route.setAuthor(publicUser());
        route.setTitle("Lhasa route");
        route.setContent("Route content");
        route.setDays(3);
        route.setBudget("budget");
        route.setPreference("culture");
        route.setViewCount(10);
        route.setLikeCount(2);
        route.setCommentCount(1);
        route.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        route.setUpdatedAt(LocalDateTime.parse("2026-01-03T03:04:05"));
        return route;
    }

    private static TravelQuestion question() {
        TravelQuestion question = new TravelQuestion();
        question.setId(200L);
        question.setAuthor(publicUser());
        question.setTitle("Altitude question");
        question.setContent("How to prepare?");
        question.setTags("altitude");
        question.setViewCount(20);
        question.setAnswerCount(1);
        question.setLikeCount(3);
        question.setIsResolved(false);
        question.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        question.setUpdatedAt(LocalDateTime.parse("2026-01-03T03:04:05"));
        return question;
    }
}
