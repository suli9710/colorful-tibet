package com.tibet.tourism.common.security.antibot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.antibot.infra.BehaviorLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BehaviorLogRetentionServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-31T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void purgesLogsOlderThanConfiguredRetentionWindow() {
        BehaviorLogRepository repository = mock(BehaviorLogRepository.class);
        AntibotProperties properties = new AntibotProperties();
        properties.getRetention().setDays(90);
        LocalDateTime expectedCutoff = LocalDateTime.of(2026, 5, 2, 0, 0);
        when(repository.deleteCreatedBefore(expectedCutoff)).thenReturn(7);

        BehaviorLogRetentionService service =
                new BehaviorLogRetentionService(repository, properties, FIXED_CLOCK);

        assertThat(service.purgeExpiredLogs()).isEqualTo(7);
        verify(repository).deleteCreatedBefore(expectedCutoff);
    }

    @Test
    void skipsPurgeWhenRetentionCleanupIsDisabled() {
        BehaviorLogRepository repository = mock(BehaviorLogRepository.class);
        AntibotProperties properties = new AntibotProperties();
        properties.getRetention().setEnabled(false);

        BehaviorLogRetentionService service =
                new BehaviorLogRetentionService(repository, properties, FIXED_CLOCK);

        assertThat(service.purgeExpiredLogs()).isZero();
        verify(repository, never()).deleteCreatedBefore(org.mockito.ArgumentMatchers.any());
    }
}
