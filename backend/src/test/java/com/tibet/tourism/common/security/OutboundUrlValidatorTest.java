package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;

class OutboundUrlValidatorTest {

    private final OutboundUrlValidator validator = new OutboundUrlValidator();

    @Test
    void allowsPublicHttpsEndpoint() throws Exception {
        InetAddress publicAddress = InetAddress.getByAddress(new byte[] {8, 8, 8, 8});
        try (MockedStatic<InetAddress> dns = Mockito.mockStatic(InetAddress.class, Mockito.CALLS_REAL_METHODS)) {
            dns.when(() -> InetAddress.getAllByName("ark.cn-beijing.volces.com"))
                    .thenReturn(new InetAddress[] {publicAddress});
            assertThatCode(() -> validator.validateHttpsUrl(
                    "ark.api.url", "https://ark.cn-beijing.volces.com/api/v3/responses"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsMixedPublicAndPrivateDnsAnswers() throws Exception {
        InetAddress publicAddress = InetAddress.getByAddress(new byte[] {8, 8, 8, 8});
        InetAddress privateAddress = InetAddress.getByAddress(new byte[] {10, 0, 0, 1});
        try (MockedStatic<InetAddress> dns = Mockito.mockStatic(InetAddress.class, Mockito.CALLS_REAL_METHODS)) {
            dns.when(() -> InetAddress.getAllByName("mixed.example"))
                    .thenReturn(new InetAddress[] {publicAddress, privateAddress});
            assertThatThrownBy(() -> validator.validateHttpsUrl("ark.api.url", "https://mixed.example/api"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("blocked network address");
        }
    }

    @Test
    void failsClosedWhenDnsResolutionFails() {
        try (MockedStatic<InetAddress> dns = Mockito.mockStatic(InetAddress.class, Mockito.CALLS_REAL_METHODS)) {
            dns.when(() -> InetAddress.getAllByName("unresolved.example"))
                    .thenThrow(new UnknownHostException("deterministic DNS failure"));
            assertThatThrownBy(() -> validator.validateHttpsUrl("ark.api.url", "https://unresolved.example/api"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("could not be resolved safely")
                    .hasCauseInstanceOf(UnknownHostException.class);
        }
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
