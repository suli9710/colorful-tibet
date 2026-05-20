package com.tibet.tourism.modules.hotel.infra;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Optional<Hotel> findByName(String name);
}
