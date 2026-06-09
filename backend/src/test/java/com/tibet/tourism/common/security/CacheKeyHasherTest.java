package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CacheKeyHasherTest {

    @Test
    void cacheKeyIsDeterministicOpaqueAndNamespaceScoped() {
        CacheKeyHasher hasher = new CacheKeyHasher("cache-key-hmac-secret-for-tests");

        String first = hasher.cacheKey("user", "42");
        String repeat = hasher.cacheKey("user", "42");
        String otherNamespace = hasher.cacheKey("quota", "42");

        assertThat(first).isEqualTo(repeat);
        assertThat(first).startsWith("user#").doesNotContain("42");
        assertThat(first).isNotEqualTo(otherNamespace);
    }

    @Test
    void emptyValuesUseStableNamespaceOnlyLabel() {
        CacheKeyHasher hasher = new CacheKeyHasher("cache-key-hmac-secret-for-tests");

        assertThat(hasher.cacheKey(" user ", "  "))
                .isEqualTo("user#empty");
        assertThat(hasher.cacheKey("", null))
                .isEqualTo("cache#empty");
    }

    @Test
    void differentSecretsProduceDifferentLabels() {
        String first = new CacheKeyHasher("cache-key-hmac-secret-for-tests-a")
                .cacheKey("ip-location", "192.168.1.25");
        String second = new CacheKeyHasher("cache-key-hmac-secret-for-tests-b")
                .cacheKey("ip-location", "192.168.1.25");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("192.168.1.25");
        assertThat(second).doesNotContain("192.168.1.25");
    }
}
