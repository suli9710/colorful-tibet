package com.tibet.tourism.modules.admin.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.AuthEntryPointJwt;
import com.tibet.tourism.common.security.CsrfCookieFilter;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.common.security.UserDetailsServiceImpl;
import com.tibet.tourism.common.security.UserSessionVersionService;
import com.tibet.tourism.common.security.WebSecurityConfig;
import com.tibet.tourism.modules.admin.application.AdminSecurityPostureService;
import com.tibet.tourism.modules.user.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminSecurityPostureController.class)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        CsrfCookieFilter.class,
        TrustedProxyIpResolver.class
})
class AdminSecurityPostureControllerTest {

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

    @MockitoBean
    private AdminSecurityPostureService securityPostureService;

    @Test
    void adminSecurityPostureEndpointRequiresAuthentication() throws Exception {
        when(securityPostureService.getSecurityPosture()).thenReturn(null);

        mockMvc.perform(get("/api/admin/security-posture"))
                .andExpect(status().isUnauthorized());
    }
}
