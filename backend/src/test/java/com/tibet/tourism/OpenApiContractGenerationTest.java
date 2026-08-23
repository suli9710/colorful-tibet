package com.tibet.tourism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.config.DataSeeder;
import com.tibet.tourism.modules.ai.application.AiRouteRecordService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = OpenApiContractGenerationTest.OpenApiTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "app.seed.content.enabled=false",
        "app.seed.demo-users.enabled=false",
        "app.security.public-docs-enabled=true",
        "app.security.require-strong-secrets=false",
        "app.security.rate-limit.redis-enabled=false",
        "app.security.brute-force.redis-enabled=false",
        "springdoc.api-docs.enabled=true",
        "recommendation.item-similarity.preload-on-startup=false",
        "scrapling.health.enabled=false"
})
class OpenApiContractGenerationTest {

    private static final Path OUTPUT = Path.of("target", "openapi", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataSeeder dataSeeder;

    @MockBean
    private AiRouteRecordService aiRouteRecordService;

    @Test
    void exportsNonEmptyOpenApiContractForFrontendTypeGeneration() throws Exception {
        byte[] document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        JsonNode root = objectMapper.readTree(document);
        JsonNode paths = root.path("paths");
        JsonNode schemas = root.path("components").path("schemas");
        assertThat(root.path("openapi").asText()).startsWith("3.");
        assertThat(paths.isObject()).isTrue();
        assertThat(paths.size()).isGreaterThan(25);
        assertThat(paths.has("/api/spots/{id}")).isTrue();
        assertThat(paths.has("/api/auth/login")).isTrue();
        assertThat(schemas.isObject()).isTrue();
        assertThat(schemas.size()).isGreaterThan(25);

        Files.createDirectories(OUTPUT.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.toFile(), root);
        assertThat(Files.size(OUTPUT)).isGreaterThan(10_000L);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = "com.tibet.tourism",
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = TibetTourismApplication.class)
            })
    static class OpenApiTestApplication {
    }
}
