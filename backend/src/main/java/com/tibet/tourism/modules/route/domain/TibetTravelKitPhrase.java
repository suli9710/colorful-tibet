package com.tibet.tourism.modules.route.domain;
import jakarta.persistence.*;

@Entity
@Table(name = "tibet_travel_kit_phrases", indexes = {
        @Index(name = "idx_tibet_kit_phrases_kit_sort", columnList = "travel_kit_id, sort_order")
})
public class TibetTravelKitPhrase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_kit_id", nullable = false)
    private TibetTravelKit travelKit;

    @Column(length = 48)
    private String category;

    @Column(nullable = false, length = 160)
    private String chinese;

    @Column(nullable = false, length = 220)
    private String tibetan;

    @Column(length = 160)
    private String english;

    @Column(length = 160)
    private String pronunciation;

    @Column(columnDefinition = "TEXT")
    private String usageText;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TibetTravelKit getTravelKit() { return travelKit; }
    public void setTravelKit(TibetTravelKit travelKit) { this.travelKit = travelKit; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getChinese() { return chinese; }
    public void setChinese(String chinese) { this.chinese = chinese; }

    public String getTibetan() { return tibetan; }
    public void setTibetan(String tibetan) { this.tibetan = tibetan; }

    public String getEnglish() { return english; }
    public void setEnglish(String english) { this.english = english; }

    public String getPronunciation() { return pronunciation; }
    public void setPronunciation(String pronunciation) { this.pronunciation = pronunciation; }

    public String getUsageText() { return usageText; }
    public void setUsageText(String usageText) { this.usageText = usageText; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
