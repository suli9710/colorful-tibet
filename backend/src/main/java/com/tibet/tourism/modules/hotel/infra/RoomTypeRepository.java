package com.tibet.tourism.modules.hotel.infra;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelIdOrderBySortOrderAsc(Long hotelId);
    List<RoomType> findByHotelId(Long hotelId);
    void deleteByHotelId(Long hotelId);

    // Explicit single current read: do not let an ORM join/fetch plan defer this mutex.
    @Query(value = "SELECT * FROM room_types WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<RoomType> findByIdForUpdate(@Param("id") Long id);
    // Scalar projection bypasses the persistence context's previously managed room state.
    @Query(value = "SELECT inventory_units FROM room_types WHERE id = :id FOR UPDATE", nativeQuery = true)
    Integer findInventoryUnitsForUpdate(@Param("id") Long id);
}
