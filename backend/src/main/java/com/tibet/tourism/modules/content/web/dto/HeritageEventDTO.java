package com.tibet.tourism.modules.content.web.dto;
import com.tibet.tourism.modules.content.domain.HeritageEvent;
import java.time.LocalDate;

public class HeritageEventDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalDate endDate;
    private String location;
    private String imageUrl;
    private String contactInfo;
    private Long heritageItemId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public Long getHeritageItemId() { return heritageItemId; }
    public void setHeritageItemId(Long heritageItemId) { this.heritageItemId = heritageItemId; }

    public static HeritageEventDTO fromEntity(HeritageEvent event, String locale) {
        HeritageEventDTO dto = new HeritageEventDTO();
        dto.setId(event.getId());
        dto.setTitle("bo".equals(locale) && event.getTitleTibetan() != null && !event.getTitleTibetan().isEmpty()
                ? event.getTitleTibetan() : event.getTitle());
        dto.setDescription("bo".equals(locale) && event.getDescriptionTibetan() != null && !event.getDescriptionTibetan().isEmpty()
                ? event.getDescriptionTibetan() : event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setEndDate(event.getEndDate());
        dto.setLocation(event.getLocation());
        dto.setImageUrl(event.getImageUrl());
        dto.setContactInfo(event.getContactInfo());
        if (event.getHeritageItem() != null) {
            dto.setHeritageItemId(event.getHeritageItem().getId());
        }
        return dto;
    }
}
