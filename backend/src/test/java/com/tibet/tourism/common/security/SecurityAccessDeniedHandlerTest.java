package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent.Source;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class SecurityAccessDeniedHandlerTest {

    @Test
    void publishesSanitizedAuditClassificationAndKeepsGeneric403() throws Exception {
        AdminAccessDeniedAuditPublisher publisher = mock(AdminAccessDeniedAuditPublisher.class);
        SecurityAccessDeniedHandler handler = new SecurityAccessDeniedHandler(publisher);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Bearer denied-secret"));

        verify(publisher).publish(request, Source.SECURITY_FILTER);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("Forbidden", "Access denied")
                .doesNotContain("denied-secret", "query-secret", "header-secret");
    }

    @Test
    void auditFailureCannotReplaceForbiddenResponse() throws Exception {
        AdminAccessDeniedAuditPublisher publisher = mock(AdminAccessDeniedAuditPublisher.class);
        MockHttpServletRequest request = request();
        doThrow(new IllegalStateException("request-body-secret"))
                .when(publisher).publish(request, Source.SECURITY_FILTER);
        SecurityAccessDeniedHandler handler = new SecurityAccessDeniedHandler(publisher);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).doesNotContain("request-body-secret");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.setServletPath("/api/admin/users");
        request.setQueryString("token=query-secret");
        request.addHeader("Authorization", "Bearer header-secret");
        return request;
    }
}
