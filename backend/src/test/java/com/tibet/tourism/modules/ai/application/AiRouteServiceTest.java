package com.tibet.tourism.modules.ai.application;
import com.tibet.tourism.modules.ai.web.dto.AiRouteGenerateResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
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
        assertEquals(Map.of("type", "disabled"), body.get("thinking"));
        assertEquals(2400, body.get("max_tokens"));
        assertEquals(Boolean.TRUE, body.get("stream"));
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
