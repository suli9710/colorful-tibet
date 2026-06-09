package com.tibet.tourism.modules.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.domain.HeritageLike;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.content.infra.HeritageLikeRepository;
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
class HeritageControllerLikeIdempotencyTest {

    @Mock
    private JwtAuthSupport jwtAuthSupport;

    @Mock
    private HeritageItemRepository heritageItemRepository;

    @Mock
    private HeritageLikeRepository heritageLikeRepository;

    @Mock
    private HttpServletRequest request;

    private HeritageController controller;

    @BeforeEach
    void setUp() {
        controller = new HeritageController();
        ReflectionTestUtils.setField(controller, "jwtAuthSupport", jwtAuthSupport);
        ReflectionTestUtils.setField(controller, "heritageItemRepository", heritageItemRepository);
        ReflectionTestUtils.setField(controller, "heritageLikeRepository", heritageLikeRepository);
    }

    @Test
    void duplicateInitialLikeUsesInsertIgnoreAndOnlyIncrementsOnce() {
        User user = user(7L);
        HeritageItem item = item(1L);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(user);
        when(heritageItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(heritageLikeRepository.existsByUserIdAndHeritageItemId(7L, 1L)).thenReturn(false, false);
        when(heritageLikeRepository.insertIgnore(7L, 1L)).thenReturn(1, 0);
        when(heritageLikeRepository.countByHeritageItemId(1L)).thenReturn(1);

        ResponseEntity<?> first = controller.toggleLike(1L, request);
        ResponseEntity<?> duplicate = controller.toggleLike(1L, request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(duplicate).get("liked")).isEqualTo(true);
        assertThat(body(duplicate).get("likeCount")).isEqualTo(1);
        verify(heritageItemRepository, times(1)).incrementLikeCount(1L);
        verify(heritageLikeRepository, times(2)).insertIgnore(7L, 1L);
        verify(heritageLikeRepository, never()).saveAndFlush(any(HeritageLike.class));
    }

    @Test
    void unlikeWithAlreadyDeletedRowDoesNotDecrementAgain() {
        User user = user(7L);
        HeritageItem item = item(1L);
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(user);
        when(heritageItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(heritageLikeRepository.existsByUserIdAndHeritageItemId(7L, 1L)).thenReturn(true);
        when(heritageLikeRepository.deleteByUserIdAndHeritageItemId(7L, 1L)).thenReturn(0L);
        when(heritageLikeRepository.countByHeritageItemId(1L)).thenReturn(0);

        ResponseEntity<?> response = controller.toggleLike(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(response).get("liked")).isEqualTo(false);
        assertThat(body(response).get("likeCount")).isEqualTo(0);
        verify(heritageItemRepository, never()).decrementLikeCount(1L);
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

    private static HeritageItem item(Long id) {
        HeritageItem item = new HeritageItem();
        item.setId(id);
        return item;
    }
}
