package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class CommunityRouteUpdateRequest {

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @Size(max = 12000, message = "内容长度不能超过12000个字符")
    private String content;

    @Min(value = 1, message = "天数必须在1到15之间")
    @Max(value = 15, message = "天数必须在1到15之间")
    private Integer days;

    @Size(max = 50, message = "预算长度不能超过50个字符")
    private String budget;

    @Size(max = 50, message = "旅行偏好长度不能超过50个字符")
    private String preference;

    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }
}
