package com.tibet.tourism.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class MustChangePasswordFilterTest {

    private UserRepository userRepository;
    private MustChangePasswordFilter filter;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        filter = new MustChangePasswordFilter(userRepository);
        authenticate("admin");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksProtectedApiWhenPasswordChangeRequired() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(true)));

        MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/admin/users"));

        assertEquals(403, response.getStatus());
        assertEquals("{\"error\":\"Password change required\",\"code\":\"PASSWORD_CHANGE_REQUIRED\"}",
                response.getContentAsString());
    }

    @Test
    void allowsChangePasswordEndpointWhenPasswordChangeRequired() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(true)));

        MockHttpServletResponse response = doFilter(apiRequest("POST", "/api/auth/me/change-password"));

        assertEquals(200, response.getStatus());
    }

    @Test
    void allowsProtectedApiWhenPasswordChangeNotRequired() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(false)));

        MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/admin/users"));

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletRequest apiRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private MockHttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    private User user(boolean mustChangePassword) {
        User user = new User();
        user.setUsername("admin");
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
