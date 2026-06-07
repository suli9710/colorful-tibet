package com.tibet.tourism.modules.spot.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PriceScheduledServiceTest {

    @Mock
    private PriceUpdateService priceUpdateService;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PriceScheduledService service;

    @BeforeEach
    void setUp() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        service = new PriceScheduledService(priceUpdateService, redisTemplateProvider);
        ReflectionTestUtils.setField(service, "priceUpdateEnabled", true);
        ReflectionTestUtils.setField(service, "forceUpdate", false);
        ReflectionTestUtils.setField(service, "lockEnabled", true);
        ReflectionTestUtils.setField(service, "lockTtlSeconds", 60L);
    }

    @Test
    void scheduledJobSkipsWhenDistributedLockIsHeld() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("price-update:scheduled:daily"), anyString(), any(Duration.class)))
                .thenReturn(false);

        service.scheduledPriceUpdate();

        verify(priceUpdateService, never()).batchUpdatePrices(anyBoolean());
    }

    @Test
    void scheduledJobRunsWhenLockingIsDisabled() {
        ReflectionTestUtils.setField(service, "lockEnabled", false);
        when(priceUpdateService.batchUpdatePrices(false))
                .thenReturn(new PriceUpdateService.BatchUpdateResult(1, 0, 0, 1));

        service.scheduledPriceUpdate();

        verify(priceUpdateService).batchUpdatePrices(false);
    }
}
