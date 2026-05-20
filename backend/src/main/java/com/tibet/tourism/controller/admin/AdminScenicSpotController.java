package com.tibet.tourism.controller.admin;

import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.CommentLikeRepository;
import com.tibet.tourism.repository.CommentRepository;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.SpotTagRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import com.tibet.tourism.security.InputSanitizer;
import com.tibet.tourism.service.TibetanTranslationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static com.tibet.tourism.util.RequestParseUtils.safeImageUrl;

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
    public ResponseEntity<Page<ScenicSpot>> getAllSpots(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(scenicSpotRepository.findAllWithoutTags(pageable));
    }

    @PutMapping("/spots/{id}")
    public ResponseEntity<?> updateSpot(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<ScenicSpot> spotOpt = scenicSpotRepository.findById(id);
        if (spotOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ScenicSpot spot = spotOpt.get();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        if (request.containsKey("name")) {
            String name = InputSanitizer.requiredPlainText((String) request.get("name"), 200, "景点名称");
            spot.setName(name);
            if (autoTranslate && (spot.getNameTibetan() == null || spot.getNameTibetan().isEmpty())) {
                String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.entity.TibetanDictionary.Type.WORD);
                if (tibetanName != null) {
                    spot.setNameTibetan(tibetanName);
                }
            }
        }

        if (request.containsKey("description")) {
            String description = InputSanitizer.optionalTextBlock((String) request.get("description"), 5000, "景点描述");
            spot.setDescription(description);
            if (autoTranslate && description != null && (spot.getDescriptionTibetan() == null || spot.getDescriptionTibetan().isEmpty())) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            }
        }

        if (request.containsKey("nameTibetan")) {
            spot.setNameTibetan(InputSanitizer.optionalPlainText((String) request.get("nameTibetan"), 200, "藏语名称"));
        }
        if (request.containsKey("descriptionTibetan")) {
            spot.setDescriptionTibetan(InputSanitizer.optionalTextBlock((String) request.get("descriptionTibetan"), 5000, "藏语描述"));
        }
        if (request.containsKey("imageUrl")) {
            spot.setImageUrl(safeImageUrl(request.get("imageUrl"), "景点图片"));
        }
        if (request.containsKey("ticketPrice")) {
            Object priceObj = request.get("ticketPrice");
            if (priceObj instanceof Number) {
                spot.setTicketPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
            } else if (priceObj instanceof String) {
                try {
                    spot.setTicketPrice(new BigDecimal((String) priceObj));
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "无效的价格格式"));
                }
            }
        }
        if (request.containsKey("altitude")) {
            spot.setAltitude(InputSanitizer.optionalPlainText((String) request.get("altitude"), 100, "海拔"));
        }
        if (request.containsKey("location")) {
            spot.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));
        }
        if (request.containsKey("category")) {
            try {
                spot.setCategory(ScenicSpot.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("num")) spot.setNum(((Number) request.get("num")).intValue());
        if (request.containsKey("openInfo")) spot.setOpenInfo(InputSanitizer.optionalPlainText((String) request.get("openInfo"), 500, "开放信息"));
        if (request.containsKey("entryTime")) spot.setEntryTime(InputSanitizer.optionalPlainText((String) request.get("entryTime"), 200, "入园时间"));
        if (request.containsKey("latitude")) spot.setLatitude(new BigDecimal(request.get("latitude").toString()));
        if (request.containsKey("longitude")) spot.setLongitude(new BigDecimal(request.get("longitude").toString()));

        scenicSpotRepository.save(spot);
        return ResponseEntity.ok(spot);
    }

    @PostMapping("/spots")
    public ResponseEntity<?> createSpot(@RequestBody Map<String, Object> request) {
        ScenicSpot spot = new ScenicSpot();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        String name = InputSanitizer.requiredPlainText((String) request.get("name"), 200, "景点名称");

        if (autoTranslate) {
            String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.entity.TibetanDictionary.Type.WORD);
            if (tibetanName != null) {
                spot.setNameTibetan(tibetanName);
            }
        } else if (request.containsKey("nameTibetan")) {
            spot.setNameTibetan(InputSanitizer.optionalPlainText((String) request.get("nameTibetan"), 200, "藏语名称"));
        }
        spot.setName(name);

        if (request.containsKey("description")) {
            String description = InputSanitizer.optionalTextBlock((String) request.get("description"), 5000, "景点描述");
            spot.setDescription(description);
            if (autoTranslate && description != null) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            } else if (request.containsKey("descriptionTibetan")) {
                spot.setDescriptionTibetan(InputSanitizer.optionalTextBlock((String) request.get("descriptionTibetan"), 5000, "藏语描述"));
            }
        }
        if (request.containsKey("imageUrl")) {
            spot.setImageUrl(safeImageUrl(request.get("imageUrl"), "景点图片"));
        }
        if (request.containsKey("ticketPrice")) {
            Object priceObj = request.get("ticketPrice");
            if (priceObj instanceof Number) {
                spot.setTicketPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
            }
        }
        if (request.containsKey("category")) {
            try {
                spot.setCategory(ScenicSpot.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("num")) spot.setNum(((Number) request.get("num")).intValue());
        if (request.containsKey("openInfo")) spot.setOpenInfo(InputSanitizer.optionalPlainText((String) request.get("openInfo"), 500, "开放信息"));
        if (request.containsKey("entryTime")) spot.setEntryTime(InputSanitizer.optionalPlainText((String) request.get("entryTime"), 200, "入园时间"));
        if (request.containsKey("latitude")) spot.setLatitude(new BigDecimal(request.get("latitude").toString()));
        if (request.containsKey("longitude")) spot.setLongitude(new BigDecimal(request.get("longitude").toString()));
        if (request.containsKey("altitude")) spot.setAltitude(InputSanitizer.optionalPlainText((String) request.get("altitude"), 100, "海拔"));
        if (request.containsKey("location")) spot.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));

        scenicSpotRepository.save(spot);
        return ResponseEntity.ok(spot);
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
}
