package com.tibet.tourism.modules.community.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.domain.CommentLike;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentControllerLikeIdempotencyTest {

    @Mock
    private JwtAuthSupport jwtAuthSupport;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private HttpServletRequest request;

    private CommentController controller;

    @BeforeEach
    void setUp() {
        controller = new CommentController();
        ReflectionTestUtils.setField(controller, "jwtAuthSupport", jwtAuthSupport);
        ReflectionTestUtils.setField(controller, "commentRepository", commentRepository);
        ReflectionTestUtils.setField(controller, "commentLikeRepository", commentLikeRepository);
    }

    @Test
    void duplicateInitialLikeUsesInsertIgnoreAndOnlyIncrementsOnce() {
        User user = user(7L);
        Comment comment = comment(10L);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(user);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByUserIdAndCommentId(7L, 10L)).thenReturn(false, false);
        when(commentLikeRepository.insertIgnore(7L, 10L)).thenReturn(1, 0);
        when(commentLikeRepository.countByCommentId(10L)).thenReturn(1);

        ResponseEntity<?> first = controller.toggleLike(10L, request);
        ResponseEntity<?> duplicate = controller.toggleLike(10L, request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(duplicate).get("liked")).isEqualTo(true);
        assertThat(body(duplicate).get("likeCount")).isEqualTo(1);
        verify(commentRepository, times(1)).incrementLikeCount(10L);
        verify(commentLikeRepository, times(2)).insertIgnore(7L, 10L);
        verify(commentLikeRepository, never()).saveAndFlush(any(CommentLike.class));
    }

    @Test
    void unlikeWithAlreadyDeletedRowDoesNotDecrementAgain() {
        User user = user(7L);
        Comment comment = comment(10L);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(user);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByUserIdAndCommentId(7L, 10L)).thenReturn(true);
        when(commentLikeRepository.deleteByUserIdAndCommentId(7L, 10L)).thenReturn(0L);
        when(commentLikeRepository.countByCommentId(10L)).thenReturn(0);

        ResponseEntity<?> response = controller.toggleLike(10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(response).get("liked")).isEqualTo(false);
        assertThat(body(response).get("likeCount")).isEqualTo(0);
        verify(commentRepository, never()).decrementLikeCount(10L);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Comment comment(Long id) {
        Comment comment = new Comment();
        comment.setId(id);
        return comment;
    }
}
