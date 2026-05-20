package com.tibet.tourism.modules.spot.web.dto;

public class ScenicSpotHeatmapPointDTO {
    private final Long id;
    private final String name;
    private final double longitude;
    private final double latitude;
    private final int visitCount;

    public ScenicSpotHeatmapPointDTO(Long id, String name, double longitude, double latitude, int visitCount) {
        this.id = id;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.visitCount = visitCount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public int getVisitCount() {
        return visitCount;
    }
}
