package com.tibet.tourism.dto;

import java.util.List;

/**
 * 用户偏好问卷DTO
 * 用于新用户冷启动
 */
public class UserPreferenceDTO {
    private List<String> preferredTags; // 偏好标签（如：摄影、徒步、文化等）
    private String preferredCategory; // 偏好类别（NATURAL, CULTURAL, RELIGIOUS, HISTORICAL）
    private String companionType; // 旅伴类型（ALONE, COUPLE, FAMILY, FRIENDS, GROUP）
    private Double latitude; // 当前位置纬度
    private Double longitude; // 当前位置经度
    private Integer budget; // 预算
    private Integer travelDays; // 旅行天数
    private String season; // 旅行季节
    private String weather; // 天气偏好

    // Getters and Setters
    public List<String> getPreferredTags() {
        return preferredTags;
    }

    public void setPreferredTags(List<String> preferredTags) {
        this.preferredTags = preferredTags;
    }

    public String getPreferredCategory() {
        return preferredCategory;
    }

    public void setPreferredCategory(String preferredCategory) {
        this.preferredCategory = preferredCategory;
    }

    public String getCompanionType() {
        return companionType;
    }

    public void setCompanionType(String companionType) {
        this.companionType = companionType;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }

    public Integer getTravelDays() {
        return travelDays;
    }

    public void setTravelDays(Integer travelDays) {
        this.travelDays = travelDays;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }
}




