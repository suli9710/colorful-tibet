package com.tibet.tourism.modules.spot.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.spot.application.ScenicSpotService;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import java.math.BigDecimal;
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
class ScenicSpotControllerPageResponseContractTest {

    @Mock
    private ScenicSpotService scenicSpotService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ScenicSpotController controller = new ScenicSpotController();
        ReflectionTestUtils.setField(controller, "scenicSpotService", scenicSpotService);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void spotListReturnsStablePageEnvelope() throws Exception {
        when(scenicSpotService.getAllSpots(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(scenicSpot()), PageRequest.of(1, 2), 5));

        ResultActions result = mockMvc.perform(get("/api/spots?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(11))
                .andExpect(jsonPath("$.content[0].name").value("Potala Palace"));

        expectStablePageEnvelope(result, 1, 2, 5, 3);
    }

    @Test
    void spotSearchReturnsStablePageEnvelope() throws Exception {
        when(scenicSpotService.searchSpots(eq("potala"), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(scenicSpot()), PageRequest.of(0, 4), 1));

        ResultActions result = mockMvc.perform(get("/api/spots/search?keyword=potala&size=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(11));

        expectStablePageEnvelope(result, 0, 4, 1, 1);
    }

    @Test
    void spotSearchAcceptsCategoryFilter() throws Exception {
        when(scenicSpotService.searchSpots(eq("temple"), eq(ScenicSpot.Category.CULTURAL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(scenicSpot()), PageRequest.of(0, 5), 1));

        ResultActions result = mockMvc.perform(get("/api/spots/search?keyword=temple&category=CULTURAL&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("CULTURAL"));

        expectStablePageEnvelope(result, 0, 5, 1, 1);
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

    private static ScenicSpot scenicSpot() {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(11L);
        spot.setName("Potala Palace");
        spot.setDescription("Public description");
        spot.setImageUrl("/spots/potala.jpg");
        spot.setAltitude("3700m");
        spot.setLocation("Lhasa");
        spot.setCategory(ScenicSpot.Category.CULTURAL);
        spot.setTicketPrice(BigDecimal.valueOf(200));
        spot.setRating(BigDecimal.valueOf(4.8));
        spot.setLatitude(BigDecimal.valueOf(29.6578));
        spot.setLongitude(BigDecimal.valueOf(91.1169));
        spot.setVisitCount(12345);
        return spot;
    }
}
