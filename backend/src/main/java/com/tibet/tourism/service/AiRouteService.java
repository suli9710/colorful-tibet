package com.tibet.tourism.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiRouteService {

    @Value("${doubao.api.url}")
    private String apiUrl;

    @Value("${doubao.api.key}")
    private String apiKey;

    @Value("${doubao.api.model}")
    private String model;

    private static final List<String> DEFAULT_STOPS = List.of(
            "拉萨市区适应",
            "布达拉宫与大昭寺",
            "羊卓雍措观景台",
            "纳木错圣湖",
            "林芝巴松措"
    );

    private static final Map<String, List<String>> PREFERENCE_STOPS = Map.of(
            "自然风光", List.of("拉萨适应", "纳木错圣湖", "林芝鲁朗林海", "羊卓雍措", "珠峰大本营"),
            "人文历史", List.of("拉萨适应", "布达拉宫", "大昭寺与八廓街", "扎什伦布寺", "古格王国遗址"),
            "深度摄影", List.of("拉萨适应", "南迦巴瓦峰", "巴松措晨光", "珠峰日落", "扎达土林星空"),
            "休闲度假", List.of("拉萨适应", "林芝巴松措", "鲁朗小镇", "措木及日", "日喀则温泉")
    );

    private static final Map<String, String> PREFERENCE_THEMES = Map.of(
            "自然风光", "雪山与湖泊巡礼",
            "人文历史", "藏地古文明探秘",
            "深度摄影", "黄金光线猎取计划",
            "休闲度假", "慢旅行·氧吧疗愈"
    );

    private static final Map<String, String> BUDGET_TIPS = Map.of(
            "经济型", "- 交通：拼车/大巴 ¥800-1200\n- 住宿：连锁/青旅 ¥200-300/晚\n- 餐饮：人均 ¥60-80\n- 体验：精选必去景点，控制门票开销\n**总计约**：¥4000-6000/人",
            "舒适型", "- 交通：包车/商务车 ¥2000-2600\n- 住宿：精品民宿/轻奢酒店 ¥400-600/晚\n- 餐饮：人均 ¥100-150（偶尔升级藏宴）\n- 体验：含特色项目与讲解\n**总计约**：¥7000-11000/人",
            "豪华型", "- 交通：越野车/头等舱 ¥4000+\n- 住宿：松赞/悦榕庄等高端酒店 ¥1200+/晚\n- 餐饮：定制私厨或景观餐厅\n- 体验：高端营地、直升机观光\n**总计约**：¥15000-23000/人"
    );

    private final WebClient webClient;

    public AiRouteService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> generateRoute(int days, String budget, String preference) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        String prompt = String.format(
            "你是一位在西藏生活了20年的资深金牌导游。请根据游客的具体要求，设计一份深度定制的旅行方案。\n\n" +
            "【游客画像】\n" +
            "- 游玩天数：%d天\n" +
            "- 预算水平：%s\n" +
            "- 核心偏好：%s\n\n" +
            "【设计要求】\n" +
            "1. **深度结合偏好**：\n" +
            "   - 若偏好“自然风光”，请重点安排纳木错、羊卓雍措、珠峰、南迦巴瓦等，减少寺庙行程。\n" +
            "   - 若偏好“人文历史”，请重点安排布达拉宫、大昭寺、扎什伦布寺、古格王朝，并讲解文化背景。\n" +
            "   - 若偏好“深度摄影”，请标注最佳拍摄点和日出日落时间（如：拍南迦巴瓦日照金山）。\n" +
            "   - 若偏好“休闲度假”，请安排林芝鲁朗、巴松措等低海拔氧吧，行程要松弛。\n" +
            "2. **预算匹配**：\n" +
            "   - “经济型”：推荐青旅、拼车、高性价比藏餐。\n" +
            "   - “舒适型”：推荐精品民宿/四星酒店、包车、特色体验。\n" +
            "   - “豪华型”：推荐五星/景观酒店（如松赞）、直升机/越野车、高端定制餐饮。\n" +
            "3. **行程合理性**：必须考虑海拔适应，第一天务必安排低强度适应，避免剧烈运动。\n\n" +
            "【输出格式】（请严格遵守Markdown格式）\n" +
            "## 🏔️ 定制路线：[极具吸引力的路线名称]\n\n" +
            "### ✨ 为什么这条路线适合你？\n" +
            "[用一段话解释路线如何契合游客的偏好和预算，体现专业性]\n\n" +
            "### 🌟 行程亮点\n" +
            "- [亮点1]\n" +
            "- [亮点2]\n\n" +
            "### 🗓️ 每日详细安排\n" +
            "#### Day 1: [城市/地点] - [主题]\n" +
            "- **上午**：[具体活动，包含景点特色]\n" +
            "- **下午**：[具体活动]\n" +
            "- **晚上**：[推荐体验或美食]\n" +
            "- **🏨 住宿推荐**：[根据预算推荐具体酒店或区域]\n" +
            "- **💡 贴心提示**：[针对当天的海拔或路况建议]\n\n" +
            "（...依次列出每一天...）\n\n" +
            "### 💰 预算预估 (%s)\n" +
            "- 交通：[预估]\n" +
            "- 住宿：[预估]\n" +
            "- 门票/娱乐：[预估]\n" +
            "- 餐饮：[预估]\n" +
            "**总计约**：[总金额范围]\n\n" +
            "### ⚠️ 进藏必读\n" +
            "- [高反预防]\n" +
            "- [穿衣指南]\n" +
            "- [禁忌事项]",
            days, budget, preference, budget
        );

        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "你是一位热情、专业、细致的西藏金牌导游，你的回答应该充满藏地风情，同时逻辑严密，实用性强。"),
            Map.of("role", "user", "content", prompt)
        ));

        return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("AI API Error Status: " + response.statusCode());
                                System.err.println("AI API Error Body: " + errorBody);
                                return Mono.error(new RuntimeException("AI API Error: " + errorBody));
                            });
                })
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                    return "抱歉，AI 暂时无法生成路线，请稍后再试。";
                })
                .timeout(Duration.ofSeconds(90))
                .onErrorResume(e -> {
                    System.err.println("AI Service degraded, fallback plan generated: " + e.getMessage());
                    return Mono.just(generateFallbackRoute(days, budget, preference));
                })
                .doOnError(e -> {
                    System.err.println("AI Service Exception: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    private String generateFallbackRoute(int days, String budget, String preference) {
        List<String> stops = PREFERENCE_STOPS.getOrDefault(preference, DEFAULT_STOPS);
        String theme = PREFERENCE_THEMES.getOrDefault(preference, "经典西藏深度体验");
        String routeName = preference + " · " + theme;
        StringBuilder builder = new StringBuilder();

        builder.append("## 🏔️ 定制路线：").append(routeName).append("\n\n");
        builder.append("### ✨ 为什么这条路线适合你？\n");
        builder.append(String.format("围绕“%s”偏好打造的行程，搭配“%s”预算标准，首日留在拉萨循序渐进适应海拔，随后逐步提升海拔与景观强度，兼顾体验与安全。\n\n", preference, budget));

        builder.append("### 🌟 行程亮点\n");
        builder.append("- Day 1 轻量化适应，提供氧气补给与慢行路线\n");
        builder.append("- 精选站点：").append(String.join(" / ", stops.subList(0, Math.min(stops.size(), 4)))).append("\n");
        builder.append("- 根据预算提供酒店与交通建议，确保体验可落地\n\n");

        builder.append("### 🗓️ 每日详细安排\n");
        for (int i = 0; i < days; i++) {
            String stop = stops.get(i % stops.size());
            builder.append(String.format("#### Day %d: %s\n", i + 1, stop));
            if (i == 0) {
                builder.append("- **上午**：抵达拉萨，入住酒店休息，安排姜汤/酥油茶适应\n");
                builder.append("- **下午**：八廓街散步 + 医生级高反讲解\n");
                builder.append("- **晚上**：藏式欢迎晚宴，22:00 前回酒店休息\n");
                builder.append("- **🏨 住宿推荐**：拉萨市区可供氧酒店\n");
                builder.append("- **💡 贴心提示**：补水、禁烟酒，症状持续请及时沟通\n\n");
            } else {
                builder.append("- **上午**：精选景点深度导览，包含私房拍摄机位\n");
                builder.append("- **下午**：主题体验（").append(theme).append("）\n");
                builder.append("- **晚上**：当地美食或星空茶叙\n");
                builder.append("- **🏨 住宿推荐**：").append(getStaySuggestion(budget)).append("\n");
                builder.append("- **💡 贴心提示**：保持分层保暖，注意紫外线\n\n");
            }
        }

        builder.append("### 💰 预算预估 (").append(budget).append(")\n");
        builder.append(BUDGET_TIPS.getOrDefault(budget, BUDGET_TIPS.get("舒适型"))).append("\n\n");

        builder.append("### ⚠️ 进藏必读\n");
        builder.append("- 第一天务必放慢节奏，头痛/恶心请立即沟通\n");
        builder.append("- 白天紫外线强，准备墨镜、遮阳帽、防晒霜\n");
        builder.append("- 尊重当地信仰，进入寺庙请按指引行动\n");
        builder.append("- 行程中如遇封路/限流，导游将第一时间调度备用方案\n");

        return builder.toString();
    }

    private String getStaySuggestion(String budget) {
        return switch (budget) {
            case "经济型" -> "市区连锁酒店 / 干净青旅（含电热毯）";
            case "豪华型" -> "松赞 / 悦榕庄 / 高端度假酒店";
            default -> "藏式精品民宿 / 四星舒适酒店";
        };
    }
}
