package com.tibet.tourism.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tibet_travel_kit_culture_tips", indexes = {
        @Index(name = "idx_tibet_kit_culture_kit_sort", columnList = "travel_kit_id, sort_order")
})
public class TibetTravelKitCultureTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_kit_id", nullable = false)
    private TibetTravelKit travelKit;

    @Column(length = 48)
    private String scene;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(name = "do_tips", columnDefinition = "TEXT")
    private String doTips;

    @Column(name = "avoid_tips", columnDefinition = "TEXT")
    private String avoidTips;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TibetTravelKit getTravelKit() { return travelKit; }
    public void setTravelKit(TibetTravelKit travelKit) { this.travelKit = travelKit; }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getDoTips() { return doTips; }
    public void setDoTips(String doTips) { this.doTips = doTips; }

    public String getAvoidTips() { return avoidTips; }
    public void setAvoidTips(String avoidTips) { this.avoidTips = avoidTips; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
