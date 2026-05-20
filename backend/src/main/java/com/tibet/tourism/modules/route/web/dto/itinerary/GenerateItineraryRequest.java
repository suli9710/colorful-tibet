package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class GenerateItineraryRequest {

    @Min(value = 1, message = "行程天数不能少于1天")
    @Max(value = 15, message = "行程天数不能超过15天")
    private Integer days;

    @FutureOrPresent(message = "出发日期不能早于今天")
    private LocalDate startDate;

    @Size(max = 32, message = "预算参数长度不能超过32个字符")
    private String budget;

    @Size(max = 64, message = "偏好参数长度不能超过64个字符")
    private String preference;

    @Pattern(regexp = "^(zh|bo)?$", message = "语言参数只支持 zh 或 bo")
    private String locale;

    @Size(max = 48, message = "版本类型长度不能超过48个字符")
    private String versionType;

    @Min(value = 1, message = "出行人数不能少于1人")
    @Max(value = 20, message = "出行人数不能超过20人")
    private Integer travelers;

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getVersionType() { return versionType; }
    public void setVersionType(String versionType) { this.versionType = versionType; }

    public Integer getTravelers() { return travelers; }
    public void setTravelers(Integer travelers) { this.travelers = travelers; }
}
