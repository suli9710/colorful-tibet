package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.web.dto.NewsRequest;
import com.tibet.tourism.modules.content.application.TibetanTranslationService;
import com.tibet.tourism.modules.content.domain.News;
import com.tibet.tourism.modules.content.domain.TibetanDictionary;
import com.tibet.tourism.modules.content.infra.NewsRepository;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
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
public class AdminNewsController {

    private final NewsRepository newsRepository;
    private final TibetanTranslationService translationService;

    public AdminNewsController(NewsRepository newsRepository,
                               TibetanTranslationService translationService) {
        this.newsRepository = newsRepository;
        this.translationService = translationService;
    }

    @GetMapping("/news")
    public ResponseEntity<Page<News>> getAllNews(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(newsRepository.findAll(pageable));
    }

    @PostMapping("/news")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> createNews(@Valid @RequestBody NewsRequest request) {
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
        return ResponseEntity.ok(news);
    }

    @PutMapping("/news/{id}")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> updateNews(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
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
        return ResponseEntity.ok(news);
    }

    @DeleteMapping("/news/{id}")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> deleteNews(@PathVariable Long id) {
        if (!newsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        newsRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}
