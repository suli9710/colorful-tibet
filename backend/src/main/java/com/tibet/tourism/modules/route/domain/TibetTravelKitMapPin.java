package com.tibet.tourism.modules.route.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tibet_travel_kit_map_pins", indexes = {
        @Index(name = "idx_tibet_kit_map_pins_kit_sort", columnList = "travel_kit_id, sort_order")
})
public class TibetTravelKitMapPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_kit_id", nullable = false)
    private TibetTravelKit travelKit;

    @Column(name = "pin_type", length = 48)
    private String type;

    @Column(nullable = false, length = 160)
    private String name;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "altitude_meters")
    private Integer altitudeMeters;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TibetTravelKit getTravelKit() { return travelKit; }
    public void setTravelKit(TibetTravelKit travelKit) { this.travelKit = travelKit; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public Integer getAltitudeMeters() { return altitudeMeters; }
    public void setAltitudeMeters(Integer altitudeMeters) { this.altitudeMeters = altitudeMeters; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
