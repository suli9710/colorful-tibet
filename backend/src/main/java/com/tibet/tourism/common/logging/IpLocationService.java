package com.tibet.tourism.common.logging;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TrustedProxyIpResolver trustedProxyIpResolver;

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

    @Cacheable(value = "ipLocationCache", key = "#ipAddress", unless = "#result == '未知' || #result == '本地网络'")
    public String getCityByIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "未知";
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
            return "本地网络";
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
                    return "未知";
                }
                if (jsonNode.path("error").asBoolean(false)) {
                    String message = jsonNode.path("reason").asText(jsonNode.path("message").asText("查询失败"));
                    logger.warn("IP地理位置查询失败: {}", message);
                    return "未知";
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

                return cityInfo.length() > 0 ? cityInfo.toString() : "未知";
            }
        } catch (Exception e) {
            logger.error("解析IP地址 {} 的城市信息时出错: {}", PiiMasker.maskIp(ipAddress), e.getMessage());
        }

        return "未知";
    }

    public Map<String, String> getIpLocationInfo(String ipAddress) {
        Map<String, String> info = new HashMap<>();
        info.put("ip", ipAddress);
        info.put("city", getCityByIp(ipAddress));
        return info;
    }
}
