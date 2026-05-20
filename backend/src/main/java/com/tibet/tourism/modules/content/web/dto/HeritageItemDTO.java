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
        return dto;
    }
}
