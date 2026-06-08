package com.tibet.tourism.modules.content.web;
import com.tibet.tourism.common.api.PageResponse;
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
import com.tibet.tourism.modules.content.web.dto.HeritageCommentRequest;
import com.tibet.tourism.modules.content.web.dto.HeritageEventDTO;
import com.tibet.tourism.modules.content.web.dto.HeritageInheritorDTO;
import com.tibet.tourism.modules.content.web.dto.HeritageItemDTO;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/heritage")
public class HeritageController {

    private static final Set<String> ALLOWED_HERITAGE_ITEM_SORT_FIELDS = Set.of(
            "id", "name", "category", "region", "protectionLevel", "viewCount", "likeCount", "commentCount", "createdAt");
    private static final Set<String> ALLOWED_HERITAGE_COMMENT_SORT_FIELDS = Set.of(
            "id", "createdAt", "rating");
    private static final Set<String> ALLOWED_HERITAGE_INHERITOR_SORT_FIELDS = Set.of(
            "id", "name", "level", "region", "createdAt");
    private static final Set<String> ALLOWED_HERITAGE_EVENT_SORT_FIELDS = Set.of(
            "id", "eventDate", "createdAt");
    private static final Sort DEFAULT_HERITAGE_ITEM_SORT = Sort.by("id");
    private static final Sort DEFAULT_HERITAGE_COMMENT_SORT = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
    private static final Sort DEFAULT_HERITAGE_INHERITOR_SORT = Sort.by("id");
    private static final Sort DEFAULT_HERITAGE_EVENT_SORT = Sort.by(Sort.Direction.ASC, "eventDate").and(Sort.by("id"));

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
    public PageResponse<HeritageItemDTO> getAllItems(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_HERITAGE_ITEM_SORT_FIELDS, DEFAULT_HERITAGE_ITEM_SORT, 20, 100);
        Page<HeritageItemDTO> items;
        if (keyword != null && !keyword.isBlank()) {
            if (category != null && !category.isEmpty()) {
                items = heritageService.searchItemsByCategory(category, keyword, safePageable)
                        .map(item -> HeritageItemDTO.fromEntity(item, locale));
                return PageResponse.from(items);
            }
            items = heritageService.searchItems(keyword, safePageable)
                    .map(item -> HeritageItemDTO.fromEntity(item, locale));
            return PageResponse.from(items);
        }
        if (category != null && !category.isEmpty()) {
            items = heritageService.getItemsByCategory(category, safePageable)
                    .map(item -> HeritageItemDTO.fromEntity(item, locale));
            return PageResponse.from(items);
        }
        items = heritageService.getAllItems(safePageable)
                .map(item -> HeritageItemDTO.fromEntity(item, locale));
        return PageResponse.from(items);
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
        boolean liked;
        if (exists) {
            long deleted = heritageLikeRepository.deleteByUserIdAndHeritageItemId(user.getId(), id);
            if (deleted > 0) {
                heritageItemRepository.decrementLikeCount(id);
            }
            liked = false;
        } else {
            HeritageLike like = new HeritageLike();
            like.setUser(user);
            like.setHeritageItem(item);
            try {
                heritageLikeRepository.saveAndFlush(like);
                heritageItemRepository.incrementLikeCount(id);
            } catch (DataIntegrityViolationException duplicate) {
                // Concurrent duplicate like; the unique row already represents the desired state.
            }
            liked = true;
        }
        int likeCount = heritageLikeRepository.countByHeritageItemId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        response.put("likeCount", likeCount);
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
    public PageResponse<HeritageCommentDTO> getComments(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_HERITAGE_COMMENT_SORT_FIELDS, DEFAULT_HERITAGE_COMMENT_SORT, 20, 100);
        Long currentUserId = jwtAuthSupport.resolveOptionalCurrentUser(request)
                .map(User::getId)
                .orElse(null);
        Page<HeritageCommentDTO> comments = heritageCommentRepository.findByHeritageItemIdOrderByCreatedAtDesc(id, safePageable)
                .map(comment -> HeritageCommentDTO.fromEntity(comment, currentUserId));
        return PageResponse.from(comments);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @Valid @RequestBody HeritageCommentRequest dto,
            HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        HeritageItem item = heritageItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Heritage item not found"));

        String content = InputSanitizer.requiredTextBlock(dto.getContent(), 1000, "评论内容");

        HeritageComment comment = new HeritageComment();
        comment.setUser(user);
        comment.setHeritageItem(item);
        comment.setContent(content);
        comment.setRating(dto.getRating());
        comment.setImageUrl(InputSanitizer.optionalLocalAssetPath(dto.getImageUrl(), "heritage comment image"));
        heritageCommentRepository.save(comment);

        heritageItemRepository.incrementCommentCount(id);

        return ResponseEntity.ok(HeritageCommentDTO.fromEntity(comment, user.getId()));
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
        heritageItemRepository.decrementCommentCount(heritageId);

        return ResponseEntity.noContent().build();
    }

    // ---- 传承人 ----
    @GetMapping("/{id}/inheritors")
    public PageResponse<HeritageInheritorDTO> getInheritors(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_HERITAGE_INHERITOR_SORT_FIELDS, DEFAULT_HERITAGE_INHERITOR_SORT, 20, 50);
        Page<HeritageInheritorDTO> inheritors = heritageService.getInheritorsByItemId(id, safePageable)
                .map(i -> HeritageInheritorDTO.fromEntity(i, locale));
        return PageResponse.from(inheritors);
    }

    // ---- 活动 ----
    @GetMapping("/{id}/events")
    public PageResponse<HeritageEventDTO> getEvents(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_HERITAGE_EVENT_SORT_FIELDS, DEFAULT_HERITAGE_EVENT_SORT, 20, 50);
        Page<HeritageEventDTO> events = heritageService.getEventsByItemId(id, safePageable)
                .map(e -> HeritageEventDTO.fromEntity(e, locale));
        return PageResponse.from(events);
    }

    @GetMapping("/events/upcoming")
    public PageResponse<HeritageEventDTO> getUpcomingEvents(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 10) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_HERITAGE_EVENT_SORT_FIELDS, DEFAULT_HERITAGE_EVENT_SORT, 10, 100);
        Page<HeritageEventDTO> events = heritageService.getUpcomingEvents(safePageable)
                .map(e -> HeritageEventDTO.fromEntity(e, locale));
        return PageResponse.from(events);
    }
}
