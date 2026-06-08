package com.tibet.tourism.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.CacheControl;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class WebMvcConfigResourceHandlerTest {

    @Test
    void uploadResourcesArePrivateAndRevalidated() {
        WebMvcConfig config = new WebMvcConfig();
        ReflectionTestUtils.setField(config, "uploadDir", "uploads");
        CapturingResourceHandlerRegistry registry = new CapturingResourceHandlerRegistry();

        config.addResourceHandlers(registry);

        CapturingResourceHandlerRegistration uploadRegistration = registry.registrationFor("/uploads/**");
        assertThat(uploadRegistration.locations()).hasSize(1);
        assertThat(uploadRegistration.cacheControlHeader())
                .contains("no-cache")
                .contains("private");
    }

    @Test
    void bundledImageResourcesCanUsePublicLongLivedCache() {
        WebMvcConfig config = new WebMvcConfig();
        ReflectionTestUtils.setField(config, "uploadDir", "uploads");
        CapturingResourceHandlerRegistry registry = new CapturingResourceHandlerRegistry();

        config.addResourceHandlers(registry);

        CapturingResourceHandlerRegistration imageRegistration = registry.registrationFor("/images/**");
        assertThat(imageRegistration.locations()).containsExactly("classpath:/static/images/");
        assertThat(imageRegistration.cacheControlHeader())
                .contains("max-age=2592000")
                .contains("public");
    }

    private static final class CapturingResourceHandlerRegistry extends ResourceHandlerRegistry {
        private final Map<String, CapturingResourceHandlerRegistration> registrations = new LinkedHashMap<>();

        private CapturingResourceHandlerRegistry() {
            super(new StaticApplicationContext(), new MockServletContext());
        }

        @Override
        public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
            CapturingResourceHandlerRegistration registration =
                    new CapturingResourceHandlerRegistration(pathPatterns);
            Arrays.stream(pathPatterns).forEach(pattern -> registrations.put(pattern, registration));
            return registration;
        }

        private CapturingResourceHandlerRegistration registrationFor(String pattern) {
            return registrations.get(pattern);
        }
    }

    private static final class CapturingResourceHandlerRegistration extends ResourceHandlerRegistration {
        private final List<String> locations = new ArrayList<>();
        private CacheControl cacheControl;

        private CapturingResourceHandlerRegistration(String... pathPatterns) {
            super(pathPatterns);
        }

        @Override
        public ResourceHandlerRegistration addResourceLocations(String... locations) {
            this.locations.addAll(Arrays.asList(locations));
            return this;
        }

        @Override
        public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        private List<String> locations() {
            return locations;
        }

        private String cacheControlHeader() {
            return cacheControl.getHeaderValue();
        }
    }
}
