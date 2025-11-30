package com.tibet.tourism.controller;

import com.tibet.tourism.entity.News;
import com.tibet.tourism.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping
    public List<News> getAllNews(@RequestParam(required = false, defaultValue = "zh") String locale) {
        List<News> newsList = newsService.getAllNews();
        if ("bo".equals(locale)) {
            newsList.forEach(news -> {
                if (news.getTitleTibetan() != null && !news.getTitleTibetan().isEmpty()) {
                    news.setTitle(news.getTitleTibetan());
                }
                if (news.getContentTibetan() != null && !news.getContentTibetan().isEmpty()) {
                    news.setContent(news.getContentTibetan());
                }
            });
        }
        return newsList;
    }
}
