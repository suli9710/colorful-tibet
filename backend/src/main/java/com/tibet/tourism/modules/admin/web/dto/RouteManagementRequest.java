package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class RouteManagementRequest {

    @Size(max = 200, message = "线路名称长度不能超过200个字符")
    private String title;

    @Size(max = 200, message = "线路名称长度不能超过200个字符")
    private String name;

    @Size(max = 12000, message = "线路内容长度不能超过12000个字符")
    private String content;

    @Size(max = 12000, message = "线路内容长度不能超过12000个字符")
    private String description;

    @Min(value = 1, message = "天数必须在1到15之间")
    @Max(value = 15, message = "天数必须在1到15之间")
    private Integer days;

    @Size(max = 50, message = "预算长度不能超过50个字符")
    private String budget;

    @Size(max = 50, message = "旅行偏好长度不能超过50个字符")
    private String preference;

    private BigDecimal price;

    @Size(max = 20, message = "难度长度不能超过20个字符")
    private String difficulty;

    @Size(max = 100, message = "温度说明长度不能超过100个字符")
    private String temperature;

    @Size(max = 100, message = "地理说明长度不能超过100个字符")
    private String geography;

    /**
     * Returns the effective title, checking the {@code title} field first,
     * then falling back to the {@code name} alias.
     */
    public String effectiveTitle() {
        return title != null ? title : name;
    }

    /**
     * Returns true if the caller supplied a title (via either alias).
     */
    public boolean hasTitleKey() {
        return title != null || name != null;
    }

    /**
     * Returns the effective content, checking the {@code content} field first,
     * then falling back to the {@code description} alias.
     */
    public String effectiveContent() {
        return content != null ? content : description;
    }

    /**
     * Returns true if the caller supplied content (via either alias).
     */
    public boolean hasContentKey() {
        return content != null || description != null;
    }

    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getGeography() {
        return geography;
    }

    public void setGeography(String geography) {
        this.geography = geography;
    }
}
