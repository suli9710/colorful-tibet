package com.tibet.tourism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class IpLocationService {

    private static final Logger logger = LoggerFactory.getLogger(IpLocationService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    public IpLocationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String ip = null;
        if (trustProxyHeaders) {
            ip = request.getHeader("X-Forwarded-For");
            if (!hasUsableIp(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (!hasUsableIp(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (!hasUsableIp(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (!hasUsableIp(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
        }
        if (!hasUsableIp(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    private boolean hasUsableIp(String value) {
        return value != null && !value.isBlank() && value.length() <= 128 && !"unknown".equalsIgnoreCase(value.trim());
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
            String url = "http://ip-api.com/json/" + ipAddress + "?lang=zh-CN&fields=status,message,city,regionName,country";

            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                String status = jsonNode.get("status").asText();

                if ("success".equals(status)) {
                    String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";
                    String region = jsonNode.has("regionName") ? jsonNode.get("regionName").asText() : "";
                    String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "";

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
                } else {
                    String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "查询失败";
                    logger.warn("IP地理位置查询失败: {}", message);
                    return "未知";
                }
            }
        } catch (Exception e) {
            logger.error("解析IP地址 {} 的城市信息时出错: {}", ipAddress, e.getMessage());
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
