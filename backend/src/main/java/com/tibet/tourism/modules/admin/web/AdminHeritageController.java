package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.content.domain.HeritageEvent;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.infra.HeritageCommentRepository;
import com.tibet.tourism.modules.content.infra.HeritageEventRepository;
import com.tibet.tourism.modules.content.infra.HeritageInheritorRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.content.infra.HeritageLikeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static com.tibet.tourism.common.validation.RequestParseUtils.safeImageUrl;

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
    public ResponseEntity<Page<HeritageItem>> getAllItems(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(itemRepository.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<?> createItem(@RequestBody Map<String, Object> request) {
        HeritageItem item = new HeritageItem();
        applyItemFields(item, request);
        itemRepository.save(item);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        HeritageItem item = itemRepository.findById(id).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();
        applyItemFields(item, request);
        itemRepository.save(item);
        return ResponseEntity.ok(item);
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
    public ResponseEntity<List<HeritageInheritor>> getInheritors(@PathVariable Long itemId) {
        return ResponseEntity.ok(inheritorRepository.findByHeritageItemId(itemId));
    }

    @PostMapping("/{itemId}/inheritors")
    public ResponseEntity<?> createInheritor(@PathVariable Long itemId, @RequestBody Map<String, Object> request) {
        HeritageItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        HeritageInheritor inheritor = new HeritageInheritor();
        inheritor.setHeritageItem(item);
        applyInheritorFields(inheritor, request);
        inheritorRepository.save(inheritor);
        return ResponseEntity.ok(inheritor);
    }

    @PutMapping("/inheritors/{id}")
    public ResponseEntity<?> updateInheritor(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        HeritageInheritor inheritor = inheritorRepository.findById(id).orElse(null);
        if (inheritor == null) return ResponseEntity.notFound().build();
        applyInheritorFields(inheritor, request);
        inheritorRepository.save(inheritor);
        return ResponseEntity.ok(inheritor);
    }

    @DeleteMapping("/inheritors/{id}")
    public ResponseEntity<?> deleteInheritor(@PathVariable Long id) {
        if (!inheritorRepository.existsById(id)) return ResponseEntity.notFound().build();
        inheritorRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ---- Events CRUD ----
    @GetMapping("/{itemId}/events")
    public ResponseEntity<List<HeritageEvent>> getEvents(@PathVariable Long itemId) {
        return ResponseEntity.ok(eventRepository.findByHeritageItemId(itemId));
    }

    @PostMapping("/{itemId}/events")
    public ResponseEntity<?> createEvent(@PathVariable Long itemId, @RequestBody Map<String, Object> request) {
        HeritageItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        HeritageEvent event = new HeritageEvent();
        event.setHeritageItem(item);
        applyEventFields(event, request);
        eventRepository.save(event);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        HeritageEvent event = eventRepository.findById(id).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();
        applyEventFields(event, request);
        eventRepository.save(event);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) return ResponseEntity.notFound().build();
        eventRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ---- Private helpers ----
    private void applyItemFields(HeritageItem item, Map<String, Object> req) {
        if (req.containsKey("name")) item.setName(InputSanitizer.requiredPlainText((String) req.get("name"), 200, "名称"));
        if (req.containsKey("nameTibetan")) item.setNameTibetan(InputSanitizer.optionalPlainText((String) req.get("nameTibetan"), 200, "藏语名称"));
        if (req.containsKey("description")) item.setDescription(InputSanitizer.optionalTextBlock((String) req.get("description"), 10000, "描述"));
        if (req.containsKey("descriptionTibetan")) item.setDescriptionTibetan(InputSanitizer.optionalTextBlock((String) req.get("descriptionTibetan"), 10000, "藏语描述"));
        if (req.containsKey("category")) item.setCategory(InputSanitizer.optionalPlainText((String) req.get("category"), 100, "类别"));
        if (req.containsKey("imageUrl")) item.setImageUrl(safeImageUrl(req.get("imageUrl"), "图片"));
        if (req.containsKey("videoUrl")) item.setVideoUrl(InputSanitizer.optionalPlainText((String) req.get("videoUrl"), 512, "视频链接"));
        if (req.containsKey("originStory")) item.setOriginStory(InputSanitizer.optionalTextBlock((String) req.get("originStory"), 10000, "起源故事"));
        if (req.containsKey("significance")) item.setSignificance(InputSanitizer.optionalTextBlock((String) req.get("significance"), 10000, "文化价值"));
        if (req.containsKey("baikeUrl")) item.setBaikeUrl(InputSanitizer.optionalPlainText((String) req.get("baikeUrl"), 512, "百科链接"));
        if (req.containsKey("region")) item.setRegion(InputSanitizer.optionalPlainText((String) req.get("region"), 100, "地区"));
        if (req.containsKey("protectionLevel")) item.setProtectionLevel(InputSanitizer.optionalPlainText((String) req.get("protectionLevel"), 50, "保护级别"));
    }

    private void applyInheritorFields(HeritageInheritor inheritor, Map<String, Object> req) {
        if (req.containsKey("name")) inheritor.setName(InputSanitizer.requiredPlainText((String) req.get("name"), 100, "姓名"));
        if (req.containsKey("nameTibetan")) inheritor.setNameTibetan(InputSanitizer.optionalPlainText((String) req.get("nameTibetan"), 100, "藏语姓名"));
        if (req.containsKey("avatarUrl")) inheritor.setAvatarUrl(safeImageUrl(req.get("avatarUrl"), "头像"));
        if (req.containsKey("level")) inheritor.setLevel(InputSanitizer.optionalPlainText((String) req.get("level"), 50, "级别"));
        if (req.containsKey("bio")) inheritor.setBio(InputSanitizer.optionalTextBlock((String) req.get("bio"), 5000, "简介"));
        if (req.containsKey("bioTibetan")) inheritor.setBioTibetan(InputSanitizer.optionalTextBlock((String) req.get("bioTibetan"), 5000, "藏语简介"));
        if (req.containsKey("story")) inheritor.setStory(InputSanitizer.optionalTextBlock((String) req.get("story"), 10000, "传承故事"));
        if (req.containsKey("region")) inheritor.setRegion(InputSanitizer.optionalPlainText((String) req.get("region"), 100, "所在地区"));
    }

    private void applyEventFields(HeritageEvent event, Map<String, Object> req) {
        if (req.containsKey("title")) event.setTitle(InputSanitizer.requiredPlainText((String) req.get("title"), 200, "标题"));
        if (req.containsKey("titleTibetan")) event.setTitleTibetan(InputSanitizer.optionalPlainText((String) req.get("titleTibetan"), 200, "藏语标题"));
        if (req.containsKey("description")) event.setDescription(InputSanitizer.optionalTextBlock((String) req.get("description"), 5000, "描述"));
        if (req.containsKey("descriptionTibetan")) event.setDescriptionTibetan(InputSanitizer.optionalTextBlock((String) req.get("descriptionTibetan"), 5000, "藏语描述"));
        if (req.containsKey("eventDate")) event.setEventDate(LocalDate.parse((String) req.get("eventDate")));
        if (req.containsKey("endDate")) event.setEndDate(LocalDate.parse((String) req.get("endDate")));
        if (req.containsKey("location")) event.setLocation(InputSanitizer.optionalPlainText((String) req.get("location"), 200, "地点"));
        if (req.containsKey("imageUrl")) event.setImageUrl(safeImageUrl(req.get("imageUrl"), "活动图片"));
        if (req.containsKey("contactInfo")) event.setContactInfo(InputSanitizer.optionalPlainText((String) req.get("contactInfo"), 200, "联系方式"));
    }
}
