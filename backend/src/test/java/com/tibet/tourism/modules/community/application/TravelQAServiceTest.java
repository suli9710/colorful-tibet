package com.tibet.tourism.modules.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.community.domain.QuestionLike;
import com.tibet.tourism.modules.community.infra.QuestionLikeRepository;
import com.tibet.tourism.modules.community.infra.TravelAnswerRepository;
import com.tibet.tourism.modules.community.infra.TravelQuestionRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TravelQAServiceTest {

    @Mock
    private TravelQuestionRepository questionRepository;

    @Mock
    private TravelAnswerRepository answerRepository;

    @Mock
    private QuestionLikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    private TravelQAService service;

    @BeforeEach
    void setUp() {
        service = new TravelQAService();
        ReflectionTestUtils.setField(service, "questionRepository", questionRepository);
        ReflectionTestUtils.setField(service, "answerRepository", answerRepository);
        ReflectionTestUtils.setField(service, "likeRepository", likeRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
    }

    @Test
    void getAnswersDefaultsUnpagedRequestAndUsesPagedRepository() {
        TravelQuestion question = question();
        when(questionRepository.findById(200L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestion(eq(question), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            return new PageImpl<>(List.of(answer(question)), pageable, 1);
        });

        Page<TravelAnswer> result = service.getAnswers(200L, Pageable.unpaged());

        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(answerRepository).findByQuestion(eq(question), pageableCaptor.capture());
        Pageable safePageable = pageableCaptor.getValue();
        assertThat(safePageable.isPaged()).isTrue();
        assertThat(safePageable.getSort()).containsExactly(
                Sort.Order.desc("isAccepted"),
                Sort.Order.desc("likeCount"),
                Sort.Order.asc("createdAt"),
                Sort.Order.asc("id"));
        verify(answerRepository, never())
                .findByQuestionOrderByIsAcceptedDescLikeCountDescCreatedAtAsc(any(TravelQuestion.class));
    }

    @Test
    void likeQuestionReturnsUpdatedCountWithoutIncrementingViews() {
        User user = user();
        TravelQuestion question = question();
        when(questionRepository.findById(200L)).thenReturn(Optional.of(question));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByQuestionAndUser(question, user)).thenReturn(false);
        when(likeRepository.saveAndFlush(any(QuestionLike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findLikeCountById(200L)).thenReturn(Optional.of(4));

        TravelQAService.LikeResult result = service.likeQuestion(200L, 7L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(4);
        verify(questionRepository).incrementLikeCount(200L);
        verify(questionRepository, never()).incrementViewCount(200L);
    }

    @Test
    void duplicateQuestionLikeIsIdempotentAndDoesNotIncrementAgain() {
        User user = user();
        TravelQuestion question = question();
        when(questionRepository.findById(200L)).thenReturn(Optional.of(question));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(likeRepository.existsByQuestionAndUser(question, user)).thenReturn(false);
        when(likeRepository.saveAndFlush(any(QuestionLike.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate question like"));
        when(questionRepository.findLikeCountById(200L)).thenReturn(Optional.of(3));

        TravelQAService.LikeResult result = service.likeQuestion(200L, 7L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(3);
        verify(questionRepository, never()).incrementLikeCount(200L);
        verify(questionRepository, never()).incrementViewCount(200L);
    }

    private static TravelAnswer answer(TravelQuestion question) {
        TravelAnswer answer = new TravelAnswer();
        answer.setId(400L);
        answer.setQuestion(question);
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
