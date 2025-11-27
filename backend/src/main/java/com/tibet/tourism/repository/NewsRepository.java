package com.tibet.tourism.repository;

import com.tibet.tourism.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
}
