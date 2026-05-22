package com.tibet.tourism.modules.content.web;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.content.application.HeritageService;
import com.tibet.tourism.modules.content.domain.HeritageComment;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.domain.HeritageLike;
import com.tibet.tourism.modules.content.infra.HeritageCommentRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.content.infra.HeritageLikeRepository;
import com.tibet.tourism.modules.content.web.dto.HeritageCommentDTO;
import com.tibet.tourism.modules.content.web.dto.HeritageEventDTO;
import com.tibet.tourism.modules.content.web.dto.HeritageInheritorDTO;
import com.tibet.tourism.modules.content.web.dto.HeritageItemDTO;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/heritage")
public class HeritageController {

    @Autowired
    private HeritageService heritageService;

    @Autowired
    private HeritageItemRepository heritageItemRepository;

    @Autowired
    private HeritageLikeRepository heritageLikeRepository;

    @Autowired
    private HeritageCommentRepository heritageCommentRepository;

    @Autowired
    private JwtAuthSupport jwtAuthSupport;

    @GetMapping
    public Page<HeritageItemDTO> getAllItems(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            if (category != null && !category.isEmpty()) {
                return heritageService.searchItemsByCategory(category, keyword, pageable)
                        .map(item -> HeritageItemDTO.fromEntity(item, locale));
            }
            return heritageService.searchItems(keyword, pageable)
                    .map(item -> HeritageItemDTO.fromEntity(item, locale));
        }
        if (category != null && !category.isEmpty()) {
            return heritageService.getItemsByCategory(category, pageable)
                    .map(item -> HeritageItemDTO.fromEntity(item, locale));
        }
        return heritageService.getAllItems(pageable)
                .map(item -> HeritageItemDTO.fromEntity(item, locale));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HeritageItemDTO> getItemById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        return heritageService.getItemByIdAndIncrementView(id)
                .map(item -> ResponseEntity.ok(HeritageItemDTO.fromEntity(item, locale)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- 点赞 ----
    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        HeritageItem item = heritageItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Heritage item not found"));

        boolean exists = heritageLikeRepository.existsByUserIdAndHeritageItemId(user.getId(), id);
        if (exists) {
            heritageLikeRepository.deleteByUserIdAndHeritageItemId(user.getId(), id);
            item.setLikeCount(Math.max(0, item.getLikeCount() - 1));
        } else {
            HeritageLike like = new HeritageLike();
            like.setUser(user);
            like.setHeritageItem(item);
            heritageLikeRepository.save(like);
            item.setLikeCount(item.getLikeCount() + 1);
        }
        heritageItemRepository.save(item);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", !exists);
        response.put("likeCount", item.getLikeCount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/like-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkLikeStatus(@PathVariable Long id, HttpServletRequest request) {
        Long userId = jwtAuthSupport.resolveCurrentUserId(request);
        boolean liked = heritageLikeRepository.existsByUserIdAndHeritageItemId(userId, id);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    // ---- 评论 ----
    @GetMapping("/{id}/comments")
    public Page<HeritageCommentDTO> getComments(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return heritageCommentRepository.findByHeritageItemIdOrderByCreatedAtDesc(id, pageable)
                .map(HeritageCommentDTO::fromEntity);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        HeritageItem item = heritageItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Heritage item not found"));

        String content = InputSanitizer.requiredTextBlock(
                payload.get("content") == null ? null : payload.get("content").toString(), 1000, "评论内容");
        Integer rating = null;
        if (payload.containsKey("rating")) {
            rating = Integer.valueOf(payload.get("rating").toString());
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "评分必须在1到5之间"));
            }
        }

        HeritageComment comment = new HeritageComment();
        comment.setUser(user);
        comment.setHeritageItem(item);
        comment.setContent(content);
        comment.setRating(rating);
        if (payload.containsKey("imageUrl")) {
            comment.setImageUrl(payload.get("imageUrl") == null ? null : payload.get("imageUrl").toString());
        }
        heritageCommentRepository.save(comment);

        item.setCommentCount(item.getCommentCount() == null ? 1 : item.getCommentCount() + 1);
        heritageItemRepository.save(item);

        return ResponseEntity.ok(HeritageCommentDTO.fromEntity(comment));
    }

    @DeleteMapping("/{heritageId}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> deleteComment(
            @PathVariable Long heritageId,
            @PathVariable Long commentId,
            HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        HeritageComment comment = heritageCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只能删除自己的评论"));
        }
        HeritageItem item = comment.getHeritageItem();
        if (item == null || item.getId() == null || !item.getId().equals(heritageId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Comment not found"));
        }

        heritageCommentRepository.delete(comment);
        int currentCount = item.getCommentCount() == null ? 0 : item.getCommentCount();
        item.setCommentCount(Math.max(0, currentCount - 1));
        heritageItemRepository.save(item);

        return ResponseEntity.noContent().build();
    }

    // ---- 传承人 ----
    @GetMapping("/{id}/inheritors")
    public ResponseEntity<List<HeritageInheritorDTO>> getInheritors(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        List<HeritageInheritorDTO> dtos = heritageService.getInheritorsByItemId(id)
                .stream()
                .map(i -> HeritageInheritorDTO.fromEntity(i, locale))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // ---- 活动 ----
    @GetMapping("/{id}/events")
    public ResponseEntity<List<HeritageEventDTO>> getEvents(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        List<HeritageEventDTO> dtos = heritageService.getEventsByItemId(id)
                .stream()
                .map(e -> HeritageEventDTO.fromEntity(e, locale))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/events/upcoming")
    public Page<HeritageEventDTO> getUpcomingEvents(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 10) Pageable pageable) {
        return heritageService.getUpcomingEvents(pageable)
                .map(e -> HeritageEventDTO.fromEntity(e, locale));
    }
}
