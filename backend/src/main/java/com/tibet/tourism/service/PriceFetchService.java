package com.tibet.tourism.service;

import com.tibet.tourism.dto.PriceInfo;
import com.tibet.tourism.entity.ScenicSpot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能价格获取服务
 * 支持多种策略：AI提取、网页爬虫、第三方API等
 */
@Service
public class PriceFetchService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${doubao.api.url:}")
    private String aiApiUrl;

    @Value("${doubao.api.key:}")
    private String aiApiKey;

    @Value("${doubao.api.model:}")
    private String aiModel;

    /**
     * 智能获取景点价格（自动选择最佳策略）
     */
    public PriceInfo fetchPrice(ScenicSpot spot) {
        List<PriceFetchStrategy> strategies = Arrays.asList(
            new AiExtractStrategy(webClientBuilder, aiApiUrl, aiApiKey, aiModel),
            new WebScrapingStrategy(webClientBuilder),
            new ThirdPartyApiStrategy(webClientBuilder)
        );

        // 按优先级尝试各种策略
        for (PriceFetchStrategy strategy : strategies) {
            try {
                PriceInfo priceInfo = strategy.fetch(spot);
                if (priceInfo != null && priceInfo.getBasePrice() != null && 
                    priceInfo.getBasePrice().compareTo(BigDecimal.ZERO) > 0) {
                    return priceInfo;
                }
            } catch (Exception e) {
                System.err.println("Strategy " + strategy.getClass().getSimpleName() + " failed: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * 价格获取策略接口
     */
    interface PriceFetchStrategy {
        PriceInfo fetch(ScenicSpot spot) throws Exception;
    }

    /**
     * 策略1：AI提取价格（从网页内容中智能提取）
     */
    static class AiExtractStrategy implements PriceFetchStrategy {
        private final WebClient.Builder webClientBuilder;
        private final String apiUrl;
        private final String apiKey;
        private final String model;

        public AiExtractStrategy(WebClient.Builder webClientBuilder, String apiUrl, String apiKey, String model) {
            this.webClientBuilder = webClientBuilder;
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.model = model;
        }

        @Override
        public PriceInfo fetch(ScenicSpot spot) {
            if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
                return null;
            }

            try {
                // 构建搜索URL（示例：百度搜索景点名称+门票价格）
                String searchQuery = spot.getName() + " 门票价格";
                String searchUrl = "https://www.baidu.com/s?wd=" + java.net.URLEncoder.encode(searchQuery, "UTF-8");

                // 获取搜索结果页面（简化版，实际应该使用更专业的搜索API）
                String htmlContent = webClientBuilder.build()
                    .get()
                    .uri(searchUrl != null ? searchUrl : "")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

                if (htmlContent == null || htmlContent.isEmpty()) {
                    return null;
                }

                // 使用AI提取价格信息
                String prompt = String.format(
                    "请从以下网页内容中提取景点\"%s\"的门票价格信息。\n" +
                    "请返回JSON格式：{\"basePrice\": 基础价格, \"peakSeasonPrice\": 旺季价格(可选), \"offSeasonPrice\": 淡季价格(可选)}\n" +
                    "如果找不到价格，返回null。只返回JSON，不要其他文字。\n\n" +
                    "网页内容（前2000字符）：\n%s",
                    spot.getName(),
                    htmlContent.substring(0, Math.min(2000, htmlContent.length()))
                );

                Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                        Map.of("role", "system", "content", "你是一个专业的价格信息提取助手，擅长从网页内容中提取准确的景点门票价格。"),
                        Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.1
                );

                @SuppressWarnings("unchecked")
                String response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .map(resp -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                            return (String) message.get("content");
                        }
                        return null;
                    })
                    .block();

                if (response == null) {
                    return null;
                }

                // 解析AI返回的JSON
                return parseAiResponse(response, "AI提取");

            } catch (Exception e) {
                System.err.println("AI提取价格失败: " + e.getMessage());
                return null;
            }
        }

        private PriceInfo parseAiResponse(String response, String source) {
            try {
                // 提取JSON部分
                String jsonStr = response.trim();
                if (jsonStr.startsWith("```json")) {
                    jsonStr = jsonStr.substring(7);
                }
                if (jsonStr.startsWith("```")) {
                    jsonStr = jsonStr.substring(3);
                }
                if (jsonStr.endsWith("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
                }
                jsonStr = jsonStr.trim();

                // 简单的JSON解析（实际应该使用Jackson）
                Pattern pricePattern = Pattern.compile("\"basePrice\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
                Matcher matcher = pricePattern.matcher(jsonStr);
                if (matcher.find()) {
                    BigDecimal basePrice = new BigDecimal(matcher.group(1));
                    PriceInfo info = new PriceInfo(basePrice, source);
                    info.setConfidence(0.85);

                    // 尝试提取旺季价格
                    Pattern peakPattern = Pattern.compile("\"peakSeasonPrice\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
                    Matcher peakMatcher = peakPattern.matcher(jsonStr);
                    if (peakMatcher.find()) {
                        info.setPeakSeasonPrice(new BigDecimal(peakMatcher.group(1)));
                    }

                    // 尝试提取淡季价格
                    Pattern offPattern = Pattern.compile("\"offSeasonPrice\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
                    Matcher offMatcher = offPattern.matcher(jsonStr);
                    if (offMatcher.find()) {
                        info.setOffSeasonPrice(new BigDecimal(offMatcher.group(1)));
                    }

                    return info;
                }
            } catch (Exception e) {
                System.err.println("解析AI响应失败: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 策略2：网页爬虫提取价格
     * 使用正则表达式从网页HTML中提取价格信息
     */
    static class WebScrapingStrategy implements PriceFetchStrategy {
        private final WebClient.Builder webClientBuilder;

        public WebScrapingStrategy(WebClient.Builder webClientBuilder) {
            this.webClientBuilder = webClientBuilder;
        }

        @Override
        public PriceInfo fetch(ScenicSpot spot) {
            try {
                // 构建多个搜索URL（提高成功率）
                List<String> searchUrls = buildSearchUrls(spot);
                
                List<BigDecimal> allPrices = new ArrayList<>();
                
                // 尝试多个URL
                for (String searchUrl : searchUrls) {
                    try {
                        String htmlContent = fetchHtmlContent(searchUrl);
                        if (htmlContent != null && !htmlContent.isEmpty()) {
                            List<BigDecimal> prices = extractPrices(htmlContent, spot.getName());
                            allPrices.addAll(prices);
                        }
                    } catch (Exception e) {
                        System.err.println("获取URL失败: " + searchUrl + ", 错误: " + e.getMessage());
                    }
                }

                if (!allPrices.isEmpty()) {
                    BigDecimal finalPrice = selectBestPrice(allPrices);
                    if (finalPrice != null) {
                        PriceInfo info = new PriceInfo(finalPrice, "网页爬虫");
                        info.setConfidence(calculateConfidence(allPrices, finalPrice));
                        return info;
                    }
                }

            } catch (Exception e) {
                System.err.println("网页爬虫提取价格失败: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 构建多个搜索URL
         */
        private List<String> buildSearchUrls(ScenicSpot spot) {
            List<String> urls = new ArrayList<>();
            String baseName = spot.getName();
            
            // 百度搜索
            urls.add("https://www.baidu.com/s?wd=" + encodeUrl(baseName + " 门票价格"));
            urls.add("https://www.baidu.com/s?wd=" + encodeUrl(baseName + " 门票多少钱"));
            
            // 如果景点有位置信息，可以添加位置相关的搜索
            if (spot.getLocation() != null && !spot.getLocation().isEmpty()) {
                urls.add("https://www.baidu.com/s?wd=" + encodeUrl(baseName + " " + spot.getLocation() + " 门票"));
            }
            
            return urls;
        }

        /**
         * 获取HTML内容
         */
        private String fetchHtmlContent(String url) {
            return webClientBuilder.build()
                .get()
                .uri(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();
        }

        /**
         * 使用正则表达式提取价格
         * 支持多种价格格式：
         * - 200元、200块、¥200、200元/人
         * - 旺季：250元、淡季：150元
         * - 成人票：200元、学生票：100元
         */
        private List<BigDecimal> extractPrices(String htmlContent, String spotName) {
            List<BigDecimal> prices = new ArrayList<>();
            
            // 移除HTML标签，只保留文本内容（简化处理）
            String textContent = htmlContent.replaceAll("<[^>]+>", " ");
            textContent = textContent.replaceAll("&nbsp;|&amp;|&lt;|&gt;|&quot;", " ");
            
            // 价格正则表达式模式（按优先级排序）
            List<Pattern> pricePatterns = Arrays.asList(
                // 模式1：门票价格（最精确）
                Pattern.compile("门票[：:]*\\s*(?:价格|票价|费用)[：:]*\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块|¥)"),
                // 模式2：直接价格格式
                Pattern.compile("(?:门票|票价|价格)[：:]*\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块|¥)"),
                // 模式3：旺季/淡季价格
                Pattern.compile("(?:旺季|淡季)[：:]*\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块|¥)"),
                // 模式4：成人票/学生票价格
                Pattern.compile("(?:成人票|学生票|儿童票)[：:]*\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块|¥)"),
                // 模式5：通用价格格式（在景点名称附近）
                Pattern.compile(Pattern.quote(spotName) + ".*?(\\d+(?:\\.\\d+)?)\\s*(?:元|块|¥)"),
                // 模式6：简单价格格式（最后尝试）
                Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:元|块|¥)(?:/人|/张)?")
            );

            Set<BigDecimal> uniquePrices = new HashSet<>();
            
            for (Pattern pattern : pricePatterns) {
                Matcher matcher = pattern.matcher(textContent);
                while (matcher.find()) {
                    try {
                        String priceStr = matcher.group(1);
                        BigDecimal price = new BigDecimal(priceStr);
                        
                        // 价格合理性过滤
                        if (isValidPrice(price)) {
                            uniquePrices.add(price);
                        }
                    } catch (NumberFormatException | ArithmeticException e) {
                        // 忽略无效价格
                    }
                }
            }

            prices.addAll(uniquePrices);
            return prices;
        }

        /**
         * 验证价格是否合理
         */
        private boolean isValidPrice(BigDecimal price) {
            // 门票价格通常在 10-2000 元之间
            // 过滤掉年份、日期、电话号码等
            double value = price.doubleValue();
            return value >= 10 && value <= 2000 && 
                   value == price.intValue() || (value * 10) == (int)(value * 10); // 允许小数，但最多一位
        }

        /**
         * 从多个价格中选择最佳价格
         */
        private BigDecimal selectBestPrice(List<BigDecimal> prices) {
            if (prices.isEmpty()) {
                return null;
            }

            // 统计每个价格出现的频率
            Map<BigDecimal, Integer> frequency = new HashMap<>();
            for (BigDecimal price : prices) {
                // 将价格四舍五入到最接近的10元（处理微小差异）
                BigDecimal rounded = price.divide(new BigDecimal("10"), 0, java.math.RoundingMode.HALF_UP)
                                          .multiply(new BigDecimal("10"));
                frequency.put(rounded, frequency.getOrDefault(rounded, 0) + 1);
            }

            // 选择出现频率最高的价格
            BigDecimal bestPrice = frequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

            // 如果频率相同，选择中位数
            if (bestPrice == null || frequency.get(bestPrice) == 1) {
                prices.sort(Comparator.naturalOrder());
                return prices.get(prices.size() / 2);
            }

            return bestPrice;
        }

        /**
         * 计算置信度
         */
        private double calculateConfidence(List<BigDecimal> prices, BigDecimal selectedPrice) {
            if (prices.isEmpty()) {
                return 0.5;
            }

            // 统计选中价格的出现频率
            long count = prices.stream()
                .filter(p -> {
                    BigDecimal rounded = p.divide(new BigDecimal("10"), 0, java.math.RoundingMode.HALF_UP)
                                         .multiply(new BigDecimal("10"));
                    BigDecimal selectedRounded = selectedPrice.divide(new BigDecimal("10"), 0, java.math.RoundingMode.HALF_UP)
                                                            .multiply(new BigDecimal("10"));
                    return rounded.equals(selectedRounded);
                })
                .count();

            // 置信度 = 出现频率 / 总数量 * 0.8（网页爬虫最高0.8）
            double confidence = (double) count / prices.size() * 0.8;
            return Math.max(0.5, Math.min(0.8, confidence));
        }

        /**
         * URL编码
         */
        private String encodeUrl(String text) {
            if (text == null) {
                return "";
            }
            try {
                return java.net.URLEncoder.encode(text, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return text;
            }
        }
    }

    /**
     * 策略3：第三方API（携程、去哪儿等）
     * 注意：需要申请API密钥
     */
    static class ThirdPartyApiStrategy implements PriceFetchStrategy {
        @SuppressWarnings("unused")
        private final WebClient.Builder webClientBuilder;

        public ThirdPartyApiStrategy(WebClient.Builder webClientBuilder) {
            this.webClientBuilder = webClientBuilder;
        }

        @Override
        public PriceInfo fetch(ScenicSpot spot) {
            // 示例：携程API（需要申请）
            // String apiUrl = "https://openapi.ctrip.com/scenic/price?name=" + spot.getName();
            // 实际实现需要API密钥和具体的API文档
            
            // 这里返回null，表示未配置第三方API
            return null;
        }
    }
}

