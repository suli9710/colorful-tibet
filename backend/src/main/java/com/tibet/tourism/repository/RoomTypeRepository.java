package com.tibet.tourism.repository;

import com.tibet.tourism.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelIdOrderBySortOrderAsc(Long hotelId);
    List<RoomType> findByHotelId(Long hotelId);
    void deleteByHotelId(Long hotelId);
}
