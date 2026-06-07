package com.tibet.tourism.modules.content.web;
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
    public Page<NewsDTO> getAllNews(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_NEWS_SORT_FIELDS, DEFAULT_NEWS_SORT, 20, 100);
        return newsService.getAllNews(safePageable)
                .map(news -> NewsDTO.fromEntity(news, locale));
    }
}
