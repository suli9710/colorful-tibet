package com.tibet.tourism.modules.content.web;
import com.tibet.tourism.modules.content.application.HeritageService;
import com.tibet.tourism.modules.content.web.dto.HeritageItemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/heritage")
public class HeritageController {

    @Autowired
    private HeritageService heritageService;

    @GetMapping
    public Page<HeritageItemDTO> getAllItems(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        if (category != null && !category.isEmpty()) {
            return heritageService.getItemsByCategory(category, pageable)
                    .map(item -> HeritageItemDTO.fromEntity(item, locale));
        }
        return heritageService.getAllItems(pageable)
                .map(item -> HeritageItemDTO.fromEntity(item, locale));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HeritageItemDTO> getItemById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        return heritageService.getItemById(id)
                .map(item -> ResponseEntity.ok(HeritageItemDTO.fromEntity(item, locale)))
                .orElse(ResponseEntity.notFound().build());
    }
}
