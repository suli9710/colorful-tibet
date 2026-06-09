package com.tibet.tourism.modules.ai.application;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class AiRouteServiceTest {

    @Test
    void generateRouteUsesFallbackWhenAiConfigContainsPlaceholders() {
        AtomicBoolean upstreamCalled = new AtomicBoolean(false);
        AiRouteService service = new AiRouteService(
                WebClient.builder().exchangeFunction(request -> {
                    upstreamCalled.set(true);
                    return Mono.error(new AssertionError("placeholder AI config should not call upstream"));
                }),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "your-doubao-api-key",
                "your-model-name",
                30,
                "",
                30
        );

        AiRouteGenerateResponse response = service.generateRoute(3, "comfort", "natural", null, "zh");

        assertFalse(upstreamCalled.get());
        assertEquals(3, response.getDays());
        assertFalse(response.getContent().contains("AI服务暂时不可用"));
        assertTrue(response.getContent().contains("基准路线"));
        assertTrue(response.getContent().contains("## 每日行程"));
        assertTrue(response.getContent().contains("## 安全与执行校验"));
        assertTrue(response.getContent().contains("医疗免责声明"));
    }

    @Test
    void generateRouteUsesFallbackWhenUpstreamReturnsError() {
        AiRouteService service = new AiRouteService(
                WebClient.builder().exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.BAD_GATEWAY)
                                .body("{\"error\":\"bad gateway\"}")
                                .build()
                )),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "test-model",
                30,
                "",
                30
        );

        AiRouteGenerateResponse response = service.generateRoute(5, "economy", "cultural", null, "zh");

        assertEquals(5, response.getDays());
        assertFalse(response.getContent().contains("AI服务暂时不可用"));
        assertTrue(response.getContent().contains("基准路线"));
        assertTrue(response.getContent().contains("经济型"));
    }

    @Test
    void streamRouteEmitsFallbackInsteadOfErrorWhenAiConfigIsMissing() throws Exception {
        AtomicBoolean upstreamCalled = new AtomicBoolean(false);
        AiRouteService service = new AiRouteService(
                WebClient.builder().exchangeFunction(request -> {
                    upstreamCalled.set(true);
                    return Mono.error(new AssertionError("missing AI config should not call upstream"));
                }),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "",
                "test-model",
                30,
                "",
                30
        );
        CapturingSseEmitter emitter = new CapturingSseEmitter();

        service.streamRoute(4, "comfort", "natural", null, "zh", emitter);

        assertTrue(emitter.awaitCompletion());
        assertFalse(upstreamCalled.get());
        assertTrue(emitter.payloads().stream().anyMatch(payload ->
                payload.contains("\"type\":\"delta\"") && payload.contains("基准路线")));
        assertTrue(emitter.payloads().stream().anyMatch(payload -> payload.contains("\"type\":\"done\"")));
        assertFalse(emitter.payloads().stream().anyMatch(payload -> payload.contains("\"type\":\"error\"")));
    }

    @Test
    void streamRequestBodyUsesChatCompletionsShapeForChatEndpoint() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "test-model",
                30,
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                30
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                service,
                "buildStreamRequestBody",
                "请生成路线"
        );

        assertNotNull(body);
        assertTrue(body.containsKey("messages"));
        assertFalse(body.containsKey("input"));
        assertFalse(body.containsKey("thinking"));
        assertFalse(body.containsKey("reasoning_effort"));
        assertEquals(16200, body.get("max_tokens"));
        assertEquals(Boolean.TRUE, body.get("stream"));
    }

    @Test
    void streamRequestBodyUsesResponsesShapeForResponsesEndpoint() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                service,
                "buildStreamRequestBody",
                "Generate a route"
        );

        assertNotNull(body);
        assertTrue(body.containsKey("input"));
        assertFalse(body.containsKey("messages"));
        assertFalse(body.containsKey("thinking"));
        assertFalse(body.containsKey("reasoning_effort"));
        assertEquals(16200, body.get("max_output_tokens"));
        assertEquals(Boolean.TRUE, body.get("stream"));
        assertEquals("ep-20260516173036-4dpgm", body.get("model"));
    }

    @Test
    void validatesRequestedDayCountBeforeAcceptingRouteContent() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );
        String incomplete = """
                # 西藏15天路线
                ## 路线概览
                这是一条包含预算、住宿和贴心提示的路线。
                ## 每日行程
                ### 第1天：拉萨
                - 上午：布达拉宫。
                - 下午：八廓街。
                - 晚上/住宿：拉萨。
                ### 第2天：拉萨
                - 上午：大昭寺。
                - 下午：色拉寺。
                - 晚上/住宿：拉萨。
                ### 第3天：羊湖
                - 上午：前往羊卓雍措。
                - 下午：返回拉萨。
                - 晚上/住宿：拉萨。
                ## 进藏必读
                - 预算和高原适应提示。
                """;

        String normalized = ReflectionTestUtils.invokeMethod(
                service,
                "normalizeMarkdownRoute",
                incomplete,
                15,
                "舒适型",
                "自然风光",
                "zh"
        );

        assertNotNull(normalized);
        assertTrue(normalized.contains("15"));
        assertTrue(normalized.length() > incomplete.length());
    }

    @Test
    void removesRepeatedRouteRestartBeforeValidatingDayCount() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );
        String duplicated = completeChineseRoute(4) + "\n\n，" + completeChineseRoute(4);

        String normalized = ReflectionTestUtils.invokeMethod(
                service,
                "normalizeMarkdownRoute",
                duplicated,
                4,
                "豪华型",
                "自然风光",
                "zh"
        );

        assertNotNull(normalized);
        assertFalse(normalized.contains("以下为结合经典线路、海拔节奏和预算约束生成的基准路线"));
        assertEquals(1, countOccurrences(normalized, "# 林芝寻踪：4天光影路线"));
        assertEquals(1, countOccurrences(normalized, "### 第1天："));
        assertEquals(1, countOccurrences(normalized, "### 第4天："));
        assertEquals(1, countOccurrences(normalized, "## 安全与执行校验"));
        assertTrue(normalized.contains("每天为4-8条可执行安排"));
    }

    @Test
    void replacesExistingSafetySectionWithStructuredGuardrail() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );
        String routeWithLooseSafety = completeChineseRoute(3).replace(
                "## 进藏必读",
                "## 安全与执行校验\n- 自由发挥，身体不适就休息。\n\n## 进藏必读"
        );

        String normalized = ReflectionTestUtils.invokeMethod(
                service,
                "normalizeMarkdownRoute",
                routeWithLooseSafety,
                3,
                "豪华型",
                "自然风光",
                "zh"
        );

        assertNotNull(normalized);
        assertEquals(1, countOccurrences(normalized, "## 安全与执行校验"));
        assertFalse(normalized.contains("自由发挥"));
        assertTrue(normalized.contains("路线完整性"));
        assertTrue(normalized.contains("医疗免责声明"));
    }

    @Test
    void fallsBackWhenDayOneStartsWithHighAltitudeDestination() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );
        String unsafe = completeChineseRoute(3).replace(
                "### 第1天：拉萨 — 林芝方向体验",
                "### 第1天：羊卓雍措 — 高海拔冲刺"
        );

        String normalized = ReflectionTestUtils.invokeMethod(
                service,
                "normalizeMarkdownRoute",
                unsafe,
                3,
                "豪华型",
                "自然风光",
                "zh"
        );

        assertNotNull(normalized);
        assertTrue(normalized.contains("基准路线"));
        assertTrue(normalized.contains("第1天：拉萨"));
        assertTrue(normalized.contains("## 安全与执行校验"));
    }

    @Test
    void fallsBackWhenDailyItemCountIsNotExecutable() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );

        String normalized = ReflectionTestUtils.invokeMethod(
                service,
                "normalizeMarkdownRoute",
                sparseDailyRoute(3),
                3,
                "豪华型",
                "自然风光",
                "zh"
        );

        assertNotNull(normalized);
        assertTrue(normalized.contains("基准路线"));
        assertTrue(normalized.contains("每日项目过少/过多"));
    }

    @Test
    void fallsBackWhenUniqueDailySectionsDoNotMatchRequestedDays() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );

        String normalized = ReflectionTestUtils.invokeMethod(
                service,
                "normalizeMarkdownRoute",
                completeChineseRoute(4),
                5,
                "豪华型",
                "自然风光",
                "zh"
        );

        assertNotNull(normalized);
        assertTrue(normalized.contains("基准路线"));
        assertTrue(normalized.contains("5天"));
    }

    @Test
    void streamEventDoesNotForwardReasoningDelta() throws Exception {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        AtomicInteger lineCounter = new AtomicInteger();
        AtomicInteger deltaCounter = new AtomicInteger();

        ReflectionTestUtils.invokeMethod(
                service,
                "processStreamEvent",
                "{\"type\":\"response.reasoning.delta\",\"delta\":\"hidden chain\"}",
                emitter,
                lineCounter,
                deltaCounter
        );
        ReflectionTestUtils.invokeMethod(
                service,
                "processStreamEvent",
                "{\"type\":\"response.output_text.delta\",\"delta\":\"visible route\"}",
                emitter,
                lineCounter,
                deltaCounter
        );

        assertTrue(emitter.payloads().stream().anyMatch(payload -> payload.contains("visible route")));
        assertFalse(emitter.payloads().stream().anyMatch(payload -> payload.contains("hidden chain")));
    }

    @Test
    void extractsTextFromResponsesOutputContent() {
        AiRouteService service = new AiRouteService(
                WebClient.builder(),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "ep-20260516173036-4dpgm",
                30,
                "",
                30
        );
        Map<String, Object> response = Map.of(
                "output", List.of(
                        Map.of(
                                "type", "reasoning",
                                "summary", "# Hidden route"
                        ),
                        Map.of(
                                "type", "message",
                                "content", List.of(Map.of(
                                        "type", "output_text",
                                        "text", "# Test route"
                                ))
                        )
                )
        );

        String text = ReflectionTestUtils.invokeMethod(service, "extractResponseText", response);

        assertEquals("# Test route", text);
    }

    private static String completeChineseRoute(int days) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 林芝寻踪：").append(days).append("天光影路线\n\n");
        builder.append("## 路线概览\n");
        builder.append("这是一条兼顾预算、住宿、车程和贴心提示的路线，先在拉萨适应，再顺路进入林芝方向。\n\n");
        builder.append("## 行程亮点\n");
        builder.append("- 巴松措湖心岛晨雾和湖面金光\n");
        builder.append("- 鲁朗林海与藏式村落慢游\n");
        builder.append("- 雅鲁藏布大峡谷与南迦巴瓦观景\n\n");
        builder.append("## 每日行程\n\n");
        for (int day = 1; day <= days; day++) {
            builder.append("### 第").append(day).append("天：拉萨 — 林芝方向体验\n");
            builder.append("- **清晨/上午**：8:00 出发，游玩真实景点约2小时，专车转场，注意慢走适应高原。\n");
            builder.append("- **午餐/转场**：安排藏餐或石锅鸡，人均150元，车程约2小时，路况以高速和柏油路为主。\n");
            builder.append("- **下午**：深入湖泊、林海或峡谷景观，预留拍照时间，听一段在地文化故事。\n");
            builder.append("- **傍晚**：选择观景台等日落，天气给面子就把相机留给金光。\n");
            builder.append("- **晚上/住宿**：入住景区或镇区供氧酒店，价格约1200-1800元/晚。\n");
            builder.append("- **在地彩蛋**：甜茶馆、藏式手作或小众机位，控制体力不赶场。\n");
            builder.append("- **当日理由**：顺着同一地理方向推进，兼顾自然风光、高原适应和路线效率。\n");
            builder.append("- **贴心提示**：海拔约3000-3700米，带身份证、防晒、保暖衣物和便携氧气。\n\n");
        }
        builder.append("## 预算预估\n");
        builder.append("- 按豪华型标准，交通、住宿餐饮、门票体验合计人均约8000-12000元。\n\n");
        builder.append("## 进藏必读\n");
        builder.append("- 第一天不要洗澡和奔跑，注意高原反应、边防证、穿衣防晒、通讯现金和尊重风俗。\n");
        return builder.toString();
    }

    private static String sparseDailyRoute(int days) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 西藏轻量路线\n\n");
        builder.append("## 路线概览\n");
        builder.append("这是一条包含预算、住宿、车程和贴心提示的路线，先在拉萨适应。\n\n");
        builder.append("## 行程亮点\n");
        builder.append("- 拉萨慢适应\n");
        builder.append("- 林芝自然风光\n");
        builder.append("- 藏地文化体验\n\n");
        builder.append("## 每日行程\n\n");
        for (int day = 1; day <= days; day++) {
            builder.append("### 第").append(day).append("天：拉萨 — 林芝方向体验\n");
            builder.append("- **上午**：拉萨低强度适应，慢走补水。\n");
            builder.append("- **下午**：顺路安排真实景点，控制体力。\n");
            builder.append("- **晚上/住宿**：入住供氧酒店，预算清楚。\n\n");
        }
        builder.append("## 预算预估\n");
        builder.append("- 交通、住宿餐饮、门票体验合计人均约8000-12000元。\n\n");
        builder.append("## 进藏必读\n");
        builder.append("- 注意高原反应、边防证、穿衣防晒、通讯现金和尊重风俗。\n");
        return builder.toString();
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static final class CapturingSseEmitter extends SseEmitter {
        private final List<String> payloads = new CopyOnWriteArrayList<>();
        private final CountDownLatch completed = new CountDownLatch(1);

        private CapturingSseEmitter() {
            super(1000L);
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            @SuppressWarnings("unchecked")
            Set<Object> dataItems = (Set<Object>) ReflectionTestUtils.invokeMethod(builder, "build");
            if (dataItems == null) {
                return;
            }
            for (Object item : dataItems) {
                Object data = ReflectionTestUtils.getField(item, "data");
                if (data != null) {
                    payloads.add(data.toString());
                }
            }
        }

        @Override
        public void complete() {
            completed.countDown();
        }

        private boolean awaitCompletion() throws InterruptedException {
            return completed.await(1, TimeUnit.SECONDS);
        }

        private List<String> payloads() {
            return payloads;
        }
    }
}
