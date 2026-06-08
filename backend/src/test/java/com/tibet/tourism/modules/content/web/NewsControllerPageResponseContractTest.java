package com.tibet.tourism.modules.content.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.content.application.NewsService;
import com.tibet.tourism.modules.content.domain.News;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NewsControllerPageResponseContractTest {

    @Mock
    private NewsService newsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NewsController controller = new NewsController();
        ReflectionTestUtils.setField(controller, "newsService", newsService);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void newsListReturnsStablePageEnvelope() throws Exception {
        when(newsService.getAllNews(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(news()), PageRequest.of(1, 2), 5));

        ResultActions result = mockMvc.perform(get("/api/news?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].title").value("Travel notice"));

        expectStablePageEnvelope(result, 1, 2, 5, 3);
    }

    private static void expectStablePageEnvelope(
            ResultActions result, int page, int size, long totalElements, int totalPages) throws Exception {
        result.andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(totalElements))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.numberOfElements").doesNotExist())
                .andExpect(jsonPath("$.first").doesNotExist())
                .andExpect(jsonPath("$.last").doesNotExist())
                .andExpect(jsonPath("$.empty").doesNotExist());
    }

    private static News news() {
        News news = new News();
        news.setId(42L);
        news.setTitle("Travel notice");
        news.setContent("Road updates");
        news.setCategory(News.Category.NOTICE);
        news.setImageUrl("/images/news.jpg");
        news.setViewCount(7);
        news.setCreatedAt(LocalDateTime.parse("2026-06-01T10:15:30"));
        return news;
    }
}
