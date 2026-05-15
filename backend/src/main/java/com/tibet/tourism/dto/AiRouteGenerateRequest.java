package com.tibet.tourism.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AiRouteGenerateRequest {

    @Min(value = 1, message = "行程天数不能少于1天")
    @Max(value = 15, message = "行程天数不能超过15天")
    private Integer days;

    @Size(max = 32, message = "预算参数长度不能超过32个字符")
    private String budget;

    @Size(max = 64, message = "偏好参数长度不能超过64个字符")
    private String preference;

    @Pattern(regexp = "^(zh|bo)?$", message = "语言参数只支持 zh 或 bo")
    private String locale;

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

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
