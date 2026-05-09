package com.tibet.tourism.controller;

import com.tibet.tourism.dto.NewsDTO;
import com.tibet.tourism.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping
    public Page<NewsDTO> getAllNews(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        return newsService.getAllNews(pageable)
                .map(news -> NewsDTO.fromEntity(news, locale));
    }
}
