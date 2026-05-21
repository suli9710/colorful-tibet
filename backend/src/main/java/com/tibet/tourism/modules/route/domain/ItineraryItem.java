package com.tibet.tourism.modules.route.domain;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "itinerary_items", indexes = {
        @Index(name = "idx_itinerary_items_day_sort", columnList = "day_id, sort_order"),
        @Index(name = "idx_itinerary_items_spot", columnList = "scenic_spot_id"),
        @Index(name = "idx_itinerary_items_hotel", columnList = "hotel_id")
})
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private ItineraryDay day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ItemType itemType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 16)
    private String startTime;

    private Integer durationMinutes;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private Integer altitudeMeters;

    @Column(length = 32)
    private String riskLevel;

    @Column(columnDefinition = "TEXT")
    private String alternatives;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private BookingAction bookingAction = BookingAction.NONE;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private BookingStatus bookingStatus = BookingStatus.NOT_BOOKABLE;

    @Column(name = "booking_reference_type", length = 32)
    private String bookingReferenceType;

    @Column(name = "booking_reference_id")
    private Long bookingReferenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenic_spot_id")
    private ScenicSpot scenicSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItineraryDay getDay() { return day; }
    public void setDay(ItineraryDay day) { this.day = day; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public Integer getAltitudeMeters() { return altitudeMeters; }
    public void setAltitudeMeters(Integer altitudeMeters) { this.altitudeMeters = altitudeMeters; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getAlternatives() { return alternatives; }
    public void setAlternatives(String alternatives) { this.alternatives = alternatives; }

    public BookingAction getBookingAction() { return bookingAction; }
    public void setBookingAction(BookingAction bookingAction) { this.bookingAction = bookingAction; }

    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getBookingReferenceType() { return bookingReferenceType; }
    public void setBookingReferenceType(String bookingReferenceType) { this.bookingReferenceType = bookingReferenceType; }

    public Long getBookingReferenceId() { return bookingReferenceId; }
    public void setBookingReferenceId(Long bookingReferenceId) { this.bookingReferenceId = bookingReferenceId; }

    public ScenicSpot getScenicSpot() { return scenicSpot; }
    public void setScenicSpot(ScenicSpot scenicSpot) { this.scenicSpot = scenicSpot; }

    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public enum ItemType {
        SCENIC_SPOT, HOTEL, TRANSPORT, MEAL, EXPERIENCE, NOTE
    }

    public enum BookingAction {
        BOOK_SPOT, BOOK_HOTEL, ADD_TO_TRIP, REPLACE, NONE
    }

    public enum BookingStatus {
        BOOKABLE, BOOKED, NOT_BOOKABLE
    }
}
