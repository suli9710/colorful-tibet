package com.tibet.tourism.modules.community.web.dto;

import jakarta.validation.constraints.NotBlank;

public class ShareRouteRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private Integer days;

    private String budget;

    private String preference;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
}
