package com.tibet.tourism.modules.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.content.domain.HeritageComment;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.infra.HeritageCommentRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
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
class HeritageControllerCommentSecurityTest {

    @Mock
    private JwtAuthSupport jwtAuthSupport;

    @Mock
    private HeritageCommentRepository heritageCommentRepository;

    @Mock
    private HeritageItemRepository heritageItemRepository;

    @Mock
    private HttpServletRequest request;

    private HeritageController controller;

    @BeforeEach
    void setUp() {
        controller = new HeritageController();
        ReflectionTestUtils.setField(controller, "jwtAuthSupport", jwtAuthSupport);
        ReflectionTestUtils.setField(controller, "heritageCommentRepository", heritageCommentRepository);
        ReflectionTestUtils.setField(controller, "heritageItemRepository", heritageItemRepository);
    }

    @Test
    void deleteCommentRejectsMismatchedHeritageIdWithoutMutatingCounters() {
        User owner = user(7L);
        HeritageItem actualItem = heritageItem(1L, 1);
        HeritageComment comment = comment(100L, owner, actualItem);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(owner);
        when(heritageCommentRepository.findById(100L)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.deleteComment(2L, 100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(actualItem.getCommentCount()).isEqualTo(1);
        verify(heritageCommentRepository, never()).delete(any(HeritageComment.class));
        verify(heritageItemRepository, never()).save(any(HeritageItem.class));
        verify(heritageItemRepository, never()).decrementCommentCount(anyLong());
        verify(heritageItemRepository, never()).findById(anyLong());
    }

    @Test
    void deleteOwnCommentDecrementsActualAssociatedItem() {
        User owner = user(7L);
        HeritageItem actualItem = heritageItem(1L, 3);
        HeritageComment comment = comment(100L, owner, actualItem);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(owner);
        when(heritageCommentRepository.findById(100L)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.deleteComment(1L, 100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(actualItem.getCommentCount()).isEqualTo(3);
        verify(heritageCommentRepository).delete(comment);
        verify(heritageItemRepository).decrementCommentCount(1L);
        verify(heritageItemRepository, never()).save(any(HeritageItem.class));
        verify(heritageItemRepository, never()).findById(anyLong());
    }

    @Test
    void deleteCommentStillRejectsNonOwnerWithoutMutatingCounters() {
        User owner = user(7L);
        User currentUser = user(8L);
        HeritageItem actualItem = heritageItem(1L, 1);
        HeritageComment comment = comment(100L, owner, actualItem);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(currentUser);
        when(heritageCommentRepository.findById(100L)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.deleteComment(1L, 100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(actualItem.getCommentCount()).isEqualTo(1);
        verify(heritageCommentRepository, never()).delete(any(HeritageComment.class));
        verify(heritageItemRepository, never()).save(any(HeritageItem.class));
        verify(heritageItemRepository, never()).decrementCommentCount(anyLong());
    }

    @Test
    void deleteOwnCommentWithNullCountStoresZero() {
        User owner = user(7L);
        HeritageItem actualItem = heritageItem(1L, null);
        HeritageComment comment = comment(100L, owner, actualItem);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(owner);
        when(heritageCommentRepository.findById(100L)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.deleteComment(1L, 100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(actualItem.getCommentCount()).isNull();
        verify(heritageCommentRepository).delete(comment);
        verify(heritageItemRepository).decrementCommentCount(1L);
        verify(heritageItemRepository, never()).save(any(HeritageItem.class));
    }

    @Test
    void deleteOwnCommentWithZeroCountKeepsZero() {
        User owner = user(7L);
        HeritageItem actualItem = heritageItem(1L, 0);
        HeritageComment comment = comment(100L, owner, actualItem);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(owner);
        when(heritageCommentRepository.findById(100L)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.deleteComment(1L, 100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(actualItem.getCommentCount()).isZero();
        verify(heritageCommentRepository).delete(comment);
        verify(heritageItemRepository).decrementCommentCount(1L);
        verify(heritageItemRepository, never()).save(any(HeritageItem.class));
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static HeritageItem heritageItem(Long id, Integer commentCount) {
        HeritageItem item = new HeritageItem();
        item.setId(id);
        item.setCommentCount(commentCount);
        return item;
    }

    private static HeritageComment comment(Long id, User user, HeritageItem item) {
        HeritageComment comment = new HeritageComment();
        comment.setId(id);
        comment.setUser(user);
        comment.setHeritageItem(item);
        comment.setContent("test comment");
        return comment;
    }
}
