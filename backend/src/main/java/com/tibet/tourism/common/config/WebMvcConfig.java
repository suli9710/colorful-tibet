package com.tibet.tourism.common.config;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = parseCsvProperty(allowedOrigins);
        if (origins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "X-XSRF-TOKEN",
                        "X-Requested-With",
                        "Idempotency-Key",
                        "X-Device-Fingerprint",
                        "X-Recaptcha-Token",
                        "X-Behavior-Data")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 配置上传文件访问路径
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadLocation = uploadPath.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);

        // 配置静态图片资源访问路径（项目内的图片）
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
        return WebClient.builder().exchangeStrategies(strategies);
    }

    private String[] parseCsvProperty(String value) {
        String[] values = Arrays.stream(value == null ? new String[0] : value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .filter(origin -> {
                    boolean unsafeWildcard = "*".equals(origin);
                    if (unsafeWildcard) {
                        logger.warn("Ignoring wildcard CORS origin because credentials are enabled");
                    }
                    return !unsafeWildcard;
                })
                .toArray(String[]::new);
        return values;
    }
}
