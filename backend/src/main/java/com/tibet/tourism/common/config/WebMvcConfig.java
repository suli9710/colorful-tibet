package com.tibet.tourism.common.config;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${app.outbound-proxy.url:${OUTBOUND_HTTP_PROXY:}}")
    private String outboundProxyUrl;

    @Value("${app.outbound-proxy.non-proxy-hosts:${OUTBOUND_HTTP_PROXY_NON_PROXY_HOSTS:localhost|127.*|10.*|172\\.(1[6-9]|2[0-9]|3[0-1])\\..*|192\\.168\\..*|mysql|redis|scrapling|backend|frontend|zipkin}}")
    private String outboundProxyNonProxyHosts;

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
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadLocation = uploadPath.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCacheControl(CacheControl.noCache().cachePrivate());

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();

        WebClient.Builder builder = WebClient.builder().exchangeStrategies(strategies);
        HttpClient httpClient = outboundProxyHttpClient();
        if (httpClient != null) {
            builder.clientConnector(new ReactorClientHttpConnector(httpClient));
        }
        return builder;
    }

    private HttpClient outboundProxyHttpClient() {
        if (!StringUtils.hasText(outboundProxyUrl)) {
            return null;
        }

        try {
            URI proxyUri = URI.create(outboundProxyUrl.contains("://")
                    ? outboundProxyUrl
                    : "http://" + outboundProxyUrl);
            String host = proxyUri.getHost();
            int port = proxyUri.getPort() > 0 ? proxyUri.getPort() : 80;
            if (!StringUtils.hasText(host)) {
                logger.warn("Ignoring outbound proxy URL because host is missing");
                return null;
            }
            if (StringUtils.hasText(proxyUri.getScheme()) && !"http".equalsIgnoreCase(proxyUri.getScheme())) {
                logger.warn("Ignoring outbound proxy URL because only HTTP proxies are supported for WebClient");
                return null;
            }

            logger.info("Configuring WebClient outbound proxy: host={}, port={}, nonProxyHosts={}",
                    host, port, outboundProxyNonProxyHosts);
            return HttpClient.create()
                    .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                            .host(host)
                            .port(port)
                            .nonProxyHosts(outboundProxyNonProxyHosts));
        } catch (IllegalArgumentException exception) {
            logger.warn("Ignoring invalid outbound proxy URL");
            return null;
        }
    }

    private String[] parseCsvProperty(String value) {
        String[] values = Arrays.stream(value == null ? new String[0] : value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .filter(origin -> {
                    boolean unsafeWildcard = origin.contains("*");
                    if (unsafeWildcard) {
                        logger.warn("Ignoring wildcard CORS origin because credentials are enabled: {}", origin);
                    }
                    return !unsafeWildcard;
                })
                .toArray(String[]::new);
        return values;
    }
}
