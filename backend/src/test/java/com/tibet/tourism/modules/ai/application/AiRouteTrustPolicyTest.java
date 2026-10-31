package com.tibet.tourism.modules.ai.application;
import com.tibet.tourism.modules.route.application.ItineraryTrustPolicy;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class AiRouteTrustPolicyTest {
    static IntStream supportedDays() { return IntStream.rangeClosed(1,15); }
    private AiRouteService service() {
        return new AiRouteService(WebClient.builder().exchangeFunction(r -> Mono.error(new AssertionError("No upstream allowed"))),
                "https://example.org/api", "", "", 30, "", 30);
    }
    @ParameterizedTest @MethodSource("supportedDays")
    void fallbackForEverySupportedLengthIsConservativeDraft(int days) {
        var response=service().generateRoute(days,"comfort","natural",null,"zh");
        assertEquals(days,response.getDays());
        assertFalse(ItineraryTrustPolicy.violatesOvernightPolicy(response.getContent()));
        assertFalse(response.getContent().contains("纳木错湖边客栈"));
        assertFalse(response.getContent().contains("大本营帐篷"));
        assertEquals("NEEDS_CONFIRMATION",response.getTrustStatus());
        assertTrue(response.getContent().contains("未核验"));
        assertFalse(response.getContent().contains("条可执行安排"));
        assertEquals(ItineraryTrustPolicy.VERSION,response.getRuleVersion());
    }
    @ParameterizedTest @ValueSource(strings={"zh","bo"})
    void cachedKnownHighOvernightIsRejectedRegardlessOfLanguage(String locale) {
        assertEquals("",service().normalizeCachedRoute("# 路线\n## 每日行程\n- 住宿：5200米帐篷",1,"comfort","natural",locale));
    }
}
