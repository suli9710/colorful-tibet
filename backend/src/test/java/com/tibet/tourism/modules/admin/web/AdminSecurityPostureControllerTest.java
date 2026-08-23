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
import com.tibet.tourism.modules.auth.application.AdminMfaPolicy;
import com.tibet.tourism.modules.admin.application.AdminSecurityPostureService;
import com.tibet.tourism.modules.user.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AdminMfaPolicy adminMfaPolicy;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserSessionVersionService userSessionVersionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AdminSecurityPostureService securityPostureService;

    @Test
    void adminSecurityPostureEndpointRequiresAuthentication() throws Exception {
        when(securityPostureService.getSecurityPosture()).thenReturn(null);

        mockMvc.perform(get("/api/admin/security-posture"))
                .andExpect(status().isUnauthorized());
    }
}
