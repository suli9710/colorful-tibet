package com.tibet.tourism.dto;

import com.tibet.tourism.entity.Comment;

import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String content;
    private Integer rating;
    private String imageUrl;
    private Integer likeCount;
    private String username;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static CommentDTO fromEntity(Comment comment) {
        if (comment == null) throw new IllegalArgumentException("Comment cannot be null");
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setRating(comment.getRating());
        dto.setImageUrl(comment.getImageUrl());
        dto.setLikeCount(comment.getLikeCount());
        dto.setUsername(comment.getUser() != null ? comment.getUser().getUsername() : null);
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}
