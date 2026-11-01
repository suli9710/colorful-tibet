package com.tibet.tourism.common.logging;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class IpLocationService {

    private static final Logger logger = LoggerFactory.getLogger(IpLocationService.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String UNKNOWN_LOCATION = "未知";
    private static final String PRIVATE_NETWORK_LOCATION = "本地网络";
    /**
     * The lookup runs on the login request thread and failures are deliberately not cached, so an
     * unreachable provider would otherwise add the full connect+read timeout to every single login,
     * indefinitely. After this many consecutive failures the provider is skipped for a cooldown.
     */
    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration FAILURE_COOLDOWN = Duration.ofMinutes(5);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TrustedProxyIpResolver trustedProxyIpResolver;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong skipLookupsUntil = new AtomicLong();

    @Value("${app.integrations.ip-location-url-template:${IP_LOCATION_URL_TEMPLATE:https://ipapi.co/{ip}/json/}}")
    private String ipLocationUrlTemplate;

    public IpLocationService(TrustedProxyIpResolver trustedProxyIpResolver) {
        this.trustedProxyIpResolver = trustedProxyIpResolver;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper();
    }

    public String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        return trustedProxyIpResolver.resolveClientIp(request);
    }

    @Cacheable(value = "ipLocationCache", key = "@cacheKeyHasher.cacheKey('ip-location', #ipAddress)", unless = "#result == '未知' || #result == '本地网络'")
    public String getCityByIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return UNKNOWN_LOCATION;
        }

        if ("127.0.0.1".equals(ipAddress) || "localhost".equals(ipAddress) ||
            ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") ||
            ipAddress.startsWith("172.16.") || ipAddress.startsWith("172.17.") ||
            ipAddress.startsWith("172.18.") || ipAddress.startsWith("172.19.") ||
            ipAddress.startsWith("172.20.") || ipAddress.startsWith("172.21.") ||
            ipAddress.startsWith("172.22.") || ipAddress.startsWith("172.23.") ||
            ipAddress.startsWith("172.24.") || ipAddress.startsWith("172.25.") ||
            ipAddress.startsWith("172.26.") || ipAddress.startsWith("172.27.") ||
            ipAddress.startsWith("172.28.") || ipAddress.startsWith("172.29.") ||
            ipAddress.startsWith("172.30.") || ipAddress.startsWith("172.31.")) {
            return PRIVATE_NETWORK_LOCATION;
        }

        if (System.currentTimeMillis() < skipLookupsUntil.get()) {
            return UNKNOWN_LOCATION;
        }

        try {
            String url = UriComponentsBuilder.fromUriString(ipLocationUrlTemplate)
                    .buildAndExpand(Map.of("ip", ipAddress))
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                if (jsonNode.has("status") && !"success".equals(jsonNode.get("status").asText())) {
                    String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "查询失败";
                    logger.warn("IP地理位置查询失败: {}", message);
                    recordLookupFailure();
                    return UNKNOWN_LOCATION;
                }
                if (jsonNode.path("error").asBoolean(false)) {
                    String message = jsonNode.path("reason").asText(jsonNode.path("message").asText("查询失败"));
                    logger.warn("IP地理位置查询失败: {}", message);
                    recordLookupFailure();
                    return UNKNOWN_LOCATION;
                }

                String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";
                String region = jsonNode.has("regionName")
                        ? jsonNode.get("regionName").asText()
                        : jsonNode.path("region").asText("");
                String country = jsonNode.has("country")
                        ? jsonNode.get("country").asText()
                        : jsonNode.path("country_name").asText("");

                StringBuilder cityInfo = new StringBuilder();
                if (city != null && !city.isEmpty()) {
                    cityInfo.append(city);
                }
                if (region != null && !region.isEmpty() && !region.equals(city)) {
                    if (cityInfo.length() > 0) {
                        cityInfo.append(", ");
                    }
                    cityInfo.append(region);
                }
                if (country != null && !country.isEmpty()) {
                    if (cityInfo.length() > 0) {
                        cityInfo.append(", ");
                    }
                    cityInfo.append(country);
                }

                consecutiveFailures.set(0);
                return cityInfo.length() > 0 ? cityInfo.toString() : UNKNOWN_LOCATION;
            }
            recordLookupFailure();
        } catch (Exception e) {
            logger.error("解析IP地址 {} 的城市信息时出错: {}",
                    PiiMasker.maskIp(ipAddress), SensitiveLogSanitizer.exceptionSummary(e));
            recordLookupFailure();
        }

        return UNKNOWN_LOCATION;
    }

    /**
     * Opens a cooldown once the provider has failed repeatedly, so a provider outage costs the login
     * path the connect+read timeout only until the threshold is reached rather than on every request.
     */
    private void recordLookupFailure() {
        if (consecutiveFailures.incrementAndGet() < FAILURE_THRESHOLD) {
            return;
        }
        consecutiveFailures.set(0);
        skipLookupsUntil.set(System.currentTimeMillis() + FAILURE_COOLDOWN.toMillis());
        logger.warn("Suspending IP geolocation lookups for {} minutes after {} consecutive failures",
                FAILURE_COOLDOWN.toMinutes(), FAILURE_THRESHOLD);
    }

    public Map<String, String> getIpLocationInfo(String ipAddress) {
        Map<String, String> info = new HashMap<>();
        info.put("ip", ipAddress);
        info.put("city", getCityByIp(ipAddress));
        return info;
    }
}
