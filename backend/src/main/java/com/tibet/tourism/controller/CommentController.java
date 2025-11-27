package com.tibet.tourism.controller;

import com.tibet.tourism.entity.Comment;
import com.tibet.tourism.entity.CommentLike;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.CommentLikeRepository;
import com.tibet.tourism.repository.CommentRepository;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScenicSpotRepository spotRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/spot/{spotId}")
    public List<Comment> getCommentsBySpot(@PathVariable long spotId) {
        return commentRepository.findBySpotIdOrderByCreatedAtDesc(spotId);
    }

    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody Map<String, Object> payload) {
        long userId = Long.parseLong(payload.get("userId").toString());
        long spotId = Long.parseLong(payload.get("spotId").toString());
        String content = (String) payload.get("content");
        Integer rating = Integer.valueOf(payload.get("rating").toString());
        String imageUrl = payload.get("imageUrl") != null ? payload.get("imageUrl").toString() : null;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ScenicSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found"));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setSpot(spot);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setImageUrl(imageUrl);

        commentRepository.save(comment);

        return ResponseEntity.ok(comment);
    }
    
    @PostMapping("/{commentId}/like")
    @Transactional
    public ResponseEntity<?> toggleLike(@PathVariable long commentId, @RequestBody Map<String, Object> payload) {
        long userId = Long.parseLong(payload.get("userId").toString());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
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
    public ResponseEntity<?> checkLiked(@PathVariable long commentId, @RequestParam long userId) {
        boolean liked = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-image")
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
