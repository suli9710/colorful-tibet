package com.tibet.tourism.modules.content.web.dto;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;

public class HeritageInheritorDTO {
    private Long id;
    private String name;
    private String avatarUrl;
    private String level;
    private String bio;
    private String story;
    private String region;
    private Long heritageItemId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getStory() { return story; }
    public void setStory(String story) { this.story = story; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Long getHeritageItemId() { return heritageItemId; }
    public void setHeritageItemId(Long heritageItemId) { this.heritageItemId = heritageItemId; }

    public static HeritageInheritorDTO fromEntity(HeritageInheritor inheritor, String locale) {
        HeritageInheritorDTO dto = new HeritageInheritorDTO();
        dto.setId(inheritor.getId());
        dto.setName("bo".equals(locale) && inheritor.getNameTibetan() != null && !inheritor.getNameTibetan().isEmpty()
                ? inheritor.getNameTibetan() : inheritor.getName());
        dto.setAvatarUrl(inheritor.getAvatarUrl());
        dto.setLevel(inheritor.getLevel());
        dto.setBio("bo".equals(locale) && inheritor.getBioTibetan() != null && !inheritor.getBioTibetan().isEmpty()
                ? inheritor.getBioTibetan() : inheritor.getBio());
        dto.setStory(inheritor.getStory());
        dto.setRegion(inheritor.getRegion());
        if (inheritor.getHeritageItem() != null) {
            dto.setHeritageItemId(inheritor.getHeritageItem().getId());
        }
        return dto;
    }
}
