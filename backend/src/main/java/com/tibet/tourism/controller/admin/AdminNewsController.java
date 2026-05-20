package com.tibet.tourism.controller.admin;

import com.tibet.tourism.entity.News;
import com.tibet.tourism.repository.NewsRepository;
import com.tibet.tourism.security.InputSanitizer;
import com.tibet.tourism.service.TibetanTranslationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import static com.tibet.tourism.util.RequestParseUtils.safeImageUrl;

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
    public ResponseEntity<?> createNews(@RequestBody Map<String, Object> request) {
        News news = new News();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        String title = InputSanitizer.requiredPlainText((String) request.get("title"), 200, "标题");

        if (autoTranslate) {
            String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE);
            if (tibetanTitle != null) {
                news.setTitleTibetan(tibetanTitle);
            }
        } else if (request.containsKey("titleTibetan")) {
            news.setTitleTibetan(InputSanitizer.optionalPlainText((String) request.get("titleTibetan"), 200, "藏语标题"));
        }
        news.setTitle(title);

        String content = InputSanitizer.requiredTextBlock((String) request.get("content"), 20000, "内容");

        if (autoTranslate) {
            String tibetanContent = translationService.translateDescription(content);
            if (tibetanContent != null) {
                news.setContentTibetan(tibetanContent);
            }
        } else if (request.containsKey("contentTibetan")) {
            news.setContentTibetan(InputSanitizer.optionalTextBlock((String) request.get("contentTibetan"), 20000, "藏语内容"));
        }
        news.setContent(content);

        if (request.containsKey("category")) {
            try {
                news.setCategory(News.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("imageUrl")) {
            news.setImageUrl(safeImageUrl(request.get("imageUrl"), "资讯图片"));
        }
        if (request.containsKey("viewCount")) {
            Object viewCountObj = request.get("viewCount");
            if (viewCountObj instanceof Number) {
                news.setViewCount(((Number) viewCountObj).intValue());
            }
        }

        newsRepository.save(news);
        return ResponseEntity.ok(news);
    }

    @PutMapping("/news/{id}")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> updateNews(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<News> newsOpt = newsRepository.findById(id);
        if (newsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        News news = newsOpt.get();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        if (request.containsKey("title")) {
            String title = InputSanitizer.requiredPlainText((String) request.get("title"), 200, "标题");
            news.setTitle(title);
            if (autoTranslate && (news.getTitleTibetan() == null || news.getTitleTibetan().isEmpty())) {
                String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE);
                if (tibetanTitle != null) {
                    news.setTitleTibetan(tibetanTitle);
                }
            }
        }

        if (request.containsKey("content")) {
            String content = InputSanitizer.requiredTextBlock((String) request.get("content"), 20000, "内容");
            news.setContent(content);
            if (autoTranslate && (news.getContentTibetan() == null || news.getContentTibetan().isEmpty())) {
                String tibetanContent = translationService.translateDescription(content);
                if (tibetanContent != null) {
                    news.setContentTibetan(tibetanContent);
                }
            }
        }

        if (request.containsKey("titleTibetan")) {
            news.setTitleTibetan(InputSanitizer.optionalPlainText((String) request.get("titleTibetan"), 200, "藏语标题"));
        }
        if (request.containsKey("contentTibetan")) {
            news.setContentTibetan(InputSanitizer.optionalTextBlock((String) request.get("contentTibetan"), 20000, "藏语内容"));
        }
        if (request.containsKey("category")) {
            try {
                news.setCategory(News.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("imageUrl")) {
            news.setImageUrl(safeImageUrl(request.get("imageUrl"), "资讯图片"));
        }
        if (request.containsKey("viewCount")) {
            Object viewCountObj = request.get("viewCount");
            if (viewCountObj instanceof Number) {
                news.setViewCount(((Number) viewCountObj).intValue());
            }
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
