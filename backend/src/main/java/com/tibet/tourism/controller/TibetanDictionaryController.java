package com.tibet.tourism.controller;

import com.tibet.tourism.entity.TibetanDictionary;
import com.tibet.tourism.repository.TibetanDictionaryRepository;
import com.tibet.tourism.service.TibetanTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 藏语词典管理控制器
 * 提供词典的CRUD操作和翻译功能
 */
@RestController
@RequestMapping("/api/admin/tibetan-dictionary")
@CrossOrigin(origins = "*")
public class TibetanDictionaryController {

    @Autowired
    private TibetanDictionaryRepository dictionaryRepository;

    @Autowired
    private TibetanTranslationService translationService;

    /**
     * 获取所有词典条目
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TibetanDictionary>> getAllEntries(
            @RequestParam(required = false) TibetanDictionary.Type type,
            @RequestParam(required = false) String keyword) {
        List<TibetanDictionary> entries;
        if (keyword != null && !keyword.trim().isEmpty()) {
            entries = dictionaryRepository.searchByChineseText(keyword);
        } else if (type != null) {
            entries = dictionaryRepository.findByType(type);
        } else {
            entries = dictionaryRepository.findAll();
        }
        return ResponseEntity.ok(entries);
    }

    /**
     * 根据ID获取词典条目
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TibetanDictionary> getEntryById(@PathVariable Long id) {
        Optional<TibetanDictionary> entry = dictionaryRepository.findById(id);
        return entry.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建新的词典条目
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEntry(@RequestBody TibetanDictionary entry) {
        if (entry.getChineseText() == null || entry.getChineseText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "中文文本不能为空"));
        }
        if (entry.getTibetanText() == null || entry.getTibetanText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "藏语文本不能为空"));
        }
        if (entry.getType() == null) {
            entry.setType(TibetanDictionary.Type.WORD);
        }

        // 检查是否已存在
        Optional<TibetanDictionary> existing = dictionaryRepository
                .findByChineseTextAndType(entry.getChineseText().trim(), entry.getType());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "该条目已存在"));
        }

        TibetanDictionary saved = dictionaryRepository.save(entry);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新词典条目
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEntry(@PathVariable Long id, @RequestBody TibetanDictionary entry) {
        Optional<TibetanDictionary> existing = dictionaryRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TibetanDictionary existingEntry = existing.get();
        if (entry.getChineseText() != null) {
            existingEntry.setChineseText(entry.getChineseText().trim());
        }
        if (entry.getTibetanText() != null) {
            existingEntry.setTibetanText(entry.getTibetanText().trim());
        }
        if (entry.getType() != null) {
            existingEntry.setType(entry.getType());
        }

        TibetanDictionary saved = dictionaryRepository.save(existingEntry);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除词典条目
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id) {
        if (!dictionaryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dictionaryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    /**
     * 翻译文本
     */
    @PostMapping("/translate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> translate(@RequestBody Map<String, String> request) {
        String chineseText = request.get("chineseText");
        String typeStr = request.get("type");
        
        if (chineseText == null || chineseText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "中文文本不能为空"));
        }

        String tibetanText = translationService.translate(chineseText);
        if (tibetanText == null) {
            return ResponseEntity.ok(Map.of(
                    "chineseText", chineseText,
                    "tibetanText", null,
                    "message", "未找到翻译，请手动添加词典条目"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "chineseText", chineseText,
                "tibetanText", tibetanText
        ));
    }

    /**
     * 批量添加词典条目
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchAdd(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, String> translations = (Map<String, String>) request.get("translations");
        String typeStr = (String) request.get("type");

        if (translations == null || translations.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "翻译数据不能为空"));
        }

        TibetanDictionary.Type type = TibetanDictionary.Type.WORD;
        if (typeStr != null) {
            try {
                type = TibetanDictionary.Type.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 使用默认值
            }
        }

        List<TibetanDictionary> saved = translationService.batchAddEntries(translations, type);
        return ResponseEntity.ok(Map.of(
                "message", "批量添加成功",
                "count", saved.size(),
                "entries", saved
        ));
    }

    /**
     * 初始化默认词典
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initialize() {
        translationService.initializeDefaultDictionary();
        return ResponseEntity.ok(Map.of("message", "默认词典初始化成功"));
    }

    /**
     * 搜索词典（支持中文和藏语）
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TibetanDictionary>> search(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "chinese") String searchType) {
        List<TibetanDictionary> results;
        if ("tibetan".equals(searchType)) {
            results = dictionaryRepository.searchByTibetanText(keyword);
        } else {
            results = dictionaryRepository.searchByChineseText(keyword);
        }
        return ResponseEntity.ok(results);
    }
}

