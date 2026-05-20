package com.tibet.tourism.modules.route.domain;
import jakarta.persistence.*;

@Entity
@Table(name = "tibet_travel_kit_sustainable_options", indexes = {
        @Index(name = "idx_tibet_kit_sustainable_kit_sort", columnList = "travel_kit_id, sort_order")
})
public class TibetTravelKitSustainableOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_kit_id", nullable = false)
    private TibetTravelKit travelKit;

    @Column(length = 48)
    private String category;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String impact;

    @Column(columnDefinition = "TEXT")
    private String actions;

    @Column(name = "local_benefit", columnDefinition = "TEXT")
    private String localBenefit;

    @Column(name = "carbon_hint", columnDefinition = "TEXT")
    private String carbonHint;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TibetTravelKit getTravelKit() { return travelKit; }
    public void setTravelKit(TibetTravelKit travelKit) { this.travelKit = travelKit; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }

    public String getActions() { return actions; }
    public void setActions(String actions) { this.actions = actions; }

    public String getLocalBenefit() { return localBenefit; }
    public void setLocalBenefit(String localBenefit) { this.localBenefit = localBenefit; }

    public String getCarbonHint() { return carbonHint; }
    public void setCarbonHint(String carbonHint) { this.carbonHint = carbonHint; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
