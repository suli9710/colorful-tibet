package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class HotelRequest {

    @Size(max = 200, message = "酒店名称长度不能超过200个字符")
    private String name;

    @Size(max = 200, message = "位置长度不能超过200个字符")
    private String location;

    @Size(max = 32, message = "电话长度不能超过32个字符")
    private String phone;

    @Size(max = 100, message = "价格区间长度不能超过100个字符")
    private String priceRange;

    @Size(max = 512, message = "图片地址长度不能超过512个字符")
    private String imageUrl;

    @Size(max = 500, message = "设施长度不能超过500个字符")
    private String facilities;

    private BigDecimal rating;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFacilities() {
        return facilities;
    }

    public void setFacilities(String facilities) {
        this.facilities = facilities;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }
}
