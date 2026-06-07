package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ScenicSpotRequest {

    private Boolean autoTranslate;

    @Size(max = 200, message = "景点名称长度不能超过200个字符")
    private String name;

    @Size(max = 5000, message = "景点描述长度不能超过5000个字符")
    private String description;

    @Size(max = 200, message = "藏语名称长度不能超过200个字符")
    private String nameTibetan;

    @Size(max = 5000, message = "藏语描述长度不能超过5000个字符")
    private String descriptionTibetan;

    @Size(max = 512, message = "图片地址长度不能超过512个字符")
    private String imageUrl;

    private BigDecimal ticketPrice;

    @Size(max = 100, message = "海拔长度不能超过100个字符")
    private String altitude;

    @Size(max = 200, message = "位置长度不能超过200个字符")
    private String location;

    @Size(max = 50, message = "类别长度不能超过50个字符")
    private String category;

    @Min(value = 0, message = "每日接待量不能为负数")
    private Integer num;

    @Size(max = 500, message = "开放信息长度不能超过500个字符")
    private String openInfo;

    @Size(max = 200, message = "入园时间长度不能超过200个字符")
    private String entryTime;

    private BigDecimal latitude;

    private BigDecimal longitude;

    // Getters and Setters

    public Boolean getAutoTranslate() {
        return autoTranslate;
    }

    public void setAutoTranslate(Boolean autoTranslate) {
        this.autoTranslate = autoTranslate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNameTibetan() {
        return nameTibetan;
    }

    public void setNameTibetan(String nameTibetan) {
        this.nameTibetan = nameTibetan;
    }

    public String getDescriptionTibetan() {
        return descriptionTibetan;
    }

    public void setDescriptionTibetan(String descriptionTibetan) {
        this.descriptionTibetan = descriptionTibetan;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getAltitude() {
        return altitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getOpenInfo() {
        return openInfo;
    }

    public void setOpenInfo(String openInfo) {
        this.openInfo = openInfo;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
