package com.tibet.tourism.modules.content.web;

import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.content.application.TibetanTranslationService;
import com.tibet.tourism.modules.content.domain.TibetanDictionary;
import com.tibet.tourism.modules.content.infra.TibetanDictionaryRepository;
import com.tibet.tourism.modules.content.web.dto.DictionaryBatchAddRequest;
import com.tibet.tourism.modules.content.web.dto.DictionaryBatchAddResponse;
import com.tibet.tourism.modules.content.web.dto.DictionaryMessageResponse;
import com.tibet.tourism.modules.content.web.dto.TibetanDictionaryRequest;
import com.tibet.tourism.modules.content.web.dto.TibetanDictionaryResponse;
import com.tibet.tourism.modules.content.web.dto.TibetanDictionaryUpdateRequest;
import com.tibet.tourism.modules.content.web.dto.TibetanTranslationRequest;
import com.tibet.tourism.modules.content.web.dto.TibetanTranslationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/tibetan-dictionary")
@PreAuthorize("hasRole('ADMIN')")
public class TibetanDictionaryController {

    private static final int DICTIONARY_RESULT_LIMIT = 1000;
    private static final int DICTIONARY_TYPE_MAX_LENGTH = 20;
    private static final int CHINESE_TEXT_MAX_LENGTH = 500;
    private static final int TIBETAN_TEXT_MAX_LENGTH = 10000;
    private static final int SEARCH_TYPE_MAX_LENGTH = 20;

    private final TibetanDictionaryRepository dictionaryRepository;
    private final TibetanTranslationService translationService;
    private final AdminAuditLogService auditLogService;

