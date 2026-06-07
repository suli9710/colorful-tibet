package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class HeritageEventRequest {

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @Size(max = 200, message = "藏语标题长度不能超过200个字符")
    private String titleTibetan;

    @Size(max = 5000, message = "描述长度不能超过5000个字符")
    private String description;

    @Size(max = 5000, message = "藏语描述长度不能超过5000个字符")
    private String descriptionTibetan;

    private LocalDate eventDate;

    private LocalDate endDate;

    @Size(max = 200, message = "地点长度不能超过200个字符")
    private String location;

    @Size(max = 512, message = "活动图片链接长度不能超过512个字符")
    private String imageUrl;

    @Size(max = 200, message = "联系方式长度不能超过200个字符")
    private String contactInfo;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTitleTibetan() { return titleTibetan; }
    public void setTitleTibetan(String titleTibetan) { this.titleTibetan = titleTibetan; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDescriptionTibetan() { return descriptionTibetan; }
    public void setDescriptionTibetan(String descriptionTibetan) { this.descriptionTibetan = descriptionTibetan; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
