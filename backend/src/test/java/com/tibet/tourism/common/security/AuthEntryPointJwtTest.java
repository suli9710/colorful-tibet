package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class AuthEntryPointJwtTest {

    @Test
    void doesNotEchoAuthenticationExceptionMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthEntryPointJwt().commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("user does not exist"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication required");
        assertThat(response.getContentAsString()).doesNotContain("user does not exist");
    }
}
