package com.tibet.tourism.repository;

import com.tibet.tourism.entity.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelIdOrderBySortOrderAsc(Long hotelId);
    List<RoomType> findByHotelId(Long hotelId);
    void deleteByHotelId(Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RoomType rt LEFT JOIN FETCH rt.hotel WHERE rt.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") Long id);
}
