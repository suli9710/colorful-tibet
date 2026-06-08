package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.web.dto.ScenicSpotRequest;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.content.application.TibetanTranslationService;
import com.tibet.tourism.modules.content.domain.TibetanDictionary;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.infra.SpotTagRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static com.tibet.tourism.common.validation.RequestParseUtils.safeImageUrl;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScenicSpotController {

    private final ScenicSpotRepository scenicSpotRepository;
    private final SpotTagRepository tagRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final BookingRepository bookingRepository;
    private final UserVisitHistoryRepository userVisitHistoryRepository;
    private final TibetanTranslationService translationService;

    public AdminScenicSpotController(ScenicSpotRepository scenicSpotRepository,
                                     SpotTagRepository tagRepository,
                                     CommentRepository commentRepository,
                                     CommentLikeRepository commentLikeRepository,
                                     BookingRepository bookingRepository,
                                     UserVisitHistoryRepository userVisitHistoryRepository,
                                     TibetanTranslationService translationService) {
        this.scenicSpotRepository = scenicSpotRepository;
        this.tagRepository = tagRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.bookingRepository = bookingRepository;
        this.userVisitHistoryRepository = userVisitHistoryRepository;
        this.translationService = translationService;
    }

    @GetMapping("/spots")
    public ResponseEntity<PageResponse<ScenicSpotResponse>> getAllSpots(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(scenicSpotRepository.findAllWithoutTags(pageable).map(ScenicSpotResponse::from)));
    }

    @PutMapping("/spots/{id}")
    public ResponseEntity<?> updateSpot(@PathVariable Long id, @Valid @RequestBody ScenicSpotRequest request) {
        Optional<ScenicSpot> spotOpt = scenicSpotRepository.findById(id);
        if (spotOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ScenicSpot spot = spotOpt.get();
        boolean autoTranslate = request.getAutoTranslate() == null || request.getAutoTranslate();

        if (request.getName() != null) {
            String name = InputSanitizer.requiredPlainText(request.getName(), 200, "景点名称");
            spot.setName(name);
            if (autoTranslate && (spot.getNameTibetan() == null || spot.getNameTibetan().isEmpty())) {
                String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.modules.content.domain.TibetanDictionary.Type.WORD);
                if (tibetanName != null) {
                    spot.setNameTibetan(tibetanName);
                }
            }
        }

        if (request.getDescription() != null) {
            String description = InputSanitizer.optionalTextBlock(request.getDescription(), 5000, "景点描述");
            spot.setDescription(description);
            if (autoTranslate && description != null && (spot.getDescriptionTibetan() == null || spot.getDescriptionTibetan().isEmpty())) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            }
        }

        if (request.getNameTibetan() != null) {
            spot.setNameTibetan(InputSanitizer.optionalPlainText(request.getNameTibetan(), 200, "藏语名称"));
        }
        if (request.getDescriptionTibetan() != null) {
            spot.setDescriptionTibetan(InputSanitizer.optionalTextBlock(request.getDescriptionTibetan(), 5000, "藏语描述"));
        }
        if (request.getImageUrl() != null) {
            spot.setImageUrl(safeImageUrl(request.getImageUrl(), "景点图片"));
        }
        if (request.getTicketPrice() != null) {
            spot.setTicketPrice(request.getTicketPrice());
        }
        if (request.getAltitude() != null) {
            spot.setAltitude(InputSanitizer.optionalPlainText(request.getAltitude(), 100, "海拔"));
        }
        if (request.getLocation() != null) {
            spot.setLocation(InputSanitizer.optionalPlainText(request.getLocation(), 200, "位置"));
        }
        if (request.getCategory() != null) {
            try {
                spot.setCategory(ScenicSpot.Category.valueOf(request.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.getNum() != null) spot.setNum(request.getNum());
        if (request.getOpenInfo() != null) spot.setOpenInfo(InputSanitizer.optionalPlainText(request.getOpenInfo(), 500, "开放信息"));
        if (request.getEntryTime() != null) spot.setEntryTime(InputSanitizer.optionalPlainText(request.getEntryTime(), 200, "入园时间"));
        if (request.getLatitude() != null) spot.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) spot.setLongitude(request.getLongitude());

        scenicSpotRepository.save(spot);
        return ResponseEntity.ok(ScenicSpotResponse.from(spot));
    }

    @PostMapping("/spots")
    public ResponseEntity<?> createSpot(@Valid @RequestBody ScenicSpotRequest request) {
        ScenicSpot spot = new ScenicSpot();
        boolean autoTranslate = request.getAutoTranslate() == null || request.getAutoTranslate();

        String name = InputSanitizer.requiredPlainText(request.getName(), 200, "景点名称");

        if (autoTranslate) {
            String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.modules.content.domain.TibetanDictionary.Type.WORD);
            if (tibetanName != null) {
                spot.setNameTibetan(tibetanName);
            }
        } else if (request.getNameTibetan() != null) {
            spot.setNameTibetan(InputSanitizer.optionalPlainText(request.getNameTibetan(), 200, "藏语名称"));
        }
        spot.setName(name);

        if (request.getDescription() != null) {
            String description = InputSanitizer.optionalTextBlock(request.getDescription(), 5000, "景点描述");
            spot.setDescription(description);
            if (autoTranslate && description != null) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            } else if (request.getDescriptionTibetan() != null) {
                spot.setDescriptionTibetan(InputSanitizer.optionalTextBlock(request.getDescriptionTibetan(), 5000, "藏语描述"));
            }
        }
        if (request.getImageUrl() != null) {
            spot.setImageUrl(safeImageUrl(request.getImageUrl(), "景点图片"));
        }
        if (request.getTicketPrice() != null) {
            spot.setTicketPrice(request.getTicketPrice());
        }
        if (request.getCategory() != null) {
            try {
                spot.setCategory(ScenicSpot.Category.valueOf(request.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.getNum() != null) spot.setNum(request.getNum());
        if (request.getOpenInfo() != null) spot.setOpenInfo(InputSanitizer.optionalPlainText(request.getOpenInfo(), 500, "开放信息"));
        if (request.getEntryTime() != null) spot.setEntryTime(InputSanitizer.optionalPlainText(request.getEntryTime(), 200, "入园时间"));
        if (request.getLatitude() != null) spot.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) spot.setLongitude(request.getLongitude());
        if (request.getAltitude() != null) spot.setAltitude(InputSanitizer.optionalPlainText(request.getAltitude(), 100, "海拔"));
        if (request.getLocation() != null) spot.setLocation(InputSanitizer.optionalPlainText(request.getLocation(), 200, "位置"));

        scenicSpotRepository.save(spot);
        return ResponseEntity.ok(ScenicSpotResponse.from(spot));
    }

    @DeleteMapping("/spots/{id}")
    @Transactional
    public ResponseEntity<?> deleteSpot(@PathVariable Long id) {
        if (!scenicSpotRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        commentRepository.findBySpotIdOrderByCreatedAtDesc(id)
                .forEach(comment -> commentLikeRepository.deleteByCommentId(comment.getId()));
        commentRepository.deleteBySpotId(id);
        bookingRepository.deleteBySpotId(id);
        userVisitHistoryRepository.deleteBySpotId(id);
        tagRepository.deleteBySpotId(id);
        scenicSpotRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
    public record ScenicSpotResponse(
            Long id,
            String name,
            String nameTibetan,
            String description,
            String descriptionTibetan,
            String imageUrl,
            String altitude,
            String location,
            ScenicSpot.Category category,
            BigDecimal ticketPrice,
            BigDecimal peakSeasonPrice,
            BigDecimal offSeasonPrice,
            LocalDate peakStartDate,
            LocalDate peakEndDate,
            LocalDate freeStartDate,
            LocalDate freeEndDate,
            BigDecimal rating,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer visitCount,
            Integer num,
            String openInfo,
            String entryTime,
            LocalDateTime createdAt
    ) {
        private static ScenicSpotResponse from(ScenicSpot spot) {
            return new ScenicSpotResponse(
                    spot.getId(),
                    spot.getName(),
                    spot.getNameTibetan(),
                    spot.getDescription(),
                    spot.getDescriptionTibetan(),
                    spot.getImageUrl(),
                    spot.getAltitude(),
                    spot.getLocation(),
                    spot.getCategory(),
                    spot.getTicketPrice(),
                    spot.getPeakSeasonPrice(),
                    spot.getOffSeasonPrice(),
                    spot.getPeakStartDate(),
                    spot.getPeakEndDate(),
                    spot.getFreeStartDate(),
                    spot.getFreeEndDate(),
                    spot.getRating(),
                    spot.getLatitude(),
                    spot.getLongitude(),
                    spot.getVisitCount(),
                    spot.getNum(),
                    spot.getOpenInfo(),
                    spot.getEntryTime(),
                    spot.getCreatedAt());
        }
    }
}
