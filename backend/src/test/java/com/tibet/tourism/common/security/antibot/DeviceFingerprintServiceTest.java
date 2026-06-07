package com.tibet.tourism.common.security.antibot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class DeviceFingerprintServiceTest {

    @Mock private ObjectProvider<StringRedisTemplate> redisProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private SetOperations<String, String> setOperations;

    private AntibotProperties properties;
    private DeviceFingerprintService service;

    @BeforeEach
    void setUp() {
        properties = new AntibotProperties();
        properties.setEnabled(true);
        properties.getFingerprint().setEnabled(true);

        lenient().when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);

        service = new DeviceFingerprintService(
                redisProvider,
                properties,
                "test-antibot-hmac-secret-for-unit-tests");
    }

    @Test
    void assessUsesHmacLabelsForRedisKeysAndValues() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.expire(anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(Boolean.TRUE);
        when(setOperations.members(anyString())).thenReturn(Set.of("one"));

        service.assess("raw-browser-fingerprint", 42L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(setOperations, times(2)).add(keyCaptor.capture(), valueCaptor.capture());

        assertThat(keyCaptor.getAllValues())
                .allSatisfy(key -> assertThat(key)
                        .doesNotContain("raw-browser-fingerprint")
                        .doesNotContain("42"));
        assertThat(valueCaptor.getAllValues())
                .allSatisfy(value -> assertThat(value)
                        .doesNotContain("raw-browser-fingerprint")
                        .doesNotContain("42"));
    }

    @Test
    void labelsAreStableAndDoNotContainRawInputs() {
        String first = service.fingerprintLabel("raw-browser-fingerprint");
        String second = service.fingerprintLabel("raw-browser-fingerprint");
        String userLabel = service.userLabel(42L);

        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("fp#").doesNotContain("raw-browser-fingerprint");
        assertThat(userLabel).startsWith("user#").doesNotContain("42");
    }
}
