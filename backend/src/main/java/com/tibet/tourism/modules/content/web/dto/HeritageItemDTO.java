package com.tibet.tourism.modules.content.web.dto;
import com.tibet.tourism.modules.content.domain.HeritageItem;

public class HeritageItemDTO {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private String videoUrl;
    private String originStory;
    private String significance;
    private String baikeUrl;
    private String region;
    private String protectionLevel;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getOriginStory() { return originStory; }
    public void setOriginStory(String originStory) { this.originStory = originStory; }
    public String getSignificance() { return significance; }
    public void setSignificance(String significance) { this.significance = significance; }
    public String getBaikeUrl() { return baikeUrl; }
    public void setBaikeUrl(String baikeUrl) { this.baikeUrl = baikeUrl; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getProtectionLevel() { return protectionLevel; }
    public void setProtectionLevel(String protectionLevel) { this.protectionLevel = protectionLevel; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public static HeritageItemDTO fromEntity(HeritageItem item, String locale) {
        if (item == null) throw new IllegalArgumentException("HeritageItem cannot be null");
        HeritageItemDTO dto = new HeritageItemDTO();
        dto.setId(item.getId());
        dto.setName("bo".equals(locale) && item.getNameTibetan() != null && !item.getNameTibetan().isEmpty()
                ? item.getNameTibetan() : item.getName());
        dto.setDescription("bo".equals(locale) && item.getDescriptionTibetan() != null && !item.getDescriptionTibetan().isEmpty()
                ? item.getDescriptionTibetan() : item.getDescription());
        dto.setImageUrl(item.getImageUrl());
        dto.setCategory(item.getCategory());
        dto.setVideoUrl(item.getVideoUrl());
        dto.setOriginStory(item.getOriginStory());
        dto.setSignificance(item.getSignificance());
        dto.setBaikeUrl(item.getBaikeUrl());
        dto.setRegion(item.getRegion());
        dto.setProtectionLevel(item.getProtectionLevel());
        dto.setViewCount(item.getViewCount());
        dto.setLikeCount(item.getLikeCount());
        dto.setCommentCount(item.getCommentCount());
        return dto;
    }
}
