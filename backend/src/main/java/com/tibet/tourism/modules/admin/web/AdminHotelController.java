package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.admin.web.dto.HotelRequest;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
    private final HotelBookingRepository hotelBookingRepository;
    private final AdminAuditLogService auditLogService;

    public AdminHotelController(HotelRepository hotelRepository,
                               RoomTypeRepository roomTypeRepository,
                               HotelBookingRepository hotelBookingRepository,
                               AdminAuditLogService auditLogService) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.auditLogService = auditLogService;
    }

    // ?sort= binds straight into the repository here, so restrict it to columns that are safe to expose.
    private static final Set<String> HOTEL_SORT_FIELDS = Set.of("id", "name", "rating", "createdAt");
    private static final Sort HOTEL_DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id");

    @GetMapping("/hotels")
    public ResponseEntity<PageResponse<HotelResponse>> getAllHotels(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(hotelRepository.findAll(InputSanitizer.sanitizePageable(pageable, HOTEL_SORT_FIELDS, HOTEL_DEFAULT_SORT, 50, 200)).map(HotelResponse::from)));
    }

    @PostMapping("/hotels")
    public ResponseEntity<?> createHotel(@Valid @RequestBody HotelRequest request) {
        return auditLogService.captureCreated("hotel", "hotel_create",
                () -> createHotelInternal(request),
                body -> ((HotelResponse) body).id());
    }

    private ResponseEntity<?> createHotelInternal(HotelRequest request) {
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
        return ResponseEntity.ok(HotelResponse.from(hotel));
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        return auditLogService.capture("hotel", id, "hotel_update", () -> updateHotelInternal(id, request));
    }

    private ResponseEntity<?> updateHotelInternal(Long id, HotelRequest request) {
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
        return ResponseEntity.ok(HotelResponse.from(hotel));
    }

    @DeleteMapping("/hotels/{id}")
    @Transactional
    public ResponseEntity<?> deleteHotel(@PathVariable Long id) {
        return auditLogService.capture("hotel", id, "hotel_delete", () -> deleteHotelInternal(id));
    }

    private ResponseEntity<?> deleteHotelInternal(Long id) {
        if (!hotelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // hotel_bookings 与 hotels 之间是 RESTRICT 外键：存在预订记录时直接 deleteById 会抛
        // DataIntegrityViolationException(500)。预订属于订单/PII 历史，不能随酒店级联删除，
        // 因此存在预订时拒绝删除；否则先清理房型再删除酒店。
        if (hotelBookingRepository.countByHotelId(id) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "该酒店存在预订记录，无法删除"));
        }
        roomTypeRepository.deleteByHotelId(id);
        hotelRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<?> getRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId)
                .stream()
                .map(RoomTypeResponse::from)
                .toList());
    }

    @PostMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<?> createRoomType(@PathVariable Long hotelId, @Valid @RequestBody RoomTypeRequest request) {
        return auditLogService.captureCreated("room_type", "room_type_create",
                () -> createRoomTypeInternal(hotelId, request),
                body -> ((RoomTypeResponse) body).id());
    }

    private ResponseEntity<?> createRoomTypeInternal(Long hotelId, RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            return ResponseEntity.notFound().build();
        }
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        applyRoomTypeRequest(roomType, request);
        return ResponseEntity.ok(RoomTypeResponse.from(roomTypeRepository.save(roomType)));
    }

    @PutMapping("/room-types/{id}")
    public ResponseEntity<?> updateRoomType(@PathVariable Long id, @Valid @RequestBody RoomTypeRequest request) {
        return auditLogService.capture("room_type", id, "room_type_update",
                () -> updateRoomTypeInternal(id, request));
    }

    private ResponseEntity<?> updateRoomTypeInternal(Long id, RoomTypeRequest request) {
        RoomType existing = roomTypeRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        applyRoomTypeRequest(existing, request);
        return ResponseEntity.ok(RoomTypeResponse.from(roomTypeRepository.save(existing)));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id) {
        return auditLogService.capture("room_type", id, "room_type_delete",
                () -> deleteRoomTypeInternal(id));
    }

    private ResponseEntity<?> deleteRoomTypeInternal(Long id) {
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

    public record HotelResponse(
            Long id,
            String name,
            String location,
            String phone,
            String priceRange,
            BigDecimal rating,
            String imageUrl,
            String facilities,
            LocalDateTime createdAt
    ) {
        private static HotelResponse from(Hotel hotel) {
            return new HotelResponse(
                    hotel.getId(),
                    hotel.getName(),
                    hotel.getLocation(),
                    hotel.getPhone(),
                    hotel.getPriceRange(),
                    hotel.getRating(),
                    hotel.getImageUrl(),
                    hotel.getFacilities(),
                    hotel.getCreatedAt());
        }
    }

    public record RoomTypeResponse(
            Long id,
            String name,
            BigDecimal price,
            Integer capacity,
            String imageUrl,
            String amenities,
            Integer sortOrder
    ) {
        private static RoomTypeResponse from(RoomType roomType) {
            return new RoomTypeResponse(
                    roomType.getId(),
                    roomType.getName(),
                    roomType.getPrice(),
                    roomType.getCapacity(),
                    roomType.getImageUrl(),
                    roomType.getAmenities(),
                    roomType.getSortOrder());
        }
    }
}
