package com.tibet.tourism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class IpLocationService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpLocationService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public IpLocationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取客户端IP地址
     */
    public String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        // 本地开发环境处理
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "127.0.0.1";
        }
        
        return ip;
    }
    
    /**
     * 根据IP地址解析城市信息
     * 使用免费的IP地理位置API: ip-api.com
     */
    public String getCityByIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "未知";
        }
        
        // 本地IP不查询
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
            // 使用ip-api.com免费API（限制：每分钟45次请求）
            String url = "http://ip-api.com/json/" + ipAddress + "?lang=zh-CN&fields=status,message,city,regionName,country";
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                String status = jsonNode.get("status").asText();
                
                if ("success".equals(status)) {
                    String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";
                    String region = jsonNode.has("regionName") ? jsonNode.get("regionName").asText() : "";
                    String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "";
                    
                    // 组合城市信息
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
    
    /**
     * 获取IP地址的详细信息（包含城市、省份、国家等）
     */
    public Map<String, String> getIpLocationInfo(String ipAddress) {
        Map<String, String> info = new HashMap<>();
        info.put("ip", ipAddress);
        info.put("city", getCityByIp(ipAddress));
        return info;
    }
}






