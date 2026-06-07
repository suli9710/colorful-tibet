package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Size;

public class HeritageItemRequest {

    @Size(max = 200, message = "名称长度不能超过200个字符")
    private String name;

    @Size(max = 200, message = "藏语名称长度不能超过200个字符")
    private String nameTibetan;

    @Size(max = 10000, message = "描述长度不能超过10000个字符")
    private String description;

    @Size(max = 10000, message = "藏语描述长度不能超过10000个字符")
    private String descriptionTibetan;

    @Size(max = 100, message = "类别长度不能超过100个字符")
    private String category;

    @Size(max = 512, message = "图片链接长度不能超过512个字符")
    private String imageUrl;

    @Size(max = 512, message = "视频链接长度不能超过512个字符")
    private String videoUrl;

    @Size(max = 10000, message = "起源故事长度不能超过10000个字符")
    private String originStory;

    @Size(max = 10000, message = "文化价值长度不能超过10000个字符")
    private String significance;

    @Size(max = 512, message = "百科链接长度不能超过512个字符")
    private String baikeUrl;

    @Size(max = 100, message = "地区长度不能超过100个字符")
    private String region;

    @Size(max = 50, message = "保护级别长度不能超过50个字符")
    private String protectionLevel;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameTibetan() { return nameTibetan; }
    public void setNameTibetan(String nameTibetan) { this.nameTibetan = nameTibetan; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDescriptionTibetan() { return descriptionTibetan; }
    public void setDescriptionTibetan(String descriptionTibetan) { this.descriptionTibetan = descriptionTibetan; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

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
}
