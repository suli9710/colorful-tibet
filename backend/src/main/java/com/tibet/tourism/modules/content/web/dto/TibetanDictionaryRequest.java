package com.tibet.tourism.modules.content.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TibetanDictionaryRequest {

    @NotBlank
    @Size(max = 500)
    private String chineseText;

    @NotBlank
    @Size(max = 10000)
    private String tibetanText;

    @Size(max = 20)
    @Pattern(regexp = "(?i)^(|WORD|PHRASE|SENTENCE)$")
    private String type;

    @Min(0)
    @Max(1000000)
    private Integer usageCount;

    public String getChineseText() {
        return chineseText;
    }

    public void setChineseText(String chineseText) {
        this.chineseText = chineseText;
    }

    public String getTibetanText() {
        return tibetanText;
    }

    public void setTibetanText(String tibetanText) {
        this.tibetanText = tibetanText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
}
