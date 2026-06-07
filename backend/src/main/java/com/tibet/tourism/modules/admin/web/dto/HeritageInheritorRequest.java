package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Size;

public class HeritageInheritorRequest {

    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name;

    @Size(max = 100, message = "藏语姓名长度不能超过100个字符")
    private String nameTibetan;

    @Size(max = 512, message = "头像链接长度不能超过512个字符")
    private String avatarUrl;

    @Size(max = 50, message = "级别长度不能超过50个字符")
    private String level;

    @Size(max = 5000, message = "简介长度不能超过5000个字符")
    private String bio;

    @Size(max = 5000, message = "藏语简介长度不能超过5000个字符")
    private String bioTibetan;

    @Size(max = 10000, message = "传承故事长度不能超过10000个字符")
    private String story;

    @Size(max = 100, message = "所在地区长度不能超过100个字符")
    private String region;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameTibetan() { return nameTibetan; }
    public void setNameTibetan(String nameTibetan) { this.nameTibetan = nameTibetan; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getBioTibetan() { return bioTibetan; }
    public void setBioTibetan(String bioTibetan) { this.bioTibetan = bioTibetan; }

    public String getStory() { return story; }
    public void setStory(String story) { this.story = story; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
