package com.tibet.tourism.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

@ExtendWith(MockitoExtension.class)
class CacheConfigSecurityTest {

    @Mock
    private Cache cache;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private boolean originalAdditive;
    private Level originalLevel;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(CacheConfig.class);
        originalAdditive = logger.isAdditive();
        originalLevel = logger.getLevel();
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setAdditive(originalAdditive);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void cacheErrorsLogHashedKeyAndSanitizedExceptionSummary() {
        when(cache.getName()).thenReturn("userSessions");
        CacheErrorHandler handler = new CacheConfig().errorHandler();
        String rawKey = "email=suli@example.com&token=raw-cache-token";
        RuntimeException exception = new RuntimeException(
                "redis connection failed for password=cache-secret");

        handler.handleCacheGetError(exception, cache, rawKey);

        assertThat(formattedLogMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("keyHash=")
                        .contains("type=RuntimeException")
                        .contains("messageHash="));
        assertThat(formattedLogMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain(rawKey);
            assertThat(message).doesNotContain("suli@example.com");
            assertThat(message).doesNotContain("raw-cache-token");
            assertThat(message).doesNotContain("password=cache-secret");
            assertThat(message).doesNotContain(exception.getMessage());
        });
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getThrowableProxy()).isNull());
    }

    private java.util.List<String> formattedLogMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
