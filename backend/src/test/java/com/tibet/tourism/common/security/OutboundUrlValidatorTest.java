package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OutboundUrlValidatorTest {

    private final OutboundUrlValidator validator = new OutboundUrlValidator();

    @Test
    void allowsPublicHttpsEndpoint() {
        assertThatCode(() -> validator.validateHttpsUrl(
                "ark.api.url",
                "https://ark.cn-beijing.volces.com/api/v3/responses"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonHttpsExternalEndpoint() {
        assertThatThrownBy(() -> validator.validateHttpsUrl("ark.api.url", "http://example.com/api"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must use HTTPS");
    }

    @Test
    void rejectsUserInfoInEndpoint() {
        assertThatThrownBy(() -> validator.validateHttpsUrl("ark.api.url", "https://token@example.com/api"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a valid service URL");
    }

    @Test
    void rejectsMetadataAddress() {
        assertThatThrownBy(() -> validator.validateHttpsUrl("ark.api.url", "https://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    void rejectsLoopbackAddressForExternalEndpoint() {
        assertThatThrownBy(() -> validator.validateHttpsUrl("ark.api.url", "https://127.0.0.1:8080/api"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    void allowsExplicitLocalServiceHostForInternalService() {
        assertThatCode(() -> validator.validateHttpOrHttpsServiceUrl(
                "scrapling.service.url",
                "http://scrapling:8000",
                Set.of("scrapling")))
                .doesNotThrowAnyException();
    }
}
