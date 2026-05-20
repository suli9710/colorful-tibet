package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateItineraryVersionRequest {

    @Size(max = 48, message = "版本类型长度不能超过48个字符")
    @Pattern(regexp = "^(cheaper|relaxed|hidden|family|default)?$", message = "版本类型不支持")
    private String versionType;

    public String getVersionType() {
        return versionType;
    }

    public void setVersionType(String versionType) {
        this.versionType = versionType;
    }
}
