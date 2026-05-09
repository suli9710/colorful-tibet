package com.tibet.tourism.service;

import com.tibet.tourism.entity.HeritageItem;
import com.tibet.tourism.repository.HeritageItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class HeritageService {

    @Autowired
    private HeritageItemRepository heritageItemRepository;

    public List<HeritageItem> getAllItems() {
        return heritageItemRepository.findAll();
    }

    public Page<HeritageItem> getAllItems(Pageable pageable) {
        return heritageItemRepository.findAll(pageable);
    }

    public Optional<HeritageItem> getItemById(Long id) {
        return heritageItemRepository.findById(id);
    }

    public Page<HeritageItem> getItemsByCategory(String category, Pageable pageable) {
        return heritageItemRepository.findByCategory(category, pageable);
    }
}
