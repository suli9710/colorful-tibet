package com.tibet.tourism.modules.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.antibot.AntibotProperties;
import com.tibet.tourism.common.security.antibot.RecaptchaService;
import com.tibet.tourism.modules.ai.application.AiGuideChatService;
import com.tibet.tourism.modules.ai.application.GuideChatUsageService;
import com.tibet.tourism.modules.ai.web.dto.GuideChatRequest;
import com.tibet.tourism.modules.ai.web.dto.GuideChatResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

class AiGuideChatControllerTest {

    @Test
    void anonymousQuotaIdentityIgnoresClientControlledFingerprintRotation() {
        AiGuideChatService guideChatService = mock(AiGuideChatService.class);
        GuideChatUsageService usageService = mock(GuideChatUsageService.class);
        JwtAuthSupport jwtAuthSupport = mock(JwtAuthSupport.class);
        RecaptchaService recaptchaService = mock(RecaptchaService.class);

        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.empty());
        when(usageService.tryAcquire(any(), eq(false)))
                .thenReturn(GuideChatUsageService.Decision.allowed(4, 60));
        when(guideChatService.localOnlyChat(any(), eq("zh")))
                .thenReturn(new GuideChatResponse("local", "local-fallback", true, null, null));

        AiGuideChatController controller = new AiGuideChatController(
                guideChatService,
                usageService,
                jwtAuthSupport,
                recaptchaService,
                new AntibotProperties(),
                false);

        controller.chat(request(), anonymousRequest("device-a"));
        controller.chat(request(), anonymousRequest("device-b"));

        ArgumentCaptor<GuideChatUsageService.ClientIdentity> identityCaptor =
                ArgumentCaptor.forClass(GuideChatUsageService.ClientIdentity.class);
        verify(usageService, org.mockito.Mockito.times(2)).tryAcquire(identityCaptor.capture(), eq(false));

        List<GuideChatUsageService.ClientIdentity> identities = identityCaptor.getAllValues();
        assertThat(identities).hasSize(2);
        assertThat(identities.get(0).authenticated()).isFalse();
        assertThat(identities.get(0).key()).isEqualTo(identities.get(1).key());
        assertThat(identities.get(0).key())
                .contains("ip#")
                .contains(":ua#")
                .doesNotContain("device-a")
                .doesNotContain("device-b");
    }

    private GuideChatRequest request() {
        GuideChatRequest request = new GuideChatRequest();
        request.setMessage("第一次去西藏怎么玩");
        request.setLocale("zh");
        return request;
    }

    private MockHttpServletRequest anonymousRequest(String fingerprint) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/guide/chat");
        request.setRemoteAddr("203.0.113.8");
        request.addHeader("User-Agent", "ColorfulTibetTest/1.0");
        request.addHeader("X-Device-Fingerprint", fingerprint);
        return request;
    }
}
