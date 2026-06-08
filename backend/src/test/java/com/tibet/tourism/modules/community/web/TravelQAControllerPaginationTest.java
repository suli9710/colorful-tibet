package com.tibet.tourism.modules.community.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.community.application.TravelQAService;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.community.web.dto.TravelAnswerResponse;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TravelQAControllerPaginationTest {

    @Mock
    private TravelQAService qaService;

    @Mock
    private JwtAuthSupport jwtAuthSupport;

    private TravelQAController controller;

    @BeforeEach
    void setUp() {
        controller = new TravelQAController();
        ReflectionTestUtils.setField(controller, "qaService", qaService);
        ReflectionTestUtils.setField(controller, "jwtAuthSupport", jwtAuthSupport);
    }

    @Test
    void answersClampPageSizeAndReturnPaginationHeadersWithArrayBody() {
        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.empty());
        when(qaService.getAnswers(eq(200L), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            return new PageImpl<>(List.of(answer()), pageable, 51);
        });

        ResponseEntity<?> response = controller.getAnswers(
                200L,
                PageRequest.of(1, 999, Sort.by("content")),
                new MockHttpServletRequest());

        assertThat(response.getHeaders().getFirst("X-Page")).isEqualTo("1");
        assertThat(response.getHeaders().getFirst("X-Size")).isEqualTo("50");
        assertThat(response.getHeaders().getFirst("X-Total-Elements")).isEqualTo("51");
        assertThat(response.getHeaders().getFirst("X-Total-Pages")).isEqualTo("2");
        assertThat(response.getBody()).isInstanceOf(List.class);

        List<?> body = (List<?>) response.getBody();
        assertThat(body).hasSize(1);
        assertThat(((TravelAnswerResponse) body.get(0)).id()).isEqualTo(400L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(qaService).getAnswers(eq(200L), pageableCaptor.capture());
        Pageable safePageable = pageableCaptor.getValue();
        assertThat(safePageable.getPageNumber()).isEqualTo(1);
        assertThat(safePageable.getPageSize()).isEqualTo(50);
        assertThat(safePageable.getSort()).containsExactly(
                Sort.Order.desc("isAccepted"),
                Sort.Order.desc("likeCount"),
                Sort.Order.asc("createdAt"),
                Sort.Order.asc("id"));
    }

    @Test
    void likeQuestionReturnsCountWithoutLoadingDetail() {
        when(jwtAuthSupport.resolveCurrentUserId(any())).thenReturn(7L);
        when(qaService.likeQuestion(200L, 7L))
                .thenReturn(new TravelQAService.LikeResult(true, 4));

        ResponseEntity<?> response = controller.likeQuestion(200L, new MockHttpServletRequest());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("liked")).isEqualTo(true);
        assertThat(body.get("likeCount")).isEqualTo(4);
        verify(qaService, never()).getQuestion(any());
    }

    @Test
    void unlikeQuestionReturnsCountWithoutLoadingDetail() {
        when(jwtAuthSupport.resolveCurrentUserId(any())).thenReturn(7L);
        when(qaService.unlikeQuestion(200L, 7L))
                .thenReturn(new TravelQAService.LikeResult(false, 3));

        ResponseEntity<?> response = controller.unlikeQuestion(200L, new MockHttpServletRequest());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("liked")).isEqualTo(false);
        assertThat(body.get("likeCount")).isEqualTo(3);
        verify(qaService, never()).getQuestion(any());
    }

    private static TravelAnswer answer() {
        TravelAnswer answer = new TravelAnswer();
        answer.setId(400L);
        answer.setQuestion(question());
        answer.setUser(user());
        answer.setContent("Bring warm clothes.");
        answer.setLikeCount(5);
        answer.setIsAccepted(true);
        answer.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        return answer;
    }

    private static TravelQuestion question() {
        TravelQuestion question = new TravelQuestion();
        question.setId(200L);
        question.setAuthor(user());
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

    private static User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("login-name");
        user.setNickname("Public Nickname");
        user.setAvatar("/avatars/u7.png");
        return user;
    }
}
