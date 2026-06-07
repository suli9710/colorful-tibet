package com.tibet.tourism.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebMvcConfigTest {

    @Test
    void ignoresWildcardCorsOriginsWhenCredentialsAreEnabled() {
        WebMvcConfig config = new WebMvcConfig();
        ReflectionTestUtils.setField(
                config,
                "allowedOrigins",
                "https://lengzhehao.xin,https://*.example.com,https://example.com*,*");
        CapturingCorsRegistry registry = new CapturingCorsRegistry();

        config.addCorsMappings(registry);

        assertThat(registry.registration).isNotNull();
        org.springframework.web.cors.CorsConfiguration corsConfiguration =
                ReflectionTestUtils.invokeMethod(registry.registration, "getCorsConfiguration");

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOriginPatterns())
                .containsExactly("https://lengzhehao.xin");
    }

    private static final class CapturingCorsRegistry extends CorsRegistry {
        private CorsRegistration registration;

        @Override
        public CorsRegistration addMapping(String pathPattern) {
            registration = super.addMapping(pathPattern);
            return registration;
        }
    }
}
