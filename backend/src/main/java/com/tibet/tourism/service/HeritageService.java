package com.tibet.tourism.service;

import com.tibet.tourism.entity.HeritageItem;
import com.tibet.tourism.repository.HeritageItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HeritageService {

    @Autowired
    private HeritageItemRepository heritageItemRepository;

    public List<HeritageItem> getAllItems() {
        return heritageItemRepository.findAll();
    }
}
