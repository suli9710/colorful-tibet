package com.tibet.tourism.modules.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.modules.ai.application.AiQuotaService;
import com.tibet.tourism.modules.ai.application.AiRouteGenerationJobService;
import com.tibet.tourism.modules.ai.application.AiRouteRecordService;
import com.tibet.tourism.modules.ai.application.AiRouteService;
import com.tibet.tourism.modules.ai.web.dto.AiRouteRecordResponse;
import com.tibet.tourism.modules.ai.web.dto.AiRouteRecordSummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private AiRouteGenerationJobService aiRouteGenerationJobService;

    @MockBean
    private AiRouteRecordService aiRouteRecordService;

    @MockBean
    private JwtAuthSupport jwtAuthSupport;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TrustedProxyIpResolver trustedProxyIpResolver;

    @Test
    void latestRunningAiRouteResponseIncludesJobIdForReconnect() throws Exception {
        User user = new User();
        user.setId(42L);

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiRouteRecordService.latestFor(user)).thenReturn(Optional.of(
                aiRouteRecord("job-running", "RUNNING", false)));

        mockMvc.perform(get("/api/routes/ai/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-running"));
    }

    @Test
    void latestCompletedAiRouteResponseOmitsJobId() throws Exception {
        User user = new User();
        user.setId(42L);

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiRouteRecordService.latestFor(user)).thenReturn(Optional.of(
                aiRouteRecord(null, "COMPLETED", false)));

        mockMvc.perform(get("/api/routes/ai/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").doesNotExist());
    }

    @Test
    void savedAiRouteResponsesUseSummaryDtoWithoutContent() throws Exception {
        User user = new User();
        user.setId(42L);

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiRouteRecordService.savedFor(eq(user), any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(aiRouteRecordSummary(true)),
                PageRequest.of(0, 20),
                1));

        mockMvc.perform(get("/api/routes/ai/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").doesNotExist())
                .andExpect(jsonPath("$[0].jobId").doesNotExist())
                .andExpect(jsonPath("$[0].id").value(12L))
                .andExpect(jsonPath("$[0].manuallySaved").value(true));
    }

    @Test
    void savedAiRouteDetailReturnsFullContent() throws Exception {
        User user = new User();
        user.setId(42L);

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiRouteRecordService.savedDetailFor(12L, user)).thenReturn(
                aiRouteRecord(null, "COMPLETED", true));

        mockMvc.perform(get("/api/routes/ai/saved/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12L))
                .andExpect(jsonPath("$.content").value("# Route"));
    }

    @Test
    void generateRouteFailureDoesNotExposeExceptionMessageToResponseOrLogs() throws Exception {
        User user = new User();
        user.setId(42L);
        ListAppender<ILoggingEvent> appender = attachAppender();

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiQuotaService.tryConsumeQuota(42L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
        when(aiRouteService.generateRoute(anyInt(), any(), any(), eq(user), eq("zh")))
                .thenThrow(new RuntimeException("Authorization: Bearer route-secret-token upstream body"));

        try {
            mockMvc.perform(post("/api/routes/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"days\":7,\"budget\":\"comfort\",\"preference\":\"natural\",\"locale\":\"zh\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("AI route generation failed")));
        } finally {
            detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event ->
                assertThat(event.getFormattedMessage()).contains("RuntimeException"));
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains("route-secret-token")
                        || message.contains("Authorization")
                        || message.contains("upstream body"));
    }

    @Test
    void startJobFailureDoesNotExposeExceptionMessageToResponseOrLogs() throws Exception {
        User user = new User();
        user.setId(42L);
        ListAppender<ILoggingEvent> appender = attachAppender();

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiRouteGenerationJobService.startJob(any(), eq(user), eq("zh")))
                .thenThrow(new IllegalStateException("job-secret-token redis://private-cache"));

        try {
            mockMvc.perform(post("/api/routes/generate/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"days\":7,\"budget\":\"comfort\",\"preference\":\"natural\",\"locale\":\"zh\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("AI route generation failed")));
        } finally {
            detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event ->
                assertThat(event.getFormattedMessage()).contains("IllegalStateException"));
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains("job-secret-token")
                        || message.contains("redis://private-cache"));
    }

    @Test
    void streamEndpointReturnsSseEmitterWithoutMessageConverterFailure() throws Exception {
        User user = new User();
        user.setId(42L);

        when(jwtAuthSupport.resolveCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        when(aiQuotaService.tryConsumeQuota(42L)).thenReturn(new AiQuotaService.QuotaConsumptionResult(true, 19));
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

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AiRouteController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(AiRouteController.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private AiRouteRecordResponse aiRouteRecord(String jobId, String status, boolean manuallySaved) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 8, 12, 0);
        return new AiRouteRecordResponse(
                12L,
                jobId,
                "Route",
                "# Route",
                5,
                "comfort",
                "natural",
                "zh",
                status,
                manuallySaved,
                null,
                now,
                now);
    }

    private AiRouteRecordSummaryResponse aiRouteRecordSummary(boolean manuallySaved) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 8, 12, 0);
        return new AiRouteRecordSummaryResponse(
                12L,
                "Route",
                5,
                "comfort",
                "natural",
                "zh",
                "COMPLETED",
                manuallySaved,
                null,
                now,
                now);
    }
}