    public TibetanDictionaryController(TibetanDictionaryRepository dictionaryRepository,
                                       TibetanTranslationService translationService,
                                       AdminAuditLogService auditLogService) {
        this.dictionaryRepository = dictionaryRepository;
        this.translationService = translationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<TibetanDictionaryResponse>> getAllEntries(
            @RequestParam(required = false) @Size(max = DICTIONARY_TYPE_MAX_LENGTH) String type,
            @RequestParam(required = false) @Size(max = CHINESE_TEXT_MAX_LENGTH) String keyword) {
        Optional<TibetanDictionary.Type> requestedType = parseDictionaryType(type);
        String normalizedKeyword = InputSanitizer.optionalPlainText(
                keyword, CHINESE_TEXT_MAX_LENGTH, "keyword");

        List<TibetanDictionary> entries;
        if (normalizedKeyword != null) {
            entries = dictionaryRepository.searchByChineseText(normalizedKeyword, dictionaryPage());
        } else if (requestedType.isPresent()) {
            entries = dictionaryRepository.findByType(requestedType.get(), dictionaryPage());
        } else {
            entries = dictionaryRepository.findAll(dictionaryPage()).getContent();
        }
        return ResponseEntity.ok(toResponses(entries));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TibetanDictionaryResponse> getEntryById(@PathVariable @Positive Long id) {
        return dictionaryRepository.findById(id)
                .map(TibetanDictionaryResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@Valid @RequestBody TibetanDictionaryRequest request) {
        return auditLogService.captureCreated("tibetan_dictionary", "tibetan_dictionary_create",
                () -> createEntryInternal(request),
                body -> ((TibetanDictionaryResponse) body).id());
    }

    private ResponseEntity<?> createEntryInternal(TibetanDictionaryRequest request) {
        String chineseText = InputSanitizer.requiredPlainText(
                request.getChineseText(), CHINESE_TEXT_MAX_LENGTH, "chineseText");
        String tibetanText = InputSanitizer.requiredTextBlock(
                request.getTibetanText(), TIBETAN_TEXT_MAX_LENGTH, "tibetanText");
        TibetanDictionary.Type type = parseDictionaryType(request.getType())
                .orElse(TibetanDictionary.Type.WORD);

        Optional<TibetanDictionary> existing = dictionaryRepository
                .findByChineseTextAndType(chineseText, type);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dictionary entry already exists"));
        }

        TibetanDictionary entry = new TibetanDictionary();
        entry.setChineseText(chineseText);
        entry.setTibetanText(tibetanText);
        entry.setType(type);
        entry.setUsageCount(request.getUsageCount() == null ? 0 : request.getUsageCount());

        TibetanDictionary saved = dictionaryRepository.save(entry);
        return ResponseEntity.ok(TibetanDictionaryResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable @Positive Long id,
                                         @Valid @RequestBody TibetanDictionaryUpdateRequest request) {
        return auditLogService.capture("tibetan_dictionary", id, "tibetan_dictionary_update",
                () -> updateEntryInternal(id, request));
    }

    private ResponseEntity<?> updateEntryInternal(Long id, TibetanDictionaryUpdateRequest request) {
        Optional<TibetanDictionary> existing = dictionaryRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TibetanDictionary existingEntry = existing.get();
        if (request.getChineseText() != null) {
            existingEntry.setChineseText(InputSanitizer.requiredPlainText(
                    request.getChineseText(), CHINESE_TEXT_MAX_LENGTH, "chineseText"));
        }
        if (request.getTibetanText() != null) {
            existingEntry.setTibetanText(InputSanitizer.requiredTextBlock(
                    request.getTibetanText(), TIBETAN_TEXT_MAX_LENGTH, "tibetanText"));
        }
        if (request.getType() != null) {
            existingEntry.setType(parseDictionaryType(request.getType())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid dictionary type")));
        }

        TibetanDictionary saved = dictionaryRepository.save(existingEntry);
        return ResponseEntity.ok(TibetanDictionaryResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DictionaryMessageResponse> deleteEntry(@PathVariable @Positive Long id) {
        return auditLogService.capture("tibetan_dictionary", id, "tibetan_dictionary_delete",
                () -> deleteEntryInternal(id));
    }

    private ResponseEntity<DictionaryMessageResponse> deleteEntryInternal(Long id) {
        if (!dictionaryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dictionaryRepository.deleteById(id);
        return ResponseEntity.ok(new DictionaryMessageResponse("Deleted successfully"));
    }

    @PostMapping("/translate")
    public ResponseEntity<TibetanTranslationResponse> translate(
            @Valid @RequestBody TibetanTranslationRequest request) {
        parseDictionaryType(request.getType());
        String chineseText = InputSanitizer.requiredPlainText(
                request.getChineseText(), CHINESE_TEXT_MAX_LENGTH, "chineseText");

        String tibetanText = translationService.translate(chineseText);
        if (tibetanText == null) {
            return ResponseEntity.ok(TibetanTranslationResponse.notFound(chineseText));
        }

        return ResponseEntity.ok(TibetanTranslationResponse.translated(chineseText, tibetanText));
    }

    @PostMapping("/batch")
    public ResponseEntity<DictionaryBatchAddResponse> batchAdd(
            @Valid @RequestBody DictionaryBatchAddRequest request) {
        return auditLogService.capture("tibetan_dictionary", null, "tibetan_dictionary_batch_add",
                () -> batchAddInternal(request));
    }

    private ResponseEntity<DictionaryBatchAddResponse> batchAddInternal(DictionaryBatchAddRequest request) {
        TibetanDictionary.Type type = parseDictionaryType(request.getType())
                .orElse(TibetanDictionary.Type.WORD);
        Map<String, String> translations = sanitizeTranslations(request.getTranslations());

        List<TibetanDictionary> saved = translationService.batchAddEntries(translations, type);
        return ResponseEntity.ok(new DictionaryBatchAddResponse(
                "Batch add succeeded",
                saved.size(),
                toResponses(saved)));
    }

    @PostMapping("/initialize")
    public ResponseEntity<DictionaryMessageResponse> initialize() {
        return auditLogService.capture("tibetan_dictionary", null, "tibetan_dictionary_initialize",
                this::initializeInternal);
    }

    private ResponseEntity<DictionaryMessageResponse> initializeInternal() {
        translationService.initializeDefaultDictionary();
        return ResponseEntity.ok(new DictionaryMessageResponse("Default dictionary initialized"));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TibetanDictionaryResponse>> search(
            @RequestParam @NotBlank @Size(max = CHINESE_TEXT_MAX_LENGTH) String keyword,
            @RequestParam(required = false, defaultValue = "chinese")
            @Size(max = SEARCH_TYPE_MAX_LENGTH) String searchType) {
        String normalizedKeyword = InputSanitizer.requiredPlainText(
                keyword, CHINESE_TEXT_MAX_LENGTH, "keyword");

        List<TibetanDictionary> results;
        if (isTibetanSearch(searchType)) {
            results = dictionaryRepository.searchByTibetanText(normalizedKeyword, dictionaryPage());
        } else {
            results = dictionaryRepository.searchByChineseText(normalizedKeyword, dictionaryPage());
        }
        return ResponseEntity.ok(toResponses(results));
    }

    private Optional<TibetanDictionary.Type> parseDictionaryType(String value) {
        String normalized = InputSanitizer.optionalPlainText(value, DICTIONARY_TYPE_MAX_LENGTH, "type");
        if (normalized == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(TibetanDictionary.Type.valueOf(normalized.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid dictionary type");
        }
    }

    private boolean isTibetanSearch(String searchType) {
        String normalized = InputSanitizer.optionalPlainText(
                searchType, SEARCH_TYPE_MAX_LENGTH, "searchType");
        if (normalized == null || "chinese".equalsIgnoreCase(normalized)) {
            return false;
        }
        if ("tibetan".equalsIgnoreCase(normalized)) {
            return true;
        }
        throw new IllegalArgumentException("Invalid search type");
    }

    private Map<String, String> sanitizeTranslations(Map<String, String> translations) {
        if (translations == null || translations.isEmpty()) {
            throw new IllegalArgumentException("Translations are required");
        }

        Map<String, String> sanitized = new LinkedHashMap<>();
        translations.forEach((chineseText, tibetanText) -> {
            String normalizedChinese = InputSanitizer.requiredPlainText(
                    chineseText, CHINESE_TEXT_MAX_LENGTH, "chineseText");
            String normalizedTibetan = InputSanitizer.requiredTextBlock(
                    tibetanText, TIBETAN_TEXT_MAX_LENGTH, "tibetanText");
            if (sanitized.putIfAbsent(normalizedChinese, normalizedTibetan) != null) {
                throw new IllegalArgumentException("Duplicate dictionary translation");
            }
        });
        return sanitized;
    }

    private List<TibetanDictionaryResponse> toResponses(List<TibetanDictionary> entries) {
        return entries.stream()
                .map(TibetanDictionaryResponse::from)
                .toList();
    }

    private PageRequest dictionaryPage() {
        return PageRequest.of(0, DICTIONARY_RESULT_LIMIT,
                Sort.by(Sort.Direction.DESC, "usageCount").and(Sort.by("id")));
    }
}
