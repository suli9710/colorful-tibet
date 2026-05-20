package com.tibet.tourism.modules.spot.infra;
import com.tibet.tourism.modules.spot.domain.SpotTag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotTagRepository extends JpaRepository<SpotTag, Long> {
    List<SpotTag> findBySpotId(Long spotId);
    List<SpotTag> findBySpotIdIn(Collection<Long> spotIds);
    void deleteBySpotId(Long spotId);
}
