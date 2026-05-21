package com.tibet.tourism.modules.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class AiRouteServiceConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(WebClient.Builder.class, WebClient::builder)
            .withBean(AiRouteService.class);

    @Test
    void blankArkPropertiesFallBackToDoubaoProperties() {
        contextRunner
                .withPropertyValues(
                        "ark.api.url=",
                        "ark.api.key=",
                        "ark.api.model=",
                        "ark.api.stream-url=",
                        "doubao.api.url=https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                        "doubao.api.key=doubao-test-key",
                        "doubao.api.model=doubao-test-model",
                        "doubao.api.stream-url=https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                        "ai.route.timeout-seconds=30",
                        "ai.route.stream-timeout-seconds=30")
                .run(context -> {
                    AiRouteService service = context.getBean(AiRouteService.class);

                    assertThat(ReflectionTestUtils.getField(service, "apiUrl"))
                            .isEqualTo("https://ark.cn-beijing.volces.com/api/v3/chat/completions");
                    assertThat(ReflectionTestUtils.getField(service, "apiKey")).isEqualTo("doubao-test-key");
                    assertThat(ReflectionTestUtils.getField(service, "model")).isEqualTo("doubao-test-model");
                    assertThat(ReflectionTestUtils.getField(service, "streamApiUrl"))
                            .isEqualTo("https://ark.cn-beijing.volces.com/api/v3/chat/completions");
                });
    }

    @Test
    void blankStreamUrlDefaultsToMainResponsesEndpoint() {
        contextRunner
                .withPropertyValues(
                        "ark.api.url=https://ark.cn-beijing.volces.com/api/v3/responses",
                        "ark.api.key=ark-test-key",
                        "ark.api.model=ep-20260516173036-4dpgm",
                        "ark.api.stream-url=",
                        "doubao.api.url=https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                        "doubao.api.key=doubao-test-key",
                        "doubao.api.model=doubao-test-model",
                        "doubao.api.stream-url=",
                        "ai.route.timeout-seconds=30",
                        "ai.route.stream-timeout-seconds=30")
                .run(context -> {
                    AiRouteService service = context.getBean(AiRouteService.class);

                    assertThat(ReflectionTestUtils.getField(service, "apiUrl"))
                            .isEqualTo("https://ark.cn-beijing.volces.com/api/v3/responses");
                    assertThat(ReflectionTestUtils.getField(service, "model"))
                            .isEqualTo("ep-20260516173036-4dpgm");
                    assertThat(ReflectionTestUtils.getField(service, "streamApiUrl"))
                            .isEqualTo("https://ark.cn-beijing.volces.com/api/v3/responses");
                });
    }
}
