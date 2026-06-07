package com.tibet.tourism.modules.community.web;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.domain.CommentLike;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.web.dto.AddCommentRequest;
import com.tibet.tourism.modules.community.web.dto.CommentDTO;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ScenicSpotRepository spotRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JwtAuthSupport jwtAuthSupport;

    @GetMapping("/spot/{spotId}")
    public Page<CommentDTO> getCommentsBySpot(
            @PathVariable long spotId,
            @PageableDefault(size = 20) Pageable pageable) {
        return commentRepository.findBySpotIdOrderByCreatedAtDesc(spotId, pageable)
                .map(CommentDTO::fromEntity);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addComment(@Valid @RequestBody AddCommentRequest dto, HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        String content = InputSanitizer.requiredTextBlock(dto.getContent(), 1000, "评论内容");
        String imageUrl = InputSanitizer.optionalLocalAssetPath(dto.getImageUrl(), "评论图片");

        ScenicSpot spot = spotRepository.findById(dto.getSpotId())
                .orElseThrow(() -> new ResourceNotFoundException("Spot not found"));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setSpot(spot);
        comment.setContent(content);
        comment.setRating(dto.getRating());
        comment.setImageUrl(imageUrl);

        Comment saved = commentRepository.save(comment);

        return ResponseEntity.ok(CommentDTO.fromEntity(saved));
    }
    
    @PostMapping("/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> toggleLike(@PathVariable long commentId, HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        long userId = user.getId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        
        boolean exists = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
        
        if (exists) {
            // 取消点赞
            commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
            comment.setLikeCount(comment.getLikeCount() - 1);
        } else {
            // 添加点赞
            CommentLike like = new CommentLike();
            like.setUser(user);
            like.setComment(comment);
            commentLikeRepository.save(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
        }
        
        commentRepository.save(comment);
        
        Map<String, Object> response = new HashMap<>();
        response.put("liked", !exists);
        response.put("likeCount", comment.getLikeCount());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{commentId}/liked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkLiked(@PathVariable long commentId, HttpServletRequest request) {
        long userId = jwtAuthSupport.resolveCurrentUserId(request);
        boolean liked = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> deleteComment(@PathVariable long commentId, HttpServletRequest request) {
        User user = jwtAuthSupport.resolveCurrentUser(request);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只能删除自己的评论"));
        }

        commentLikeRepository.deleteByCommentId(commentId);
        commentRepository.delete(comment);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadCommentImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String imageUrl = fileStorageService.storeCommentImage(file);
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("message", "上传失败，请稍后重试");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
