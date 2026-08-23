package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.admin.web.dto.NewsRequest;
import com.tibet.tourism.modules.content.application.TibetanTranslationService;
import com.tibet.tourism.modules.content.domain.News;
import com.tibet.tourism.modules.content.domain.TibetanDictionary;
import com.tibet.tourism.modules.content.infra.NewsRepository;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
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
public class AdminNewsController {

    private final NewsRepository newsRepository;
    private final TibetanTranslationService translationService;
    private final AdminAuditLogService auditLogService;

    public AdminNewsController(NewsRepository newsRepository,
                               TibetanTranslationService translationService,
                               AdminAuditLogService auditLogService) {
        this.newsRepository = newsRepository;
        this.translationService = translationService;
        this.auditLogService = auditLogService;
    }

    // ?sort= binds straight into the repository here, so restrict it to columns that are safe to expose.
    private static final Set<String> NEWS_SORT_FIELDS = Set.of("id", "title", "createdAt");
    private static final Sort NEWS_DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    @GetMapping("/news")
    public ResponseEntity<PageResponse<NewsResponse>> getAllNews(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(newsRepository.findAll(InputSanitizer.sanitizePageable(pageable, NEWS_SORT_FIELDS, NEWS_DEFAULT_SORT, 50, 200)).map(NewsResponse::from)));
    }

    @PostMapping("/news")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> createNews(@Valid @RequestBody NewsRequest request) {
        return auditLogService.captureCreated("news", "news_create",
                () -> createNewsInternal(request),
                body -> ((NewsResponse) body).id());
    }

    private ResponseEntity<?> createNewsInternal(NewsRequest request) {
        News news = new News();
        boolean autoTranslate = request.getAutoTranslate() == null || request.getAutoTranslate();

        String title = InputSanitizer.requiredPlainText(request.getTitle(), 200, "标题");

        if (autoTranslate) {
            String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.modules.content.domain.TibetanDictionary.Type.SENTENCE);
            if (tibetanTitle != null) {
                news.setTitleTibetan(tibetanTitle);
            }
        } else if (request.getTitleTibetan() != null) {
            news.setTitleTibetan(InputSanitizer.optionalPlainText(request.getTitleTibetan(), 200, "藏语标题"));
        }
        news.setTitle(title);

        String content = InputSanitizer.requiredTextBlock(request.getContent(), 20000, "内容");

        if (autoTranslate) {
            String tibetanContent = translationService.translateDescription(content);
            if (tibetanContent != null) {
                news.setContentTibetan(tibetanContent);
            }
        } else if (request.getContentTibetan() != null) {
            news.setContentTibetan(InputSanitizer.optionalTextBlock(request.getContentTibetan(), 20000, "藏语内容"));
        }
        news.setContent(content);

        if (request.getCategory() != null) {
            try {
                news.setCategory(News.Category.valueOf(request.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.getImageUrl() != null) {
            news.setImageUrl(safeImageUrl(request.getImageUrl(), "资讯图片"));
        }
        if (request.getViewCount() != null) {
            news.setViewCount(request.getViewCount());
        }

        newsRepository.save(news);
        return ResponseEntity.ok(NewsResponse.from(news));
    }

    @PutMapping("/news/{id}")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> updateNews(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        return auditLogService.capture("news", id, "news_update", () -> updateNewsInternal(id, request));
    }

    private ResponseEntity<?> updateNewsInternal(Long id, NewsRequest request) {
        Optional<News> newsOpt = newsRepository.findById(id);
        if (newsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        News news = newsOpt.get();
        boolean autoTranslate = request.getAutoTranslate() == null || request.getAutoTranslate();

        if (request.getTitle() != null) {
            String title = InputSanitizer.requiredPlainText(request.getTitle(), 200, "标题");
            news.setTitle(title);
            if (autoTranslate && (news.getTitleTibetan() == null || news.getTitleTibetan().isEmpty())) {
                String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.modules.content.domain.TibetanDictionary.Type.SENTENCE);
                if (tibetanTitle != null) {
                    news.setTitleTibetan(tibetanTitle);
                }
            }
        }

        if (request.getContent() != null) {
            String content = InputSanitizer.requiredTextBlock(request.getContent(), 20000, "内容");
            news.setContent(content);
            if (autoTranslate && (news.getContentTibetan() == null || news.getContentTibetan().isEmpty())) {
                String tibetanContent = translationService.translateDescription(content);
                if (tibetanContent != null) {
                    news.setContentTibetan(tibetanContent);
                }
            }
        }

        if (request.getTitleTibetan() != null) {
            news.setTitleTibetan(InputSanitizer.optionalPlainText(request.getTitleTibetan(), 200, "藏语标题"));
        }
        if (request.getContentTibetan() != null) {
            news.setContentTibetan(InputSanitizer.optionalTextBlock(request.getContentTibetan(), 20000, "藏语内容"));
        }
        if (request.getCategory() != null) {
            try {
                news.setCategory(News.Category.valueOf(request.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.getImageUrl() != null) {
            news.setImageUrl(safeImageUrl(request.getImageUrl(), "资讯图片"));
        }
        if (request.getViewCount() != null) {
            news.setViewCount(request.getViewCount());
        }

        newsRepository.save(news);
        return ResponseEntity.ok(NewsResponse.from(news));
    }

    @DeleteMapping("/news/{id}")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> deleteNews(@PathVariable Long id) {
        return auditLogService.capture("news", id, "news_delete", () -> deleteNewsInternal(id));
    }

    private ResponseEntity<?> deleteNewsInternal(Long id) {
        if (!newsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        newsRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
    public record NewsResponse(
            Long id,
            String title,
            String titleTibetan,
            String content,
            String contentTibetan,
            News.Category category,
            String imageUrl,
            Integer viewCount,
            LocalDateTime createdAt
    ) {
        private static NewsResponse from(News news) {
            return new NewsResponse(
                    news.getId(),
                    news.getTitle(),
                    news.getTitleTibetan(),
                    news.getContent(),
                    news.getContentTibetan(),
                    news.getCategory(),
                    news.getImageUrl(),
                    news.getViewCount(),
                    news.getCreatedAt());
        }
    }
}
