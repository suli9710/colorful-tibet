package com.tibet.tourism.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = IpLocationServiceCacheKeyPrivacyTest.TestConfig.class)
class IpLocationServiceCacheKeyPrivacyTest {

    @jakarta.annotation.Resource
    private IpLocationService ipLocationService;

    @jakarta.annotation.Resource
    private CapturingCache ipLocationCache;

    @Test
    void springCacheKeyUsesHmacLabelForIpAddress() {
        String firstIp = "192.168.11.27";
        String secondIp = "192.168.11.28";

        ipLocationService.getCityByIp(firstIp);
        ipLocationService.getCityByIp(secondIp);

        List<String> capturedKeys = ipLocationCache.capturedKeys();

        assertThat(capturedKeys).hasSize(2);
        assertThat(capturedKeys).allSatisfy(key -> assertThat(key)
                .startsWith("ip-location#")
                .doesNotContain(firstIp)
                .doesNotContain(secondIp));
        assertThat(capturedKeys.get(0)).isNotEqualTo(capturedKeys.get(1));
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheKeyHasher cacheKeyHasher() {
            return new CacheKeyHasher("test-cache-key-hmac-secret");
        }

        @Bean
        TrustedProxyIpResolver trustedProxyIpResolver() {
            return new TrustedProxyIpResolver();
        }

        @Bean
        IpLocationService ipLocationService(TrustedProxyIpResolver trustedProxyIpResolver) {
            return new IpLocationService(trustedProxyIpResolver);
        }

        @Bean
        CapturingCache ipLocationCache() {
            return new CapturingCache("ipLocationCache");
        }

        @Bean
        CacheManager cacheManager(CapturingCache ipLocationCache) {
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            cacheManager.setCaches(List.of(ipLocationCache));
            return cacheManager;
        }
    }

    static class CapturingCache extends ConcurrentMapCache {

        private final List<String> capturedKeys = new CopyOnWriteArrayList<>();

        CapturingCache(String name) {
            super(name, new ConcurrentHashMap<>(), false);
        }

        @Override
        protected Object lookup(Object key) {
            capturedKeys.add(String.valueOf(key));
            return super.lookup(key);
        }

        List<String> capturedKeys() {
            return capturedKeys;
        }
    }
}
