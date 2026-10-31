package com.tibet.tourism.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = UploadResourceSecurityHeadersTest.UploadProbeController.class)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        CsrfCookieFilter.class,
        TrustedProxyIpResolver.class,
        UploadResourceSecurityHeadersTest.UploadProbeController.class
})
class UploadResourceSecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private CsrfTokenService csrfTokenService;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private UserSessionVersionService userSessionVersionService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void uploadResourcesSendNosniffHeader() throws Exception {
        mockMvc.perform(get("/uploads/probe.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @RestController
    static class UploadProbeController {

        @GetMapping("/uploads/probe.jpg")
        Map<String, String> uploadProbe() {
            return Map.of("status", "ok");
        }
    }
}
