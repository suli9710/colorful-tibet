package com.tibet.tourism.modules.hotel.infra;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelIdOrderBySortOrderAsc(Long hotelId);
    List<RoomType> findByHotelId(Long hotelId);
    void deleteByHotelId(Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RoomType rt LEFT JOIN FETCH rt.hotel WHERE rt.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") Long id);
}
