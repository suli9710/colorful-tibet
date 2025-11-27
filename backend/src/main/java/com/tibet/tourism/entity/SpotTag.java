package com.tibet.tourism.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "spot_tags")
public class SpotTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "spot_id")
    @JsonIgnore  // 防止JSON循环引用
    private ScenicSpot spot;

    private String tag;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScenicSpot getSpot() {
        return spot;
    }

    public void setSpot(ScenicSpot spot) {
        this.spot = spot;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
