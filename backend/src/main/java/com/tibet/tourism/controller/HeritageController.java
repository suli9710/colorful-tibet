package com.tibet.tourism.controller;

import com.tibet.tourism.entity.HeritageItem;
import com.tibet.tourism.service.HeritageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/heritage")
@CrossOrigin(origins = "*")
public class HeritageController {

    @Autowired
    private HeritageService heritageService;

    @GetMapping
    public List<HeritageItem> getAllItems() {
        return heritageService.getAllItems();
    }
}
