package com.tibet.tourism.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.sun.net.httpserver.HttpServer;

/**
 * The geo lookup runs on the login request thread and its failures are deliberately not cached, so an
 * unreachable provider would add the full connect+read timeout to every single login, indefinitely.
 */
class IpLocationServiceCircuitBreakerTest {

    private HttpServer server;
    private final AtomicInteger upstreamCalls = new AtomicInteger();

    @BeforeEach
    void startFailingUpstream() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            upstreamCalls.incrementAndGet();
            byte[] body = "{\"error\":true,\"reason\":\"quota exceeded\"}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopUpstream() {
        server.stop(0);
    }

    @Test
    void stopsCallingTheProviderAfterRepeatedFailures() {
        IpLocationService service = new IpLocationService(new TrustedProxyIpResolver());
        ReflectionTestUtils.setField(service, "ipLocationUrlTemplate",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/{ip}/json/");

        // Three consecutive failures open the cooldown...
        for (int i = 0; i < 3; i++) {
            assertThat(service.getCityByIp("203.0.113." + i)).isEqualTo("未知");
        }
        assertThat(upstreamCalls.get()).isEqualTo(3);

        // ...after which the provider is not contacted again, so login stops paying the timeout.
        for (int i = 0; i < 5; i++) {
            assertThat(service.getCityByIp("203.0.113.1" + i)).isEqualTo("未知");
        }
        assertThat(upstreamCalls.get()).isEqualTo(3);
    }

    @Test
    void privateNetworkAddressesNeverReachTheProvider() {
        IpLocationService service = new IpLocationService(new TrustedProxyIpResolver());
        ReflectionTestUtils.setField(service, "ipLocationUrlTemplate",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/{ip}/json/");

        assertThat(service.getCityByIp("10.1.2.3")).isEqualTo("本地网络");
        assertThat(service.getCityByIp("127.0.0.1")).isEqualTo("本地网络");
        assertThat(upstreamCalls.get()).isZero();
    }
}
