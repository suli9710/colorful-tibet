package com.tibet.tourism.modules.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.common.error.ApiExceptionHandler;
import com.tibet.tourism.modules.content.application.TibetanTranslationService;
import com.tibet.tourism.modules.content.domain.TibetanDictionary;
import com.tibet.tourism.modules.content.infra.TibetanDictionaryRepository;
import com.tibet.tourism.modules.content.web.dto.TibetanDictionaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class TibetanDictionaryControllerContractTest {

    @Mock
    private TibetanDictionaryRepository dictionaryRepository;

    @Mock
    private TibetanTranslationService translationService;

    private TibetanDictionaryController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new TibetanDictionaryController(dictionaryRepository, translationService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listReturnsResponseDtosInsteadOfDictionaryEntities() throws Exception {
        TibetanDictionary entry = dictionaryEntry();
        when(dictionaryRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        List<TibetanDictionaryResponse> response = controller.getAllEntries(null, null).getBody();

        assertThat(response).containsExactly(TibetanDictionaryResponse.from(entry));

        mockMvc.perform(get("/api/admin/tibetan-dictionary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].chineseText").value("hello"))
                .andExpect(jsonPath("$[0].tibetanText").value("bod"))
                .andExpect(jsonPath("$[0].type").value("WORD"))
                .andExpect(jsonPath("$[0].usageCount").value(3))
                .andExpect(content().string(not(containsString("hibernateLazyInitializer"))));
    }

    @Test
    void createUsesValidatedDtoAndIgnoresClientControlledEntityFields() throws Exception {
        when(dictionaryRepository.findByChineseTextAndType("hello", TibetanDictionary.Type.PHRASE))
                .thenReturn(Optional.empty());
        when(dictionaryRepository.save(any(TibetanDictionary.class))).thenAnswer(invocation -> {
            TibetanDictionary submitted = invocation.getArgument(0);
            TibetanDictionary saved = new TibetanDictionary();
            saved.setId(5L);
            saved.setChineseText(submitted.getChineseText());
            saved.setTibetanText(submitted.getTibetanText());
            saved.setType(submitted.getType());
            saved.setUsageCount(submitted.getUsageCount());
            saved.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
            saved.setUpdatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
            return saved;
        });

        mockMvc.perform(post("/api/admin/tibetan-dictionary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 999,
                                  "chineseText": "  hello  ",
                                  "tibetanText": "  bod text  ",
                                  "type": "phrase",
                                  "usageCount": 12,
                                  "createdAt": "2030-01-01T00:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.chineseText").value("hello"))
                .andExpect(jsonPath("$.tibetanText").value("bod text"))
                .andExpect(jsonPath("$.type").value("PHRASE"))
                .andExpect(jsonPath("$.usageCount").value(12));

        ArgumentCaptor<TibetanDictionary> savedEntry = ArgumentCaptor.forClass(TibetanDictionary.class);
        verify(dictionaryRepository).save(savedEntry.capture());
        assertThat(savedEntry.getValue().getId()).isNull();
        assertThat(savedEntry.getValue().getCreatedAt()).isNull();
        assertThat(savedEntry.getValue().getChineseText()).isEqualTo("hello");
        assertThat(savedEntry.getValue().getTibetanText()).isEqualTo("bod text");
        assertThat(savedEntry.getValue().getType()).isEqualTo(TibetanDictionary.Type.PHRASE);
    }

    @Test
    void invalidCreateBodyReturnsStableBadRequestWithoutFieldDetails() throws Exception {
        mockMvc.perform(post("/api/admin/tibetan-dictionary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chineseText": " ",
                                  "tibetanText": "bod"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(content().string(not(containsString("chineseText"))));

        verify(dictionaryRepository, never()).save(any(TibetanDictionary.class));
    }

    @Test
    void invalidDictionaryTypeReturnsStableBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/tibetan-dictionary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chineseText": "hello",
                                  "tibetanText": "bod",
                                  "type": "ENTITY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(content().string(not(containsString("ENTITY"))));

        verify(dictionaryRepository, never()).save(any(TibetanDictionary.class));
    }

    @Test
    void updateKeepsPartialUpdateSemanticsAndReturnsResponseDto() throws Exception {
        TibetanDictionary existing = dictionaryEntry();
        when(dictionaryRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.save(any(TibetanDictionary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/admin/tibetan-dictionary/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chineseText": "  greeting  ",
                                  "type": "sentence"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.chineseText").value("greeting"))
                .andExpect(jsonPath("$.tibetanText").value("bod"))
                .andExpect(jsonPath("$.type").value("SENTENCE"));

        assertThat(existing.getChineseText()).isEqualTo("greeting");
        assertThat(existing.getTibetanText()).isEqualTo("bod");
        assertThat(existing.getType()).isEqualTo(TibetanDictionary.Type.SENTENCE);
    }

    @Test
    void translateUsesRequestAndResponseDtos() throws Exception {
        when(translationService.translate("hello")).thenReturn("bod");

        mockMvc.perform(post("/api/admin/tibetan-dictionary/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chineseText": "  hello  ",
                                  "type": "word"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chineseText").value("hello"))
                .andExpect(jsonPath("$.tibetanText").value("bod"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void batchAddValidatesAndSanitizesTranslationsBeforeServiceCall() throws Exception {
        TibetanDictionary saved = dictionaryEntry();
        saved.setType(TibetanDictionary.Type.PHRASE);
        when(translationService.batchAddEntries(any(), eq(TibetanDictionary.Type.PHRASE)))
                .thenReturn(List.of(saved));

        mockMvc.perform(post("/api/admin/tibetan-dictionary/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "translations": {
                                    " hello ": " bod "
                                  },
                                  "type": "phrase"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Batch add succeeded"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.entries[0].type").value("PHRASE"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> translations = ArgumentCaptor.forClass(Map.class);
        verify(translationService).batchAddEntries(translations.capture(), eq(TibetanDictionary.Type.PHRASE));
        assertThat(translations.getValue()).containsEntry("hello", "bod");
    }

    @Test
    void invalidSearchTypeReturnsStableBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/tibetan-dictionary/search")
                        .param("keyword", "hello")
                        .param("searchType", "entity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(content().string(not(containsString("entity"))));
    }

    private static TibetanDictionary dictionaryEntry() {
        TibetanDictionary entry = new TibetanDictionary();
        entry.setId(7L);
        entry.setChineseText("hello");
        entry.setTibetanText("bod");
        entry.setType(TibetanDictionary.Type.WORD);
        entry.setUsageCount(3);
        entry.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        entry.setUpdatedAt(LocalDateTime.parse("2026-01-03T04:05:06"));
        return entry;
    }
}
