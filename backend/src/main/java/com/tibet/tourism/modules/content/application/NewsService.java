package com.tibet.tourism.modules.content.application;
import com.tibet.tourism.common.config.CacheConfig;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.content.domain.News;
import com.tibet.tourism.modules.content.infra.NewsRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    public List<News> getAllNews() {
        return newsRepository.findAll(PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    public Page<News> getAllNews(Pageable pageable) {
        return newsRepository.findAll(pageable);
    }

    public Page<News> getNews(News.Category category, String keyword, Pageable pageable) {
        if (category == null && keyword == null) {
            return newsRepository.findAll(pageable);
        }
        return newsRepository.search(category, keyword, pageable);
    }

    @CacheEvict(value = CacheConfig.NEWS_CACHE, allEntries = true)
    public News createNews(@NonNull News news) {
        return newsRepository.save(news);
    }

    public Optional<News> getNewsById(@NonNull Long id) {
        return newsRepository.findById(id);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.NEWS_CACHE, allEntries = true)
    public News updateNews(@NonNull Long id, @NonNull News updatedNews) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        
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
    @CacheEvict(value = CacheConfig.NEWS_CACHE, allEntries = true)
    public void deleteNews(@NonNull Long id) {
        if (!newsRepository.existsById(id)) {
            throw new ResourceNotFoundException("News not found with id: " + id);
        }
        newsRepository.deleteById(id);
    }
}
