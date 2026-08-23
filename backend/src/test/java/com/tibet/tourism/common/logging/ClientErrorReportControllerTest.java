package com.tibet.tourism.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientErrorReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(OutputCaptureExtension.class)
class ClientErrorReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TrustedProxyIpResolver trustedProxyIpResolver;

    @Test
    void acceptsBoundedReportAndLogsOnlyHashesForFreeFormContent(CapturedOutput output) throws Exception {
        String secretMessage = "render failed with token=browser-secret";

        mockMvc.perform(post("/api/client-errors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "vue",
                                  "name": "TypeError",
                                  "message": "render failed with token=browser-secret",
                                  "stack": "at SecretComponent token=stack-secret",
                                  "info": "render function",
                                  "path": "/routes/42",
                                  "release": "2026.07.31-abc123",
                                  "userAgent": "Test Browser",
                                  "timestamp": "2026-07-31T00:00:00Z"
                                }
                                """))
                .andExpect(status().isAccepted());

        assertThat(output).contains("frontend_error", "source=vue", "path=/routes/42")
                .contains("messageHash=" + PiiMasker.shortHash(secretMessage))
                .doesNotContain("browser-secret", "stack-secret", "SecretComponent");
    }

    @Test
    void rejectsQueryStringsAndUnboundedDiagnosticFields() throws Exception {
        mockMvc.perform(post("/api/client-errors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "vue",
                                  "name": "TypeError",
                                  "message": "x",
                                  "path": "/route?token=secret",
                                  "release": "release",
                                  "timestamp": "2026-07-31T00:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
