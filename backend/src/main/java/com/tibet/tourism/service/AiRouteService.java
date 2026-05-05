package com.tibet.tourism.service;

import com.tibet.tourism.dto.AiRouteGenerateResponse;
import com.tibet.tourism.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiRouteService {

    private static final Logger log = LoggerFactory.getLogger(AiRouteService.class);
    private static final Map<String, String> BUDGET_LABELS = Map.of(
            "economy", "经济型",
            "comfort", "舒适型",
            "luxury", "豪华型"
    );

    private static final Map<String, String> PREFERENCE_LABELS = Map.of(
            "natural", "自然风光",
            "cultural", "人文历史",
            "photography", "深度摄影",
            "relaxation", "休闲度假"
    );

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public AiRouteService(WebClient.Builder webClientBuilder,
                          @Value("${ark.api.url:${doubao.api.url:}}") String apiUrl,
                          @Value("${ark.api.key:${doubao.api.key:}}") String apiKey,
                          @Value("${ark.api.model:${doubao.api.model:}}") String model,
                          @Value("${ai.route.timeout-seconds:180}") long timeoutSeconds) {
        this.webClient = webClientBuilder.build();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(Math.max(30, timeoutSeconds));
    }

    @PostConstruct
    public void logConfigAvailability() {
        log.info("AI route config loaded: apiUrl={}, apiKeyPresent={}, model={}, timeoutSeconds={}",
                blankToPlaceholder(apiUrl),
                apiKey != null && !apiKey.isBlank(),
                blankToPlaceholder(model),
                timeout.getSeconds());
    }

    public AiRouteGenerateResponse generateRoute(int days, String budgetKey, String preferenceKey, User currentUser) {
        validateConfig();

        int safeDays = Math.max(1, days);
        String budgetLabel = BUDGET_LABELS.getOrDefault(normalizeKey(budgetKey), BUDGET_LABELS.get("comfort"));
        String preferenceLabel = PREFERENCE_LABELS.getOrDefault(normalizeKey(preferenceKey), PREFERENCE_LABELS.get("natural"));
        String prompt = buildPrompt(safeDays, budgetLabel, preferenceLabel, currentUser);

        Map<String, Object> requestBody = buildRequestBody(prompt);
        log.info("AI route request prepared: model={}, promptLength={}, promptPreview={}",
                blankToPlaceholder(model),
                prompt.length(),
                previewText(prompt, 240));

        try {
            Map<?, ?> response = webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("AI service request failed")
                            .flatMap(errorBody -> {
                                String responseSummary = String.format("HTTP %s", clientResponse.statusCode().value());
                                log.warn("AI route upstream error: {}, body={}", responseSummary, previewText(errorBody, 1200));
                                return Mono.error(new IllegalStateException(responseSummary + ": " + errorBody));
                            }))
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .block(timeout.plusSeconds(5));

            if (response == null) {
                throw new IllegalStateException("AI service returned empty response");
            }

            log.info("AI route response received, top-level keys={}", response.keySet());

            String rawContent = extractResponseText(response);
            String content = normalizeMarkdownRoute(rawContent, safeDays, budgetLabel, preferenceLabel);
            if (content.isBlank()) {
                throw new IllegalStateException("AI service response did not contain usable text");
            }

            log.info("AI route content received: originalLength={}, validatedLength={}", rawContent.length(), content.length());
            return new AiRouteGenerateResponse(content, model, budgetLabel, preferenceLabel, safeDays, null);
        } catch (Exception e) {
            log.error("AI route generation failed", e);
            throw new IllegalStateException("AI route generation failed: " + extractErrorMessage(e), e);
        }
    }

    private void validateConfig() {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("AI API URL is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API key is not configured");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("AI model is not configured");
        }
    }

    private String blankToPlaceholder(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", List.of(
                Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "input_text",
                                        "text", prompt
                                )
                        )
                )
        ));
        // Disable thinking/reasoning to get direct markdown output
        requestBody.put("thinking", Map.of("type", "disabled"));
        requestBody.put("temperature", 0.6);
        requestBody.put("top_p", 0.9);
        requestBody.put("max_output_tokens", 6000);
        requestBody.put("stream", false);
        return requestBody;
    }

    private String buildPrompt(int days, String budget, String preference, User currentUser) {
        String userContext = currentUser == null ? "" : String.format("\n- 用户昵称：%s", safeText(currentUser.getNickname()));

        String budgetGuidance;
        switch (budget) {
            case "经济型":
                budgetGuidance = "经济型：青旅或经济酒店120-250元/晚，公共交通+拼车为主，简餐或当地小吃，优先免费或低价景点。每日人均花费控制在300-500元。";
                break;
            case "舒适型":
                budgetGuidance = "舒适型：三星酒店或品质民宿300-500元/晚，包车+拼车组合，特色藏餐+品质餐厅，可含1-2个收费较高的核心景点。每日人均花费500-800元。";
                break;
            case "豪华型":
                budgetGuidance = "豪华型：五星酒店或精品藏式庄园600元+/晚，专车+向导，精选餐厅+高端藏式体验，优选最佳观景位和深度私享项目。每日人均花费800元+。";
                break;
            default:
                budgetGuidance = "中等消费，舒适酒店为主，包车+拼车结合，兼顾品质与性价比。";
                break;
        }

        String preferenceGuidance;
        switch (preference) {
            case "自然风光":
                preferenceGuidance = "偏好自然风光：行程核心是湖泊、雪山、草原、峡谷。每天必须安排至少1个自然景观目的地。推荐方向：羊卓雍措、纳木错、巴松措、雅鲁藏布大峡谷、南迦巴瓦峰、珠峰大本营、米堆冰川、然乌湖、色林错、当惹雍错。在最佳光线时段（日出后2小时、日落前2小时）安排核心观景点。";
                break;
            case "人文历史":
                preferenceGuidance = "偏好人文字史：行程核心是寺庙、宫殿、遗址、非遗体验。每天必须安排至少1个人文景点。推荐方向：布达拉宫、大昭寺、色拉寺（辩经）、甘丹寺、扎什伦布寺、萨迦寺、古格王朝遗址、江孜宗山、昌珠寺、桑耶寺。注重历史背景讲解和深度文化体验，可在寺庙停留2小时以上。";
                break;
            case "深度摄影":
                preferenceGuidance = "偏好深度摄影：行程围绕最佳拍摄机位和时间展开。日出前30分钟到达拍摄点，日落后30分钟再离开。推荐机位：药王山拍布达拉宫日出、羊卓雍措全景台、纳木错扎西半岛、色季拉山口拍南迦巴瓦、加乌拉山口拍珠峰群峰、札达土林日落、古格王朝星空。每天车程预留充足拍摄停留时间，不走马观花。";
                break;
            case "休闲度假":
                preferenceGuidance = "偏好休闲度假：行程节奏慢，每天1-2个核心体验即可，重在放松和享受。推荐：鲁朗小镇林海漫步、巴松措湖畔下午茶、羊卓雍措野餐、德仲温泉、嘎玛沟轻徒步、拉萨甜茶馆慢时光、藏式SPA。住宿优先选择有景观阳台或花园的酒店，减少赶路，多一些停留。";
                break;
            default:
                preferenceGuidance = "兼顾自然与人文，每天2-3个景点，节奏适中。";
                break;
        }

        // Build region routing guidance based on days
        String routingGuidance;
        if (days <= 3) {
            routingGuidance = "短途行程，以拉萨周边为主，可延伸到羊卓雍措（单程2小时）或纳木错（单程4小时）一日游。不要安排林芝或日喀则方向过夜。";
        } else if (days <= 5) {
            routingGuidance = "中等行程，拉萨2天适应后，选择林芝方向（巴松措+鲁朗+雅鲁藏布大峡谷，3天）或日喀则方向（羊卓雍措+江孜+日喀则，3天），二选一，不可同时覆盖两个方向。";
        } else if (days <= 8) {
            routingGuidance = "充裕行程，拉萨2-3天适应+一个主要方向深入（林芝4-5天或日喀则-珠峰线5天），可以在主要方向内深度探索多个景点。";
        } else {
            routingGuidance = "长线行程，拉萨2-3天+林芝方向4-5天+日喀则方向3-4天，或挑战阿里环线（需9天以上单独安排）。景点之间按地理顺序排列，不折返。";
        }

        return String.format(""
                + "【重要指令】你是西藏旅行规划师。直接输出下方 Markdown 格式的旅行计划，不要输出任何思考过程、开场白、解释、分析或客套话。你的回复从第一行 # 标题开始，到「进藏必读」结束，中间不得有任何额外内容。\n\n"
                + "%s\n\n"
                + "═══════════════════════════════════\n"
                + "游客需求\n"
                + "═══════════════════════════════════\n"
                + "- 旅行天数：%d天\n"
                + "- 预算水平：%s —— %s\n"
                + "- 旅行偏好：%s —— %s\n"
                + "- 路线约束：%s\n\n"
                + "═══════════════════════════════════\n"
                + "核心规划原则（必须遵守）\n"
                + "═══════════════════════════════════\n"
                + "1.【高原适应】第1天必须在拉萨（海拔3650米）轻度活动，仅安排布达拉宫广场、八廓街、大昭寺外围等低强度项目。严禁第1天安排高海拔景点或长途车程。\n"
                + "2.【离开拉萨】第2天起必须离开拉萨市区，前往西藏其他地区的具体景点。每一天的目的地必须是拉萨以外的真实景点，严禁连续两天都在拉萨市区。\n"
                + "3.【具体景点】每一天必须写出具体的景点名称（如羊卓雍措、巴松措、扎什伦布寺、雅鲁藏布大峡谷、纳木错等），不能笼统地说「游览」或「参观」。\n"
                + "4.【地理合理】相邻天数的目的地必须在同一地理方向上。西藏景点之间车程长（日均不超过350公里或6小时），路线不得出现折返或跳跃。\n"
                + "5.【偏好驱动】每一天的活动选择必须紧密围绕旅行偏好展开。如果偏好自然风光，每天至少1个自然景观；如果偏好人文字史，每天至少1个人文景点。\n"
                + "6.【预算体现】住宿等级、交通方式（公共交通/拼车/包车/专车）、餐饮档次必须在每天安排中体现预算差异。\n"
                + "7.【可执行性】每天的时间安排必须合理（含车程时间），上午、下午的活动不能有时间冲突。高海拔地区（4500米以上）不宜安排过夜。\n\n"
                + "═══════════════════════════════════\n"
                + "输出格式（从下一行开始就是你的回复内容，不要写任何前言）\n"
                + "═══════════════════════════════════\n\n"
                + "# [富有诗意的路线标题]\n\n"
                + "## 路线概览\n"
                + "用3-5句话概括本条路线的核心特色，点明主要目的地和适合的旅行者类型。\n\n"
                + "## 行程亮点\n"
                + "- 列出5-8个本路线最独特的体验\n"
                + "- 每条一句话，写出具体景点和感受\n"
                + "- 让读者一看就想出发\n\n"
                + "## 每日行程\n\n"
                + "### 第1天：拉萨 —— 高原初适应\n"
                + "- **上午**：具体安排（含时间、交通方式）\n"
                + "- **下午**：具体安排与游玩时长\n"
                + "- **晚上**：休闲与美食推荐\n"
                + "- **住宿推荐**：酒店名称或类型 + 价格区间 + 推荐理由\n"
                + "- **贴心提示**：当日海拔、穿衣建议、注意事项\n\n"
                + "### 第2天：[城市/地区] —— [当日主题，如「羊卓雍措环湖之旅」]\n"
                + "[同上结构，上午/下午/晚上/住宿推荐/贴心提示缺一不可]\n\n"
                + "[逐日输出至第%d天，每天必须包含完整的上午/下午/晚上/住宿推荐/贴心提示]\n\n"
                + "## 预算预估\n"
                + "按%s标准，分段列出交通、住宿、餐饮、门票、其他（人均/人民币），注明省钱或升级建议。\n\n"
                + "## 进藏必读\n"
                + "列出6-8条实用信息：边防证办理、高原反应应对、最佳旅行季节、穿衣指南、防晒保湿、通讯信号、现金准备、尊重当地风俗。\n\n"
                + "【再次强调】直接从 # 标题开始回复，不要输出任何其他内容。"
                + "",
                userContext,
                days, budget, budgetGuidance,
                preference, preferenceGuidance,
                routingGuidance,
                days,
                budget);
    }

    private String safeText(String value) {
        return value == null ? "" : value.replace("\n", " ").trim();
    }

    private String previewText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String extractErrorMessage(Exception e) {
        if (e == null) {
            return "unknown error";
        }
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private String extractResponseText(Map<?, ?> response) {
        String text = extractTextFromContent(response.get("output_text"));
        if (!text.isBlank()) {
            return text;
        }

        Object output = response.get("output");
        if (output instanceof List<?> outputList) {
            for (Object item : outputList) {
                text = extractTextFromContent(item);
                if (!text.isBlank()) {
                    return text;
                }
            }
        }

        Object choices = response.get("choices");
        if (choices instanceof List<?> choiceList) {
            for (Object item : choiceList) {
                text = extractTextFromContent(item);
                if (!text.isBlank()) {
                    return text;
                }
            }
        }

        for (String key : List.of("content", "result", "data", "message", "response")) {
            text = extractTextFromContent(response.get(key));
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String extractTextFromContent(Object content) {
        if (content instanceof String text) {
            return text.trim();
        }
        if (content instanceof Map<?, ?> map) {
            for (String key : List.of("text", "content", "output_text", "summary", "message")) {
                Object value = map.get(key);
                String text = extractTextFromContent(value);
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        if (content instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            for (Object item : list) {
                String text = extractTextFromContent(item);
                if (!text.isBlank()) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(text);
                }
            }
            return builder.toString().trim();
        }
        return "";
    }

    private String normalizeMarkdownRoute(String rawContent, int days, String budgetLabel, String preferenceLabel) {
        if (rawContent == null) {
            return "";
        }

        String sanitized = sanitizeResponseText(rawContent);
        if (sanitized.isBlank()) {
            return "";
        }

        String canonical = validateMarkdownRoute(sanitized, days, budgetLabel, preferenceLabel);
        return canonical == null ? "" : canonical;
    }

    private String sanitizeResponseText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String result = text.trim();
        // Remove BOM character
        result = result.replace("﻿", "");
        // Strip markdown code fences that may wrap the response
        result = result.replaceAll("(?s)```[a-zA-Z]*\\s*", "");
        result = result.replaceAll("(?s)```", "");
        // Remove thinking/reasoning blocks (some models output these internally)
        result = result.replaceAll("(?is)<think>.*?</think>", "");
        result = result.replaceAll("(?is)<thought>.*?</thought>", "");
        result = result.replaceAll("(?is)<thinking>.*?</thinking>", "");
        // Strip leading conversational openers
        // Covers: "好的！以下是...", "没问题，为您...", "当然，下面是...", "OK, here is...", etc.
        result = result.replaceFirst("^(?s)(好的|没问题|当然|以下是|为您|下面|这是|OK|Sure|Here|以下).{0,80}?\\n+", "");
        // Strip standalone preamble ending with colon (e.g. "以下是您的西藏7日行程：")
        result = result.replaceFirst("^(?s).{0,120}(?:行程|路线|itinerary|plan)[：:]\\s*\\n+", "");
        // Strip any leading non-markdown lines (lines not starting with #)
        if (!result.startsWith("#") && result.contains("\n#")) {
            result = result.substring(result.indexOf("\n#") + 1);
        }
        return result.trim();
    }

    private String validateMarkdownRoute(String content, int days, String budgetLabel, String preferenceLabel) {
        String normalized = content.replace("\uFEFF", "").trim();
        if (normalized.isBlank()) {
            return "";
        }

        String[] lines = normalized.split("\\R");
        StringBuilder body = new StringBuilder();
        boolean hasTitle = false;
        boolean hasDailySections = false;
        int dailyHeadingCount = 0;
        boolean containsRequiredSections = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (body.length() > 0 && body.charAt(body.length() - 1) != '\n') {
                    body.append('\n');
                }
                continue;
            }

            if (!hasTitle && isHeadingLine(trimmed)) {
                hasTitle = true;
            }
            if (isDailySectionLine(trimmed)) {
                dailyHeadingCount++;
            }
            if (containsRequiredKeywords(trimmed)) {
                containsRequiredSections = true;
            }
            body.append(trimmed).append('\n');
        }

        // Also check the entire content body for required keywords (fallback check)
        if (!containsRequiredSections) {
            containsRequiredSections = containsRequiredKeywords(normalized);
        }
        hasDailySections = dailyHeadingCount >= Math.min(days, 2);
        log.info("AI route validation: title={}, dailySections={}(count={}), requiredSections={}, days={}",
                hasTitle, hasDailySections, dailyHeadingCount, containsRequiredSections, days);
        if (!hasTitle || !hasDailySections || !containsRequiredSections) {
            log.warn("AI route markdown validation failed: title={}, dailySections={}, requiredSections={}, dayCount={}, contentPreview={}",
                    hasTitle, hasDailySections, containsRequiredSections, dailyHeadingCount, previewText(normalized, 300));
            return buildFallbackMarkdown(days, budgetLabel, preferenceLabel);
        }

        return body.toString().trim();
    }

    private boolean isHeadingLine(String text) {
        return text.startsWith("#")
                || text.startsWith("路线名称") || text.startsWith("行程名称")
                || text.startsWith("路线概览") || text.startsWith("适合原因")
                || text.startsWith("Route Overview") || text.startsWith("Trip Overview")
                || text.startsWith("路线") || text.startsWith("行程");
    }

    private boolean isDailySectionLine(String text) {
        return text.matches("^(?:#|##|###)?\\s*(?:第[一二三四五六七八九十0-9]+天|Day\\s*\\d+).*$");
    }

    private boolean containsRequiredKeywords(String text) {
        String normalized = text.replace(" ", "");
        return normalized.contains("上午") || normalized.contains("下午") || normalized.contains("晚上")
                || normalized.contains("Morning") || normalized.contains("Afternoon") || normalized.contains("Evening")
                || normalized.contains("住宿") || normalized.contains("Accommodation") || normalized.contains("hotel")
                || normalized.contains("贴心提示") || normalized.contains("Tips") || normalized.contains("tips")
                || normalized.contains("预算") || normalized.contains("Budget") || normalized.contains("budget")
                || normalized.contains("进藏必读") || normalized.contains("Essentials") || normalized.contains("essentials")
                || normalized.contains("路线概览") || normalized.contains("Overview") || normalized.contains("overview");
    }

    private String buildFallbackMarkdown(int days, String budgetLabel, String preferenceLabel) {
        // Build a sensible fallback itinerary with real destinations, not just Lhasa
        StringBuilder builder = new StringBuilder();
        builder.append("# 西藏").append(days).append("天经典").append(preferenceLabel).append("之旅\n\n");
        builder.append("## 路线概览\n");
        builder.append("本条路线为西藏经典").append(days).append("天行程，以").append(preferenceLabel).append("为核心，兼顾高原适应与深度体验。AI服务暂时不可用，以下为推荐基准路线，你可参考并自行调整。\n\n");
        builder.append("## 行程亮点\n");
        builder.append("- 拉萨市区深度游览，感受藏文化心脏\n");
        builder.append("- 探访西藏经典自然与人文景观\n");
        builder.append("- 体验高原风光与藏地民俗\n\n");
        builder.append("## 每日行程\n\n");

        // Day 1 always Lhasa adaptation
        builder.append("### 第1天：拉萨 —— 高原初适应\n");
        builder.append("- **上午**：抵达拉萨，机场/火车站前往市区（约1小时），入住酒店后休息\n");
        builder.append("- **下午**：布达拉宫广场漫步，八廓街转经，适应高原环境\n");
        builder.append("- **晚上**：品尝藏式甜茶和藏面，早早休息\n");
        builder.append("- **住宿推荐**：拉萨市区酒店，").append(budgetLabel).append("标准\n");
        builder.append("- **贴心提示**：拉萨海拔3650米，第一天不洗澡、不饮酒、避免剧烈运动\n\n");

        // Route definitions based on days
        String[][] spots;
        if (days <= 3) {
            spots = new String[][]{
                {"拉萨 —— 圣城朝圣", "布达拉宫参观（需提前预约，游览约3小时）", "大昭寺朝圣，在八廓街跟随信徒转经", "八廓街藏式餐厅，品尝酥油茶和糌粑", "拉萨市区" + budgetLabel + "酒店", "布达拉宫需爬台阶，放慢节奏；大昭寺下午光线最佳"},
                {"羊卓雍措 —— 碧玉湖一日游", "拉萨出发前往羊卓雍措（车程约2.5小时），沿途欣赏雅鲁藏布江河谷", "环湖游览，在岗巴拉山口俯瞰羊卓雍措全景", "返回拉萨，享用藏式火锅", "拉萨市区" + budgetLabel + "酒店", "羊卓雍措海拔4441米，山口风大注意保暖；沿途限速，车程较长"}
            };
        } else if (days <= 5) {
            spots = new String[][]{
                {"拉萨 —— 圣城朝圣", "布达拉宫参观（需提前预约，游览约3小时）", "大昭寺+八廓街深度游，色拉寺观辩经（15:00开始）", "八廓街周边藏式餐厅", "拉萨市区" + budgetLabel + "酒店", "布达拉宫需爬台阶，放慢节奏"},
                {"拉萨 → 巴松措 → 林芝（车程约5小时）", "沿318国道前往林芝，途中游览巴松措湖心岛", "巴松措徒步或乘船游览（2小时），继续前往林芝八一镇", "林芝石锅鸡（特色美食），尼洋河畔散步", "林芝八一镇" + budgetLabel + "酒店", "林芝海拔约3000米，气候湿润舒适；巴松措门票120元"},
                {"林芝 —— 鲁朗林海与南迦巴瓦", "前往鲁朗林海（车程约1.5小时），骑马或徒步穿越原始森林", "色季拉山口远眺南迦巴瓦峰（海拔7782米），前往雅鲁藏布大峡谷入口", "鲁朗石锅鸡，夜宿峡谷入口处", "派镇或峡谷入口" + budgetLabel + "客栈", "南迦巴瓦峰常年云雾缭绕，冬季和清晨最易看到"},
                {"雅鲁藏布大峡谷 → 拉萨（车程约6小时）", "上午游览雅鲁藏布大峡谷，拍摄南迦巴瓦峰与雅鲁藏布江同框", "沿318国道返回拉萨", "抵达拉萨后休息，回顾行程", "拉萨市区" + budgetLabel + "酒店", "返程车程较长，备好零食和水"}
            };
        } else if (days <= 8) {
            spots = new String[][]{
                {"拉萨 —— 圣城深度游", "布达拉宫参观（约3小时）", "大昭寺+八廓街，下午色拉寺观辩经", "八廓街藏式餐厅", "拉萨市区" + budgetLabel + "酒店", "初到高原放慢节奏，适应为主"},
                {"拉萨 → 羊卓雍措 → 江孜（车程约5小时）", "沿307省道前往羊卓雍措，岗巴拉山口俯瞰全景（停留1小时）", "沿湖前行至浪卡子县，继续前往江孜，参观白居寺和宗山古堡", "江孜县城藏餐", "江孜县城" + budgetLabel + "酒店", "羊卓雍措海拔4441米，山口风大；江孜海拔约4000米"},
                {"江孜 → 日喀则（车程约2小时）", "上午继续游览江孜白居寺十万佛塔", "前往日喀则，参观扎什伦布寺（班禅驻锡地，游览约3小时）", "日喀则市区藏式餐厅", "日喀则市区" + budgetLabel + "酒店", "日喀则海拔约3840米；扎什伦布寺是后藏最重要寺院"},
                {"日喀则 → 珠峰大本营（车程约8小时）", "清晨出发，经加乌拉山口（可同时看到5座8000米级雪山）", "抵达珠峰大本营（海拔5200米），等待日落金山", "大本营简餐，拍摄星空（如天气允许）", "大本营帐篷营地或绒布寺招待所", "海拔5200米，注意高反！备好氧气瓶、保暖衣物和头灯"},
                {"珠峰大本营 → 日喀则（车程约8小时）", "拍摄珠峰日出金山，随后返程", "沿318国道返回日喀则，途中可停靠拉孜县休息", "日喀则市区休息", "日喀则市区" + budgetLabel + "酒店", "返程漫长，备好食物和水；注意限速"},
                {"日喀则 → 拉萨（车程约5小时）", "沿雅鲁藏布江河谷返回拉萨", "抵达拉萨后自由活动，可逛冲赛康市场或小昭寺", "藏式告别晚餐，推荐娜玛瑟德餐厅", "拉萨市区" + budgetLabel + "酒店", "最后一天放松为主，整理旅途照片和回忆"}
            };
        } else {
            spots = new String[][]{
                {"拉萨 —— 圣城深度游", "布达拉宫参观（约3小时）", "大昭寺+八廓街，下午色拉寺观辩经", "八廓街藏式餐厅", "拉萨市区" + budgetLabel + "酒店", "初到高原放慢节奏，适应为主"},
                {"拉萨 —— 人文探索", "西藏博物馆（了解藏地历史文化，约2小时）", "小昭寺+罗布林卡（达赖夏宫）", "拉萨河畔散步，甜茶馆体验", "拉萨市区" + budgetLabel + "酒店", "博物馆周一闭馆，注意避开"},
                {"拉萨 → 羊卓雍措 → 江孜（车程约5小时）", "沿307省道前往羊卓雍措，岗巴拉山口俯瞰全景", "沿湖至浪卡子，前往江孜参观白居寺和宗山古堡", "江孜县城藏餐", "江孜县城" + budgetLabel + "酒店", "羊卓雍措海拔4441米"},
                {"江孜 → 日喀则（车程约2小时）", "继续游览白居寺十万佛塔", "参观扎什伦布寺（约3小时）", "日喀则市区藏式餐厅", "日喀则市区" + budgetLabel + "酒店", "扎什伦布寺是后藏格鲁派最重要寺院"},
                {"日喀则 → 珠峰大本营（车程约8小时）", "清晨出发，经加乌拉山口", "抵达珠峰大本营，等待日落金山", "大本营简餐，星空拍摄", "大本营帐篷营地", "海拔5200米，注意高反"},
                {"珠峰大本营 → 日喀则（车程约8小时）", "拍摄珠峰日出，随后返程", "返回日喀则休整", "日喀则市区休息", "日喀则市区" + budgetLabel + "酒店", "返程漫长，备好食物"},
                {"日喀则 → 纳木错（车程约7小时）", "经拉萨绕行至纳木错，途经当雄草原", "纳木错扎西半岛游览，拍摄湖光山色", "纳木错湖边简易客栈", "纳木错湖边客栈", "纳木错海拔4718米，夜间极冷"},
                {"纳木错 → 拉萨（车程约4小时）", "拍摄纳木错日出，环扎西半岛", "返回拉萨，自由活动", "藏式告别晚餐", "拉萨市区" + budgetLabel + "酒店", "旅途结束，整理美好回忆"}
            };
        }

        int remainingDays = days - 1; // Day 1 already handled
        for (int i = 0; i < remainingDays && i < spots.length; i++) {
            String[] day = spots[i];
            builder.append("### 第").append(toChineseDay(i + 2)).append("天：").append(day[0]).append("\n");
            builder.append("- **上午**：").append(day[1]).append("\n");
            builder.append("- **下午**：").append(day[2]).append("\n");
            builder.append("- **晚上**：").append(day[3]).append("\n");
            builder.append("- **住宿推荐**：").append(day[4]).append("\n");
            builder.append("- **贴心提示**：").append(day[5]).append("\n\n");
        }

        // If spots array is shorter than remaining days, fill remaining days
        for (int i = spots.length; i < remainingDays; i++) {
            int dayNum = i + 2;
            builder.append("### 第").append(toChineseDay(dayNum)).append("天：拉萨 —— 自由探索\n");
            builder.append("- **上午**：根据个人兴趣自由安排，可前往未游览的寺庙或市场\n");
            builder.append("- **下午**：购买纪念品，体验藏式甜茶馆慢时光\n");
            builder.append("- **晚上**：回顾旅程，品尝藏式美食\n");
            builder.append("- **住宿推荐**：拉萨市区").append(budgetLabel).append("酒店\n");
            builder.append("- **贴心提示**：注意高原反应，保持充足休息\n\n");
        }

        builder.append("## 预算预估\n");
        builder.append("按").append(budgetLabel).append("标准估算：交通约占40%，住宿占30%，餐饮占15%，门票占10%，其他占5%。实际花费以出行时市场价格为准。\n\n");
        builder.append("## 进藏必读\n");
        builder.append("- **边防证**：前往珠峰、阿里、墨脱等边境地区需提前在户籍所在地办理边防证\n");
        builder.append("- **高原反应**：提前一周服用红景天，抵达后放慢节奏，多喝水，备好氧气瓶和常用药品\n");
        builder.append("- **最佳季节**：5-10月为最佳旅行季，冬季部分景区和垭口可能封闭\n");
        builder.append("- **穿衣指南**：高原昼夜温差大，「早穿棉袄午穿纱」，备好冲锋衣和保暖内衣\n");
        builder.append("- **防晒保湿**：紫外线极强，SPF50+防晒霜+墨镜+遮阳帽必备，润唇膏和保湿霜不可少\n");
        builder.append("- **通讯信号**：城镇区域4G信号良好，偏远地区信号不稳定，提前下载离线地图\n");
        builder.append("- **现金准备**：部分寺庙和偏远景区只收现金，建议备2000-3000元现金\n");
        builder.append("- **尊重风俗**：寺庙内不拍照或关闪光灯、顺时针转经、不摸藏族头部、不踩门槛\n");
        return builder.toString().trim();
    }

    private String toChineseDay(int day) {
        if (day == 1) return "一";
        if (day == 2) return "二";
        if (day == 3) return "三";
        if (day == 4) return "四";
        if (day == 5) return "五";
        if (day == 6) return "六";
        if (day == 7) return "七";
        if (day == 8) return "八";
        if (day == 9) return "九";
        if (day == 10) return "十";
        return String.valueOf(day);
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
