package com.tibet.tourism.controller.admin;

import com.tibet.tourism.entity.Hotel;
import com.tibet.tourism.entity.RoomType;
import com.tibet.tourism.repository.HotelRepository;
import com.tibet.tourism.repository.RoomTypeRepository;
import com.tibet.tourism.security.InputSanitizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

import static com.tibet.tourism.util.RequestParseUtils.safeImageUrl;

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
    public ResponseEntity<?> createHotel(@RequestBody Map<String, Object> request) {
        Hotel hotel = new Hotel();
        String name = InputSanitizer.requiredPlainText((String) request.get("name"), 200, "酒店名称");
        hotel.setName(name);
        if (request.containsKey("location")) hotel.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));
        if (request.containsKey("phone")) hotel.setPhone(InputSanitizer.optionalPlainText((String) request.get("phone"), 32, "电话"));
        if (request.containsKey("priceRange")) hotel.setPriceRange(InputSanitizer.optionalPlainText((String) request.get("priceRange"), 100, "价格区间"));
        if (request.containsKey("imageUrl")) hotel.setImageUrl(safeImageUrl(request.get("imageUrl"), "酒店图片"));
        if (request.containsKey("facilities")) hotel.setFacilities(InputSanitizer.optionalPlainText((String) request.get("facilities"), 500, "设施"));
        if (request.containsKey("rating")) {
            hotel.setRating(new BigDecimal(request.get("rating").toString()));
        }
        hotelRepository.save(hotel);
        return ResponseEntity.ok(hotel);
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.containsKey("name")) hotel.setName(InputSanitizer.requiredPlainText((String) request.get("name"), 200, "酒店名称"));
        if (request.containsKey("location")) hotel.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));
        if (request.containsKey("phone")) hotel.setPhone(InputSanitizer.optionalPlainText((String) request.get("phone"), 32, "电话"));
        if (request.containsKey("priceRange")) hotel.setPriceRange(InputSanitizer.optionalPlainText((String) request.get("priceRange"), 100, "价格区间"));
        if (request.containsKey("imageUrl")) hotel.setImageUrl(safeImageUrl(request.get("imageUrl"), "酒店图片"));
        if (request.containsKey("facilities")) hotel.setFacilities(InputSanitizer.optionalPlainText((String) request.get("facilities"), 500, "设施"));
        if (request.containsKey("rating")) {
            hotel.setRating(new BigDecimal(request.get("rating").toString()));
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

    @GetMapping("/public/room-types/{hotelId}")
    public ResponseEntity<?> getPublicRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId));
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
