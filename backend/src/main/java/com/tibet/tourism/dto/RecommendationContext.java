package com.tibet.tourism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 推荐上下文信息
 * 用于上下文感知推荐，考虑用户当前的环境和需求
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationContext {
    
    /**
     * 季节：SPRING(春), SUMMER(夏), AUTUMN(秋), WINTER(冬)
     * 如果为null，则根据当前月份自动判断
     */
    private String season;
    
    /**
     * 天气：SUNNY(晴), CLOUDY(多云), RAINY(雨), SNOWY(雪)
     */
    private String weather;
    
    /**
     * 当前位置（城市或地区名称）
     */
    private String currentLocation;
    
    /**
     * 当前位置的经纬度（可选，用于距离计算）
     */
    private Double currentLatitude;
    private Double currentLongitude;
    
    /**
     * 时间段：MORNING(早), AFTERNOON(中), EVENING(晚), NIGHT(夜)
     */
    private String timeOfDay;
    
    /**
     * 旅伴类型：ALONE(独自), COUPLE(情侣), FAMILY(家庭), FRIENDS(朋友), GROUP(团队)
     */
    private String companion;
    
    /**
     * 预算（元）
     */
    private Integer budget;
    
    /**
     * 旅行天数
     */
    private Integer travelDays;
    
    /**
     * 偏好活动类型（多个，逗号分隔）：PHOTOGRAPHY(摄影), HIKING(徒步), CULTURE(文化), RELIGION(宗教), NATURE(自然)
     */
    private String preferredActivities;
    
    /**
     * 是否考虑距离：true表示优先推荐距离近的景点
     */
    private Boolean considerDistance = true;
    
    /**
     * 是否考虑预算：true表示过滤超出预算的景点
     */
    private Boolean considerBudget = true;

    // Getters and Setters
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

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getCompanion() {
        return companion;
    }

    public void setCompanion(String companion) {
        this.companion = companion;
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

    public String getPreferredActivities() {
        return preferredActivities;
    }

    public void setPreferredActivities(String preferredActivities) {
        this.preferredActivities = preferredActivities;
    }

    public Boolean getConsiderDistance() {
        return considerDistance;
    }

    public void setConsiderDistance(Boolean considerDistance) {
        this.considerDistance = considerDistance;
    }

    public Boolean getConsiderBudget() {
        return considerBudget;
    }

    public void setConsiderBudget(Boolean considerBudget) {
        this.considerBudget = considerBudget;
    }
}


