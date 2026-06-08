package com.tibet.tourism.common.security.antibot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class DeviceFingerprintServiceTest {

    @Mock private ObjectProvider<StringRedisTemplate> redisProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private SetOperations<String, String> setOperations;

    private AntibotProperties properties;
    private DeviceFingerprintService service;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private boolean originalAdditive;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(DeviceFingerprintService.class);
        originalAdditive = logger.isAdditive();
        logger.setAdditive(false);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        properties = new AntibotProperties();
        properties.setEnabled(true);
        properties.getFingerprint().setEnabled(true);

        lenient().when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);

        service = new DeviceFingerprintService(
                redisProvider,
                properties,
                "test-antibot-hmac-secret-for-unit-tests");
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setAdditive(originalAdditive);
        appender.stop();
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

    @Test
    void assessmentFailureLogsSanitizedSummaryWithoutRawFingerprintPayload() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), anyString())).thenThrow(new RuntimeException(
                "redis failed for raw-browser-fingerprint token=fingerprint-secret user=42"));

        int risk = service.assess("raw-browser-fingerprint", 42L);

        assertThat(risk).isZero();
        assertThat(formattedLogMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("type=RuntimeException")
                        .contains("messageHash="));
        assertThat(formattedLogMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("raw-browser-fingerprint");
            assertThat(message).doesNotContain("fingerprint-secret");
            assertThat(message).doesNotContain("user=42");
        });
    }

    private java.util.List<String> formattedLogMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
