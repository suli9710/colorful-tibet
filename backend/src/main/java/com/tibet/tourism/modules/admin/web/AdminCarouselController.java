package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.common.validation.RequestParseUtils;
import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.content.domain.Carousel;
import com.tibet.tourism.modules.content.infra.CarouselRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.tibet.tourism.common.validation.RequestParseUtils.safeImageUrl;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCarouselController {

    public record CarouselRequest(
            @NotBlank(message = "标题不能为空") @Size(max = 80, message = "标题长度不能超过80个字符") String title,
            @Size(max = 160, message = "副标题长度不能超过160个字符") String subtitle,
            @Size(max = 40, message = "标签长度不能超过40个字符") String tag,
            @Size(max = 512, message = "图片地址长度不能超过512个字符") String imageUrl,
            @Size(max = 512, message = "跳转地址长度不能超过512个字符") String linkUrl,
            @Min(value = 0, message = "排序值不能为负数") @Max(value = 10000, message = "排序值过大") Integer sortOrder,
            Boolean active
    ) {}

    private final CarouselRepository carouselRepository;
    private final AdminAuditLogService auditLogService;

    public AdminCarouselController(CarouselRepository carouselRepository,
                                   AdminAuditLogService auditLogService) {
        this.carouselRepository = carouselRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/carousels")
    public ResponseEntity<List<Carousel>> getAllCarousels() {
        return ResponseEntity.ok(carouselRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")));
    }

    @PostMapping("/carousels")
    public ResponseEntity<?> createCarousel(@Valid @RequestBody CarouselRequest request) {
        return auditLogService.captureCreated("carousel", "carousel_create",
                () -> createCarouselInternal(request),
                body -> ((Carousel) body).getId());
    }

    private ResponseEntity<?> createCarouselInternal(CarouselRequest request) {
        Carousel carousel = new Carousel();
        applyCarouselRequest(carousel, request);
        return ResponseEntity.ok(carouselRepository.save(carousel));
    }

    @PutMapping("/carousels/{id}")
    public ResponseEntity<?> updateCarousel(@PathVariable Long id, @Valid @RequestBody CarouselRequest request) {
        return auditLogService.capture("carousel", id, "carousel_update",
                () -> updateCarouselInternal(id, request));
    }

    private ResponseEntity<?> updateCarouselInternal(Long id, CarouselRequest request) {
        Carousel existing = carouselRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        applyCarouselRequest(existing, request);
        return ResponseEntity.ok(carouselRepository.save(existing));
    }

    @DeleteMapping("/carousels/{id}")
    public ResponseEntity<?> deleteCarousel(@PathVariable Long id) {
        return auditLogService.capture("carousel", id, "carousel_delete",
                () -> deleteCarouselInternal(id));
    }

    private ResponseEntity<?> deleteCarouselInternal(Long id) {
        if (!carouselRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        carouselRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private void applyCarouselRequest(Carousel carousel, CarouselRequest request) {
        carousel.setTitle(InputSanitizer.requiredPlainText(request.title(), 80, "轮播图标题"));
        carousel.setSubtitle(InputSanitizer.optionalPlainText(request.subtitle(), 160, "轮播图副标题"));
        carousel.setTag(InputSanitizer.optionalPlainText(request.tag(), 40, "轮播图标签"));
        carousel.setImageUrl(safeImageUrl(request.imageUrl(), "轮播图图片"));
        carousel.setLinkUrl(InputSanitizer.optionalSafeLinkUrl(request.linkUrl(), "轮播图跳转地址"));
        carousel.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        carousel.setActive(request.active() == null || request.active());
    }
}
