package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.web.dto.HotelRequest;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.tibet.tourism.common.validation.RequestParseUtils.safeImageUrl;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHotelController {

    public record RoomTypeRequest(
            @NotBlank(message = "房型名称不能为空") @Size(max = 80, message = "房型名称长度不能超过80个字符") String name,
            @DecimalMin(value = "0.00", message = "价格不能为负数") BigDecimal price,
            @Min(value = 1, message = "入住人数至少为1") @Max(value = 20, message = "入住人数过大") Integer capacity,
            @Size(max = 512, message = "图片地址长度不能超过512个字符") String imageUrl,
            @Size(max = 500, message = "设施描述长度不能超过500个字符") String amenities,
            @Min(value = 0, message = "排序值不能为负数") @Max(value = 10000, message = "排序值过大") Integer sortOrder
    ) {}

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    public AdminHotelController(HotelRepository hotelRepository, RoomTypeRepository roomTypeRepository) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @GetMapping("/hotels")
    public ResponseEntity<Page<Hotel>> getAllHotels(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(hotelRepository.findAll(pageable));
    }

    @PostMapping("/hotels")
    public ResponseEntity<?> createHotel(@Valid @RequestBody HotelRequest request) {
        Hotel hotel = new Hotel();
        String name = InputSanitizer.requiredPlainText(request.getName(), 200, "酒店名称");
        hotel.setName(name);
        if (request.getLocation() != null) hotel.setLocation(InputSanitizer.optionalPlainText(request.getLocation(), 200, "位置"));
        if (request.getPhone() != null) hotel.setPhone(InputSanitizer.optionalPlainText(request.getPhone(), 32, "电话"));
        if (request.getPriceRange() != null) hotel.setPriceRange(InputSanitizer.optionalPlainText(request.getPriceRange(), 100, "价格区间"));
        if (request.getImageUrl() != null) hotel.setImageUrl(safeImageUrl(request.getImageUrl(), "酒店图片"));
        if (request.getFacilities() != null) hotel.setFacilities(InputSanitizer.optionalPlainText(request.getFacilities(), 500, "设施"));
        if (request.getRating() != null) {
            hotel.setRating(request.getRating());
        }
        hotelRepository.save(hotel);
        return ResponseEntity.ok(hotel);
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.getName() != null) hotel.setName(InputSanitizer.requiredPlainText(request.getName(), 200, "酒店名称"));
        if (request.getLocation() != null) hotel.setLocation(InputSanitizer.optionalPlainText(request.getLocation(), 200, "位置"));
        if (request.getPhone() != null) hotel.setPhone(InputSanitizer.optionalPlainText(request.getPhone(), 32, "电话"));
        if (request.getPriceRange() != null) hotel.setPriceRange(InputSanitizer.optionalPlainText(request.getPriceRange(), 100, "价格区间"));
        if (request.getImageUrl() != null) hotel.setImageUrl(safeImageUrl(request.getImageUrl(), "酒店图片"));
        if (request.getFacilities() != null) hotel.setFacilities(InputSanitizer.optionalPlainText(request.getFacilities(), 500, "设施"));
        if (request.getRating() != null) {
            hotel.setRating(request.getRating());
        }
        hotelRepository.save(hotel);
        return ResponseEntity.ok(hotel);
    }

    @DeleteMapping("/hotels/{id}")
    public ResponseEntity<?> deleteHotel(@PathVariable Long id) {
        if (!hotelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hotelRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<?> getRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId));
    }

    @PostMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<?> createRoomType(@PathVariable Long hotelId, @Valid @RequestBody RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            return ResponseEntity.notFound().build();
        }
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        applyRoomTypeRequest(roomType, request);
        return ResponseEntity.ok(roomTypeRepository.save(roomType));
    }

    @PutMapping("/room-types/{id}")
    public ResponseEntity<?> updateRoomType(@PathVariable Long id, @Valid @RequestBody RoomTypeRequest request) {
        RoomType existing = roomTypeRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        applyRoomTypeRequest(existing, request);
        return ResponseEntity.ok(roomTypeRepository.save(existing));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id) {
        if (!roomTypeRepository.existsById(id)) return ResponseEntity.notFound().build();
        roomTypeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private void applyRoomTypeRequest(RoomType roomType, RoomTypeRequest request) {
        roomType.setName(InputSanitizer.requiredPlainText(request.name(), 80, "房型名称"));
        roomType.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
        roomType.setCapacity(request.capacity() == null ? 1 : request.capacity());
        roomType.setImageUrl(safeImageUrl(request.imageUrl(), "房型图片"));
        roomType.setAmenities(InputSanitizer.optionalPlainText(request.amenities(), 500, "房型设施"));
        roomType.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }
}
