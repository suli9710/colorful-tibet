package com.tibet.tourism.modules.content.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.content.application.NewsService;
import com.tibet.tourism.modules.content.domain.News;
import com.tibet.tourism.modules.content.web.dto.NewsDTO;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private static final Set<String> ALLOWED_NEWS_SORT_FIELDS = Set.of(
            "id", "title", "category", "viewCount", "createdAt");
    private static final Sort DEFAULT_NEWS_SORT = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));

    @Autowired
    private NewsService newsService;

    @GetMapping
    public PageResponse<NewsDTO> getAllNews(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_NEWS_SORT_FIELDS, DEFAULT_NEWS_SORT, 20, 100);
        News.Category safeCategory = parseCategory(category);
        String safeKeyword = InputSanitizer.optionalPlainText(keyword, 100, "keyword");
        Page<NewsDTO> newsPage = newsService.getNews(safeCategory, safeKeyword, safePageable)
                .map(news -> NewsDTO.fromEntity(news, locale));
        return PageResponse.from(newsPage);
    }

    private News.Category parseCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        try {
            return News.Category.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("category不合法");
        }
    }
}
