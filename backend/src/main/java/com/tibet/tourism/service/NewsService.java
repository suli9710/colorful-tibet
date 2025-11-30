package com.tibet.tourism.service;

import com.tibet.tourism.entity.News;
import com.tibet.tourism.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    public List<News> getAllNews() {
        return newsRepository.findAll();
    }

    public News createNews(@NonNull News news) {
        return newsRepository.save(news);
    }

    public Optional<News> getNewsById(@NonNull Long id) {
        return newsRepository.findById(id);
    }

    @Transactional
    public News updateNews(@NonNull Long id, @NonNull News updatedNews) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        
        if (updatedNews.getTitle() != null) {
            news.setTitle(updatedNews.getTitle());
        }
        if (updatedNews.getContent() != null) {
            news.setContent(updatedNews.getContent());
        }
        if (updatedNews.getCategory() != null) {
            news.setCategory(updatedNews.getCategory());
        }
        if (updatedNews.getImageUrl() != null) {
            news.setImageUrl(updatedNews.getImageUrl());
        }
        if (updatedNews.getViewCount() != null) {
            news.setViewCount(updatedNews.getViewCount());
        }
        
        return newsRepository.save(news);
    }

    @Transactional
    public void deleteNews(@NonNull Long id) {
        if (!newsRepository.existsById(id)) {
            throw new RuntimeException("News not found with id: " + id);
        }
        newsRepository.deleteById(id);
    }
}
