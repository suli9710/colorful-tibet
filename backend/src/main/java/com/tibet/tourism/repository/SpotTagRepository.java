package com.tibet.tourism.repository;

import com.tibet.tourism.entity.SpotTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpotTagRepository extends JpaRepository<SpotTag, Long> {
    List<SpotTag> findBySpotId(Long spotId);
    List<SpotTag> findBySpotIdIn(Collection<Long> spotIds);
    void deleteBySpotId(Long spotId);
}
