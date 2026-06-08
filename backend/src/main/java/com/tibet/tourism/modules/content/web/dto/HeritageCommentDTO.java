package com.tibet.tourism.modules.content.web.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tibet.tourism.modules.content.domain.HeritageComment;
import java.time.LocalDateTime;
import org.springframework.util.StringUtils;

public class HeritageCommentDTO {
    private Long id;
    private String content;
    private String imageUrl;
    private Integer rating;
    private boolean owner;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nickname;
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    public boolean isOwner() { return owner; }
    public void setOwner(boolean owner) { this.owner = owner; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static HeritageCommentDTO fromEntity(HeritageComment comment) {
        return fromEntity(comment, null);
    }

    public static HeritageCommentDTO fromEntity(HeritageComment comment, Long currentUserId) {
        HeritageCommentDTO dto = new HeritageCommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setImageUrl(comment.getImageUrl());
        dto.setRating(comment.getRating());
        dto.setCreatedAt(comment.getCreatedAt());
        if (comment.getUser() != null) {
            boolean owner = currentUserId != null && currentUserId.equals(comment.getUser().getId());
            dto.setOwner(owner);
            if (owner) {
                dto.setNickname(publicNickname(comment.getUser().getUsername(), comment.getUser().getNickname()));
                dto.setAvatar(comment.getUser().getAvatar());
            }
        }
        return dto;
    }

    private static String publicNickname(String username, String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return null;
        }
        String normalizedNickname = nickname.trim();
        if (StringUtils.hasText(username) && normalizedNickname.equalsIgnoreCase(username.trim())) {
            return null;
        }
        return normalizedNickname;
    }
}
