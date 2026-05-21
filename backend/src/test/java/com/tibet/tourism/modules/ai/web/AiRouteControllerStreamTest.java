package com.tibet.tourism.modules.ai.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.modules.ai.application.AiQuotaService;
import com.tibet.tourism.modules.ai.application.AiRouteService;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(controllers = AiRouteController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiRouteControllerStreamTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiRouteService aiRouteService;

    @MockBean
    private AiQuotaService aiQuotaService;

    @MockBean
    private JwtAuthSupport jwtAuthSupport;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TrustedProxyIpResolver trustedProxyIpResolver;

    @Test
    void streamEndpointReturnsSseEmitterWithoutMessageConverterFailure() throws Exception {
        User user = new User();
        user.setId(42L);

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiQuotaService.isQuotaExceeded(42L)).thenReturn(false);
        doAnswer(invocation -> {
            SseEmitter emitter = invocation.getArgument(5);
            emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
            emitter.complete();
            return null;
        }).when(aiRouteService).streamRoute(
                anyInt(),
                eq("comfort"),
                eq("natural"),
                eq(user),
                eq("zh"),
                any(SseEmitter.class));

        MvcResult result = mockMvc.perform(post("/api/routes/generate/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"days\":7,\"budget\":\"comfort\",\"preference\":\"natural\",\"locale\":\"zh\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("\"type\":\"done\"")));
    }
}
