package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.web.dto.HeritageEventRequest;
import com.tibet.tourism.modules.admin.web.dto.HeritageInheritorRequest;
import com.tibet.tourism.modules.admin.web.dto.HeritageItemRequest;
import com.tibet.tourism.modules.content.domain.HeritageEvent;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.infra.HeritageCommentRepository;
import com.tibet.tourism.modules.content.infra.HeritageEventRepository;
import com.tibet.tourism.modules.content.infra.HeritageInheritorRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.content.infra.HeritageLikeRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/heritage")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHeritageController {

    private final HeritageItemRepository itemRepository;
    private final HeritageInheritorRepository inheritorRepository;
    private final HeritageEventRepository eventRepository;
    private final HeritageLikeRepository likeRepository;
    private final HeritageCommentRepository commentRepository;

    public AdminHeritageController(HeritageItemRepository itemRepository,
                                   HeritageInheritorRepository inheritorRepository,
                                   HeritageEventRepository eventRepository,
                                   HeritageLikeRepository likeRepository,
                                   HeritageCommentRepository commentRepository) {
        this.itemRepository = itemRepository;
        this.inheritorRepository = inheritorRepository;
        this.eventRepository = eventRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    // ---- Heritage Items CRUD ----
    @GetMapping
    public ResponseEntity<PageResponse<HeritageItemResponse>> getAllItems(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(itemRepository.findAll(pageable).map(HeritageItemResponse::from)));
    }

    @PostMapping
    public ResponseEntity<?> createItem(@RequestBody @Valid HeritageItemRequest request) {
        HeritageItem item = new HeritageItem();
        applyItemFields(item, request);
        itemRepository.save(item);
        return ResponseEntity.ok(HeritageItemResponse.from(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody @Valid HeritageItemRequest request) {
        HeritageItem item = itemRepository.findById(id).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();
        applyItemFields(item, request);
        itemRepository.save(item);
        return ResponseEntity.ok(HeritageItemResponse.from(item));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) return ResponseEntity.notFound().build();
        likeRepository.deleteByHeritageItemId(id);
        commentRepository.deleteByHeritageItemId(id);
        inheritorRepository.deleteByHeritageItemId(id);
        eventRepository.deleteByHeritageItemId(id);
        itemRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ---- Inheritors CRUD ----
    @GetMapping("/{itemId}/inheritors")
    public ResponseEntity<List<HeritageInheritorResponse>> getInheritors(@PathVariable Long itemId) {
        return ResponseEntity.ok(inheritorRepository.findByHeritageItemId(itemId)
                .stream()
                .map(HeritageInheritorResponse::from)
                .toList());
    }

    @PostMapping("/{itemId}/inheritors")
    public ResponseEntity<?> createInheritor(@PathVariable Long itemId, @RequestBody @Valid HeritageInheritorRequest request) {
        HeritageItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        HeritageInheritor inheritor = new HeritageInheritor();
        inheritor.setHeritageItem(item);
        applyInheritorFields(inheritor, request);
        inheritorRepository.save(inheritor);
        return ResponseEntity.ok(HeritageInheritorResponse.from(inheritor));
    }

    @PutMapping("/inheritors/{id}")
    public ResponseEntity<?> updateInheritor(@PathVariable Long id, @RequestBody @Valid HeritageInheritorRequest request) {
        HeritageInheritor inheritor = inheritorRepository.findById(id).orElse(null);
        if (inheritor == null) return ResponseEntity.notFound().build();
        applyInheritorFields(inheritor, request);
        inheritorRepository.save(inheritor);
        return ResponseEntity.ok(HeritageInheritorResponse.from(inheritor));
    }

    @DeleteMapping("/inheritors/{id}")
    public ResponseEntity<?> deleteInheritor(@PathVariable Long id) {
        if (!inheritorRepository.existsById(id)) return ResponseEntity.notFound().build();
        inheritorRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ---- Events CRUD ----
    @GetMapping("/{itemId}/events")
    public ResponseEntity<List<HeritageEventResponse>> getEvents(@PathVariable Long itemId) {
        return ResponseEntity.ok(eventRepository.findByHeritageItemId(itemId)
                .stream()
                .map(HeritageEventResponse::from)
                .toList());
    }

    @PostMapping("/{itemId}/events")
    public ResponseEntity<?> createEvent(@PathVariable Long itemId, @RequestBody @Valid HeritageEventRequest request) {
        HeritageItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        HeritageEvent event = new HeritageEvent();
        event.setHeritageItem(item);
        applyEventFields(event, request);
        eventRepository.save(event);
        return ResponseEntity.ok(HeritageEventResponse.from(event));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody @Valid HeritageEventRequest request) {
        HeritageEvent event = eventRepository.findById(id).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();
        applyEventFields(event, request);
        eventRepository.save(event);
        return ResponseEntity.ok(HeritageEventResponse.from(event));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) return ResponseEntity.notFound().build();
        eventRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    public record HeritageItemResponse(
            Long id,
            String name,
            String nameTibetan,
            String description,
            String descriptionTibetan,
            String category,
            String imageUrl,
            String videoUrl,
            String originStory,
            String significance,
            String baikeUrl,
            String region,
            String protectionLevel,
            Integer viewCount,
            Integer likeCount,
            Integer commentCount,
            LocalDateTime createdAt
    ) {
        private static HeritageItemResponse from(HeritageItem item) {
            return new HeritageItemResponse(
                    item.getId(),
                    item.getName(),
                    item.getNameTibetan(),
                    item.getDescription(),
                    item.getDescriptionTibetan(),
                    item.getCategory(),
                    item.getImageUrl(),
                    item.getVideoUrl(),
                    item.getOriginStory(),
                    item.getSignificance(),
                    item.getBaikeUrl(),
                    item.getRegion(),
                    item.getProtectionLevel(),
                    item.getViewCount(),
                    item.getLikeCount(),
                    item.getCommentCount(),
                    item.getCreatedAt());
        }
    }

    public record HeritageInheritorResponse(
            Long id,
            String name,
            String nameTibetan,
            String avatarUrl,
            String level,
            String bio,
            String bioTibetan,
            String story,
            String region,
            Long heritageItemId,
            LocalDateTime createdAt
    ) {
        private static HeritageInheritorResponse from(HeritageInheritor inheritor) {
            HeritageItem item = inheritor.getHeritageItem();
            return new HeritageInheritorResponse(
                    inheritor.getId(),
                    inheritor.getName(),
                    inheritor.getNameTibetan(),
                    inheritor.getAvatarUrl(),
                    inheritor.getLevel(),
                    inheritor.getBio(),
                    inheritor.getBioTibetan(),
                    inheritor.getStory(),
                    inheritor.getRegion(),
                    item == null ? null : item.getId(),
                    inheritor.getCreatedAt());
        }
    }

    public record HeritageEventResponse(
            Long id,
            String title,
            String titleTibetan,
            String description,
            String descriptionTibetan,
            LocalDate eventDate,
            LocalDate endDate,
            String location,
            String imageUrl,
            String contactInfo,
            Long heritageItemId,
            LocalDateTime createdAt
    ) {
        private static HeritageEventResponse from(HeritageEvent event) {
            HeritageItem item = event.getHeritageItem();
            return new HeritageEventResponse(
                    event.getId(),
                    event.getTitle(),
                    event.getTitleTibetan(),
                    event.getDescription(),
                    event.getDescriptionTibetan(),
                    event.getEventDate(),
                    event.getEndDate(),
                    event.getLocation(),
                    event.getImageUrl(),
                    event.getContactInfo(),
                    item == null ? null : item.getId(),
                    event.getCreatedAt());
        }
    }

    // ---- Private helpers ----
    private void applyItemFields(HeritageItem item, HeritageItemRequest req) {
        if (req.getName() != null) item.setName(InputSanitizer.requiredPlainText(req.getName(), 200, "名称"));
        if (req.getNameTibetan() != null) item.setNameTibetan(InputSanitizer.optionalPlainText(req.getNameTibetan(), 200, "藏语名称"));
        if (req.getDescription() != null) item.setDescription(InputSanitizer.optionalTextBlock(req.getDescription(), 10000, "描述"));
        if (req.getDescriptionTibetan() != null) item.setDescriptionTibetan(InputSanitizer.optionalTextBlock(req.getDescriptionTibetan(), 10000, "藏语描述"));
        if (req.getCategory() != null) item.setCategory(InputSanitizer.optionalPlainText(req.getCategory(), 100, "类别"));
        if (req.getImageUrl() != null) item.setImageUrl(InputSanitizer.optionalPublicImageUrl(req.getImageUrl(), "图片"));
        if (req.getVideoUrl() != null) item.setVideoUrl(InputSanitizer.optionalSafeLinkUrl(req.getVideoUrl(), "视频链接"));
        if (req.getOriginStory() != null) item.setOriginStory(InputSanitizer.optionalTextBlock(req.getOriginStory(), 10000, "起源故事"));
        if (req.getSignificance() != null) item.setSignificance(InputSanitizer.optionalTextBlock(req.getSignificance(), 10000, "文化价值"));
        if (req.getBaikeUrl() != null) item.setBaikeUrl(InputSanitizer.optionalSafeLinkUrl(req.getBaikeUrl(), "百科链接"));
        if (req.getRegion() != null) item.setRegion(InputSanitizer.optionalPlainText(req.getRegion(), 100, "地区"));
        if (req.getProtectionLevel() != null) item.setProtectionLevel(InputSanitizer.optionalPlainText(req.getProtectionLevel(), 50, "保护级别"));
    }

    private void applyInheritorFields(HeritageInheritor inheritor, HeritageInheritorRequest req) {
        if (req.getName() != null) inheritor.setName(InputSanitizer.requiredPlainText(req.getName(), 100, "姓名"));
        if (req.getNameTibetan() != null) inheritor.setNameTibetan(InputSanitizer.optionalPlainText(req.getNameTibetan(), 100, "藏语姓名"));
        if (req.getAvatarUrl() != null) inheritor.setAvatarUrl(InputSanitizer.optionalPublicImageUrl(req.getAvatarUrl(), "头像"));
        if (req.getLevel() != null) inheritor.setLevel(InputSanitizer.optionalPlainText(req.getLevel(), 50, "级别"));
        if (req.getBio() != null) inheritor.setBio(InputSanitizer.optionalTextBlock(req.getBio(), 5000, "简介"));
        if (req.getBioTibetan() != null) inheritor.setBioTibetan(InputSanitizer.optionalTextBlock(req.getBioTibetan(), 5000, "藏语简介"));
        if (req.getStory() != null) inheritor.setStory(InputSanitizer.optionalTextBlock(req.getStory(), 10000, "传承故事"));
        if (req.getRegion() != null) inheritor.setRegion(InputSanitizer.optionalPlainText(req.getRegion(), 100, "所在地区"));
    }

    private void applyEventFields(HeritageEvent event, HeritageEventRequest req) {
        if (req.getTitle() != null) event.setTitle(InputSanitizer.requiredPlainText(req.getTitle(), 200, "标题"));
        if (req.getTitleTibetan() != null) event.setTitleTibetan(InputSanitizer.optionalPlainText(req.getTitleTibetan(), 200, "藏语标题"));
        if (req.getDescription() != null) event.setDescription(InputSanitizer.optionalTextBlock(req.getDescription(), 5000, "描述"));
        if (req.getDescriptionTibetan() != null) event.setDescriptionTibetan(InputSanitizer.optionalTextBlock(req.getDescriptionTibetan(), 5000, "藏语描述"));
        if (req.getEventDate() != null) event.setEventDate(req.getEventDate());
        if (req.getEndDate() != null) event.setEndDate(req.getEndDate());
        if (req.getLocation() != null) event.setLocation(InputSanitizer.optionalPlainText(req.getLocation(), 200, "地点"));
        if (req.getImageUrl() != null) event.setImageUrl(InputSanitizer.optionalPublicImageUrl(req.getImageUrl(), "活动图片"));
        if (req.getContactInfo() != null) event.setContactInfo(InputSanitizer.optionalPlainText(req.getContactInfo(), 200, "联系方式"));
    }
}
