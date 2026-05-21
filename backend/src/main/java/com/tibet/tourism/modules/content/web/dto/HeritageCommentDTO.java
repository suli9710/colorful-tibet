package com.tibet.tourism.modules.content.web.dto;
import com.tibet.tourism.modules.content.domain.HeritageComment;
import java.time.LocalDateTime;

public class HeritageCommentDTO {
    private Long id;
    private String content;
    private String imageUrl;
    private Integer rating;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static HeritageCommentDTO fromEntity(HeritageComment comment) {
        HeritageCommentDTO dto = new HeritageCommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setImageUrl(comment.getImageUrl());
        dto.setRating(comment.getRating());
        dto.setCreatedAt(comment.getCreatedAt());
        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId());
            dto.setUsername(comment.getUser().getUsername());
            dto.setNickname(comment.getUser().getNickname());
            dto.setAvatar(comment.getUser().getAvatar());
        }
        return dto;
    }
}
