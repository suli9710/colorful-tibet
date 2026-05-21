package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.tibet.tourism.modules.ai.web.dto.GuideChatRequest;
import com.tibet.tourism.modules.ai.web.dto.GuideChatResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class AiGuideChatServiceTest {

    @Test
    void chatUsesFallbackWhenAiConfigContainsPlaceholders() {
        AtomicBoolean upstreamCalled = new AtomicBoolean(false);
        AiGuideChatService service = new AiGuideChatService(
                WebClient.builder().exchangeFunction(request -> {
                    upstreamCalled.set(true);
                    return Mono.error(new AssertionError("placeholder AI config should not call upstream"));
                }),
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "your-doubao-api-key",
                "your-model-name",
                10
        );
        GuideChatRequest request = new GuideChatRequest();
        request.setMessage("帮我规划一条西藏路线");

        GuideChatResponse response = service.chat(request, "zh");

        assertThat(upstreamCalled).isFalse();
        assertThat(response.isFallback()).isTrue();
        assertThat(response.getModel()).isEqualTo("local-fallback");
        assertThat(response.getAction()).isEqualTo("navigate");
        assertThat(response.getContent()).contains("天数");
    }

    @Test
    void chatExtractsTextFromChatCompletionsResponse() {
        AiGuideChatService service = new AiGuideChatService(
                WebClient.builder().exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"choices\":[{\"message\":{\"content\":\"建议先在拉萨适应一天，再去羊卓雍措。\"}}]}")
                                .build()
                )),
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "test-api-key",
                "test-model",
                10
        );
        GuideChatRequest request = new GuideChatRequest();
        request.setMessage("第一次去西藏怎么玩");

        GuideChatResponse response = service.chat(request, "zh");

        assertThat(response.isFallback()).isFalse();
        assertThat(response.getModel()).isEqualTo("test-model");
        assertThat(response.getContent()).contains("拉萨").contains("羊卓雍措");
    }

    @Test
    void chatFallsBackWhenUpstreamReturnsError() {
        AiGuideChatService service = new AiGuideChatService(
                WebClient.builder().exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.BAD_GATEWAY)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"error\":\"bad gateway\"}")
                                .build()
                )),
                "https://ark.cn-beijing.volces.com/api/v3/responses",
                "test-api-key",
                "test-model",
                10
        );
        GuideChatRequest request = new GuideChatRequest();
        request.setMessage("高反严重吗");

        GuideChatResponse response = service.chat(request, "zh");

        assertThat(response.isFallback()).isTrue();
        assertThat(response.getContent()).contains("高原反应");
    }

    @Test
    void chatRejectsOffTopicMessageBeforeCallingUpstream() {
        AtomicBoolean upstreamCalled = new AtomicBoolean(false);
        AiGuideChatService service = new AiGuideChatService(
                WebClient.builder().exchangeFunction(request -> {
                    upstreamCalled.set(true);
                    return Mono.error(new AssertionError("off-topic guide chat should not call upstream"));
                }),
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "test-api-key",
                "test-model",
                10
        );
        GuideChatRequest request = new GuideChatRequest();
        request.setMessage("帮我写一段股票交易策略");

        GuideChatResponse response = service.chat(request, "zh");

        assertThat(upstreamCalled).isFalse();
        assertThat(response.isFallback()).isTrue();
        assertThat(response.getModel()).isEqualTo("local-guardrail");
        assertThat(response.getContent()).contains("西藏旅行").contains("藏地");
    }
}
