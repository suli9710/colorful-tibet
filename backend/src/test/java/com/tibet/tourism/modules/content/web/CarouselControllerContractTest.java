package com.tibet.tourism.modules.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.content.domain.Carousel;
import com.tibet.tourism.modules.content.infra.CarouselRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CarouselControllerContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private CarouselRepository carouselRepository;

    @Test
    void publicCarouselResponseExposesOnlyDisplayContract() throws Exception {
        Carousel carousel = new Carousel();
        carousel.setId(7L);
        carousel.setTitle("Hero");
        carousel.setSubtitle("Welcome");
        carousel.setTag("Featured");
        carousel.setImageUrl("/uploads/hero.jpg");
        carousel.setLinkUrl("/spots");
        carousel.setSortOrder(3);
        carousel.setActive(true);
        carousel.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        when(carouselRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(carousel));

        CarouselController controller = new CarouselController();
        ReflectionTestUtils.setField(controller, "carouselRepository", carouselRepository);

        String json = objectMapper.writeValueAsString(controller.getActiveCarousels().getBody());

        assertThat(json)
                .contains("\"title\":\"Hero\"")
                .contains("\"imageUrl\":\"/uploads/hero.jpg\"")
                .doesNotContain("\"id\"")
                .doesNotContain("\"active\"")
                .doesNotContain("\"sortOrder\"")
                .doesNotContain("\"createdAt\"");
    }
}
