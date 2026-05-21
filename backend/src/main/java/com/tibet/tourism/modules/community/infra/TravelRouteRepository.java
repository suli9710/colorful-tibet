package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.TravelRoute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelRouteRepository extends JpaRepository<TravelRoute, Long> {
}
